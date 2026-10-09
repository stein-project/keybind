/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Breno Xavier
 */

package dev.xavier.stein.keybind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.lwjgl.input.Keyboard;

import dev.xavier.stein.loader.api.Chat;
import dev.xavier.stein.loader.api.Entities;
import dev.xavier.stein.loader.api.Keys;
import dev.xavier.stein.loader.api.Lang;
import dev.xavier.stein.loader.api.Log;
import dev.xavier.stein.loader.api.ModConfig;
import dev.xavier.stein.loader.api.ModContext;
import dev.xavier.stein.loader.api.Option;
import dev.xavier.stein.loader.api.Page;
import dev.xavier.stein.loader.api.Screens;
import dev.xavier.stein.loader.api.SteinMod;

/**
 * Mensagens e comandos numa tecla: aperta F, o jogo manda "/home". Cada bind tem uma tecla, um modificador opcional
 * (Ctrl, Shift ou Alt) e um texto; o texto pode ter varias mensagens separadas por "|", que saem uma depois da outra.
 *
 * <p>Tudo sai pelo {@link Chat#send}, o mesmo caminho de quem digita no chat: o servidor ve uma mensagem normal. As
 * mensagens saem em fila, com um intervalo entre elas (o padrao e meio segundo), para o servidor nao expulsar por
 * spam; e a fila tem teto, entao apertar a tecla vinte vezes nao vira vinte rodadas.
 *
 * <p>So dispara com o jogo aberto, sem tela na frente: com o chat ou o Painel abertos a tecla e do campo de texto. E
 * so no aperto — segurar nao repete.
 */
public final class SteinKeybind implements SteinMod {

    /** Quantos binds cabem na pagina (o Painel monta as linhas de todos e mostra as que estao em uso). */
    static final int MAX = 20;
    /** Teto da fila de mensagens: o que passar disso e descartado. */
    static final int MAX_QUEUE = 10;
    /** Modificadores, na ordem do {@link Bind#modifier}. */
    static final String[] MODIFIERS = {"none", "ctrl", "shift", "alt"};

    /** Um bind: tecla (codigo do LWJGL 2, negativo = botao do mouse), modificador e o texto a mandar. */
    static final class Bind {
        int key;
        int modifier;
        String text = "";
    }

    /** config/steinkeybind.json. */
    static final class Config {
        List<Bind> binds = new ArrayList<>();
        /** Ticks entre uma mensagem e a proxima da fila. */
        int delayTicks = 10;
    }

    private final ModContext ctx = ModContext.of(this);
    private final Log log = ctx.log();
    private final ModConfig<Config> config = ModConfig.of("steinkeybind", Config.class);

    /** Mensagens esperando a vez. */
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    /** Ticks ate a proxima mensagem da fila poder sair. */
    private int wait;

    @Override
    public void afterStartGame() {
        Config c = config.get();
        if (c.binds == null) {
            c.binds = new ArrayList<>();
        }
        c.binds.removeIf(b -> b == null);
        for (Bind b : c.binds) {
            if (b.text == null) {
                b.text = "";
            }
        }
        log.info("iniciado (" + c.binds.size() + " binds)");
    }

    @Override
    public void onKeyInput() {
        if (Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()) {
            pressed(Keyboard.getEventKey());
        }
    }

    @Override
    public void onMouseInput() {
        int button = Keys.mouseButton();
        if (button >= 0 && Keys.mousePressed()) {
            pressed(button - 100);
        }
    }

    @Override
    public void onTickEnd() {
        if (wait > 0) {
            wait--;
        }
        pump();
    }

    @Override
    public void onJoinGame() {
        queue.clear();
        wait = 0;
    }

    @Override
    public void onDisconnected(String address, String reason) {
        queue.clear();
        wait = 0;
    }

    @Override
    public void onShutdown() {
        config.save();
    }

    /**
     * Uma tecla apertada no jogo. Quando a mesma tecla tem um bind com modificador e outro sem, so vale o mais
     * especifico: Ctrl+F nao manda tambem o que esta no F.
     */
    private void pressed(int key) {
        if (key == Keys.KEY_NONE || Screens.isOpen() || Entities.self() == null) {
            return;
        }
        List<Bind> plain = new ArrayList<>();
        List<Bind> modified = new ArrayList<>();
        for (Bind b : config.get().binds) {
            if (b.key != key || b.text.isBlank()) {
                continue;
            }
            if (b.modifier == 0) {
                plain.add(b);
            } else if (held(b.modifier)) {
                modified.add(b);
            }
        }
        for (Bind b : modified.isEmpty() ? plain : modified) {
            for (String part : b.text.split("\\|")) {
                String msg = part.trim();
                if (!msg.isEmpty() && queue.size() < MAX_QUEUE) {
                    queue.add(msg);
                }
            }
        }
        pump();
    }

    private static boolean held(int modifier) {
        return switch (modifier) {
            case 1 -> Keys.isCtrlDown();
            case 2 -> Keys.isShiftDown();
            case 3 -> Keys.isAltDown();
            default -> false;
        };
    }

    /** Manda a proxima mensagem da fila, se o intervalo ja passou. */
    private void pump() {
        if (wait > 0 || queue.isEmpty()) {
            return;
        }
        if (Entities.self() == null) {
            queue.clear();
            return;
        }
        Chat.send(queue.poll());
        wait = config.get().delayTicks;
    }

    @Override
    public void onPage(Page page) {
        List<Bind> binds = config.get().binds;
        page.icon("key");
        page.section(tr("steinkeybind.binds"));
        String[] modifiers = new String[MODIFIERS.length];
        for (int i = 0; i < MODIFIERS.length; i++) {
            modifiers[i] = tr("steinkeybind.modifier." + MODIFIERS[i]);
        }
        // As linhas de todos os binds existem desde ja e aparecem conforme a lista cresce: a pagina e montada so
        // quando o Painel abre, e acrescentar um bind tem de aparecer na mesma abertura.
        for (int i = 0; i < MAX; i++) {
            final int n = i;
            page.option(shown(keyRow(Lang.tr("steinkeybind.bind", n + 1), () -> bind(n).key, v -> {
                bind(n).key = v;
                config.saveSoon();
            }), () -> n < binds.size()));
            page.option(shown(Option.cycle(tr("steinkeybind.modifier"), modifiers, () -> bind(n).modifier, v -> {
                bind(n).modifier = v;
                config.saveSoon();
            }), () -> n < binds.size()));
            page.option(shown(new Option.Text(tr("steinkeybind.text"), tr("steinkeybind.hint"), () -> bind(n).text,
                    v -> {
                bind(n).text = v;
                config.saveSoon();
            }), () -> n < binds.size()));
            page.option(shown(Option.action(tr("steinkeybind.remove"), "", () -> {
                if (n < binds.size()) {
                    binds.remove(n);
                    config.saveSoon();
                }
            }), () -> n < binds.size()));
        }
        page.option(Option.action(tr("steinkeybind.add"), "", () -> {
            if (binds.size() < MAX) {
                binds.add(new Bind());
                config.saveSoon();
            }
        }).visibleWhen(() -> binds.size() < MAX));
        page.restore(() -> {
            binds.clear();
            queue.clear();
            config.save();
        });

        page.section(tr("steinkeybind.sending"));
        page.option(Option.slider(tr("steinkeybind.delay"), 0, 40, 1, () -> config.get().delayTicks, v -> {
            config.get().delayTicks = (int) Math.round(v);
            config.saveSoon();
        }, v -> Lang.tr("steinkeybind.seconds", String.format("%.2f", v / 20))));
        page.restore(() -> {
            config.get().delayTicks = new Config().delayTicks;
            config.save();
        });
    }

    /**
     * A linha da tecla de um bind: o nome e a etiqueta quadrada da tecla ({@link Option#withKey}), o mesmo desenho das
     * teclas do Combat. Clicar em qualquer ponto da linha faz o mesmo que clicar na etiqueta: o esquerdo espera a
     * proxima tecla (Esc desiste), o direito limpa.
     */
    private static Option keyRow(String label, IntSupplier get, IntConsumer set) {
        Option row = new Option(label) {
            @Override
            public String value() {
                return "";
            }

            @Override
            public void click(boolean forward) {
                // Pelo tipo Option, como no shown: o key() e da Option.
                ((Option) this).key().click(forward);
            }
        };
        return row.withKey(get, set);
    }

    /**
     * A opcao so aparece enquanto {@code when}. Chamado pelo tipo Option, e nao pela subclasse: a conferencia de
     * cobertura procura o metodo na classe em que a chamada diz que ele esta, e o visibleWhen e da Option.
     */
    private static Option shown(Option option, java.util.function.BooleanSupplier when) {
        return option.visibleWhen(when);
    }

    /** O bind n, ou um vazio quando ele acabou de sair da lista (a linha ainda pode estar sendo desenhada). */
    private Bind bind(int n) {
        List<Bind> binds = config.get().binds;
        return n < binds.size() ? binds.get(n) : new Bind();
    }

    private static String tr(String key) {
        return Lang.tr(key);
    }
}

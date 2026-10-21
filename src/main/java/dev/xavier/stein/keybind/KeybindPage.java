/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Breno Xavier
 */

package dev.xavier.stein.keybind;

import dev.xavier.stein.loader.api.Lang;
import dev.xavier.stein.loader.api.ModConfig;
import dev.xavier.stein.loader.api.Option;
import dev.xavier.stein.loader.api.Page;
import dev.xavier.stein.loader.api.Server;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** The Keybind page on the Panel: the list of binds and the gap between messages. */
final class KeybindPage {

    private final ModConfig<KeybindConfig> config;
    private final MessageQueue queue;

    KeybindPage(ModConfig<KeybindConfig> config, MessageQueue queue) {
        this.config = config;
        this.queue = queue;
    }

    void build(Page page) {
        page.icon("key");
        serverOff(page);
        binds(page);
        sending(page);
    }

    /**
     * When the current server does not allow chat macros, a line at the top says so: the binds stay
     * editable, they just do not fire there. The page is built when the Panel opens, so the server
     * is the one the player is on at that moment.
     */
    private void serverOff(Page page) {
        String server = Server.restrictedBy("chatMacros");
        if (!server.isEmpty()) {
            page.option(Option.action(Lang.tr("steinkeybind.serverOff", server), "", () -> {}));
        }
    }

    private void binds(Page page) {
        List<Bind> binds = config.get().binds;
        page.section(Lang.tr("steinkeybind.binds"));
        String[] modifiers = new String[Bind.MODIFIERS.length];
        for (int i = 0; i < modifiers.length; i++) {
            modifiers[i] = Lang.tr("steinkeybind.modifier." + Bind.MODIFIERS[i]);
        }
        // The rows of every bind exist from the start and show up as the list grows: the page is
        // built only when the Panel opens, and adding a bind has to show in that same opening.
        for (int i = 0; i < KeybindConfig.MAX_BINDS; i++) {
            bindRows(page, i, modifiers);
        }
        page.option(
                Option.action(
                                Lang.tr("steinkeybind.add"),
                                "",
                                () -> {
                                    if (binds.size() < KeybindConfig.MAX_BINDS) {
                                        binds.add(new Bind());
                                        config.saveSoon();
                                    }
                                })
                        .visibleWhen(() -> binds.size() < KeybindConfig.MAX_BINDS));
        page.restore(
                () -> {
                    binds.clear();
                    queue.clear();
                    config.save();
                });
    }

    /** The four rows of bind {@code n}: key, modifier, text and remove. */
    private void bindRows(Page page, int n, String[] modifiers) {
        KeybindConfig c = config.get();
        BooleanSupplier exists = () -> n < c.binds.size();
        page.option(
                shown(
                        keyRow(
                                Lang.tr("steinkeybind.bind", n + 1),
                                () -> c.bind(n).key,
                                v -> {
                                    c.bind(n).key = v;
                                    config.saveSoon();
                                }),
                        exists));
        page.option(
                shown(
                        Option.cycle(
                                Lang.tr("steinkeybind.modifier"),
                                modifiers,
                                () -> c.bind(n).modifier,
                                v -> {
                                    c.bind(n).modifier = v;
                                    config.saveSoon();
                                }),
                        exists));
        page.option(
                shown(
                        new Option.Text(
                                Lang.tr("steinkeybind.text"),
                                Lang.tr("steinkeybind.hint"),
                                () -> c.bind(n).text,
                                v -> {
                                    c.bind(n).text = v;
                                    config.saveSoon();
                                }),
                        exists));
        page.option(
                shown(
                        Option.action(
                                Lang.tr("steinkeybind.remove"),
                                "",
                                () -> {
                                    if (n < c.binds.size()) {
                                        c.binds.remove(n);
                                        config.saveSoon();
                                    }
                                }),
                        exists));
    }

    private void sending(Page page) {
        page.section(Lang.tr("steinkeybind.sending"));
        page.option(
                Option.slider(
                        Lang.tr("steinkeybind.delay"),
                        0,
                        40,
                        1,
                        () -> config.get().delayTicks,
                        v -> {
                            config.get().delayTicks = (int) Math.round(v);
                            config.saveSoon();
                        },
                        v -> Lang.tr("steinkeybind.seconds", String.format("%.2f", v / 20))));
        page.restore(
                () -> {
                    config.get().delayTicks = KeybindConfig.DEFAULT_DELAY_TICKS;
                    config.save();
                });
    }

    /**
     * A bind's key row: the name and the square key label ({@link Option#withKey}), drawn like the
     * keys of the other Stein mods. Clicking anywhere on the row does the same as clicking the
     * label: the left button waits for the next key (Esc gives up), the right one clears it.
     */
    private static Option keyRow(String label, IntSupplier get, IntConsumer set) {
        Option row =
                new Option(label) {
                    @Override
                    public String value() {
                        return "";
                    }

                    @Override
                    public void click(boolean forward) {
                        // Called through the Option type: key() is declared there (see shown).
                        ((Option) this).key().click(forward);
                    }
                };
        return row.withKey(get, set);
    }

    /**
     * The option shows only while {@code when}. Called through the Option type rather than the
     * subclass, so the compiled call names the class that declares visibleWhen: the Loader's
     * reference check looks the method up in the class the call names.
     */
    private static Option shown(Option option, BooleanSupplier when) {
        return option.visibleWhen(when);
    }
}

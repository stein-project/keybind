/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Breno Xavier
 */

package dev.xavier.stein.keybind;

import dev.xavier.stein.loader.api.Entities;
import dev.xavier.stein.loader.api.Keys;
import dev.xavier.stein.loader.api.ModConfig;
import dev.xavier.stein.loader.api.ModContext;
import dev.xavier.stein.loader.api.Page;
import dev.xavier.stein.loader.api.Screens;
import dev.xavier.stein.loader.api.Server;
import dev.xavier.stein.loader.api.SteinMod;

import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat messages and commands on a key: press F and the game sends "/home". Each bind has a key, an
 * optional modifier (Ctrl, Shift or Alt) and a text; the text may hold several messages separated
 * by "|", sent one after the other through a {@link MessageQueue}.
 *
 * <p>Fires only in the game, with no screen in front: with the chat or the Panel open the key
 * belongs to the text field. And only on the press: holding the key does not repeat.
 */
public final class SteinKeybind implements SteinMod {

    private final ModContext ctx = ModContext.of(this);
    private final ModConfig<KeybindConfig> config = ctx.config(KeybindConfig.class);
    private final MessageQueue queue = new MessageQueue(() -> config.get().delayTicks);

    @Override
    public void afterStartGame() {
        KeybindConfig c = config.get();
        c.normalize();
        ctx.log().info("started (" + c.binds.size() + " binds)");
    }

    @Override
    public void onKeyInput() {
        // The Loader's Keys has no keyboard event yet (only the mouse one): the game's LWJGL 2
        // keyboard, as in any 1.8.9 mod.
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
        queue.tick();
    }

    @Override
    public void onJoinGame() {
        queue.reset();
    }

    @Override
    public void onDisconnected(String address, String reason) {
        queue.reset();
    }

    @Override
    public void onShutdown() {
        config.save();
    }

    @Override
    public void onPage(Page page) {
        new KeybindPage(config, queue).build(page);
    }

    /**
     * A key pressed in the game. When the same key has a bind with a modifier and one without, only
     * the more specific one counts: Ctrl+F does not also send what is on F.
     */
    private void pressed(int key) {
        if (key == Keys.KEY_NONE || Screens.isOpen() || Entities.self() == null) {
            return;
        }
        // A bind is a chat macro: some servers (Hypixel) do not allow them, and there nothing
        // fires while the player is on it; the binds themselves are kept.
        if (!Server.allows("chatMacros")) {
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
            } else if (b.modifierHeld()) {
                modified.add(b);
            }
        }
        for (Bind b : modified.isEmpty() ? plain : modified) {
            queue.add(b.text);
        }
        queue.pump();
    }
}

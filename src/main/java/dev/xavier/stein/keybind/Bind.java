/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Breno Xavier
 */

package dev.xavier.stein.keybind;

import dev.xavier.stein.loader.api.Keys;

/**
 * One bind of {@code config/steinkeybind.json}: a key, an optional modifier and the text to send.
 * The field names are the JSON keys players already have: do not rename them.
 */
final class Bind {

    /** The modifier names, in the order of {@link #modifier} (also their translation keys). */
    static final String[] MODIFIERS = {"none", "ctrl", "shift", "alt"};

    /** LWJGL 2 key code; a mouse button is {@code button - 100} (negative). */
    int key;

    /** Index into {@link #MODIFIERS}: 0 none, 1 Ctrl, 2 Shift, 3 Alt. */
    int modifier;

    /** Messages or commands, several separated by {@code |}. */
    String text = "";

    /** Whether this bind's modifier is held right now (false for "none"). */
    boolean modifierHeld() {
        return switch (modifier) {
            case 1 -> Keys.isCtrlDown();
            case 2 -> Keys.isShiftDown();
            case 3 -> Keys.isAltDown();
            default -> false;
        };
    }
}

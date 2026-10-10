/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Breno Xavier
 */

package dev.xavier.stein.keybind;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code config/steinkeybind.json}. The field names are the JSON keys players already have: do not
 * rename them.
 */
final class KeybindConfig {

    /** How many binds fit on the page. */
    static final int MAX_BINDS = 20;

    /** The default of {@link #delayTicks}: half a second. */
    static final int DEFAULT_DELAY_TICKS = 10;

    List<Bind> binds = new ArrayList<>();

    /** Ticks between one queued message and the next. */
    int delayTicks = DEFAULT_DELAY_TICKS;

    /** Repairs what a hand-edited file may leave: no list, null binds, binds without text. */
    void normalize() {
        if (binds == null) {
            binds = new ArrayList<>();
        }
        binds.removeIf(b -> b == null);
        for (Bind b : binds) {
            if (b.text == null) {
                b.text = "";
            }
        }
    }

    /**
     * Bind {@code n}, or an empty one when it has just left the list (its row may still be drawn).
     */
    Bind bind(int n) {
        return n < binds.size() ? binds.get(n) : new Bind();
    }
}

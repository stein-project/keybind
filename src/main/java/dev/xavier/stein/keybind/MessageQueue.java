/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Breno Xavier
 */

package dev.xavier.stein.keybind;

import dev.xavier.stein.loader.api.Chat;
import dev.xavier.stein.loader.api.Entities;

import java.util.ArrayDeque;
import java.util.function.IntSupplier;

/**
 * The messages waiting to be sent, one at a time with a gap between them so the server does not
 * kick the player for spam. The queue has a cap: pressing a key twenty times does not become twenty
 * rounds.
 *
 * <p>Everything goes out through {@link Chat#send}, the same path as typing in the chat: the server
 * sees a normal message.
 */
final class MessageQueue {

    /** Queue cap: what goes past it is dropped. */
    static final int MAX = 10;

    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private final IntSupplier delayTicks;

    /** Ticks until the next message may go out. */
    private int wait;

    MessageQueue(IntSupplier delayTicks) {
        this.delayTicks = delayTicks;
    }

    /** Queues every non-empty part of {@code text} (split by {@code |}), up to the cap. */
    void add(String text) {
        for (String part : text.split("\\|")) {
            String msg = part.trim();
            if (!msg.isEmpty() && queue.size() < MAX) {
                queue.add(msg);
            }
        }
    }

    /** One client tick went by. */
    void tick() {
        if (wait > 0) {
            wait--;
        }
        pump();
    }

    /** Sends the next message, if the gap has passed. */
    void pump() {
        if (wait > 0 || queue.isEmpty()) {
            return;
        }
        if (Entities.self() == null) {
            queue.clear();
            return;
        }
        Chat.send(queue.poll());
        wait = delayTicks.getAsInt();
    }

    /** Drops everything waiting (the binds were cleared); the current gap still applies. */
    void clear() {
        queue.clear();
    }

    /** Drops everything waiting and the gap (a new world, a disconnection). */
    void reset() {
        queue.clear();
        wait = 0;
    }
}

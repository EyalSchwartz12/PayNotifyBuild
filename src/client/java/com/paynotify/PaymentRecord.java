package com.paynotify;

import net.minecraft.network.chat.Component;

/**
 * One detected payment message.
 *
 * @param id        unique id, embedded in that message's button commands
 * @param player    name of the player who paid
 * @param rawAmount the amount exactly as written in chat (without the dollar sign)
 * @param amount    parsed amount, or NaN if it could not be parsed
 * @param message   the exact component that was added to chat (used to find it again when hiding it)
 */
record PaymentRecord(int id, String player, String rawAmount, double amount, Component message) {
}

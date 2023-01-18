package net.stellarica.server.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.minimessage.MiniMessage

fun Audience.sendRichMessage(message: String) {
	this.sendMessage(MiniMessage.miniMessage().deserialize(message))
}
fun Audience.sendRichActionBar(message: String) {
	this.sendActionBar(MiniMessage.miniMessage().deserialize(message))
}
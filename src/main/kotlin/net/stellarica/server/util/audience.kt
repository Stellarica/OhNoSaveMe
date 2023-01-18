package net.stellarica.server.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

fun Audience.sendRichMessage(message: String) {
	this.sendMessage(message.toMiniMessage())
}
fun Audience.sendRichActionBar(message: String) {
	this.sendActionBar(message.toMiniMessage())
}

fun String.toMiniMessage(): Component = MiniMessage.miniMessage().deserialize(this)
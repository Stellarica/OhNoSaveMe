package net.stellarica.server.util.gui

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.inventory.InventoryItemChangeEvent
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.player.PlayerTickEvent
import net.minestom.server.event.player.PlayerUseItemEvent
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.item.ItemStack

object GuiManager {
	data class GuiData(
		val originalHotbar: List<ItemStack>,
		val hotbarMenu: HotbarMenu
	)

	fun registerListeners() {
		val node = EventNode.type("gui-listener", EventFilter.INVENTORY)
		node.addListener(InventoryPreClickEvent::class.java) { event  ->
			if (!players.containsKey(event.player)) return@addListener
			if (event.clickType == ClickType.LEFT_CLICK) {
				players[event.player]!!.hotbarMenu.onButtonClicked(event.slot)
			}
			event.isCancelled = true
		}
		val node2 = EventNode.type("gui-listener", EventFilter.INSTANCE)
		node2.addListener(PlayerTickEvent::class.java) { event ->
			if (!players.containsKey(event.player)) return@addListener
			players[event.player]!!.hotbarMenu.onTick()
		}
		MinecraftServer.getGlobalEventHandler().addListener(
			PlayerUseItemEvent::class.java) {event ->
			if (!players.containsKey(event.player)) return@addListener
				players[event.player]!!.hotbarMenu.onButtonClicked(event.player.heldSlot.toInt())
			}
	}

	val players = mutableMapOf<Player, GuiData>()


	fun open(player: Player, menu: HotbarMenu) {
		players[player] = GuiData((0..8).map{ player.inventory.getItemStack(it) }, menu)
		for (i in 0..8) {
			player.inventory.setItemStack(i, menu.hotbarContents[i])
		}
	}


	fun close(player: Player) {
		val data = players.remove(player) ?: return
		data.hotbarMenu.onMenuClosed()
		for (i in 0..8) {
			player.inventory.setItemStack(i, data.originalHotbar[i])
		}
	}
}
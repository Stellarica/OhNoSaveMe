package net.stellarica.server.util.gui

import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack

public interface HotbarMenu {
	val player: Player
	val hotbarContents: List<ItemStack>

	/**
	 * Called when the player selects the item at index
	 */
	open fun onButtonClicked(index: Int) {}

	/**
	 * Called when the player changes their slot selection
	 */
	open fun onChangeSelectedSlot(oldIndex: Int, newIndex: Int) {}

	/**
	 * Called before the menu closes
	 */
	open fun onMenuClosed() {}

	/**
	 * Called every tick this is open
	 */
	open fun onTick() {}
}
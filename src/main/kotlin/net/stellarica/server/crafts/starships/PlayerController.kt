package net.stellarica.server.crafts.starships

import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.utils.Rotation
import net.stellarica.server.util.gui.HotbarMenu
import net.stellarica.server.util.toMiniMessage

class PlayerController(private val ship: Starship, override val player: Player): HotbarMenu {

	override val hotbarContents = listOf(
		ItemStack.of(Material.GREEN_STAINED_GLASS_PANE).withDisplayName("<b>Cruise".toMiniMessage()),
		ItemStack.of(Material.RED_STAINED_GLASS_PANE).withDisplayName("<b>Stop".toMiniMessage()),
		ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE).withDisplayName("<b>Precision".toMiniMessage()),
		ItemStack.AIR,
		ItemStack.of(Material.BLUE_STAINED_GLASS_PANE).withDisplayName("<b>Left".toMiniMessage()),
		ItemStack.of(Material.BLUE_STAINED_GLASS_PANE).withDisplayName("<b>Right".toMiniMessage()),
		ItemStack.AIR,
		ItemStack.AIR,
		ItemStack.of(Material.BARRIER).withDisplayName("<b>Unpilot".toMiniMessage()),
	)

	private var counter = 0;

	override fun onTick() {
		counter++
		if (counter == 10 && ship.cruiseSpeed > 0) {
			counter = 0
			ship.move(ship.cruiseDirection.mul(ship.cruiseSpeed.toDouble()))
		}
	}

	override fun onButtonClicked(index: Int) {
		when (index) {
			0 -> {
				ship.cruiseDirection = player.position.direction()
				ship.cruiseSpeed = 5
			}
			1 -> ship.cruiseSpeed = 0
			2 -> ship.move(player.position.direction().mul(1.5))
			4 -> ship.rotate(Rotation.CLOCKWISE)
			5 -> ship.rotate(Rotation.COUNTER_CLOCKWISE)
			8 -> ship.unpilot()
		}
	}

	override fun onMenuClosed() {

	}
}
package net.stellarica.server.crafts.starships

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.stellarica.server.crafts.Craft
import net.stellarica.server.util.gui.GuiManager


class Starship(origin: Point, world: Instance, owner: Player) : Craft(origin, world, owner) {
	var cruiseDirection = Vec.ZERO!!
	var cruiseSpeed = 0
	var pilot: Player? = null

	//val components = mutableSetOf<ShipComponent>()

	lateinit var controller: PlayerController

	fun pilot(player: Player) {
	//	components.forEach { it.onPilot(player) }
		pilot = player
		controller = PlayerController(this, player)
		GuiManager.open(player, controller)
	}

	fun unpilot() {
		pilot?.let { GuiManager.close(it) }
	//	components.forEach { it.onUnpilot() }
	}
}
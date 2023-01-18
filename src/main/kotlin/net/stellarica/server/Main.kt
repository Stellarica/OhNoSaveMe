package net.stellarica.server

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.stellarica.server.crafts.starships.Starship
import net.stellarica.server.multiblocks.MultiblockHandler
import net.stellarica.server.multiblocks.MultiblockType
import net.stellarica.server.util.OriginRelative
import net.stellarica.server.util.gui.GuiManager


fun main(args: Array<String>) {
	val minecraftServer = MinecraftServer.init()
	val instanceManager = MinecraftServer.getInstanceManager()

	val instanceContainer = instanceManager.createInstanceContainer()
	instanceContainer.setGenerator { unit: GenerationUnit ->
		unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK)
	}

	val globalEventHandler = MinecraftServer.getGlobalEventHandler()
	globalEventHandler.addListener(
		PlayerLoginEvent::class.java
	) { event: PlayerLoginEvent ->
		val player: Player = event.player
		event.setSpawningInstance(instanceContainer)
		player.respawnPoint = Pos(0.0, 42.0, 0.0)
		player.gameMode = GameMode.CREATIVE
	}
	globalEventHandler.addListener(
		PlayerBlockInteractEvent::class.java
	) {	event ->
		if (event.block == Block.EMERALD_BLOCK) {
			Starship(event.blockPosition, event.instance, event.player).also {
				it.detect()
				it.pilot(event.player)
			}
		}
	}


	MultiblockHandler.registerListeners()
	GuiManager.registerListeners()

	MultiblockHandler.types.add(MultiblockType(
		"test",
		mapOf(
			OriginRelative(0,0,0) to Block.DIAMOND_BLOCK,
			OriginRelative(0,-1,0) to Block.IRON_BLOCK,
			OriginRelative(-1,0,0) to Block.REDSTONE_BLOCK,
			OriginRelative(1,0,0) to Block.REDSTONE_BLOCK,
			OriginRelative(0,0,1) to Block.REDSTONE_BLOCK,
			OriginRelative(0,0,-1) to Block.IRON_BLOCK,
		)
	))

	minecraftServer.start("0.0.0.0", 25565)
}
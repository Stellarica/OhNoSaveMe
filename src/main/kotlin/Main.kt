import net.minestom.server.MinecraftServer

fun main(args: Array<String>) {
	val minecraftServer = MinecraftServer.init();
	minecraftServer.start("0.0.0.0", 25565);
}
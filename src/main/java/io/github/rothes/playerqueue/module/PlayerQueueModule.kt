package io.github.rothes.playerqueue.module

import com.google.common.io.ByteStreams
import io.github.rothes.esu.bukkit.module.BukkitModule
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.ConfigLoader
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.configuration.data.TitleData
import io.github.rothes.playerqueue.Listeners
import io.github.rothes.playerqueue.QueueManager
import io.github.rothes.playerqueue.plugin
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.HandlerList
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import java.time.Duration as JDuration

object PlayerQueueModule: BukkitModule<PlayerQueueModule.ModuleConfig, PlayerQueueModule.ModuleLang>(
    ModuleConfig::class.java, ModuleLang::class.java
) {

    override val moduleFolder: Path
        get() = plugin.dataPath

    private lateinit var data: ModuleData
    private val dataPath = moduleFolder.resolve("data.yml")

    override fun enable() {
        data = ConfigLoader.load(dataPath)
        for (uuid in data.queue) {
            Bukkit.getPlayer(uuid)?.let {
                QueueManager.addPlayerToQueue(it)
            }
        }

        Bukkit.getPluginManager().registerEvents(Listeners, plugin)
        Scheduler.global(5, 20, plugin) {
            val player = Bukkit.getOnlinePlayers().firstOrNull() ?: return@global

            with(ByteStreams.newDataOutput()) {
                writeUTF("PlayerCount")
                writeUTF(config.targetServer)
                player.sendPluginMessage(plugin, "BungeeCord", toByteArray())
            }
        }
        Scheduler.global(5, 10, plugin) {
            for (player in QueueManager.pending.keys) {
                plugin.trySendPlayer(player)
            }
        }
    }

    override fun disable() {
        super.disable()
        HandlerList.unregisterAll(Listeners)
        Scheduler.cancelTasks(plugin)

        data.queue.clear()
        data.queue.addAll(QueueManager.pending.map { it.key.uniqueId })
        data.queue.addAll(QueueManager.queue.map { it.key.uniqueId })
        ConfigLoader.save(dataPath, data)
    }

    override fun reloadConfig() {
        super.reloadConfig()
    }

    override fun perm(shortPerm: String): String {
        return "playerqueue.$shortPerm"
    }

    data class ModuleData(
        val queue: MutableList<UUID> = arrayListOf(),
    ): ConfigurationPart

    data class ModuleConfig(
        @field:Comment("The proxy server targets to.")
        val targetServer: String = "proxy_server_name",
        val limitPlayers: Int = 40,
        @field:Comment("Player must wait at least this duration to be sent. This is to avoid player has no time to interact with other things.")
        val minQueueTime: JDuration = Duration.parse("5s").toJavaDuration(),
        @field:Comment("The interval between each sent attempt.")
        val connectAttemptInterval: JDuration = Duration.parse("5s").toJavaDuration(),
        @field:Comment("Player must wait at least this duration to join the target server again after a sent.")
        val playerJoinInterval: JDuration = Duration.parse("2m").toJavaDuration(),
        @field:Comment("Configures in-queue messages.\n" +
                "'key' is also the `key` of in-queue-message in lang.\n" +
                "'interval' determines the interval between each message.\n" +
                "'cache' tells plugin to cache last message, and skip current message if their contents are same.")
        val queueMessages: List<QueueMessage> = listOf(QueueMessage()),
    ): ConfigurationPart {

        data class QueueMessage(
            val key: String = "queueing",
            val interval: JDuration = Duration.parse("1s").toJavaDuration(),
            val cache: Boolean = true,
        ): ConfigurationPart
    }

    data class ModuleLang(
        val bypassedQueue: MessageData = "<pc>You owns queue-free access, <queue> players skipped.".message,
        val inQueueMessage: Map<String, MessageData> = linkedMapOf(
            "queueing" to MessageData(
                title = TitleData(
                    "",
                    "<pdc><online> <pc><b>Playing</b>  <tc>|  <pc><b>Position in queue: </b><pdc><position>",
                    TitleData.Times(50.milliseconds, 1.hours, 0.milliseconds)
                )
            )
        ),
        val joinTimeLimiting: MessageData = MessageData(
            actionBar = "<pc>Joining the server in <pdc><duration><pc>; Double click <pdc><key:key.sneak> <pc>to cancel"
        ),
        val frequentWarning: MessageData = MessageData(
            title = TitleData(
                "<ec>Please do not access the server frequently",
                "<ec>You will back to the queue in <edc><duration><ec>",
                TitleData.Times(0.milliseconds, 1.minutes, 0.milliseconds))
        ),
        val connecting: MessageData = "<dark_green>Connecting to the server...".message,
        val suspending: MessageData = MessageData(
            actionBar = "<pc>You have left the queue; Double sneak to rejoin the queue",
            title = TitleData("", ""),
            sound = SoundData("minecraft", Sound.BLOCK_NOTE_BLOCK_BELL.key().value())
        ),
    ): ConfigurationPart

}
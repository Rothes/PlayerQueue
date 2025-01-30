package io.github.rothes.playerqueue.module

import io.github.rothes.esu.bukkit.module.BukkitModule
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.data.SoundData
import io.github.rothes.esu.core.configuration.data.TitleData
import io.github.rothes.playerqueue.plugin
import org.bukkit.Sound
import org.spongepowered.configurate.objectmapping.meta.Comment
import java.nio.file.Path
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

    override fun enable() {

    }

    override fun disable() {
        super.disable()
    }

    override fun reloadConfig() {
        super.reloadConfig()
    }

    override fun perm(shortPerm: String): String {
        return "playerqueue.$shortPerm"
    }

    data class ModuleConfig(
        val targetServer: String = "proxy_server_name",
        val limitPlayers: Int = 40,
        val minQueueTime: JDuration = Duration.parse("5s").toJavaDuration(),
        val connectAttemptInterval: JDuration = Duration.parse("5s").toJavaDuration(),
        val playerJoinInterval: JDuration = Duration.parse("2m").toJavaDuration(),
        val queueMessages: List<QueueMessage> = listOf(QueueMessage()),
    ): ConfigurationPart {

        data class QueueMessage(
            @field:Comment("The key of in-queue-message in lang")
            val key: String = "queueing",
            @field:Comment("Interval between each message")
            val interval: JDuration = Duration.parse("1s").toJavaDuration(),
            @field:Comment("Cache last message, and skip sending if their contents are same")
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
                "<ec>You will back to the queue in <edc><duration><ec>.",
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
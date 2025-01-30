package io.github.rothes.playerqueue

import com.google.common.io.ByteStreams
import fr.xephi.authme.api.v3.AuthMeApi
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.module.ModuleManager
import io.github.rothes.esu.core.util.ComponentUtils.duration
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.esu.core.util.InitOnce
import io.github.rothes.playerqueue.module.PlayerQueueModule
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.messaging.PluginMessageListener
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinDuration

class PlayerQueuePlugin: JavaPlugin(), PluginMessageListener {

    var onlineCount = Int.MAX_VALUE

    override fun onEnable() {
        instance = this
        ModuleManager.addModule(PlayerQueueModule)

        Bukkit.getPluginManager().registerEvents(Listeners, this)
        this.server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        this.server.messenger.registerIncomingPluginChannel(this, "BungeeCord", this)

        for (player in Bukkit.getOnlinePlayers()) {
            if (AuthMeApi.getInstance().isAuthenticated(player)) {
                QueueManager.addPlayerToQueue(player)
            }
        }

        Scheduler.global(5, 20, plugin) {
            val player = Bukkit.getOnlinePlayers().firstOrNull() ?: return@global

            with(ByteStreams.newDataOutput()) {
                writeUTF("PlayerCount")
                writeUTF(PlayerQueueModule.config.targetServer)
                player.sendPluginMessage(plugin, "BungeeCord", toByteArray())
            }
        }
        Scheduler.global(5, 10, plugin) {
            for (player in QueueManager.pending.keys) {
                plugin.trySendPlayer(player)
            }
        }
    }

    override fun onDisable() {
        ModuleManager.removeModule(PlayerQueueModule)
        Scheduler.cancelTasks(this)
        this.server.messenger.unregisterOutgoingPluginChannel(this)
        this.server.messenger.unregisterIncomingPluginChannel(this)
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel != "BungeeCord") {
            return
        }
        val input = ByteStreams.newDataInput(message)
        if (input.readUTF() == "PlayerCount") {
            val server = input.readUTF()
            onlineCount = input.readInt()

            sendNextPlayers(PlayerQueueModule.config.limitPlayers - (onlineCount + QueueManager.inQueueSize))

            for ((index, entry) in QueueManager.queue.entries.withIndex()) {
                val (player, info) = entry
                val user = player.user

                val tags = arrayOf(unparsed("position", index + 1 + QueueManager.pending.size), unparsed("online", onlineCount))
                for (msg in PlayerQueueModule.config.queueMessages) {
                    if (System.currentTimeMillis() - (info.lastMessageTime[msg.key] ?: 0) < msg.interval.toMillis()) {
                        continue
                    }
                    val messageData = user.localed(PlayerQueueModule.locale) { inQueueMessage[msg.key] }
                    val parsed = messageData.parsed(user, *tags)
                    if (msg.cache && parsed == info.lastMessage[msg.key]) {
                        continue
                    }
                    info.lastMessage[msg.key] = parsed
                    user.message(parsed)
                }
            }
        }
    }

    fun sendNextPlayers(amount: Int) {
        for (i in 0 ..< amount) {
            if (!QueueManager.hasNext()) return

            val player = QueueManager.nextAndPend()
            trySendPlayer(player)
            val user = player.user
            user.clearTitle()
            user.clearActionBar()
        }
    }
    fun trySendPlayer(player: Player) {
        val joinOn: Long = QueueManager.joinTime(player)
        val user = player.user
        val lastSent = QueueManager.sentInfo.getIfPresent(user)
        val joinInterval = PlayerQueueModule.config.playerJoinInterval.toKotlinDuration()
        if (lastSent != null && lastSent.left && System.currentTimeMillis() - lastSent.joinTime < joinInterval.inWholeMilliseconds) {
            user.message(PlayerQueueModule.locale, { frequentWarning },
                duration(joinInterval - (System.currentTimeMillis() - lastSent.joinTime - 1000).milliseconds, user) // Adds extra 1s to avoid ms unit being displayed
            )
            return
        }

        val minQueueTime = PlayerQueueModule.config.minQueueTime.toKotlinDuration()
        if (System.currentTimeMillis() - joinOn < minQueueTime.inWholeMilliseconds) {
            user.message(PlayerQueueModule.locale, { joinTimeLimiting },
                duration(minQueueTime - (System.currentTimeMillis() - joinOn - 1000).milliseconds, user) // Adds extra 1s to avoid ms unit being displayed
            )
        } else {
            user.message(PlayerQueueModule.locale, { connecting })

            with(ByteStreams.newDataOutput()) {
                writeUTF("Connect")
                writeUTF(PlayerQueueModule.config.targetServer)

                player.sendPluginMessage(plugin, "BungeeCord", toByteArray())
            }
            QueueManager.sentInfo.put(user, QueueManager.SentInfo())
        }
    }

    companion object {
        var instance: PlayerQueuePlugin by InitOnce()
    }

}
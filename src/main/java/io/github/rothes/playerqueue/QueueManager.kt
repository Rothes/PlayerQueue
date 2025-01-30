package io.github.rothes.playerqueue

import com.google.common.cache.CacheBuilder
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.data.ParsedMessageData
import io.github.rothes.esu.core.util.ComponentUtils.unparsed
import io.github.rothes.playerqueue.module.PlayerQueueModule
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit

object QueueManager {

    val queue = linkedMapOf<Player, QueueInfo>()
    val pending = linkedMapOf<Player, QueueInfo>()
    val joined = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build<PlayerUser, JoinInfo>()

    val inQueueSize
        get() = queue.size + pending.size

    fun hasNext(): Boolean {
        return !queue.isEmpty()
    }

    fun joinTime(player: Player): Long {
        return queue[player]?.joinTime ?: pending[player]?.joinTime ?: error("Failed to get joinTime for player ${player.name}")
    }

    fun nextAndPend(): Player {
        synchronized(queue) {
            val (player, info) = queue.pollFirstEntry()
            pending[player] = info
            return player
        }
    }

    fun addPlayerToQueue(player: Player) {
        synchronized(queue) {
            if (queue.containsKey(player) || pending.containsKey(player)) {
                return
            }
            val info = QueueInfo()
            if (player.hasPermission("playerqueue.bypass")) {
                player.user.message(PlayerQueueModule.locale, { bypassedQueue }, unparsed("queue", queue.size))
                pending[player] = info
            } else if (plugin.onlineCount + inQueueSize < PlayerQueueModule.config.limitPlayers) {
                pending[player] = info
            } else {
                queue[player] = info
            }
        }
    }

    fun removePlayerFromQueue(player: Player) {
        synchronized(queue) {
            queue.remove(player)
            pending.remove(player)
        }
    }

    fun suspending(player: Player): Boolean {
        return !queue.contains(player) && !pending.contains(player)
    }

    fun position(player: Player) = queue.entries.indexOfFirst { it.key == player }

    data class QueueInfo(
        val joinTime: Long = System.currentTimeMillis(),
        val lastMessage: MutableMap<String, ParsedMessageData> = hashMapOf(),
        val lastMessageTime: MutableMap<String, Long> = hashMapOf(),
        var lastSend: Long = 0
    )

    data class JoinInfo(
        val joinTime: Long = System.currentTimeMillis(),
    )

}
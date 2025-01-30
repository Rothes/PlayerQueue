package io.github.rothes.playerqueue

import com.google.common.cache.CacheBuilder
import fr.xephi.authme.api.v3.AuthMeApi
import fr.xephi.authme.events.LoginEvent
import fr.xephi.authme.events.LogoutEvent
import io.github.rothes.esu.bukkit.user
import io.github.rothes.playerqueue.module.PlayerQueueModule
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object Listeners: Listener {

    @EventHandler
    fun onLogin(e: LoginEvent) {
        QueueManager.addPlayerToQueue(e.player)
    }

    @EventHandler
    fun onLogOut(e: LogoutEvent) {
        QueueManager.removePlayerFromQueue(e.player)
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {
        QueueManager.removePlayerFromQueue(e.player)
        QueueManager.sentInfo.getIfPresent(e.player)?.left = true
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerKickEvent) {
        QueueManager.removePlayerFromQueue(e.player)
        QueueManager.sentInfo.getIfPresent(e.player)?.left = true
    }

    private val lastSneak = CacheBuilder.newBuilder().expireAfterWrite(400.milliseconds.toJavaDuration()).build<Player, Long>()

    @EventHandler
    fun onSneak(e: PlayerToggleSneakEvent) {
        val player = e.player
        if (!e.isSneaking || !player.isOnGround)
            return

        val get = lastSneak.getIfPresent(player)
        if (get == null) {
            lastSneak.put(player, System.currentTimeMillis())
            return
        }
        lastSneak.invalidate(player)

        if (QueueManager.suspending(player)) {
            if (AuthMeApi.getInstance().isAuthenticated(player)) {
                QueueManager.addPlayerToQueue(player)
            }
        } else if (QueueManager.pending.contains(player)) {
            QueueManager.removePlayerFromQueue(player)
            e.player.user.message(PlayerQueueModule.locale, { suspending })
        }

    }


    //    @EventHandler
    //    public void onMove(PlayerMoveEvent e) {
    //        if (!e.getPlayer().isOp() && e.getFrom().distanceSquared(e.getTo()) != 0) {
    //            e.getPlayer().setGameMode(GameMode.ADVENTURE);
    //            e.getPlayer().setFlySpeed(0);
    //            e.getPlayer().setWalkSpeed(0);
    //        }
    //    }
    @EventHandler
    fun onDamage(e: EntityDamageEvent) {
        if (e.cause == EntityDamageEvent.DamageCause.FALL) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockProtect(e: BlockBreakEvent) {
        if (!e.player.isOp) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockProtect(e: BlockPlaceEvent) {
        if (!e.player.isOp) {
            e.isCancelled = true
        }
    }

    @EventHandler
    fun onBlockProtect(e: PlayerInteractEvent) {
        if (!e.player.isOp) {
            e.isCancelled = true
        }
    }

//    @EventHandler
//    fun onChunkLoad(e: ChunkLoadEvent) {
//        for (entity in e.getChunk().entities) {
//            if (entity is Player) {
//                continue
//            }
//            entity.remove()
//        }
//    }
    //    @EventHandler
    //    public void onEntitySpawn(EntitySpawnEvent e) {
    //        if (e.getEntity().getType() != EntityType.ITEM)
    //            e.setCancelled(true);
    //    }


}
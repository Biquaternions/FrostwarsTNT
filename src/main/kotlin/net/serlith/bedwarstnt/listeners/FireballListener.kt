package net.serlith.bedwarstnt.listeners

import com.cryptomorin.xseries.messages.Titles
import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.configs.MainConfig
import net.serlith.bedwarstnt.util.FireballRunnable
import net.serlith.bedwarstnt.util.Reloadable
import net.serlith.bedwarstnt.util.scheduleBlockRemoval
import net.serlith.bedwarstnt.util.sendCooldownTitle
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Fireball
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class FireballListener (
    private val plugin: BedwarsTNT,
    private val mainConfig: MainConfig,
) : Listener, Reloadable(10) {

    private lateinit var spawnLocation: Location
    private val lastUsed = mutableMapOf<UUID, Long>()

    init {
        this.plugin.server.pluginManager.registerEvents(this, plugin)
        this.reload()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.isCancelled) return
        if (event.material != Material.FIREBALL) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.player.location.distance(this.spawnLocation) < this.mainConfig.globalSection.spawnProtection) return
        event.isCancelled = true

        val player = event.player
        val current = System.currentTimeMillis()
        this.lastUsed[player.uniqueId]?.let {
            val time = current - it
            val cd = this.mainConfig.fireballSection.cooldown
            if (time >= cd) return@let
            sendCooldownTitle(player, (cd - time.toFloat()), this.plugin.mainConfig.globalSection, this.plugin.messagesConfig.messagesSection)
            return
        }
        this.lastUsed[player.uniqueId] = current

        val fireball = player.launchProjectile(Fireball::class.java)
        fireball.yield = this.mainConfig.fireballSection.yield
        if (player.itemInHand.amount == 1) player.itemInHand = null
        else player.itemInHand.amount -= 1

        FireballRunnable(
            fireball,
            event.player.location,
            this.mainConfig.fireballSection.speed,
            this.mainConfig.fireballSection.despawnDistance
        ).runTaskTimerAsynchronously(plugin, 0L, 5L)
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.entityType != EntityType.FIREBALL) return
        val section = this.mainConfig.fireballSection
        this.plugin.server.scheduler.runTaskAsynchronously(this.plugin) {
            blocks.forEach { block ->
                if (block.type in section.affectedBlocks) {
                    block.type = Material.AIR
                }
            }
        }

        entity.world.players.forEach players@ { player ->
            if (player.location.distance(entity.location) > section.radius) return@players
            player.velocity = player.velocity.add(extraMomentum(
                player,
                entity,
                section.knockback.horizontalExtra,
                section.knockback.verticalExtra,
                section.knockback.multiplier
            ))
            if (player.velocity.length() > section.knockback.speedLimit) {
                player.velocity = player.velocity.normalize().multiply(section.knockback.speedLimit)
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        this.lastUsed.remove(event.player.uniqueId)
    }

    override fun reload() {
        this.spawnLocation = Location(this.mainConfig.globalSection.world, 0.0, 0.0, 0.0)
    }

}
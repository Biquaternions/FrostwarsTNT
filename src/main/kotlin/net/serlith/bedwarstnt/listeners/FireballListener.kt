package net.serlith.bedwarstnt.listeners

import club.frozed.frost.Frost
import club.frozed.frost.managers.MatchManager
import com.cryptomorin.xseries.messages.Titles
import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.configs.MainConfig
import net.serlith.bedwarstnt.util.FireballRunnable
import net.serlith.bedwarstnt.util.Reloadable
import net.serlith.bedwarstnt.util.extraMomentum
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
import kotlin.collections.set

class FireballListener (
    private val plugin: BedwarsTNT,
    private val mainConfig: MainConfig,
) : Listener, Reloadable(10) {

    private lateinit var spawnLocation: Location
    private val lastUsed = mutableMapOf<UUID, Long>()

    private var matchManager: MatchManager? = null
    private val fireballOwners = mutableMapOf<UUID, UUID>()

    init {
        val frost = this.plugin.server.pluginManager.getPlugin("Frost")
        frost?.let {
            this.matchManager = (it as Frost).managerHandler.matchManager
        }

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
            Titles(this.plugin.messagesConfig.messagesSection.fireballCooldown.replace("<seconds>", "%.1f".format((cd - time.toFloat()) / 1000)),
                "",
                this.plugin.mainConfig.globalSection.title.fadeIn,
                this.plugin.mainConfig.globalSection.title.stay,
                this.plugin.mainConfig.globalSection.title.fadeOut
            ).send(player)
            return
        }
        this.lastUsed[player.uniqueId] = current

        val fireball = player.launchProjectile(Fireball::class.java)
        fireball.yield = this.mainConfig.fireballSection.yield
        if (player.itemInHand.amount == 1) player.itemInHand = null
        else player.itemInHand.amount -= 1

        this.fireballOwners[fireball.uniqueId] = player.uniqueId

        FireballRunnable(
            fireball,
            event.player.location,
            this.mainConfig.fireballSection.speed,
            this.mainConfig.fireballSection.despawnDistance
        ).runTaskTimerAsynchronously(plugin, 0L, 5L)
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        if (event.entityType != EntityType.FIREBALL) return

        val blocks = event.blockList().toList()
        event.blockList().clear()

        val section = this.mainConfig.fireballSection
        this.plugin.server.scheduler.runTaskAsynchronously(this.plugin) {
            blocks.forEach { block ->
                if (block.type in section.affectedBlocks) {
                    block.type = Material.AIR
                }
            }
        }
        this.fireballOwners.remove(event.entity.uniqueId)

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
        this.fireballOwners.entries.removeIf { it.value == event.player.uniqueId }
    }

    override fun reload() {
        this.spawnLocation = Location(this.mainConfig.globalSection.world, 0.0, 0.0, 0.0)
    }

}
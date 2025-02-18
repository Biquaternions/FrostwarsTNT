package net.serlith.bedwarstnt.listeners

import club.frozed.frost.Frost
import club.frozed.frost.managers.MatchManager
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
import kotlin.collections.set

class FireballListener (
    private val plugin: BedwarsTNT,
    private val mainConfig: MainConfig,
) : Listener, Reloadable(10) {

    private lateinit var spawnLocation: Location
    private val lastUsed = mutableMapOf<UUID, Long>()

    private var matchManager: MatchManager? = null
    private val fireballOwners: MutableMap<UUID, UUID> = mutableMapOf()

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
            sendCooldownTitle(player, (cd - time.toFloat()), this.plugin.mainConfig.globalSection, this.plugin.messagesConfig.messagesSection)
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
        if (event.entityType != EntityType.FIREBALL) return
        val section = this.mainConfig.fireballSection
        scheduleBlockRemoval(
            this.plugin,
            this.matchManager,
            event,
            this.fireballOwners,
            section.affectedBlocks,
            section.knockback,
            section.radius,
        )
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
package net.serlith.bedwarstnt.listeners

import club.frozed.frost.Frost
import club.frozed.frost.managers.MatchManager
import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.configs.MainConfig
import net.serlith.bedwarstnt.util.Reloadable
import net.serlith.bedwarstnt.util.scheduleBlockRemoval
import net.serlith.bedwarstnt.util.sendCooldownTitle
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class TntListener (
    private val plugin: BedwarsTNT,
    private val mainConfig: MainConfig,
) : Listener, Reloadable(10) {

    private lateinit var spawnLocation: Location
    private val lastUsed = mutableMapOf<UUID, Long>()

    private var matchManager: MatchManager? = null
    private val tntOwners: MutableMap<UUID, UUID> = mutableMapOf()

    init {
        val frost = this.plugin.server.pluginManager.getPlugin("Frost")
        frost?.let {
            this.matchManager = (it as Frost).managerHandler.matchManager
        }

        this.plugin.server.pluginManager.registerEvents(this, plugin)
        this.reload()
    }

    @Suppress("Deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return
        val block = event.block
        if (block.type != Material.TNT) return
        if (block.location.distance(this.spawnLocation) < this.mainConfig.globalSection.spawnProtection) {
            event.isCancelled = true
            return
        }

        val player = event.player
        val current = System.currentTimeMillis()
        this.lastUsed[player.uniqueId]?.let {
            val time = current - it
            val cd = this.mainConfig.tntSection.cooldown
            if (time >= cd) return@let
            sendCooldownTitle(player, (cd - time.toFloat()), this.plugin.mainConfig.globalSection, this.plugin.messagesConfig.messagesSection)
            event.isCancelled = true
            return
        }
        this.lastUsed[player.uniqueId] = current

        val primedTnt = event.player.world.spawn(block.location.add(0.5, 0.5, 0.5), TNTPrimed::class.java)
        block.type = Material.AIR
        primedTnt.fuseTicks = 50

        this.tntOwners[primedTnt.uniqueId] = player.uniqueId
    }

    @EventHandler
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (event.cause != DamageCause.ENTITY_EXPLOSION && event.cause != DamageCause.BLOCK_EXPLOSION) return
        if (event.entity !is Player) return
        event.damage *= this.plugin.mainConfig.globalSection.damageMultiplier
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.entityType != EntityType.PRIMED_TNT) return
        val section = this.mainConfig.tntSection
        scheduleBlockRemoval(
            this.plugin,
            this.matchManager,
            event,
            this.tntOwners,
            section.affectedBlocks,
            section.knockback,
            section.radius,
        )
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        this.lastUsed.remove(event.player.uniqueId)
        this.tntOwners.entries.removeIf { it.value == event.player.uniqueId }
    }

    override fun reload() {
        this.spawnLocation = Location(this.mainConfig.globalSection.world, 0.0, 0.0, 0.0)
    }

}
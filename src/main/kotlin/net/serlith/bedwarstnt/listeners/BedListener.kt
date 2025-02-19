package net.serlith.bedwarstnt.listeners

import club.frozed.frost.Frost
import club.frozed.frost.managers.MatchManager
import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.configs.MainConfig
import net.serlith.bedwarstnt.util.Reloadable
import net.serlith.bedwarstnt.util.scheduleBlockRemovalBlock
import net.serlith.bedwarstnt.util.sendCooldownTitle
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID

class BedListener (
    private val plugin: BedwarsTNT,
    private val mainConfig: MainConfig,
) : Listener, Reloadable(10) {

    private lateinit var spawnLocation: Location
    private val lastUsed = mutableMapOf<UUID, Long>()

    private val matchManager: MatchManager
    private val bedOwners: MutableMap<Location, UUID> = mutableMapOf()

    init {
        this.plugin.server.pluginManager.getPlugin("Frost")!!.let {
            this.matchManager = (it as Frost).managerHandler.matchManager
        }

        this.plugin.server.pluginManager.registerEvents(this, plugin)
        this.reload()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.isCancelled) return
        val block = event.block
        if (block.type != Material.BED_BLOCK && block.type != Material.BED) return
        if (block.location.distance(this.spawnLocation) < this.mainConfig.globalSection.spawnProtection) {
            event.isCancelled = true
            return
        }

        val player = event.player
        val current = System.currentTimeMillis()
        this.lastUsed[player.uniqueId]?.let {
            val time = current - it
            val cd = this.mainConfig.bedSection.cooldown
            if (time >= cd) return@let
            sendCooldownTitle(player, time.toFloat(), this.plugin.mainConfig.globalSection, this.plugin.messagesConfig.messagesSection)
            event.isCancelled = true
            return
        }
        this.lastUsed[player.uniqueId] = current
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.isCancelled) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock
        if (block.type != Material.BED_BLOCK && block.type != Material.BED) return
        if (block.world.environment == World.Environment.NORMAL) return
        this.bedOwners[block.location] = event.player.uniqueId
    }

    @EventHandler
    fun onEntityDamageByEntityEvent(event: EntityDamageByBlockEvent) {
        if (event.cause != DamageCause.ENTITY_EXPLOSION) return
        if (event.entity !is Player) return
        if (event.damager.type != Material.BED_BLOCK && event.damager.type != Material.BED) return
        event.damage *= this.plugin.mainConfig.bedSection.damageMultiplier
    }

    @EventHandler
    fun onBlockExplode(event: BlockExplodeEvent) {
        if (event.block.type != Material.BED_BLOCK && event.block.type != Material.BED) return
        val section = this.mainConfig.bedSection
        scheduleBlockRemovalBlock(
            this.plugin,
            this.matchManager,
            event,
            bedOwners,
            section.affectedBlocks,
            section.knockback,
            section.radius
        )
    }

    override fun reload() {
        this.spawnLocation = Location(this.mainConfig.globalSection.world, 0.0, 0.0, 0.0)
    }

}
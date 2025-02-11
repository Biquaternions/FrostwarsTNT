package net.serlith.bedwarstnt.listeners

import com.cryptomorin.xseries.messages.Titles
import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.configs.MainConfig
import net.serlith.bedwarstnt.util.Reloadable
import net.serlith.bedwarstnt.util.extraMomentum
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
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

    init {
        this.plugin.server.pluginManager.registerEvents(this, plugin)
        this.reload()
    }

    @Suppress("Deprecation")
    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
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
            Titles(this.plugin.messagesConfig.messagesSection.tntCooldown.replace("<seconds>", "%.1f".format((cd - time.toFloat()) / 1000)),
                "",
                this.plugin.mainConfig.globalSection.title.fadeIn,
                this.plugin.mainConfig.globalSection.title.stay,
                this.plugin.mainConfig.globalSection.title.fadeOut
            ).send(player)
            event.isCancelled = true
            return
        }
        this.lastUsed[player.uniqueId] = current

        val primedTnt = event.player.world.spawn(block.location.add(0.5, 0.5, 0.5), TNTPrimed::class.java)
        block.type = Material.AIR
        primedTnt.fuseTicks = 50
    }

    @EventHandler
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        if (event.cause != DamageCause.ENTITY_EXPLOSION && event.cause != DamageCause.BLOCK_EXPLOSION) return
        if (event.entity !is Player) return
        event.damage *= this.plugin.mainConfig.globalSection.damageMultiplier
    }

    @EventHandler
    fun onEntityExplode(event: EntityExplodeEvent) {
        val entity = event.entity
        if (event.entityType != EntityType.PRIMED_TNT) return

        val section = this.mainConfig.tntSection
        event.blockList().toList().forEach { block ->
            if (block.type !in section.affectedBlocks) {
                event.blockList().remove(block)
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
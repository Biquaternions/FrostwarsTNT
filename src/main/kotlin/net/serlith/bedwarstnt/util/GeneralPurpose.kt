package net.serlith.bedwarstnt.util

import club.frozed.frost.managers.MatchManager
import com.cryptomorin.xseries.messages.Titles
import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.configs.MainConfig.GlobalConfigSection
import net.serlith.bedwarstnt.configs.MessagesConfig.MessagesConfigSection
import net.serlith.bedwarstnt.configs.modules.KnockbackConfigSection
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.util.Vector
import java.util.UUID

fun extraMomentum(
    target: Location,
    source: Location,
    horizontal: Double,
    vertical: Double,
    multiplier: Double
): Vector {
    var vector = target.toVector().subtract(source.toVector())
    val extra = Vector(
        horizontal * (vector.x / (vector.x + vector.z)),
        vertical,
        horizontal * (vector.z / (vector.x + vector.z)),
    )

    vector = vector.add(extra)
    val length = vector.length()
    vector = vector.normalize()

    return vector.multiply(multiplier / length)
}

fun scheduleBlockRemoval(
    plugin: BedwarsTNT,
    matchManager: MatchManager?,
    event: EntityExplodeEvent,
    explosiveOwners: MutableMap<UUID, UUID>,
    affectedMaterials: Collection<Material>,
    knockback: KnockbackConfigSection,
    radius: Double,
) {
    val blocks = event.blockList().toList()
    event.blockList().clear()

    plugin.server.scheduler.runTaskAsynchronously(plugin) {
        val match = explosiveOwners[event.entity.uniqueId]?.let {
            matchManager?.getMatch(it)
        }
        blocks.filter { it.type in affectedMaterials && match?.isBreakable(it) == true }.forEach { block ->
            block.type = Material.AIR
        }
        explosiveOwners.remove(event.entity.uniqueId)
    }

    event.entity.world.players.forEach players@ { player ->
        if (player.location.distance(event.entity.location) > radius) return@players
        player.velocity = player.velocity.add(extraMomentum(
            player.location,
            event.entity.location,
            knockback.horizontalExtra,
            knockback.verticalExtra,
            knockback.multiplier
        ))
        if (player.velocity.length() > knockback.speedLimit) {
            player.velocity = player.velocity.normalize().multiply(knockback.speedLimit)
        }
    }
}

fun scheduleBlockRemovalBlock(
    plugin: BedwarsTNT,
    matchManager: MatchManager?,
    event: BlockExplodeEvent,
    explosiveOwners: MutableMap<Location, UUID>,
    affectedMaterials: Collection<Material>,
    knockback: KnockbackConfigSection,
    radius: Double,
) {
    val blocks = event.blockList().toList()
    event.blockList().clear()

    plugin.server.scheduler.runTaskAsynchronously(plugin) {
        val match = explosiveOwners[event.block.location]?.let {
            matchManager?.getMatch(it)
        }
        blocks.filter { it.type in affectedMaterials && match?.isBreakable(it) == true }.forEach { block ->
            block.type = Material.AIR
        }
        explosiveOwners.remove(event.block.location)
    }

    event.block.world.players.forEach players@ { player ->
        if (player.location.distance(event.block.location) > radius) return@players
        player.velocity = player.velocity.add(extraMomentum(
            player.location,
            event.block.location,
            knockback.horizontalExtra,
            knockback.verticalExtra,
            knockback.multiplier
        ))
        if (player.velocity.length() > knockback.speedLimit) {
            player.velocity = player.velocity.normalize().multiply(knockback.speedLimit)
        }
    }
}

fun sendCooldownTitle(
    player: Player,
    cooldown: Float,
    globalSection: GlobalConfigSection,
    messagesSection: MessagesConfigSection,
) {
    Titles(messagesSection.fireballCooldown.replace("<seconds>", "%.1f".format(cooldown / 1000)),
        "",
        globalSection.title.fadeIn,
        globalSection.title.stay,
        globalSection.title.fadeOut
    ).send(player)
}

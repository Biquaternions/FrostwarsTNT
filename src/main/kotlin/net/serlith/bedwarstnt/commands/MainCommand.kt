package net.serlith.bedwarstnt.commands

import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.exceptions.ReloadException
import net.serlith.bedwarstnt.util.Reloadable
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginIdentifiableCommand
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.SimplePluginManager

class MainCommand (
    private val plugin: BedwarsTNT,
) : Command("bedwarstnt"), PluginIdentifiableCommand {

    private val empty = emptyList<String>()
    private val options = listOf("reload", "details")
    private val detailsOptions = listOf("knockback", "other")

    init {
        this.permission = "bedwarstnt.admin"
        this.usage = "/bedwarstnt [reload | details]"
        this.description = "Main BedwarsTNT administration command"
        this.permissionMessage = this.plugin.prefix + this.plugin.messagesConfig.messagesSection.noPermission

        val field = SimplePluginManager::class.java.declaredFields.first { it.type == SimpleCommandMap::class.java }.also { it.isAccessible = true }
        val commandMap = field.get(this.plugin.server.pluginManager) as SimpleCommandMap
        commandMap.register("bedwarstnt", this)

    }

    override fun execute(
        sender: CommandSender,
        commandLabel: String,
        args: Array<out String?>
    ): Boolean {
        if (args.isEmpty() || args.size > 2) {
            sender.sendMessage(this.plugin.prefix + this.plugin.messagesConfig.messagesSection.invalidCommand)
            return false
        }

        when (args[0]) {
            "reload" -> {
                try {
                    Reloadable.reload()
                    sender.sendMessage(this.plugin.prefix + this.plugin.messagesConfig.messagesSection.reloadSuccessful)
                } catch (e: ReloadException) {
                    sender.sendMessage(this.plugin.prefix + this.plugin.messagesConfig.messagesSection.reloadFailed.replace("<file>", e.name))
                    return false
                }
            }
            "details" -> {
                if (args.size != 2) {
                    sender.sendMessage(this.plugin.prefix + this.plugin.messagesConfig.messagesSection.invalidCommand)
                    return false
                }
                when (args[1]) {
                    "knockback" -> {
                        this.plugin.messagesConfig.messagesSection.knockbackDetails.forEach {
                            sender.sendMessage(it
                                .replace("<tnt-h-extra>", "%.2f".format(this.plugin.mainConfig.tntSection.knockback.horizontalExtra))
                                .replace("<tnt-v-extra>", "%.2f".format(this.plugin.mainConfig.tntSection.knockback.verticalExtra))
                                .replace("<tnt-multiplier>", "%.2f".format(this.plugin.mainConfig.tntSection.knockback.multiplier))
                                .replace("<tnt-speed-limit>", "%.2f".format(this.plugin.mainConfig.tntSection.knockback.speedLimit))
                                .replace("<fireball-h-extra>", "%.2f".format(this.plugin.mainConfig.fireballSection.knockback.horizontalExtra))
                                .replace("<fireball-v-extra>", "%.2f".format(this.plugin.mainConfig.fireballSection.knockback.verticalExtra))
                                .replace("<fireball-multiplier>", "%.2f".format(this.plugin.mainConfig.fireballSection.knockback.multiplier))
                                .replace("<fireball-speed-limit>", "%.2f".format(this.plugin.mainConfig.fireballSection.knockback.speedLimit))
                                .replace("<bed-h-extra>", "%.2f".format(this.plugin.mainConfig.bedSection.knockback.horizontalExtra))
                                .replace("<bed-v-extra>", "%.2f".format(this.plugin.mainConfig.bedSection.knockback.verticalExtra))
                                .replace("<bed-multiplier>", "%.2f".format(this.plugin.mainConfig.bedSection.knockback.multiplier))
                                .replace("<bed-speed-limit>", "%.2f".format(this.plugin.mainConfig.bedSection.knockback.speedLimit))
                            )
                        }
                        return true
                    }
                    "other" -> {
                        this.plugin.messagesConfig.messagesSection.otherDetails.forEach {
                            sender.sendMessage(it
                                .replace("<spawn-protection>", this.plugin.mainConfig.globalSection.spawnProtection.toString())
                                .replace("<tnt-radius>", "%.2f".format(this.plugin.mainConfig.tntSection.radius))
                                .replace("<tnt-cooldown>", "${this.plugin.mainConfig.tntSection.cooldown}ms")
                                .replace("<tnt-multiplier>", "%.2f".format(this.plugin.mainConfig.tntSection.damageMultiplier))
                                .replace("<fireball-radius>", "%.2f".format(this.plugin.mainConfig.fireballSection.radius))
                                .replace("<fireball-yield>", "%.2f".format(this.plugin.mainConfig.fireballSection.yield))
                                .replace("<fireball-speed>", "%.2f".format(this.plugin.mainConfig.fireballSection.speed))
                                .replace("<fireball-despawn>", this.plugin.mainConfig.fireballSection.despawnDistance.toString())
                                .replace("<fireball-cooldown>", "${this.plugin.mainConfig.fireballSection.cooldown}ms")
                                .replace("<fireball-multiplier>", "%.2f".format(this.plugin.mainConfig.fireballSection.damageMultiplier))
                                .replace("<bed-radius>", "%.2f".format(this.plugin.mainConfig.bedSection.radius))
                                .replace("<bed-cooldown>", "${this.plugin.mainConfig.bedSection.cooldown}ms")
                                .replace("<bed-multiplier>", "%.2f".format(this.plugin.mainConfig.bedSection.damageMultiplier))
                            )
                        }
                        return true
                    }
                    else -> {
                        sender.sendMessage(this.plugin.prefix + this.plugin.messagesConfig.messagesSection.invalidCommand)
                        return false
                    }
                }
            }
            else -> {
                sender.sendMessage(this.plugin.prefix + this.plugin.messagesConfig.messagesSection.invalidCommand)
                return false
            }
        }

        return true
    }

    override fun tabComplete(sender: CommandSender?, alias: String?, args: Array<out String>): List<String?>? {
        if (args.isEmpty()) return this.empty
        if (args.size == 1) return this.options.filter { it.startsWith(args[0]) }
        if (args[0] == "details" && args.size == 2) return this.detailsOptions.filter { it.startsWith(args[1]) }
        return this.empty
    }

    override fun getPlugin(): Plugin? = this.plugin
}
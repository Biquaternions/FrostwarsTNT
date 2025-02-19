package net.serlith.bedwarstnt

import net.serlith.bedwarstnt.commands.MainCommand
import net.serlith.bedwarstnt.configs.MainConfig
import net.serlith.bedwarstnt.configs.MessagesConfig
import net.serlith.bedwarstnt.listeners.BedListener
import net.serlith.bedwarstnt.listeners.FireballListener
import net.serlith.bedwarstnt.listeners.TntListener
import org.bukkit.plugin.java.JavaPlugin

class BedwarsTNT : JavaPlugin() {

    lateinit var mainConfig: MainConfig
        private set
    lateinit var messagesConfig: MessagesConfig
        private set

    val prefix = "§7[§cBedwars§4TNT§7]§r "

    override fun onEnable() {
        this.mainConfig = MainConfig(this)
        this.messagesConfig = MessagesConfig(this)

        TntListener(this, this.mainConfig)
        FireballListener(this, this.mainConfig)
        BedListener(this, this.mainConfig)

        MainCommand(this)

        this.printBanner()
    }

    private fun printBanner() {
        this.server.consoleSender.sendMessage("§c ________________")
        this.server.consoleSender.sendMessage("§c|                |§4      ____           _                          _______ _   _ _______")
        this.server.consoleSender.sendMessage("§c|§f----------------§c|§4     |  _ \\         | |                        |__   __| \\ | |__   __|")
        this.server.consoleSender.sendMessage("§c|§f  ___      ___  §c|§4     | |_) | ___  __| |_      ____ _ _ __ ___     | |  |  \\| |  | |")
        this.server.consoleSender.sendMessage("§c|§f   |  |\\ |  |   §c|§4     |  _ < / _ \\/ _` \\ \\ /\\ / / _` | '__/ __|    | |  | . ` |  | |")
        this.server.consoleSender.sendMessage("§c|§f   |  | \\|  |   §c|§4     | |_) |  __/ (_| |\\ V  V / (_| | |  \\__ \\    | |  | |\\  |  | |")
        this.server.consoleSender.sendMessage("§c|§f                §c|§4     |____/ \\___|\\__,_| \\_/\\_/ \\__,_|_|  |___/    |_|  |_| \\_|  |_|")
        this.server.consoleSender.sendMessage("§c|§f----------------§c|§4                                                      §6by Biquaternions")
        this.server.consoleSender.sendMessage("§c|________________|")
    }

}

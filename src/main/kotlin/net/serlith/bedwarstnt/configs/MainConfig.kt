package net.serlith.bedwarstnt.configs

import net.serlith.bedwarstnt.BedwarsTNT
import net.serlith.bedwarstnt.configs.modules.KnockbackConfigSection
import net.serlith.bedwarstnt.configs.modules.TitleConfigSection
import net.serlith.bedwarstnt.exceptions.ReloadException
import net.serlith.bedwarstnt.util.Reloadable
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.jvm.Throws

class MainConfig (
    private val plugin: BedwarsTNT
) : Reloadable(0) {

    private val fileName = "config.yml"
    private val configPath = File(this.plugin.dataFolder, fileName)
    private var config = YamlConfiguration.loadConfiguration(configPath)

    init {
        this.reload()
    }

    lateinit var globalSection: GlobalConfigSection
        private set
    class GlobalConfigSection (
        val world: World,
        val spawnProtection: Long,
        val title: TitleConfigSection,
    ) {
        companion object {
            @JvmStatic
            fun deserialize(section: ConfigurationSection) = GlobalConfigSection(
                Bukkit.getWorld(section.getString("world")),
                section.getLong("spawn-protection"),
                TitleConfigSection.deserialize(section.getConfigurationSection("title")),
            )
        }
    }

    lateinit var tntSection: TntConfigSection
        private set
    class TntConfigSection(
        val radius: Double,
        val cooldown: Long,
        val damageMultiplier: Double,
        val affectedBlocks: List<Material>,
        val knockback: KnockbackConfigSection
    ) {
        companion object {
            @JvmStatic
            fun deserialize(section: ConfigurationSection) = TntConfigSection(
                section.getDouble("radius"),
                section.getLong("cooldown"),
                section.getDouble("damage-multiplier"),
                section.getStringList("affected-blocks")!!.map { Material.getMaterial(it) },
                KnockbackConfigSection.deserialize(section.getConfigurationSection("knockback"))
            )
        }
    }

    lateinit var fireballSection: FireballConfigSection
        private set

    class FireballConfigSection(
        val radius: Double,
        val yield: Float,
        val speed: Double,
        val despawnDistance: Long,
        val cooldown: Long,
        val damageMultiplier: Double,
        val affectedBlocks: List<Material>,
        val knockback: KnockbackConfigSection
    ) {
        companion object {
            @JvmStatic
            fun deserialize(section: ConfigurationSection) = FireballConfigSection(
                section.getDouble("radius"),
                section.getDouble("yield").toFloat(),
                section.getDouble("speed"),
                section.getLong("despawn-distance"),
                section.getLong("cooldown"),
                section.getDouble("damage-multiplier"),
                section.getStringList("affected-blocks")!!.map { Material.getMaterial(it) },
                KnockbackConfigSection.deserialize(section.getConfigurationSection("knockback"))
            )
        }
    }

    lateinit var bedSection: BedConfigurationSection
        private set

    class BedConfigurationSection(
        val radius: Double,
        val cooldown: Long,
        val damageMultiplier: Double,
        val affectedBlocks: List<Material>,
        val knockback: KnockbackConfigSection,
    ) {
        companion object {
            @JvmStatic
            fun deserialize(section: ConfigurationSection) = BedConfigurationSection(
                section.getDouble("radius"),
                section.getLong("cooldown"),
                section.getDouble("damage-multiplier"),
                section.getStringList("affected-blocks")!!.map { Material.getMaterial(it) },
                KnockbackConfigSection.deserialize(section.getConfigurationSection("knockback"))
            )
        }
    }

    @Throws(ReloadException::class)
    override fun reload() {
        if (!this.configPath.exists()) {
            this.plugin.saveResource(this.fileName, false)
        }

        try {
            this.config = YamlConfiguration.loadConfiguration(this.configPath)
            this.globalSection = GlobalConfigSection.deserialize(this.config.getConfigurationSection("global"))
            this.tntSection = TntConfigSection.deserialize(this.config.getConfigurationSection("tnt"))
            this.fireballSection = FireballConfigSection.deserialize(this.config.getConfigurationSection("fireball"))
            this.bedSection = BedConfigurationSection.deserialize(this.config.getConfigurationSection("bed"))
        } catch (_: Exception) {
            throw ReloadException(this.fileName)
        }

    }

}
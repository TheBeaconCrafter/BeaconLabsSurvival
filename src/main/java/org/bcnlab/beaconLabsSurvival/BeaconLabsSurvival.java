package org.bcnlab.beaconLabsSurvival;

import org.bcnlab.beaconLabsSurvival.command.LabsSurvivalCommand;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BeaconLabsSurvival extends JavaPlugin {

    private String pluginPrefix;
    private String pluginVersion = "1.0";
    private String noPermsMessage = "&cYou do not have permission to use this command.";

    @Override
    public void onEnable() {
        // Config
        createDefaultConfig();
        loadConfig();

        // Commands
        getCommand("labssurvival").setExecutor(new LabsSurvivalCommand(this));

        // Finished
        getLogger().info("BeaconLabsSurvival has been enabled!");
    }

    @Override
    public void onDisable() {
        // Finished
        getLogger().info("BeaconLabsSurvival has been disabled!");
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', pluginPrefix);
    }

    public String getVersion() {
        return pluginVersion;
    }

    public String getNoPermsMessage() {
        return ChatColor.translateAlternateColorCodes('&', noPermsMessage);
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        pluginPrefix = config.getString("plugin-prefix", "&6BeaconLabs &8» ");
    }

    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.addDefault("plugin-prefix", "&6BeaconLabs &8» ");
        saveConfig();
    }
}

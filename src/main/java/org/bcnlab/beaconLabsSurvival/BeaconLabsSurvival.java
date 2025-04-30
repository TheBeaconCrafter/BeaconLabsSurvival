package org.bcnlab.beaconLabsSurvival;

import org.bcnlab.beaconLabsSurvival.command.LabsSurvivalCommand;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;
import java.util.HashMap;

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
        org.bcnlab.beaconLabsSurvival.command.HomesCommand homesCommand = new org.bcnlab.beaconLabsSurvival.command.HomesCommand(this);
        getCommand("homes").setExecutor(homesCommand);
        getCommand("sethome").setExecutor(homesCommand);
        getCommand("home").setExecutor(homesCommand);
        getCommand("delhome").setExecutor(homesCommand);
        getCommand("home").setTabCompleter(homesCommand);
        getCommand("delhome").setTabCompleter(homesCommand);
        getCommand("sethome").setTabCompleter(homesCommand);

        // Chunk commands and listener
        org.bcnlab.beaconLabsSurvival.db.ChunksDatabase chunksDb = new org.bcnlab.beaconLabsSurvival.db.ChunksDatabase(new java.io.File(getDataFolder(), "chunks.db").getAbsolutePath());
        org.bcnlab.beaconLabsSurvival.command.ChunkMainCommand chunkMainCommand = new org.bcnlab.beaconLabsSurvival.command.ChunkMainCommand(this, chunksDb);
        getCommand("chunk").setExecutor(chunkMainCommand);
        getCommand("chunk").setTabCompleter(chunkMainCommand);
        getCommand("bypassbuild").setExecutor(new org.bcnlab.beaconLabsSurvival.command.BypassBuildCommand(this));
        getServer().getPluginManager().registerEvents(new org.bcnlab.beaconLabsSurvival.listener.ChunkProtectionListener(this, chunksDb), this);
        getServer().getPluginManager().registerEvents(new org.bcnlab.beaconLabsSurvival.listener.ChunkFlowTntMinecartListener(this, chunksDb), this);

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

    private int defaultMaxHomes;
    private Map<String, Integer> permissionHomes = new HashMap<>();
    private int defaultMaxChunks;
    private Map<String, Integer> permissionChunks = new HashMap<>();
    private String homesBypassPerm;
    private String chunksBypassPerm;
    private int homeTeleportDelay;

    public int getDefaultMaxChunks() { return defaultMaxChunks; }
    public Map<String, Integer> getPermissionChunks() { return permissionChunks; }
    public String getChunksBypassPerm() { return chunksBypassPerm; }
    public String getHomesBypassPerm() { return homesBypassPerm; }
    private void loadConfig() {
        FileConfiguration config = getConfig();
        pluginPrefix = config.getString("plugin-prefix", "&6BeaconLabs &8» ");
        defaultMaxHomes = config.getInt("homes.default-max-homes", 3);
        permissionHomes.clear();
        if (config.isConfigurationSection("homes.permission-homes")) {
            for (String perm : config.getConfigurationSection("homes.permission-homes").getKeys(false)) {
                int value = config.getInt("homes.permission-homes." + perm, defaultMaxHomes);
                permissionHomes.put(perm, value);
            }
        }
        // Chunks config
        defaultMaxChunks = config.getInt("chunks.default-max-chunks", 5);
        permissionChunks.clear();
        if (config.isConfigurationSection("chunks.permission-chunks")) {
            for (String perm : config.getConfigurationSection("chunks.permission-chunks").getKeys(false)) {
                int value = config.getInt("chunks.permission-chunks." + perm, defaultMaxChunks);
                permissionChunks.put(perm, value);
            }
        }
        homesBypassPerm = config.getString("homes.bypass-perm", "beaconlabs.homes.bypass");
        homeTeleportDelay = config.getInt("homes.teleport-delay", 5);
        chunksBypassPerm = config.getString("chunks.bypass-perm", "beaconlabs.chunks.bypass");
    }

    public int getMaxHomes(org.bukkit.entity.Player player) {
        int max = defaultMaxHomes;
        for (Map.Entry<String, Integer> entry : permissionHomes.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                max = Math.max(max, entry.getValue());
            }
        }
        return max;
    }

    private void createDefaultConfig() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        config.addDefault("plugin-prefix", "&6BeaconLabs &8» ");
        config.addDefault("homes.default-max-homes", 3);
        config.addDefault("homes.permission-homes.example.permission.vip", 5);
        config.addDefault("homes.permission-homes.example.permission.elite", 10);
        config.addDefault("homes.bypass-perm", "beaconlabs.homes.bypass");
        config.addDefault("homes.teleport-delay", 5);
        // Chunks config
        config.addDefault("chunks.default-max-chunks", 5);
        config.addDefault("chunks.permission-chunks.example.permission.vip", 10);
        config.addDefault("chunks.permission-chunks.example.permission.elite", 20);
        config.addDefault("chunks.bypass-perm", "beaconlabs.chunks.bypass");
        saveConfig();
    }

    public int getHomeTeleportDelay() { return homeTeleportDelay; }
}

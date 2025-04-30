package org.bcnlab.beaconLabsSurvival.command;

import org.bcnlab.beaconLabsSurvival.BeaconLabsSurvival;
import org.bcnlab.beaconLabsSurvival.db.ChunksDatabase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ChunkMainCommand implements CommandExecutor, TabCompleter {
    private final BeaconLabsSurvival plugin;
    private final ChunksDatabase db;

    private static final List<String> SUBCOMMANDS = Arrays.asList("info", "claim", "unclaim", "trust", "untrust", "notify", "deny", "allow");

    public ChunkMainCommand(BeaconLabsSurvival plugin, ChunksDatabase db) {
        this.plugin = plugin;
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        Chunk chunk = player.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID uuid = player.getUniqueId();
        if (args.length == 0) {
            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "/chunk <info|claim|unclaim|trust|untrust>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "info":
                UUID owner = db.getChunkOwner(world, x, z);
                List<UUID> trustedList = db.getTrusted(world, x, z);
                String ownerName = owner == null ? "None" : Bukkit.getOfflinePlayer(owner).getName();
                player.sendMessage(plugin.getPrefix() + ChatColor.AQUA + "Chunk Info:");
                player.sendMessage(ChatColor.YELLOW + "Owner: " + ownerName);
                if (!trustedList.isEmpty()) {
                    String trustedNames = trustedList.stream().map(u -> Bukkit.getOfflinePlayer(u).getName()).collect(Collectors.joining(", "));
                    player.sendMessage(ChatColor.YELLOW + "Trusted: " + trustedNames);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Trusted: none");
                }
                // Denied players
                List<String> deniedList = db.getDenied(world, x, z);
                if (!deniedList.isEmpty()) {
                    String deniedNames = deniedList.stream().map(d -> d.equals("*") ? "* (all)" : Bukkit.getOfflinePlayer(UUID.fromString(d)).getName()).collect(Collectors.joining(", "));
                    player.sendMessage(ChatColor.YELLOW + "Denied: " + deniedNames);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Denied: none");
                }
                showChunkBorder(player, chunk);
                break;
            case "claim":
                if (db.isChunkClaimed(world, x, z)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "This chunk is already claimed.");
                } else {
                    // Check chunk limit unless bypass
                    if (!player.hasPermission(plugin.getChunksBypassPerm())) {
                        int max = plugin.getDefaultMaxChunks();
                        for (Map.Entry<String, Integer> entry : plugin.getPermissionChunks().entrySet()) {
                            if (player.hasPermission(entry.getKey())) {
                                max = Math.max(max, entry.getValue());
                            }
                        }
                        int claimed = db.getClaimedCount(uuid);
                        if (claimed >= max) {
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You have reached your maximum claimed chunks (" + max + ").");
                            return true;
                        }
                    }
                    if (db.claimChunk(world, x, z, uuid)) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Chunk claimed!");
                    } else {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to claim chunk.");
                    }
                }
                break;
            case "unclaim":
                owner = db.getChunkOwner(world, x, z);
                if (owner == null) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "This chunk is not claimed.");
                } else if (!owner.equals(uuid) && !player.hasPermission("beaconlabs.chunk.admin")) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not own this chunk.");
                } else {
                    if (db.unclaimChunk(world, x, z, owner)) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Chunk unclaimed.");
                    } else {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to unclaim chunk.");
                    }
                }
                break;
            case "trust":
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunk trust <player>");
                    return true;
                }
                owner = db.getChunkOwner(world, x, z);
                if (owner == null || !owner.equals(uuid)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not own this chunk.");
                    return true;
                }
                Player trusted = Bukkit.getPlayer(args[1]);
                if (trusted == null) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Player not found.");
                    return true;
                }
                if (db.trustPlayer(world, x, z, trusted.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + trusted.getName() + " is now trusted in this chunk.");
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to trust player.");
                }
                break;
            case "untrust":
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunk untrust <player>");
                    return true;
                }
                owner = db.getChunkOwner(world, x, z);
                if (owner == null || !owner.equals(uuid)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not own this chunk.");
                    return true;
                }
                trusted = Bukkit.getPlayer(args[1]);
                if (trusted == null) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Player not found.");
                    return true;
                }
                if (db.untrustPlayer(world, x, z, trusted.getUniqueId())) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + trusted.getName() + " is no longer trusted in this chunk.");
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to untrust player.");
                }
                break;
            case "notify":
                boolean enabled = db.isNotifyEnabled(uuid);
                if (args.length == 1) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Chunk entry notifications are currently " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF") + ChatColor.YELLOW + ". Use /chunk notify on|off to change.");
                } else if (args.length == 2) {
                    String arg = args[1].toLowerCase();
                    boolean set;
                    if (arg.equals("on")) set = true;
                    else if (arg.equals("off")) set = false;
                    else {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunk notify [on|off]");
                        return true;
                    }
                    db.setNotifyEnabled(uuid, set);
                    player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Chunk entry notifications are now " + (set ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF") + ChatColor.YELLOW + ".");
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunk notify [on|off]");
                }
                break;
            case "deny":
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunk deny <player|*>");
                    return true;
                }
                owner = db.getChunkOwner(world, x, z);
                if (owner == null || !owner.equals(uuid)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not own this chunk.");
                    return true;
                }
                String denyTarget = args[1].equals("*") ? "*" : Bukkit.getOfflinePlayer(args[1]).getUniqueId().toString();
                if (db.denyPlayer(world, x, z, denyTarget)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + (args[1].equals("*") ? "All players are now denied in this chunk." : args[1] + " is now denied in this chunk."));
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to deny player.");
                }
                break;
            case "allow":
                if (args.length < 2) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunk allow <player|*>");
                    return true;
                }
                owner = db.getChunkOwner(world, x, z);
                if (owner == null || !owner.equals(uuid)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not own this chunk.");
                    return true;
                }
                String allowTarget = args[1].equals("*") ? "*" : Bukkit.getOfflinePlayer(args[1]).getUniqueId().toString();
                if (db.allowPlayer(world, x, z, allowTarget)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + (args[1].equals("*") ? "All players are now allowed in this chunk." : args[1] + " is now allowed in this chunk."));
                } else {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to allow player.");
                }
                break;
            default:
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "/chunk <info|claim|unclaim|trust|untrust|notify|deny|allow>");
        }
        return true;
    }

    private void showChunkBorder(Player player, Chunk chunk) {
        World world = chunk.getWorld();
        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int y = player.getLocation().getBlockY();
        // Show particles for 5 seconds (100 ticks), resending every 10 ticks
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                for (int x = minX; x <= maxX; x++) {
                    player.spawnParticle(Particle.FLAME, x, y + 1, minZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, x, y + 1, maxZ, 1, 0, 0, 0, 0);
                }
                for (int z = minZ; z <= maxZ; z++) {
                    player.spawnParticle(Particle.FLAME, minX, y + 1, z, 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.FLAME, maxX, y + 1, z, 1, 0, 0, 0, 0);
                }
                ticks += 10;
                if (ticks >= 100) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if ((args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust")) && args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}

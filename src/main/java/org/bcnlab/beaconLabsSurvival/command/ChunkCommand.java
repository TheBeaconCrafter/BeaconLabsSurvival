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
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChunkCommand implements CommandExecutor {
    private final BeaconLabsSurvival plugin;
    private final ChunksDatabase db;

    public ChunkCommand(BeaconLabsSurvival plugin) {
        this.plugin = plugin;
        this.db = new ChunksDatabase(new java.io.File(plugin.getDataFolder(), "chunks.db").getAbsolutePath());
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
        String cmd = command.getName().toLowerCase();
        switch (cmd) {
            case "claim":
                if (db.isChunkClaimed(world, x, z)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "This chunk is already claimed.");
                } else {
                    if (db.claimChunk(world, x, z, uuid)) {
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Chunk claimed!");
                    } else {
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to claim chunk.");
                    }
                }
                break;
            case "unclaim":
                UUID owner = db.getChunkOwner(world, x, z);
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
            case "chunktrust":
                if (args.length < 1) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunktrust <player>");
                    return true;
                }
                owner = db.getChunkOwner(world, x, z);
                if (owner == null || !owner.equals(uuid)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not own this chunk.");
                    return true;
                }
                Player trusted = Bukkit.getPlayer(args[0]);
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
            case "chunkuntrust":
                if (args.length < 1) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /chunkuntrust <player>");
                    return true;
                }
                owner = db.getChunkOwner(world, x, z);
                if (owner == null || !owner.equals(uuid)) {
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You do not own this chunk.");
                    return true;
                }
                trusted = Bukkit.getPlayer(args[0]);
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
            case "chunkinfo":
                owner = db.getChunkOwner(world, x, z);
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
                // Draw chunk border with particles
                showChunkBorder(player, chunk);
                break;
        }
        return true;
    }

    private void showChunkBorder(Player player, Chunk chunk) {
        World world = chunk.getWorld();
        int minY = world.getMinHeight();
        int maxY = Math.min(minY + 3, world.getMaxHeight());
        int bx = chunk.getX() << 4;
        int bz = chunk.getZ() << 4;
        for (int x = bx; x < bx + 16; x++) {
            for (int z = bz; z < bz + 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    if (x == bx || x == bx + 15 || z == bz || z == bz + 15) {
                        player.spawnParticle(Particle.HAPPY_VILLAGER, x + 0.5, y + 0.1, z + 0.5, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    public ChunksDatabase getDb() {
        return db;
    }
}

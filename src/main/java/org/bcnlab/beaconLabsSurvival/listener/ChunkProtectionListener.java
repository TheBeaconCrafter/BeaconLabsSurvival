package org.bcnlab.beaconLabsSurvival.listener;

import org.bcnlab.beaconLabsSurvival.BeaconLabsSurvival;
import org.bcnlab.beaconLabsSurvival.db.ChunksDatabase;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Material;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class ChunkProtectionListener implements Listener {
    private final ChunksDatabase db;
    private final Map<UUID, String> lastChunk = new HashMap<>(); // player UUID -> world:x:z
    private final BeaconLabsSurvival plugin;

    public ChunkProtectionListener(BeaconLabsSurvival plugin, ChunksDatabase db) {
        this.plugin = plugin;
        this.db = db;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID owner = db.getChunkOwner(world, x, z);
        if (owner == null) return; // Not claimed
        if (owner.equals(player.getUniqueId())) return; // Owner
        if (db.getTrusted(world, x, z).contains(player.getUniqueId())) return; // Trusted
        if (org.bcnlab.beaconLabsSurvival.command.BypassBuildCommand.isBypassing(player)) return; // Bypass toggle
        if (db.isDenied(world, x, z, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§cYou are denied from building in this claimed chunk.");
            return;
        }
        event.setCancelled(true);
        player.sendMessage(plugin.getPrefix() + "§cYou cannot build in this claimed chunk.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID owner = db.getChunkOwner(world, x, z);
        if (owner == null) return; // Not claimed
        if (owner.equals(player.getUniqueId())) return; // Owner
        if (db.getTrusted(world, x, z).contains(player.getUniqueId())) return; // Trusted
        if (org.bcnlab.beaconLabsSurvival.command.BypassBuildCommand.isBypassing(player)) return; // Bypass toggle
        if (db.isDenied(world, x, z, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§cYou are denied from breaking blocks in this claimed chunk.");
            return;
        }
        event.setCancelled(true);
        player.sendMessage(plugin.getPrefix() + "§cYou cannot break blocks in this claimed chunk.");
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (event.getEntity() instanceof org.bukkit.entity.Player) return; // Don't interfere with PvP
        Chunk chunk = event.getEntity().getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID owner = db.getChunkOwner(world, x, z);
        if (owner == null) return;
        if (owner.equals(player.getUniqueId())) return;
        if (db.getTrusted(world, x, z).contains(player.getUniqueId())) return;
        if (org.bcnlab.beaconLabsSurvival.command.BypassBuildCommand.isBypassing(player)) return;
        if (db.isDenied(world, x, z, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§cYou are denied from damaging entities in this claimed chunk.");
            return;
        }
        event.setCancelled(true);
        player.sendMessage(plugin.getPrefix() + "§cYou cannot damage entities in this claimed chunk.");
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID owner = db.getChunkOwner(world, x, z);
        if (owner == null) return;
        if (owner.equals(player.getUniqueId())) return;
        if (db.getTrusted(world, x, z).contains(player.getUniqueId())) return;
        if (org.bcnlab.beaconLabsSurvival.command.BypassBuildCommand.isBypassing(player)) return;
        if (db.isDenied(world, x, z, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§cYou are denied from interacting with entities in this claimed chunk.");
            return;
        }
        // Prevent entity rotation or destruction (armor stands, item frames, etc)
        event.setCancelled(true);
        player.sendMessage(plugin.getPrefix() + "§cYou cannot interact with entities in this claimed chunk.");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk from = event.getFrom().getChunk();
        Chunk to = event.getTo().getChunk();
        if (from.getX() == to.getX() && from.getZ() == to.getZ() && from.getWorld().equals(to.getWorld())) return;
        String key = to.getWorld().getName() + ":" + to.getX() + ":" + to.getZ();
        UUID uuid = player.getUniqueId();
        // Only notify if preference is enabled
        if (!db.isNotifyEnabled(uuid)) return;
        UUID owner = db.getChunkOwner(to.getWorld().getName(), to.getX(), to.getZ());
        if (owner == null) return;
        String ownerName = org.bukkit.Bukkit.getOfflinePlayer(owner).getName();
        String last = lastChunk.get(uuid);
        if (key.equals(last)) return;
        lastChunk.put(uuid, key);
        if (owner.equals(uuid)) {
            player.sendMessage(plugin.getPrefix() + "§eYou entered your own claimed chunk.");
        } else {
            player.sendMessage(plugin.getPrefix() + "§eYou entered §b" + ownerName + "§e's claimed chunk.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastChunk.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlockClicked().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID owner = db.getChunkOwner(world, x, z);
        if (owner == null) return;
        if (owner.equals(player.getUniqueId())) return;
        if (db.getTrusted(world, x, z).contains(player.getUniqueId())) return;
        if (org.bcnlab.beaconLabsSurvival.command.BypassBuildCommand.isBypassing(player)) return;
        if (db.isDenied(world, x, z, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§cYou are denied from using buckets in this claimed chunk.");
            return;
        }
        Material bucket = event.getBucket();
        if (bucket == Material.LAVA_BUCKET || bucket == Material.WATER_BUCKET) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§cYou cannot place lava or water in this claimed chunk.");
        }
    }
}


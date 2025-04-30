package org.bcnlab.beaconLabsSurvival.listener;

import org.bcnlab.beaconLabsSurvival.BeaconLabsSurvival;
import org.bcnlab.beaconLabsSurvival.db.ChunksDatabase;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.List;

public class ChunkFlowTntMinecartListener implements Listener {
    private final ChunksDatabase db;
    private final BeaconLabsSurvival plugin;

    public ChunkFlowTntMinecartListener(BeaconLabsSurvival plugin, ChunksDatabase db) {
        this.plugin = plugin;
        this.db = db;
    }

    // Prevent water/lava from flowing into claimed chunks from untrusted sources
    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Block fromBlock = event.getBlock();
        Block toBlock = event.getToBlock();
        Material mat = fromBlock.getType();
        if (mat != Material.WATER && mat != Material.LAVA) return;
        Chunk fromChunk = fromBlock.getChunk();
        Chunk toChunk = toBlock.getChunk();
        String toWorld = toChunk.getWorld().getName();
        int toX = toChunk.getX();
        int toZ = toChunk.getZ();
        UUID owner = db.getChunkOwner(toWorld, toX, toZ);
        if (owner == null) return; // Not claimed
        // Only allow flow if source and destination are owned by same owner or trusted
        UUID fromOwner = db.getChunkOwner(fromChunk.getWorld().getName(), fromChunk.getX(), fromChunk.getZ());
        if (fromOwner != null && fromOwner.equals(owner)) return;
        // Could add trusted logic here if needed
        event.setCancelled(true);
    }

    // Prevent TNT and TNT minecart explosions in claimed chunks unless triggered by owner/trusted
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        Chunk chunk = entity.getLocation().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID owner = db.getChunkOwner(world, x, z);
        if (owner == null) return;
        if (entity instanceof ExplosiveMinecart || entity.getType() == EntityType.TNT || entity.getType() == EntityType.TNT_MINECART) {
            // Check if triggered by player (if possible)
            // For now, always cancel unless owner/trusted
            event.setCancelled(true);
        }
    }

    // Prevent fire placement in claimed chunks unless owner/trusted
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;
        Chunk chunk = event.getClickedBlock().getChunk();
        String world = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        UUID owner = db.getChunkOwner(world, x, z);
        if (owner == null) return;
        if (owner.equals(player.getUniqueId())) return;
        if (db.getTrusted(world, x, z).contains(player.getUniqueId())) return;
        if (player.hasPermission("beaconlabs.chunk.admin")) return;
        Material item = player.getInventory().getItemInMainHand().getType();
        if (item == Material.FLINT_AND_STEEL || item == Material.FIRE_CHARGE) {
            event.setCancelled(true);
            player.sendMessage(plugin.getPrefix() + "§cYou cannot ignite fire in this claimed chunk.");
        }
    }
}

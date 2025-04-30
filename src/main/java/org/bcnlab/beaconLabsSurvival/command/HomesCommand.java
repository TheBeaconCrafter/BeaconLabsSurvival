package org.bcnlab.beaconLabsSurvival.command;

import org.bcnlab.beaconLabsSurvival.BeaconLabsSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bcnlab.beaconLabsSurvival.db.HomesDatabase;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.command.TabCompleter;

public class HomesCommand implements CommandExecutor, TabCompleter {
    private final BeaconLabsSurvival plugin;
    private final HomesDatabase homesDb;

    public HomesCommand(BeaconLabsSurvival plugin) {
        this.plugin = plugin;
        this.homesDb = new HomesDatabase(new java.io.File(plugin.getDataFolder(), "homes.db").getAbsolutePath());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("homes")) {
            handleListHomes(player);
        } else if (cmd.equals("sethome")) {
            handleSetHome(player, args);
        } else if (cmd.equals("home")) {
            handleTeleportHome(player, args);
        } else if (cmd.equals("delhome")) {
            handleDeleteHome(player, args);
        }
        return true;
    }

    private void handleListHomes(Player player) {
        List<String> homes = homesDb.getHomes(player);
        if (homes.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "You have no homes set.");
        } else {
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Your homes: " + String.join(", ", homes));
        }
    }

    private void handleSetHome(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /sethome <name>");
            return;
        }
        String homeName = args[0].toLowerCase();
        List<String> homes = homesDb.getHomes(player);
        int maxHomes = plugin.getMaxHomes(player);
        if (!homes.contains(homeName) && homes.size() >= maxHomes) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "You have reached your maximum number of homes (" + maxHomes + ").");
            return;
        }
        homesDb.setHome(player, homeName, player.getLocation());
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Home '" + homeName + "' set!");
    }

    private void handleTeleportHome(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /home <name>");
            return;
        }
        String homeName = args[0].toLowerCase();
        Location loc = homesDb.getHome(player, homeName);
        if (loc == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "No home found with that name.");
            return;
        }
        int delay = plugin.getHomeTeleportDelay();
        String bypassPerm = plugin.getHomesBypassPerm();
        if (delay <= 0 || player.hasPermission(bypassPerm)) {
            player.teleport(loc);
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Teleported to home '" + homeName + "'.");
            return;
        }
        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Teleporting to home '" + homeName + "' in " + delay + " seconds. Don't move!");
        Location startLoc = player.getLocation();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Location currLoc = player.getLocation();
            if (currLoc.getBlockX() != startLoc.getBlockX() || currLoc.getBlockY() != startLoc.getBlockY() || currLoc.getBlockZ() != startLoc.getBlockZ()) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Teleport cancelled because you moved.");
                return;
            }
            player.teleport(loc);
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Teleported to home '" + homeName + "'.");
        }, delay * 20L);
    }

    private void handleDeleteHome(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Usage: /delhome <name>");
            return;
        }
        String homeName = args[0].toLowerCase();
        boolean deleted = homesDb.deleteHome(player, homeName);
        if (deleted) {
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Home '" + homeName + "' deleted.");
        } else {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "No home found with that name.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;
        String cmd = command.getName().toLowerCase();
        if ((cmd.equals("home") || cmd.equals("delhome")) && args.length == 1) {
            List<String> homes = homesDb.getHomes(player);
            String prefix = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String home : homes) {
                if (home.startsWith(prefix)) completions.add(home);
            }
            return completions;
        }
        return Collections.emptyList();
    }
}


package org.bcnlab.beaconLabsSurvival.command;

import org.bcnlab.beaconLabsSurvival.BeaconLabsSurvival;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

public class BypassBuildCommand implements CommandExecutor {
    private static final Set<UUID> bypassing = new HashSet<>();
    private final BeaconLabsSurvival plugin;

    public BypassBuildCommand(BeaconLabsSurvival plugin) {
        this.plugin = plugin;
    }

    public static boolean isBypassing(Player player) {
        return bypassing.contains(player.getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        if (bypassing.contains(uuid)) {
            bypassing.remove(uuid);
            player.sendMessage(ChatColor.YELLOW + "Build bypass disabled.");
        } else {
            bypassing.add(uuid);
            player.sendMessage(ChatColor.GREEN + "Build bypass enabled. You can now build anywhere.");
        }
        return true;
    }
}

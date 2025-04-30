package org.bcnlab.beaconLabsSurvival.command;

import org.bcnlab.beaconLabsSurvival.BeaconLabsSurvival;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class LabsSurvivalCommand implements CommandExecutor {

    private final BeaconLabsSurvival plugin;

    public LabsSurvivalCommand(BeaconLabsSurvival plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "BeaconLabsSurvival Version " + ChatColor.GOLD + plugin.getVersion() + ChatColor.RED + " by ItsBeacon");
        return true;
    }
}

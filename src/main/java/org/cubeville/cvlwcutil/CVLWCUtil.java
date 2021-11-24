package org.cubeville.cvlwcutil;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.CommandParser;

import java.util.logging.Logger;

public class CVLWCUtil extends JavaPlugin {

    private Logger logger;

    private CommandParser cvlwcutilParser;

    @Override
    public void onEnable() {
        this.logger = getLogger();

        cvlwcutilParser = new CommandParser();
        cvlwcutilParser.addCommand(new CVLWCUtilCommand(this));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("cvlwcutil")) {
            return cvlwcutilParser.execute(sender, args);
        }
        return false;
    }

    @Override
    public void onDisable() {
        logger.info(ChatColor.LIGHT_PURPLE + "Plugin Disabled Successfully");
    }
}

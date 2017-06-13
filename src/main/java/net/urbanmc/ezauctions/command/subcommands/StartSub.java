package net.urbanmc.ezauctions.command.subcommands;

import net.urbanmc.ezauctions.object.SubCommand;
import org.bukkit.command.CommandSender;

public class StartSub extends SubCommand{


    public StartSub(String sub, String permission, boolean playerOnly) {
        super("start", "start", true, "s");
    }

    public void run(CommandSender sender, String[] args) {

    }
}

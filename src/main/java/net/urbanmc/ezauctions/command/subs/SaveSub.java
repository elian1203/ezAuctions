package net.urbanmc.ezauctions.command.subs;

import net.urbanmc.ezauctions.manager.AuctionsPlayerManager;
import net.urbanmc.ezauctions.object.Permission;
import org.bukkit.command.CommandSender;

public class SaveSub extends SubCommand {

    public SaveSub() {
        super("save", Permission.COMMAND_SAVE, false);
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        AuctionsPlayerManager.getInstance().asyncSaveData();

        sendPropMessage(sender, "command.auction.save");
    }
}

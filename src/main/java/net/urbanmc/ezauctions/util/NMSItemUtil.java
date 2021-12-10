package net.urbanmc.ezauctions.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class NMSItemUtil {

    public static String getItemJson(Object nmsStack) {
        CompoundTag tag = new CompoundTag();

        ItemStack item = (ItemStack) nmsStack;
        item.save(tag);

        return tag.toString();
    }

    public static int getXPForRepair(Object nmsStack) {
        ItemStack item = (ItemStack) nmsStack;

        if (item.hasTag() && item.getTag().contains("RepairCost")) {
            int repairCost = item.getTag().getInt("RepairCost");
            boolean repairable = repairCost <= 40;
            return repairable ? repairCost : -1;
        }

        return 0;
    }
}

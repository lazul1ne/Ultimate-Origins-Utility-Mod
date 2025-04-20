package xyz.lazuline.utils;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class InventoryUtilsOLD {
    public static void replaceItem(PlayerEntity player, Item oldItem, Item newItem) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == oldItem) {
                ItemStack newStack = new ItemStack(newItem, stack.getCount());
                if (stack.hasNbt()) {
                    newStack.setNbt(stack.getNbt().copy()); // Copy NBT if you want
                }
                player.getInventory().setStack(i, newStack);
            }
        }
    }
}
package com.jxyzp.sortersatchel;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class SatchelMenu implements InventoryHolder {
    static final int DEPOSIT_BUTTON_SLOT = 13;
    private final Inventory inventory = Bukkit.createInventory(this, 27, "Sorter’s Satchel");

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

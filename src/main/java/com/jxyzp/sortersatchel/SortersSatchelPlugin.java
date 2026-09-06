package com.jxyzp.sortersatchel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class SortersSatchelPlugin extends JavaPlugin implements Listener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private NamespacedKey satchelKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        satchelKey = new NamespacedKey(this, "sorters-satchel");
        registerRecipe();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private void registerRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "sorters-satchel-recipe");
        Bukkit.removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createSatchel());
        recipe.shape("LEL", "CBC", "LHL");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('C', Material.COPPER_INGOT);
        recipe.setIngredient('B', Material.BUNDLE);
        recipe.setIngredient('H', Material.HOPPER);
        Bukkit.addRecipe(recipe);
    }

    private ItemStack createSatchel() {
        ItemStack item = new ItemStack(Material.BUNDLE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Sorter’s Satchel").color(net.kyori.adventure.text.format.NamedTextColor.GOLD));
        meta.lore(List.of(
            Component.text("Right-click to organize nearby storage.")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY),
            Component.text("Matches existing chest contents.")
                .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)
        ));
        meta.getPersistentDataContainer().set(satchelKey, PersistentDataType.BYTE, (byte) 1);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isSatchel(final ItemStack item) {
        return item != null && item.getType() == Material.BUNDLE && item.hasItemMeta()
            && item.getItemMeta().getPersistentDataContainer().has(satchelKey, PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isSatchel(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    private void openMenu(final Player player) {
        SatchelMenu menu = new SatchelMenu();
        Inventory inventory = menu.getInventory();
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        ItemStack button = named(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Deposit nearby items");
        ItemMeta meta = button.getItemMeta();
        meta.setLore(List.of(
            ChatColor.GRAY + "Moves inventory stacks into nearby",
            ChatColor.GRAY + "chests that already contain them."
        ));
        button.setItemMeta(meta);
        inventory.setItem(SatchelMenu.DEPOSIT_BUTTON_SLOT, button);
        player.openInventory(inventory);
    }

    private static ItemStack named(final Material material, final String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onMenuClick(final InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SatchelMenu)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() != SatchelMenu.DEPOSIT_BUTTON_SLOT || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        DepositResult result = deposit(player);
        player.closeInventory();
        String path = result.items() == 0 ? "messages.nothing" : "messages.deposited";
        String fallback = result.items() == 0
            ? "<yellow>No inventory items had matching stacks in nearby chests."
            : "<green>Deposited <amount> items into <chests> nearby chests.";
        String message = getConfig().getString(path, fallback)
            .replace("<amount>", Integer.toString(result.items()))
            .replace("<chests>", Integer.toString(result.chests()));
        player.sendActionBar(MINI_MESSAGE.deserialize(message));
        player.playSound(player.getLocation(),
            result.items() == 0 ? Sound.BLOCK_NOTE_BLOCK_BASS : Sound.ENTITY_ITEM_PICKUP,
            0.8F, result.items() == 0 ? 0.8F : 1.25F);
    }

    @EventHandler
    public void onMenuDrag(final InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SatchelMenu) {
            event.setCancelled(true);
        }
    }

    private DepositResult deposit(final Player player) {
        List<Destination> destinations = findDestinations(player);
        Set<Inventory> changed = new HashSet<>();
        int movedTotal = 0;
        int firstSlot = getConfig().getBoolean("include-hotbar", false) ? 0 : 9;
        for (int slot = firstSlot; slot <= 35; slot++) {
            ItemStack source = player.getInventory().getItem(slot);
            if (source == null || source.getType().isAir() || isSatchel(source)) {
                continue;
            }
            List<Destination> matches = destinations.stream()
                .filter(destination -> matchingSlots(destination.inventory(), source) > 0)
                .filter(destination -> capacity(destination.inventory(), source) > 0)
                .sorted(Comparator
                    .comparingInt((Destination destination) -> matchingSlots(destination.inventory(), source)).reversed()
                    .thenComparingDouble(Destination::distanceSquared))
                .toList();
            int before = source.getAmount();
            for (Destination destination : matches) {
                if (source.getAmount() == 0) {
                    break;
                }
                if (insert(destination.inventory(), source) > 0) {
                    changed.add(destination.inventory());
                }
            }
            movedTotal += before - source.getAmount();
            player.getInventory().setItem(slot, source.getAmount() == 0 ? null : source);
        }
        return new DepositResult(movedTotal, changed.size());
    }

    private List<Destination> findDestinations(final Player player) {
        double radius = Math.max(1.0, Math.min(64.0, getConfig().getDouble("search-radius", 16.0)));
        double radiusSquared = radius * radius;
        int chunkRadius = (int) Math.ceil(radius / 16.0);
        int centerX = player.getLocation().getBlockX() >> 4;
        int centerZ = player.getLocation().getBlockZ() >> 4;
        Set<Inventory> seen = new HashSet<>();
        List<Destination> found = new ArrayList<>();
        for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) {
            for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
                if (!player.getWorld().isChunkLoaded(x, z)) {
                    continue;
                }
                Chunk chunk = player.getWorld().getChunkAt(x, z);
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof Chest chest)) {
                        continue;
                    }
                    double distance = chest.getLocation().add(0.5, 0.5, 0.5).distanceSquared(player.getLocation());
                    Inventory inventory = chest.getInventory();
                    if (distance <= radiusSquared && seen.add(inventory)) {
                        found.add(new Destination(inventory, distance));
                    }
                }
            }
        }
        return found;
    }

    private static int matchingSlots(final Inventory inventory, final ItemStack template) {
        int count = 0;
        for (ItemStack existing : inventory.getStorageContents()) {
            if (existing != null && existing.isSimilar(template)) {
                count++;
            }
        }
        return count;
    }

    private static int capacity(final Inventory inventory, final ItemStack template) {
        int capacity = 0;
        for (ItemStack existing : inventory.getStorageContents()) {
            if (existing == null || existing.getType().isAir()) {
                capacity += template.getMaxStackSize();
            } else if (existing.isSimilar(template)) {
                capacity += existing.getMaxStackSize() - existing.getAmount();
            }
        }
        return capacity;
    }

    private static int insert(final Inventory inventory, final ItemStack source) {
        int before = source.getAmount();
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length && source.getAmount() > 0; slot++) {
            ItemStack existing = contents[slot];
            if (existing == null || !existing.isSimilar(source) || existing.getAmount() >= existing.getMaxStackSize()) {
                continue;
            }
            int moved = Math.min(existing.getMaxStackSize() - existing.getAmount(), source.getAmount());
            existing.setAmount(existing.getAmount() + moved);
            source.setAmount(source.getAmount() - moved);
        }
        for (int slot = 0; slot < contents.length && source.getAmount() > 0; slot++) {
            ItemStack existing = contents[slot];
            if (existing != null && !existing.getType().isAir()) {
                continue;
            }
            int moved = Math.min(source.getMaxStackSize(), source.getAmount());
            ItemStack placed = source.clone();
            placed.setAmount(moved);
            contents[slot] = placed;
            source.setAmount(source.getAmount() - moved);
        }
        inventory.setStorageContents(contents);
        return before - source.getAmount();
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label,
                             final String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("give")) {
            if (!player.hasPermission("sortersatchel.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission.");
                return true;
            }
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(createSatchel());
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendActionBar(MINI_MESSAGE.deserialize(getConfig().getString(
                "messages.received", "<green>You received a Sorter's Satchel.")));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("open")) {
            if (containsSatchel(player)) {
                openMenu(player);
            } else {
                player.sendMessage(ChatColor.RED + "You need a Sorter's Satchel.");
            }
            return true;
        }
        player.sendMessage(ChatColor.YELLOW + "Right-click a Sorter's Satchel, or use /sortersatchel open.");
        return true;
    }

    private boolean containsSatchel(final Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSatchel(item)) {
                return true;
            }
        }
        return false;
    }

    private record Destination(Inventory inventory, double distanceSquared) {
    }

    private record DepositResult(int items, int chests) {
    }
}

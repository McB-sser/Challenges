package de.mcbesser.challenges.service;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class MenuItemService {

    private static final String MENU_ITEM_ID = "challenge_menu_item";
    private final NamespacedKey itemIdKey;
    private final NamespacedKey recipeKey;

    public MenuItemService(JavaPlugin plugin) {
        this.itemIdKey = new NamespacedKey(plugin, "menu_item_id");
        this.recipeKey = new NamespacedKey(plugin, "challenge_menu_flower_recipe");
    }

    public void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createMenuItem());
        recipe.shape("BBB", "BSB", "BBB");
        recipe.setIngredient('B', Material.BONE_MEAL);
        recipe.setIngredient('S', Material.SUNFLOWER);
        Bukkit.addRecipe(recipe);
    }

    public ItemStack createMenuItem() {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Herausforderungs-Hub");
        meta.setLore(List.of(
                ChatColor.GRAY + "Rechtsklick:",
                ChatColor.AQUA + "Menü öffnen"
        ));
        meta.addEnchant(Enchantment.LUCK, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, MENU_ITEM_ID);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMenuItem(ItemStack item) {
        if (item == null || item.getType() != Material.SUNFLOWER || !item.hasItemMeta()) {
            return false;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        return MENU_ITEM_ID.equals(id);
    }

    public NamespacedKey getRecipeKey() {
        return recipeKey;
    }

    public boolean hasMenuItemInInventory(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isMenuItem(stack)) {
                return true;
            }
        }
        return isMenuItem(player.getInventory().getItemInOffHand());
    }
}

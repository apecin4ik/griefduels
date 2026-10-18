package dev.duel;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * Шесть китов из схематик kit.schem и kits.schem (сундуки). Индекс массива = номер слота в сундуке.
 * Устанавливаются в kits.yml при первом запуске (или после обновления набора) и командой /duel defaultkits.
 *
 *  1 Старт · 2 Jester · 3 Gosudar · 4 Подарок Санты · 5 Palatin · 6 Набор YouTube
 */
final class KitDefaults {
    private KitDefaults() { }

    static ItemStack[] get(int id) {
        switch (id) {
            case 1: return starter();
            case 2: return jester();
            case 3: return gosudar();
            case 4: return santa();
            case 5: return palatin();
            case 6: return youtube();
            default: return new ItemStack[0];
        }
    }

    // ------------------------------------------------------------ 1: Старт (кожа)

    private static ItemStack[] starter() {
        return new ItemStack[]{
                leather(Material.LEATHER_HELMET, 13803743),
                leather(Material.LEATHER_CHESTPLATE, 14919903),
                leather(Material.LEATHER_LEGGINGS, 14919903),
                leather(Material.LEATHER_BOOTS, 14919903),
                new ItemStack(Material.STONE_SWORD),
                new ItemStack(Material.BOW),
                new ItemStack(Material.STONE_AXE),
                new ItemStack(Material.COOKED_PORKCHOP, 16),
                new ItemStack(Material.SHIELD),
                new ItemStack(Material.ARROW, 16)
        };
    }

    // ------------------------------------------------------------ 2: Jester (алмаз, слабее Gosudar)

    private static ItemStack[] jester() {
        String t = "&r&f&lНабор &1&lJester";
        return new ItemStack[]{
                named(Material.DIAMOND_HELMET, 1, t, "protection", 3, "unbreaking", 2),
                named(Material.DIAMOND_CHESTPLATE, 1, t, "protection", 3, "unbreaking", 2),
                named(Material.DIAMOND_LEGGINGS, 1, t, "protection", 3, "unbreaking", 2),
                named(Material.DIAMOND_BOOTS, 1, t, "protection", 3, "unbreaking", 2),
                named(Material.DIAMOND_SWORD, 1, t, "sharpness", 3, "fire_aspect", 1, "unbreaking", 2),
                named(Material.BOW, 1, t, "power", 1, "flame", 1, "infinity", 1),
                named(Material.DIAMOND_AXE, 1, t, "efficiency", 3, "unbreaking", 2),
                new ItemStack(Material.COOKED_PORKCHOP, 64),
                new ItemStack(Material.GOLDEN_APPLE, 8),
                new ItemStack(Material.ARROW, 64),
                new ItemStack(Material.TOTEM_OF_UNDYING)
        };
    }

    // ------------------------------------------------------------ 3: Gosudar (алмаз)

    private static ItemStack[] gosudar() {
        String t = "&r&f&lНабор &5&lGosudar";
        return new ItemStack[]{
                named(Material.DIAMOND_HELMET, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.DIAMOND_CHESTPLATE, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.DIAMOND_LEGGINGS, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.DIAMOND_BOOTS, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.DIAMOND_SWORD, 1, t, "sharpness", 4, "fire_aspect", 2, "unbreaking", 2),
                named(Material.BOW, 1, t, "power", 2, "flame", 1, "infinity", 1),
                named(Material.DIAMOND_AXE, 1, t, "efficiency", 4, "unbreaking", 3),
                named(Material.SHIELD, 1, t),
                new ItemStack(Material.GOLDEN_APPLE, 10),
                new ItemStack(Material.COOKED_BEEF, 64),
                new ItemStack(Material.ARROW, 64),
                new ItemStack(Material.TOTEM_OF_UNDYING)
        };
    }

    // ------------------------------------------------------------ 4: Подарок Санты

    private static ItemStack[] santa() {
        String t = "&r&b❄ &4&lПодарок &f&lСанты &b❄";
        return new ItemStack[]{
                named(Material.DIAMOND_HELMET, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.DIAMOND_CHESTPLATE, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.DIAMOND_LEGGINGS, 1, null, "protection", 4, "unbreaking", 3),   // в схематике без названия
                named(Material.DIAMOND_BOOTS, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.DIAMOND_SWORD, 1, t, "sharpness", 5, "fire_aspect", 2, "unbreaking", 2),
                new ItemStack(Material.GOLDEN_APPLE, 16),
                named(Material.GOLDEN_CARROT, 64, "&r&c🔥 &4&lНебесная Арена Инферно &c🔥"),
                potion(PotionType.STRENGTH),
                potion(PotionType.SPEED),
                potion(PotionType.STRENGTH),
                potion(PotionType.SPEED),
                potion(PotionType.STRENGTH),
                potion(PotionType.SPEED),
                new ItemStack(Material.TOTEM_OF_UNDYING),
                new ItemStack(Material.TOTEM_OF_UNDYING)
        };
    }

    // ------------------------------------------------------------ 5: Palatin (незерит)

    private static ItemStack[] palatin() {
        String t = "&r&f&lНабор &4&lPalatin";
        return new ItemStack[]{
                named(Material.NETHERITE_HELMET, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.NETHERITE_CHESTPLATE, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.NETHERITE_LEGGINGS, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.NETHERITE_BOOTS, 1, t, "protection", 4, "unbreaking", 3),
                named(Material.NETHERITE_SWORD, 1, t, "sharpness", 5, "fire_aspect", 2, "unbreaking", 2),
                named(Material.BOW, 1, t, "power", 4, "flame", 1, "infinity", 1),
                named(Material.NETHERITE_AXE, 1, t, "efficiency", 5, "unbreaking", 3),
                named(Material.SHIELD, 1, t),
                new ItemStack(Material.GOLDEN_APPLE, 16),
                new ItemStack(Material.COOKED_BEEF, 64),
                new ItemStack(Material.TOTEM_OF_UNDYING),
                new ItemStack(Material.TOTEM_OF_UNDYING),
                potion(PotionType.STRENGTH),
                potion(PotionType.STRENGTH),
                potion(PotionType.SPEED),
                potion(PotionType.SPEED)
        };
    }

    // ------------------------------------------------------------ 6: Набор YouTube (незерит, максимум)

    private static ItemStack[] youtube() {
        String t = "&r&f&lНабор &7[&4&lYou&f&lTube&7]";
        return new ItemStack[]{
                named(Material.NETHERITE_HELMET, 1, t, "protection", 4, "unbreaking", 3, "mending", 1, "respiration", 3, "aqua_affinity", 1),
                named(Material.NETHERITE_CHESTPLATE, 1, t, "protection", 4, "unbreaking", 3, "mending", 1),
                named(Material.NETHERITE_LEGGINGS, 1, t, "protection", 4, "unbreaking", 3, "mending", 1),
                named(Material.NETHERITE_BOOTS, 1, t, "protection", 4, "unbreaking", 3, "mending", 1, "feather_falling", 4, "depth_strider", 3),
                named(Material.NETHERITE_SWORD, 1, t, "sharpness", 5, "fire_aspect", 2, "looting", 3, "sweeping", 3, "unbreaking", 3, "mending", 1),
                named(Material.BOW, 1, t, "power", 5, "punch", 2, "flame", 1, "infinity", 1, "mending", 1),
                named(Material.NETHERITE_AXE, 1, t, "efficiency", 5, "fortune", 3, "unbreaking", 3, "mending", 1),
                new ItemStack(Material.GOLDEN_CARROT, 64),
                new ItemStack(Material.GOLDEN_APPLE, 32),
                new ItemStack(Material.ENCHANTED_GOLDEN_APPLE),
                new ItemStack(Material.TOTEM_OF_UNDYING),
                new ItemStack(Material.TOTEM_OF_UNDYING),
                new ItemStack(Material.TOTEM_OF_UNDYING),
                new ItemStack(Material.TOTEM_OF_UNDYING),
                new ItemStack(Material.EXPERIENCE_BOTTLE, 64),
                new ItemStack(Material.EXPERIENCE_BOTTLE, 64),
                new ItemStack(Material.EXPERIENCE_BOTTLE, 64),
                potion(PotionType.STRENGTH),
                potion(PotionType.SPEED),
                potion(PotionType.STRENGTH),
                potion(PotionType.SPEED),
                potion(PotionType.STRENGTH),
                potion(PotionType.SPEED),
                tippedArrows(PotionType.INSTANT_DAMAGE, 64)
        };
    }

    // ------------------------------------------------------------ helpers

    private static ItemStack leather(Material m, int rgb) {
        ItemStack i = new ItemStack(m);
        LeatherArmorMeta lm = (LeatherArmorMeta) i.getItemMeta();
        lm.setColor(Color.fromRGB(rgb));
        i.setItemMeta(lm);
        return i;
    }

    /** Предмет с названием (null — без названия) и зачарованиями (пары: ключ зачарования, уровень). */
    private static ItemStack named(Material m, int amount, String title, Object... ench) {
        ItemStack i = new ItemStack(m, amount);
        if (title != null) {
            ItemMeta im = i.getItemMeta();
            im.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
            i.setItemMeta(im);
        }
        for (int k = 0; k + 1 < ench.length; k += 2) {
            Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft((String) ench[k]));
            if (e != null) i.addUnsafeEnchantment(e, (Integer) ench[k + 1]);
        }
        return i;
    }

    /** Усиленное зелье (strong_*, уровень II). */
    private static ItemStack potion(PotionType type) {
        ItemStack i = new ItemStack(Material.POTION);
        PotionMeta pm = (PotionMeta) i.getItemMeta();
        pm.setBasePotionData(new PotionData(type, false, true));
        i.setItemMeta(pm);
        return i;
    }

    private static ItemStack tippedArrows(PotionType type, int amount) {
        ItemStack i = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta pm = (PotionMeta) i.getItemMeta();
        pm.setBasePotionData(new PotionData(type, false, true));
        i.setItemMeta(pm);
        return i;
    }
}

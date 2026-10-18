package dev.duel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Выбор кита для дуэли (6 китов).
 * ЛКМ — создать вызов с этим китом, ПКМ — посмотреть, что внутри.
 */
public class KitMenu implements Listener {

    public static class Holder implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    /** Окно предпросмотра содержимого кита. */
    public static class Preview implements InventoryHolder {
        Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private static final int[] KIT_SLOTS = {11, 13, 15, 20, 22, 24};
    private static final int BACK = 31;          // в меню выбора
    private static final int PREVIEW_BACK = 49;  // в предпросмотре

    private final DuelPlugin plugin;
    private final DuelManager manager;
    private final DuelMenu menu;

    public KitMenu(DuelPlugin plugin, DuelManager manager, DuelMenu menu) {
        this.plugin = plugin;
        this.manager = manager;
        this.menu = menu;
    }

    // ---------------------------------------------------------------- выбор кита

    public void open(Player p) {
        Holder h = new Holder();
        h.inv = Bukkit.createInventory(h, 36, plugin.raw("kit-menu-title"));
        render(h);
        p.openInventory(h.inv);
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.8f);
    }

    private void render(Holder h) {
        Inventory inv = h.inv;
        ItemStack black = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack gray = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            boolean border = i < 9 || i >= 27 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, border ? (i % 2 == 0 ? black : gray) : black);
        }
        inv.setItem(4, item(Material.NETHERITE_SWORD, "&4&lДуэль с китами",
                "&7Бой на отдельной арене высоко в небе.",
                "&7Кит выдаётся автоматически и пропадает",
                "&7после дуэли, ваши вещи вернутся.",
                "",
                "&eЛКМ &7— создать вызов",
                "&bПКМ &7— посмотреть, что в ките"));

        for (int i = 0; i < KIT_SLOTS.length; i++) {
            int id = i + 1;
            Material icon = Material.matchMaterial(plugin.getConfig().getString("kits.names." + id + ".icon", "IRON_SWORD"));
            if (icon == null) icon = Material.IRON_SWORD;
            List<String> lore = new ArrayList<>(plugin.getConfig().getStringList("kits.names." + id + ".lore"));
            lore.add("");
            if (manager.kits().isDefined(id)) {
                lore.add("&eЛКМ &7— создать вызов");
                lore.add("&bПКМ &7— посмотреть содержимое");
            } else {
                lore.add("&cКит пока не настроен");
            }
            inv.setItem(KIT_SLOTS[i], item(icon, manager.kitName(id), lore.toArray(new String[0])));
        }
        inv.setItem(BACK, item(Material.ARROW, "&7Назад", "&8К списку дуэлей"));
    }

    // ---------------------------------------------------------------- предпросмотр

    /** Окно, как инвентарь игрока: броня и левая рука сверху, основной инвентарь и хотбар ниже. */
    public void openPreview(Player p, int id) {
        if (!manager.kits().isDefined(id)) { p.sendMessage(plugin.msg("kit-empty")); return; }
        Preview h = new Preview();
        // в 1.16 заголовок окна не длиннее 32 символов — берём название без цветов
        h.inv = Bukkit.createInventory(h, 54, plugin.raw("kit-preview-title", "%kit%", manager.kitPlainName(id)));
        Inventory inv = h.inv;

        ItemStack black = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack gray = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, i % 2 == 0 ? black : gray);
        for (int i = 45; i < 54; i++) inv.setItem(i, i % 2 == 0 ? black : gray);

        ItemStack[] armor = manager.kits().armorOf(id);          // [ботинки, поножи, нагрудник, шлем]
        inv.setItem(1, orPane(armor[3], "&8Шлем"));
        inv.setItem(2, orPane(armor[2], "&8Нагрудник"));
        inv.setItem(3, orPane(armor[1], "&8Поножи"));
        inv.setItem(4, orPane(armor[0], "&8Ботинки"));
        inv.setItem(6, orPane(manager.kits().offhandOf(id), "&8Вторая рука"));

        ItemStack[] st = manager.kits().storageOf(id);
        for (int i = 9; i < 36; i++) inv.setItem(i, st[i]);       // основной инвентарь
        for (int i = 0; i < 9; i++) inv.setItem(36 + i, st[i]);   // хотбар

        inv.setItem(PREVIEW_BACK, item(Material.ARROW, "&7Назад", "&8К выбору кита"));
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.1f);
    }

    private ItemStack orPane(ItemStack it, String emptyName) {
        return it != null ? it : item(Material.GRAY_STAINED_GLASS_PANE, emptyName);
    }

    // ---------------------------------------------------------------- события

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Object holder = e.getInventory().getHolder();
        if (holder instanceof Preview) {
            e.setCancelled(true);
            if (e.getClickedInventory() == e.getInventory() && e.getRawSlot() == PREVIEW_BACK) {
                final Player p = (Player) e.getWhoClicked();
                p.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> open(p));
            }
            return;
        }
        if (!(holder instanceof Holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;
        final Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == BACK) {
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> menu.open(p));
            return;
        }
        for (int i = 0; i < KIT_SLOTS.length; i++) {
            if (slot != KIT_SLOTS[i]) continue;
            final int id = i + 1;
            p.closeInventory();
            if (e.getClick().isRightClick()) {
                Bukkit.getScheduler().runTask(plugin, () -> openPreview(p, id));   // ПКМ — посмотреть содержимое
            } else {
                manager.createOpen(p, DuelManager.Mode.KIT, id);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.7f);
                Bukkit.getScheduler().runTask(plugin, () -> menu.open(p));   // вернуть в список — там виден свой вызов
            }
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Object h = e.getInventory().getHolder();
        if (h instanceof Holder || h instanceof Preview) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- helpers

    private String c(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta im = i.getItemMeta();
        im.setDisplayName(c(name));
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        im.setLore(l);
        im.addItemFlags(ItemFlag.values());
        i.setItemMeta(im);
        return i;
    }
}

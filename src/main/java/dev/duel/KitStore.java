package dev.duel;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Киты (kits.yml) и «снимки» настоящего инвентаря игроков на время китовой дуэли.
 * Снимок пишется на диск: если игрок вылетит/сервер упадёт — вещи не потеряются,
 * они вернутся при следующем входе.
 */
public class KitStore {

    private final DuelPlugin plugin;
    private final File kitsFile;
    private final File snapDir;
    private YamlConfiguration kits;

    public KitStore(DuelPlugin plugin) {
        this.plugin = plugin;
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        this.snapDir = new File(plugin.getDataFolder(), "snapshots");
        //noinspection ResultOfMethodCallIgnored
        snapDir.mkdirs();
        reload();
    }

    public void reload() {
        kits = kitsFile.exists() ? YamlConfiguration.loadConfiguration(kitsFile) : new YamlConfiguration();
    }

    private void saveKits() {
        try { kits.save(kitsFile); } catch (IOException ex) { plugin.getLogger().warning("kits.yml: " + ex.getMessage()); }
    }

    // ------------------------------------------------------------ определения китов

    public static int kitCount() { return 6; }

    /** Версия набора китов по умолчанию: при её росте киты переустанавливаются один раз. */
    private static final int DEFAULTS_VERSION = 2;

    public boolean isDefined(int id) {
        ConfigurationSection s = kits.getConfigurationSection("kits." + id);
        if (s == null) return false;
        ConfigurationSection st = s.getConfigurationSection("storage");
        ConfigurationSection ar = s.getConfigurationSection("armor");
        return (st != null && !st.getKeys(false).isEmpty()) || (ar != null && !ar.getKeys(false).isEmpty());
    }

    /**
     * Ставит три кита из схематики (KitDefaults). force=false — только те, что ещё не настроены.
     * Раскладка та же, что у /duel setkit из сундука: броня и щит — по слотам, остальное — по порядку.
     */
    public void installDefaults(boolean force) {
        boolean upgrade = kits.getInt("defaults-version", 0) < DEFAULTS_VERSION;   // набор изменился — ставим заново
        for (int id = 1; id <= kitCount(); id++) {
            if (!force && !upgrade && isDefined(id)) continue;
            Inventory tmp = Bukkit.createInventory(null, 54);
            ItemStack[] items = KitDefaults.get(id);
            for (int i = 0; i < items.length && i < 54; i++) tmp.setItem(i, items[i]);
            saveFromContainer(id, tmp);
        }
        kits.set("defaults-version", DEFAULTS_VERSION);
        saveKits();
    }

    /** Для предпросмотра: слоты хранилища (0-8 хотбар, 9-35 инвентарь). */
    public ItemStack[] storageOf(int id) {
        ConfigurationSection s = kits.getConfigurationSection("kits." + id);
        return readSlots(s == null ? null : s.getConfigurationSection("storage"), 36);
    }

    /** Броня: [ботинки, поножи, нагрудник, шлем]. */
    public ItemStack[] armorOf(int id) {
        ConfigurationSection s = kits.getConfigurationSection("kits." + id);
        return readSlots(s == null ? null : s.getConfigurationSection("armor"), 4);
    }

    public ItemStack offhandOf(int id) {
        ConfigurationSection s = kits.getConfigurationSection("kits." + id);
        ItemStack o = s == null ? null : s.getItemStack("offhand");
        return o == null ? null : o.clone();
    }

    /** Все предметы кита (для превью в меню). */
    public List<ItemStack> allItems(int id) {
        List<ItemStack> out = new ArrayList<>();
        ConfigurationSection s = kits.getConfigurationSection("kits." + id);
        if (s == null) return out;
        for (ItemStack i : readSlots(s.getConfigurationSection("armor"), 4)) if (i != null) out.add(i);
        ItemStack off = s.getItemStack("offhand");
        if (off != null) out.add(off);
        for (ItemStack i : readSlots(s.getConfigurationSection("storage"), 36)) if (i != null) out.add(i);
        return out;
    }

    /** Сохраняет кит из контейнера (сундука). Броня и щит раскладываются по слотам автоматически. Возвращает число предметов. */
    public int saveFromContainer(int id, Inventory src) {
        ItemStack[] storage = new ItemStack[36];
        ItemStack[] armor = new ItemStack[4];
        ItemStack off = null;
        int n = 0, next = 0;
        for (ItemStack it : src.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            ItemStack c = it.clone();
            int a = armorSlot(c.getType());
            if (a >= 0 && armor[a] == null) { armor[a] = c; n++; continue; }
            if (c.getType() == Material.SHIELD && off == null) { off = c; n++; continue; }
            if (next >= 36) continue;
            storage[next++] = c;
            n++;
        }
        write(id, storage, armor, off);
        return n;
    }

    /** Сохраняет кит из собственного инвентаря игрока. */
    public int saveFromPlayer(int id, Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] storage = clone(inv.getStorageContents(), 36);
        ItemStack[] armor = clone(inv.getArmorContents(), 4);
        ItemStack off = inv.getItemInOffHand();
        off = (off == null || off.getType() == Material.AIR) ? null : off.clone();
        int n = off != null ? 1 : 0;
        for (ItemStack i : storage) if (i != null) n++;
        for (ItemStack i : armor) if (i != null) n++;
        write(id, storage, armor, off);
        return n;
    }

    private void write(int id, ItemStack[] storage, ItemStack[] armor, ItemStack off) {
        String base = "kits." + id;
        kits.set(base, null);
        for (int i = 0; i < storage.length; i++) if (storage[i] != null) kits.set(base + ".storage." + i, storage[i]);
        for (int i = 0; i < armor.length; i++) if (armor[i] != null) kits.set(base + ".armor." + i, armor[i]);
        if (off != null) kits.set(base + ".offhand", off);
        saveKits();
    }

    /** Выдаёт кит: инвентарь предварительно очищается. */
    public void give(Player p, int id) {
        ConfigurationSection s = kits.getConfigurationSection("kits." + id);
        p.closeInventory();
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        if (s == null) return;
        inv.setStorageContents(readSlots(s.getConfigurationSection("storage"), 36));
        inv.setArmorContents(readSlots(s.getConfigurationSection("armor"), 4));
        ItemStack off = s.getItemStack("offhand");
        if (off != null) inv.setItemInOffHand(off.clone());
        p.updateInventory();
    }

    private static int armorSlot(Material m) {
        String n = m.name();
        if (n.endsWith("_BOOTS")) return 0;
        if (n.endsWith("_LEGGINGS")) return 1;
        if (n.endsWith("_CHESTPLATE")) return 2;
        if (n.endsWith("_HELMET")) return 3;
        return -1;
    }

    // ------------------------------------------------------------ снимки инвентаря

    private File snapFile(UUID id) { return new File(snapDir, id + ".yml"); }

    public boolean hasSnapshot(UUID id) { return snapFile(id).exists(); }

    @SuppressWarnings("deprecation")
    public void snapshot(Player p) {
        // если снимок уже есть (остался после краша) — он и есть настоящие вещи, не затираем его
        if (snapFile(p.getUniqueId()).exists()) return;
        YamlConfiguration y = new YamlConfiguration();
        PlayerInventory inv = p.getInventory();
        ItemStack[] storage = inv.getStorageContents();
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < storage.length; i++) if (storage[i] != null) y.set("storage." + i, storage[i]);
        for (int i = 0; i < armor.length; i++) if (armor[i] != null) y.set("armor." + i, armor[i]);
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() != Material.AIR) y.set("offhand", off);
        y.set("level", p.getLevel());
        y.set("exp", (double) p.getExp());
        y.set("health", p.getHealth());
        y.set("food", p.getFoodLevel());
        y.set("saturation", (double) p.getSaturation());
        y.set("gamemode", p.getGameMode().name());
        y.set("effects", new ArrayList<>(p.getActivePotionEffects()));
        try { y.save(snapFile(p.getUniqueId())); }
        catch (IOException ex) { plugin.getLogger().warning("snapshot " + p.getName() + ": " + ex.getMessage()); }
    }

    /** Возвращает игроку его настоящие вещи/опыт/эффекты и удаляет снимок. Если снимка нет — ничего не делает. */
    @SuppressWarnings("deprecation")
    public boolean restore(Player p) {
        File f = snapFile(p.getUniqueId());
        if (!f.exists()) return false;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        p.closeInventory();
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        inv.setItemInOffHand(null);
        inv.setStorageContents(readSlots(y.getConfigurationSection("storage"), 36));
        inv.setArmorContents(readSlots(y.getConfigurationSection("armor"), 4));
        ItemStack off = y.getItemStack("offhand");
        if (off != null) inv.setItemInOffHand(off);

        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        for (Object o : y.getList("effects", new ArrayList<>())) {
            if (o instanceof PotionEffect) p.addPotionEffect((PotionEffect) o);
        }
        p.setLevel(y.getInt("level", 0));
        p.setExp((float) y.getDouble("exp", 0));
        p.setFoodLevel(y.getInt("food", 20));
        p.setSaturation((float) y.getDouble("saturation", 5));
        if (!p.isDead()) {
            double max = p.getMaxHealth();
            p.setHealth(Math.max(1.0, Math.min(max, y.getDouble("health", max))));
        }
        p.setFireTicks(0);
        p.setFallDistance(0f);
        try {
            GameMode gm = GameMode.valueOf(y.getString("gamemode", "SURVIVAL"));
            if (gm != p.getGameMode()) p.setGameMode(gm);
        } catch (IllegalArgumentException ignored) { }
        p.updateInventory();
        //noinspection ResultOfMethodCallIgnored
        f.delete();
        return true;
    }

    // ------------------------------------------------------------ helpers

    private static ItemStack[] readSlots(ConfigurationSection s, int size) {
        ItemStack[] arr = new ItemStack[size];
        if (s == null) return arr;
        for (String k : s.getKeys(false)) {
            try {
                int i = Integer.parseInt(k);
                if (i < 0 || i >= size) continue;
                ItemStack it = s.getItemStack(k);
                if (it != null) arr[i] = it.clone();
            } catch (NumberFormatException ignored) { }
        }
        return arr;
    }

    private static ItemStack[] clone(ItemStack[] src, int size) {
        ItemStack[] arr = new ItemStack[size];
        for (int i = 0; i < size && i < src.length; i++) {
            if (src[i] != null && src[i].getType() != Material.AIR) arr[i] = src[i].clone();
        }
        return arr;
    }
}

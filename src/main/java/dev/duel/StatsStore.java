package dev.duel;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** Победы и поражения игроков во всех режимах дуэлей (stats.yml). */
public class StatsStore {

    public static class Entry {
        public final UUID id;
        public String name;
        public int wins, losses;
        Entry(UUID id, String name) { this.id = id; this.name = name; }
    }

    private final DuelPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> data = new HashMap<>();

    public StatsStore(DuelPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("players");
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                Entry e = new Entry(id, s.getString(key + ".name", "?"));
                e.wins = s.getInt(key + ".wins", 0);
                e.losses = s.getInt(key + ".losses", 0);
                data.put(id, e);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Entry e : data.values()) {
            String b = "players." + e.id;
            y.set(b + ".name", e.name);
            y.set(b + ".wins", e.wins);
            y.set(b + ".losses", e.losses);
        }
        try { y.save(file); } catch (IOException ex) { plugin.getLogger().warning("stats.yml: " + ex.getMessage()); }
    }

    private Entry entry(UUID id, String name) {
        Entry e = data.get(id);
        if (e == null) { e = new Entry(id, name); data.put(id, e); }
        if (name != null && !name.equals("?")) e.name = name;
        return e;
    }

    public void addWin(UUID id, String name) { entry(id, name).wins++; save(); }

    public void addLoss(UUID id, String name) { entry(id, name).losses++; save(); }

    /** Лучшие игроки по числу побед (только те, у кого есть победы). */
    public List<Entry> top(int n) {
        List<Entry> list = new ArrayList<>();
        for (Entry e : data.values()) if (e.wins > 0) list.add(e);
        list.sort((a, b) -> {
            if (a.wins != b.wins) return Integer.compare(b.wins, a.wins);
            if (a.losses != b.losses) return Integer.compare(a.losses, b.losses);
            return a.name.compareToIgnoreCase(b.name);
        });
        return list.size() > n ? new ArrayList<>(list.subList(0, n)) : list;
    }
}

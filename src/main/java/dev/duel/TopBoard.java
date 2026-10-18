package dev.duel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.*;

/**
 * Топ-10 по победам в дуэлях — в углу экрана (сайдбар справа).
 * Число справа от ника — количество побед. Один общий скорборд на всех, обновляется раз в N секунд
 * и сразу после каждой дуэли. Отключается: top.sidebar: false. Игрок может скрыть его: /duel board.
 */
public class TopBoard {
    private final DuelPlugin plugin;
    private final StatsStore stats;
    private Scoreboard sb;
    private Objective obj;
    private final Set<UUID> hidden = new HashSet<>();

    public TopBoard(DuelPlugin plugin, StatsStore stats) {
        this.plugin = plugin;
        this.stats = stats;
    }

    private boolean enabled() { return plugin.getConfig().getBoolean("top.sidebar", true); }

    public void start() {
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;
        sb = mgr.getNewScoreboard();
        obj = sb.registerNewObjective("duelTop", "dummy", plugin.raw("top-title"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        refresh();
        long period = Math.max(5, plugin.getConfig().getInt("top.update-seconds", 30)) * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, period, period);
        for (Player p : Bukkit.getOnlinePlayers()) show(p);
    }

    /** Перерисовывает топ-10. */
    public void refresh() {
        if (sb == null || obj == null) return;
        for (String e : new ArrayList<>(sb.getEntries())) sb.resetScores(e);
        List<StatsStore.Entry> top = stats.top(10);
        if (top.isEmpty()) {
            obj.getScore(plugin.raw("top-empty-line")).setScore(0);
            return;
        }
        String[] colors = {"§6", "§7", "§c"};   // золото, серебро, бронза
        Set<String> used = new HashSet<>();
        for (int i = 0; i < top.size(); i++) {
            StatsStore.Entry e = top.get(i);
            String label = (i < 3 ? colors[i] : "§f") + e.name;
            while (!used.add(label)) label += "§r";   // уникальность строки
            obj.getScore(label).setScore(e.wins);
        }
    }

    public void show(Player p) {
        if (sb == null || !enabled() || hidden.contains(p.getUniqueId())) return;
        p.setScoreboard(sb);
    }

    /** /duel board — скрыть / показать топ в углу для себя. */
    public void toggle(Player p) {
        if (sb == null || !enabled()) { p.sendMessage(plugin.msg("board-disabled")); return; }
        if (hidden.remove(p.getUniqueId())) {
            p.setScoreboard(sb);
            p.sendMessage(plugin.msg("board-on"));
        } else {
            hidden.add(p.getUniqueId());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            p.sendMessage(plugin.msg("board-off"));
        }
    }

    public void shutdown() {
        if (sb == null || Bukkit.getScoreboardManager() == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() == sb) p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
}

package dev.duel;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DuelCommand implements CommandExecutor, TabCompleter {
    private final DuelPlugin plugin;
    private final DuelManager manager;
    private final DuelMenu menu;

    public DuelCommand(DuelPlugin plugin, DuelManager manager, DuelMenu menu) {
        this.plugin = plugin;
        this.manager = manager;
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(s instanceof Player)) { s.sendMessage("Только для игроков."); return true; }
            Player p = (Player) s;
            if (!p.hasPermission("duel.use")) { p.sendMessage(plugin.msg("no-perm")); return true; }
            if (manager.isInDuel(p)) { p.sendMessage(plugin.msg("already-in-duel")); return true; }
            menu.open(p);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("cancel") && s instanceof Player) {
            manager.cancelOpen((Player) s);
            return true;
        }
        if (sub.equals("top")) {
            java.util.List<StatsStore.Entry> top = manager.stats().top(10);
            s.sendMessage(plugin.raw("top-header"));
            if (top.isEmpty()) s.sendMessage(plugin.raw("top-empty"));
            int n = 1;
            for (StatsStore.Entry e : top) {
                s.sendMessage(plugin.raw("top-line", "%n%", String.valueOf(n++), "%name%", e.name,
                        "%wins%", String.valueOf(e.wins), "%losses%", String.valueOf(e.losses)));
            }
            return true;
        }
        if (sub.equals("board") && s instanceof Player) {
            manager.board().toggle((Player) s);
            return true;
        }
        if (!s.hasPermission("duel.admin")) { s.sendMessage(plugin.msg("no-perm")); return true; }
        switch (sub) {
            case "build":
                manager.build(s);
                break;
            case "reload":
                plugin.reloadConfig();
                manager.kits().reload();
                s.sendMessage(plugin.msg("reloaded"));
                break;
            case "setkit": {
                if (!(s instanceof Player)) { s.sendMessage("Только для игроков."); return true; }
                int id = args.length > 1 ? parseKit(args[1]) : 0;
                if (id == 0) { s.sendMessage(plugin.msg("kit-usage")); return true; }
                manager.saveKit((Player) s, id);
                break;
            }
            case "defaultkits":
                manager.kits().installDefaults(true);
                s.sendMessage(plugin.msg("kit-defaults"));
                break;
            case "kitpreview":
                if (!(s instanceof Player)) return true;
                if (args.length > 1 && args[1].equalsIgnoreCase("remove")) manager.kitPreviewRemove((Player) s);
                else manager.kitPreview((Player) s);
                break;
            case "setcenter":
            case "sethome":
            case "setarena":
                if (!(s instanceof Player)) return true;
                Location l = ((Player) s).getLocation();
                plugin.getConfig().set("world", l.getWorld().getName());
                plugin.getConfig().set("arena.center.x", l.getBlockX());
                plugin.getConfig().set("arena.center.y", l.getBlockY());
                plugin.getConfig().set("arena.center.z", l.getBlockZ());
                plugin.saveConfig();
                s.sendMessage(plugin.msg("setcenter"));
                break;
            case "setspawn":
                if (!(s instanceof Player)) return true;
                Location sp = ((Player) s).getLocation();
                plugin.getConfig().set("spawn.world", sp.getWorld().getName());
                plugin.getConfig().set("spawn.x", sp.getX());
                plugin.getConfig().set("spawn.y", sp.getY());
                plugin.getConfig().set("spawn.z", sp.getZ());
                plugin.getConfig().set("spawn.yaw", (double) sp.getYaw());
                plugin.getConfig().set("spawn.pitch", (double) sp.getPitch());
                plugin.saveConfig();
                s.sendMessage(plugin.msg("setspawn"));
                break;
            default:
                s.sendMessage("§c/duel [build|setcenter|sethome|setspawn|setkit <1-6>|defaultkits|kitpreview [remove]|reload|cancel]");
        }
        return true;
    }

    private int parseKit(String s) {
        try {
            int n = Integer.parseInt(s);
            return n >= 1 && n <= KitStore.kitCount() ? n : 0;
        } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("cancel"); out.add("top"); out.add("board");
            if (s.hasPermission("duel.admin")) {
                out.add("build"); out.add("setcenter"); out.add("setspawn"); out.add("reload");
                out.add("setkit"); out.add("defaultkits"); out.add("kitpreview");
            }
        } else if (args.length == 2 && s.hasPermission("duel.admin")) {
            if (args[0].equalsIgnoreCase("setkit")) { for (int i = 1; i <= KitStore.kitCount(); i++) out.add(String.valueOf(i)); }
            else if (args[0].equalsIgnoreCase("kitpreview")) out.add("remove");
        }
        return out;
    }
}

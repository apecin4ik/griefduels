package dev.duel;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * «Багровый некрополь» — временная запечатанная арена для дуэлей с китами (высоко в небе).
 *
 *  - пол: шахматка чёрного камня, печать из плачущего обсидиана, багровые спицы и кольцо из бородавчатых блоков
 *  - стены: чёрный камень, контрфорсы с фонарями, окна-решётки (за ними светится гриб-светильник)
 *  - свод: рёбра, сталактиты, цепи с души-фонарями, светящееся «око»
 *  - снаружи, снизу и сверху — бедрок
 *
 * Строится и удаляется по слоям, чанки на время дуэли удерживаются тикетами плагина.
 */
public class KitArenaBuilder {

    // ---------------------------------------------------------------- публичное API

    /** Строит арену. Возвращает задачу (её можно отменить). done вызывается, когда всё построено. */
    public static BukkitTask build(final DuelPlugin plugin, Location c, final int R, final int H, final Runnable done) {
        final World w = c.getWorld();
        final int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        final int W = R + 2;
        final List<int[]> chunks = chunks(c, R);
        return new BukkitRunnable() {
            int ci = 0;
            int dy = -4;

            @Override
            public void run() {
                // фаза 1: подгружаем и удерживаем чанки (по 2 за тик, чтобы не лагало)
                if (ci < chunks.size()) {
                    for (int k = 0; k < 2 && ci < chunks.size(); k++, ci++) {
                        int[] ch = chunks.get(ci);
                        w.addPluginChunkTicket(ch[0], ch[1], plugin);
                        w.getChunkAt(ch[0], ch[1]);
                    }
                    return;
                }
                // фаза 2: слой за слоем
                if (dy > H) { cancel(); done.run(); return; }
                for (int dx = -W; dx <= W; dx++) {
                    for (int dz = -W; dz <= W; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d > W + 0.5) continue;
                        Material m = pick(dx, dy, dz, d, R, H);
                        Block b = w.getBlockAt(cx + dx, cy + dy, cz + dz);
                        b.setType(m, false);
                        if ((m == Material.SOUL_LANTERN && dy == H - 6) || (m == Material.LANTERN && dy == H - 2)) {
                            Lantern l = (Lantern) b.getBlockData();
                            l.setHanging(true);
                            b.setBlockData(l, false);
                        }
                    }
                }
                dy++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Разбирает арену по слоям (сверху вниз) и отпускает чанки. */
    public static void remove(final DuelPlugin plugin, Location c, final int R, final int H, final Runnable done) {
        final World w = c.getWorld();
        final int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        final int W = R + 2;
        final List<int[]> chunks = chunks(c, R);
        new BukkitRunnable() {
            int dy = H;

            @Override
            public void run() {
                for (int k = 0; k < 2 && dy >= -4; k++, dy--) clearLayer(w, cx, cy + dy, cz, W);
                if (dy < -4) {
                    cancel();
                    release(plugin, w, chunks);
                    if (done != null) done.run();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Мгновенный снос (для выключения сервера и /duel kitpreview remove). */
    public static void removeNow(DuelPlugin plugin, Location c, int R, int H) {
        World w = c.getWorld();
        int W = R + 2;
        for (int dy = H; dy >= -4; dy--) clearLayer(w, c.getBlockX(), c.getBlockY() + dy, c.getBlockZ(), W);
        release(plugin, w, chunks(c, R));
    }

    /** Все чанки, которые захватывает арена. */
    public static List<int[]> chunks(Location c, int R) {
        int W = R + 2;
        List<int[]> out = new ArrayList<>();
        int minX = (c.getBlockX() - W) >> 4, maxX = (c.getBlockX() + W) >> 4;
        int minZ = (c.getBlockZ() - W) >> 4, maxZ = (c.getBlockZ() + W) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) out.add(new int[]{x, z});
        return out;
    }

    private static void release(DuelPlugin plugin, World w, List<int[]> chunks) {
        for (int[] ch : chunks) w.removePluginChunkTicket(ch[0], ch[1], plugin);
    }

    private static void clearLayer(World w, int cx, int y, int cz, int W) {
        if (y < 0 || y >= w.getMaxHeight()) return;
        for (int dx = -W; dx <= W; dx++) {
            for (int dz = -W; dz <= W; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) > W + 0.5) continue;
                Block b = w.getBlockAt(cx + dx, y, cz + dz);
                if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static int hash(int x, int y, int z) {
        return ((x * 73856093) ^ (y * 83492791) ^ (z * 19349663)) & 0x7fffffff;
    }

    private static double angle(int dx, int dz) {
        double a = Math.toDegrees(Math.atan2(dz, dx));
        return a < 0 ? a + 360 : a;
    }

    /** Расстояние (в блоках по дуге) до ближайшей линии с шагом step градусов, сдвинутой на offset. */
    private static double arcFrom(int dx, int dz, double d, double step, double offset) {
        double a = (angle(dx, dz) + offset) % step;
        double diff = Math.min(a, step - a);
        return Math.toRadians(diff) * d;
    }

    private static boolean nearSpawn(int dx, int dz, int R) {
        return Math.hypot(Math.abs(dx) - (R - 6), dz) < 5;
    }

    private static boolean rib(int dx, int dz, double d, int R) {
        return arcFrom(dx, dz, d, 30, 0) <= 0.6
                || Math.abs(d - R * 0.5) <= 0.6
                || Math.abs(d - R * 0.8) <= 0.6;
    }

    // ---------------------------------------------------------------- слои

    private static Material pick(int dx, int dy, int dz, double d, int R, int H) {
        if (dy == -4 || dy == H) return Material.BEDROCK;       // дно и крыша
        if (d > R + 1.5) {                                      // внешняя оболочка
            // за решётками окон светится гриб-светильник
            if (dy >= 3 && dy <= H - 5 && arcFrom(dx, dz, d, 30, 15) <= 1.0) return Material.SHROOMLIGHT;
            return Material.BEDROCK;
        }
        if (dy <= -2) return Material.BLACKSTONE;
        if (d > R + 0.5) return wall(dx, dy, dz, d, H);         // внутренний слой стены
        if (dy == -1) return floor(dx, dz, d, R);
        return interior(dx, dy, dz, d, R, H);
    }

    private static Material floor(int dx, int dz, double d, int R) {
        // стартовые площадки
        int off = R - 6;
        for (int s = -1; s <= 1; s += 2) {
            double pd = Math.hypot(dx - s * off, dz);
            if (pd <= 1.6) return Material.CRYING_OBSIDIAN;
            if (pd <= 2.6) return Material.OBSIDIAN;
        }
        // центральная печать
        if (d <= 2.5) return Material.CRYING_OBSIDIAN;
        if (d <= 3.5) return Material.GILDED_BLACKSTONE;
        // края и кольца
        if (d >= R - 1.5) return Material.OBSIDIAN;
        if (d >= R - 3.5 && d <= R - 2.5) return Material.CRYING_OBSIDIAN;
        if (Math.abs(d - R * 0.5) <= 0.6) return Material.NETHER_WART_BLOCK;
        // 4 багровые спицы + 4 диагональные из базальта
        if (d > 3.5 && d < R - 3.5) {
            if (arcFrom(dx, dz, d, 90, 0) <= 0.8) return Material.RED_NETHER_BRICKS;
            if (arcFrom(dx, dz, d, 90, 45) <= 0.7) return (((int) d) % 2 == 0) ? Material.POLISHED_BASALT : Material.BASALT;
        }
        int h = hash(dx, 0, dz) % 100;
        if (h < 8) return Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
        if (h < 16) return Material.BLACKSTONE;
        return (((dx + dz) & 1) == 0) ? Material.POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS;
    }

    private static Material wall(int dx, int dy, int dz, double d, int H) {
        if (dy <= 1) return Material.BLACKSTONE;
        if (dy == 2) return Material.RED_NETHER_BRICKS;   // «кровавая» полоса у основания
        if (dy >= 3 && dy <= H - 5 && arcFrom(dx, dz, d, 30, 15) <= 1.0) return Material.IRON_BARS;
        if (dy == H - 2) return Material.CRYING_OBSIDIAN;
        if (arcFrom(dx, dz, d, 30, 0) <= 1.6) {
            return dy == H - 3 ? Material.CHISELED_POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS;
        }
        int h = hash(dx, dy, dz) % 100;
        if (h < 3) return Material.GILDED_BLACKSTONE;
        if (h < 28) return Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
        return Material.BLACKSTONE;
    }

    private static Material interior(int dx, int dy, int dz, double d, int R, int H) {
        // 1) контрфорсы у стены (12 штук) + фонари на вершинах
        if (d > R - 1.5 && dy <= H - 1) {
            // контрфорс доходит до самого свода: наверху негде встать
            if (arcFrom(dx, dz, d, 30, 0) <= 1.5) {
                return dy == H - 3 ? Material.CHISELED_POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS;
            }
        }
        // фонарь, подвешенный к ребру свода рядом с контрфорсом
        if (dy == H - 2 && d > R - 2.5 && d <= R - 1.5 && arcFrom(dx, dz, d, 30, 0) <= 0.6) return Material.LANTERN;

        // 2) 6 колонн-укрытий (2x2, высота 5) на середине радиуса (не на линии стартовых площадок)
        int pr = R / 2;
        for (int k = 0; k < 6; k++) {
            double a = Math.toRadians(30 + 60 * k);
            int px = (int) Math.round(pr * Math.cos(a)), pz = (int) Math.round(pr * Math.sin(a));
            if ((dx == px || dx == px + 1) && (dz == pz || dz == pz + 1)) {
                if (dy <= 3) return Material.POLISHED_BLACKSTONE_BRICKS;
                if (dy == 4) return Material.CHISELED_POLISHED_BLACKSTONE;
                if (dy == 5 && dx == px && dz == pz) return Material.LANTERN;
                return Material.AIR;
            }
        }

        // 3) 12 цепей с души-фонарями под сводом
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(15 + 30 * k);
            int hx = (int) Math.round((R - 4) * Math.cos(a)), hz = (int) Math.round((R - 4) * Math.sin(a));
            if (dx == hx && dz == hz) {
                if (dy >= H - 5) return Material.CHAIN;
                if (dy == H - 6) return Material.SOUL_LANTERN;
            }
        }

        // 3b) подвешенные клетки с души-фонарём внутри
        if (d > 6 && d < R - 4 && hash(dx, 9, dz) % 110 == 0 && !rib(dx, dz, d, R)) {
            if (dy >= H - 3) return Material.CHAIN;
            if (dy == H - 4 || dy == H - 5) return Material.IRON_BARS;
            if (dy == H - 6) return Material.SOUL_LANTERN;
        }
        // 3c) случайные обрывки цепей под потолком
        if (d > 3 && !rib(dx, dz, d, R) && hash(dx, 5, dz) % 37 == 0) {
            int clen = 2 + hash(dx, 6, dz) % 4;
            if (dy >= H - 1 - clen) return Material.CHAIN;
        }

        // 4) свод: рёбра, «око», паутина по краям, сталактиты
        if (dy == H - 1) {
            if (d <= 1.5) return Material.SHROOMLIGHT;
            if (d <= 3.0) return Material.CRYING_OBSIDIAN;
            if (rib(dx, dz, d, R)) return Material.POLISHED_BLACKSTONE_BRICKS;
            if (d > R - 2 && hash(dx, 3, dz) % 3 == 0) return Material.COBWEB;
        } else if (dy >= H - 4 && dy < H - 1 && d > 3 && rib(dx, dz, d, R) && hash(dx, 0, dz) % 5 == 0) {
            int len = 1 + hash(dx, 1, dz) % 3;
            if (dy >= H - 1 - len) return Material.POLISHED_BLACKSTONE_BRICK_WALL;
        }

        // 5) черепа и обломки на полу
        if (dy == 0 && d > 5 && d < R - 4 && !nearSpawn(dx, dz, R)) {
            int hs = hash(dx, 7, dz) % 80;
            if (hs == 0) return Material.SKELETON_SKULL;
            if (hs == 1) return Material.WITHER_SKELETON_SKULL;
            if (hs == 2) return Material.POLISHED_BLACKSTONE_BRICK_SLAB;
            if (hs == 3) return Material.BLACKSTONE_SLAB;
        }
        return Material.AIR;
    }
}

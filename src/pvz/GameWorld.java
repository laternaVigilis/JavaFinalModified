package pvz;

import java.awt.Color;
import java.util.*;
import pvz.Plant.*;
import pvz.Zombie.*;

public class GameWorld {

    public enum UpdateResult { NONE, WIN, LOSE }

    // Game objects (model)
    public final Plant[][]        grid;
    public final List<Zombie>     zombies;
    public final List<Pea>        peas;
    public final List<Sun>        suns;
    public final List<Explosion>  explosions;
    public final List<FloatText>  floatTexts;

    // Resources
    public int sunCount;
    public int wave;
    public int maxWaves;
    public int zombiesKilled;
    public int zombiesPerWave;
    public int zombiesSpawnedThisWave;

    // Timing (internal to model)
    private long lastSunFall;
    private long lastZombieSpawn;

    public GameWorld() {
        grid = new Plant[Constants.ROWS][Constants.COLS];
        zombies = new ArrayList<>();
        peas = new ArrayList<>();
        suns = new ArrayList<>();
        explosions = new ArrayList<>();
        floatTexts = new ArrayList<>();
        maxWaves = 10;
        zombiesPerWave = 5;
        start();
    }

    public void start() {
        for (Plant[] row : grid) Arrays.fill(row, null);
        zombies.clear(); peas.clear(); suns.clear(); explosions.clear(); floatTexts.clear();
        sunCount = Constants.START_SUN;
        wave = 1;
        zombiesKilled = 0;
        zombiesSpawnedThisWave = 0;
        lastSunFall = System.currentTimeMillis();
        lastZombieSpawn = System.currentTimeMillis() + 3000;
    }

    public UpdateResult update(double dt) {
        long now = System.currentTimeMillis();
        if (dt > 0.1) dt = 0.1;

        updateSuns(dt, now);
        updateZombieSpawn(now);
        updateZombies(dt, now);
        updatePeas(dt);
        updatePlants(now);
        updateExplosions(dt);
        updateFloatTexts();
        checkCollisions();

        // Win / Lose determination (model-level only)
        // Lose: any zombie reached the left side
        for (Zombie z : zombies) {
            if (z.x < Constants.GRID_X - 40) {
                return UpdateResult.LOSE;
            }
        }
        // Win: survived all waves and no zombies left
        if (wave > maxWaves && zombies.isEmpty()) {
            return UpdateResult.WIN;
        }

        return UpdateResult.NONE;
    }

    // --- Internal update helpers (migrated from GamePanel) ---
    private void updateSuns(double dt, long now) {
        if (now - lastSunFall > Constants.SUN_FALL_INTERVAL) {
            lastSunFall = now;
            double sx = 80 + Math.random() * (Constants.WINDOW_WIDTH - 160);
            double ty = 80 + Math.random() * (Constants.GRID_Y + Constants.ROWS * Constants.CELL_H - 160);
            suns.add(new Sun(sx, -30, ty));
        }
        for (Sun s : suns) s.update(dt);
        suns.removeIf(s -> s.collected || s.isExpired());
    }

    private void updateZombieSpawn(long now) {
        if (wave > maxWaves) return;
        int totalThisWave = zombiesPerWave + (wave - 1) * 2;
        if (zombiesSpawnedThisWave >= totalThisWave) {
            if (zombies.isEmpty()) {
                wave++;
                zombiesSpawnedThisWave = 0;
                if (wave % 3 == 0) {
                    Zombie.addGlobalHpBonus(80);
                    addFloatText("All zombies +80 HP!", Constants.WINDOW_WIDTH / 2, 170,
                            new Color(255, 140, 0), 20, 1800);
                }
                if (wave <= maxWaves) {
                    addFloatText("第 " + wave + " 波！", Constants.WINDOW_WIDTH / 2, 200,
                            new Color(255, 80, 80), 30, 2500);
                    lastZombieSpawn = now + 1000;
                }
            }
            return;
        }

        long spawnInterval = Math.max(2000, Constants.ZOMBIE_SPAWN_BASE - wave * 400L);
        if (now - lastZombieSpawn > spawnInterval) {
            lastZombieSpawn = now;
            int row = (int)(Math.random() * Constants.ROWS);
            int zType;
            double r = Math.random();
            if (wave <= 2) {
                zType = Zombie.TEMPLATE_NORMAL;
            } else if (wave <= 4) {
                zType = r < 0.75 ? Zombie.TEMPLATE_NORMAL : Zombie.TEMPLATE_FAST;
            } else {
                if (r < 0.15) {
                    zType = Zombie.TEMPLATE_CHERRY;
                } else if (r < 0.70) {
                    zType = Zombie.TEMPLATE_NORMAL;
                } else if (r < 0.95) {
                    zType = Zombie.TEMPLATE_FAST;
                } else {
                    zType = Zombie.TEMPLATE_TANK;
                }
            }
            int maxLevel = Math.min(4, 1 + wave / 2);
            double biasFactor = Math.max(0.45, 1.0 - (wave - 1) * 0.06);
            double rnd = Math.random();
            double biased = Math.pow(rnd, biasFactor);
            int level = 1 + (int)(biased * maxLevel);
            level = Math.max(1, Math.min(level, maxLevel));
            Zombie z;
            if (zType == Zombie.TEMPLATE_FAST) {
                z = new FastZombie(row, level);
            } else if (zType == Zombie.TEMPLATE_TANK) {
                z = new TankZombie(row, level);
            } else if (zType == Zombie.TEMPLATE_CHERRY) {
                z = new CherryZombie(row, level);
            } else {
                z = new NormalZombie(row, level);
            }
            zombies.add(z);
            zombiesSpawnedThisWave++;
        }
    }

    private void updateZombies(double dt, long now) {
        for (Zombie z : zombies) {
            Plant target = getPlantInFront(z);
            if (target != null) {
                z.attacking = true;
                if (now - z.lastAttackTime > Constants.ZOMBIE_ATTACK_RATE) {
                    z.lastAttackTime = now;
                    int dmg = z.attackPower();
                    target.hp -= dmg;
                    addFloatText("-" + dmg, target.cx(), target.cy() - 10,
                            new Color(220, 60, 60), 16, 700);
                    if (target.isDead()) {
                        grid[target.row][target.col] = null;
                    }
                }
            } else {
                z.attacking = false;
            }
            z.update(dt);
        }
    }

    private Plant getPlantInFront(Zombie z) {
        int col = (int)((z.x - Constants.GRID_X) / Constants.CELL_W);
        if (col < 0) col = 0;
        if (col >= Constants.COLS) return null;
        return grid[z.row][col];
    }

    private void updatePeas(double dt) {
        for (Pea p : peas) p.update(dt);
        peas.removeIf(p -> p.dead);
    }

    private void updatePlants(long now) {
        for (int r = 0; r < Constants.ROWS; r++) {
            for (int c = 0; c < Constants.COLS; c++) {
                Plant p = grid[r][c];
                if (p == null) continue;
                switch (p.type) {
                    case PEASHOOTER -> {
                        if (hasZombieInRow(r, c) && now - p.lastShootTime > Constants.SHOOT_INTERVAL) {
                            p.lastShootTime = now;
                            double px = p.cx() + 20;
                            double py = p.cy();
                            peas.add(new Pea(px, py, r, false));
                        }
                    }
                    case SNOWPEA -> {
                        if (hasZombieInRow(r, c) && now - p.lastShootTime > Constants.SHOOT_INTERVAL) {
                            p.lastShootTime = now;
                            peas.add(new Pea(p.cx() + 20, p.cy(), r, true));
                        }
                    }
                    case SUNFLOWER -> {
                        if (now - p.lastSunTime > Constants.SF_SUN_INTERVAL) {
                            p.lastSunTime = now;
                            suns.add(new Sun(p.cx(), p.cy() - 30));
                        }
                    }
                    case CHERRYBOMB -> {
                        if (!p.exploding) {
                            p.exploding = true;
                            p.explodeStartTime = now;
                        }
                        if (now - p.explodeStartTime > Constants.CHERRY_FUSE) {
                            explodeCherry(p);
                            grid[r][c] = null;
                        }
                    }
                    default -> {}
                }
            }
        }
    }

    private boolean hasZombieInRow(int row, int fromCol) {
        double minX = Constants.GRID_X + fromCol * Constants.CELL_W;
        for (Zombie z : zombies) {
            if (z.row == row && z.x > minX) return true;
        }
        return false;
    }

    private void explodeCherry(Plant p) {
        int cx = p.cx(), cy = p.cy();
        explosions.add(new Explosion(cx, cy, Constants.CHERRY_RADIUS));
        addFloatText("💥 BOOM!", cx, cy - 30, new Color(255, 120, 0), 28, 1200);

        Iterator<Zombie> it = zombies.iterator();
        while (it.hasNext()) {
            Zombie z = it.next();
            int zx = (int)z.x + Zombie.ZOMBIE_W / 2;
            int zy = z.getPixelY() + Zombie.ZOMBIE_H / 2;
            double dist = Math.hypot(zx - cx, zy - cy);
            if (dist <= Constants.CHERRY_RADIUS) {
                z.hp -= Constants.CHERRY_DMG;
                if (z.isDead()) {
                    handleZombieDeathEffects(z);
                    it.remove();
                    zombiesKilled++;
                    sunCount += 5;
                }
            }
        }
    }

    private void updateExplosions(double dt) {
        for (Explosion ex : explosions) ex.update(dt);
        explosions.removeIf(ex -> ex.done);
    }

    private void updateFloatTexts() {
        floatTexts.removeIf(ft -> System.currentTimeMillis() > ft.expireTime);
        for (FloatText ft : floatTexts) ft.y -= 0.5;
    }

    private void checkCollisions() {
        Iterator<Pea> pi = peas.iterator();
        while (pi.hasNext()) {
            Pea pea = pi.next();
            boolean hit = false;
            for (Zombie z : zombies) {
                if (z.row == pea.row && pea.getBounds().intersects(z.getBounds())) {
                    int dmg = pea.frozen ? Constants.SNOW_PEA_DMG : Constants.PEA_DMG;
                    z.hp -= dmg;
                    addFloatText("-" + dmg, (int)z.x + 20, z.getPixelY(),
                            pea.frozen ? new Color(100, 200, 255) : new Color(100, 220, 100), 14, 500);
                    if (pea.frozen) {
                        z.speedFactor = Constants.SNOW_SLOW_FACTOR;
                        z.slowUntil   = System.currentTimeMillis() + Constants.SLOW_DURATION;
                    }
                    if (z.isDead()) {
                        zombiesKilled++;
                        sunCount += 5;
                        addFloatText("+5 ☀", (int)z.x + 20, z.getPixelY() - 10,
                                new Color(255, 220, 0), 16, 800);
                        handleZombieDeathEffects(z);
                    }
                    hit = true;
                    break;
                }
            }
            if (hit) { pi.remove(); }
            zombies.removeIf(Zombie::isDead);
        }
    }

    private void handleZombieDeathEffects(Zombie z) {
        if (z instanceof CherryZombie) {
            int zx = (int)z.x + Zombie.ZOMBIE_W / 2;
            int zy = z.getPixelY() + Zombie.ZOMBIE_H / 2;
            explosions.add(new Explosion(zx, zy, Constants.CHERRY_ZOMBIE_RADIUS));
            addFloatText("💥 BOOM!", zx, zy - 30, new Color(255, 120, 0), 28, 1200);

            for (int r = 0; r < Constants.ROWS; r++) {
                for (int c = 0; c < Constants.COLS; c++) {
                    Plant p = grid[r][c];
                    if (p == null) continue;
                    double pdx = p.cx() - zx;
                    double pdy = p.cy() - zy;
                    double dist = Math.hypot(pdx, pdy);
                    if (dist <= Constants.CHERRY_ZOMBIE_RADIUS) {
                        p.hp -= Constants.CHERRY_ZOMBIE_DMG;
                        if (p.isDead()) {
                            grid[r][c] = null;
                        }
                    }
                }
            }
        }
    }

    public void addFloatText(String text, int x, int y, Color color, int size, int duration) {
        floatTexts.add(new FloatText(text, x, y, color, size, System.currentTimeMillis() + duration));
    }
}



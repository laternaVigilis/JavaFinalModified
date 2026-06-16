package pvz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    // ── Game state ─────────────────────────────────────────────────────────────
    public enum State { MENU, PLAYING, PAUSED, WIN, LOSE }
    private State state = State.MENU;

    // ── Game objects ───────────────────────────────────────────────────────────
    private final Plant[][]        grid       = new Plant[Constants.ROWS][Constants.COLS];
    private final List<Zombie>     zombies    = new ArrayList<>();
    private final List<Pea>        peas       = new ArrayList<>();
    private final List<Sun>        suns       = new ArrayList<>();
    private final List<Explosion>  explosions = new ArrayList<>();
    private final List<FloatText>  floatTexts = new ArrayList<>();

    // ── Resources ──────────────────────────────────────────────────────────────
    private int sunCount = Constants.START_SUN;
    private int wave     = 0;
    private int maxWaves = 10;
    private int zombiesKilled = 0;
    private int zombiesPerWave = 5;

    // ── Timing ─────────────────────────────────────────────────────────────────
    private long lastSunFall;
    private long lastZombieSpawn;
    private long lastUpdate;
    private long gameStartTime;
    private long pauseStart;
    private long totalPauseTime;
    private int  zombiesSpawnedThisWave = 0;

    // ── Input ──────────────────────────────────────────────────────────────────
    private PlantType selectedPlant = null;
    private int       hoverCol = -1, hoverRow = -1;
    // Selected deployed tile (for showing remove button)
    private int       selectedTileCol = -1, selectedTileRow = -1;
    // Bounds for the remove (X) button
    private Rectangle removeBtnBounds = null;
    // Bounds for end-screen buttons (restart / main menu)
    private Rectangle endBtnRestartBounds = null;
    private Rectangle endBtnMenuBounds = null;
    // Bounds for pause-menu buttons
    private Rectangle pauseBtnContinueBounds = null;
    private Rectangle pauseBtnRestartBounds = null;
    private Rectangle pauseBtnEndBounds = null;
    private Point     mousePos = new Point();

    // ── Timer ──────────────────────────────────────────────────────────────────
    private final javax.swing.Timer timer;

    // ── Fonts ──────────────────────────────────────────────────────────────────
    private Font bigFont, medFont, smallFont;

    public GamePanel() {
        setPreferredSize(new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        setBackground(new Color(80, 140, 60));
        addMouseListener(this);
        addMouseMotionListener(this);
        timer = new javax.swing.Timer(16, this); // ~60fps
        initFonts();
    }

    private void initFonts() {
        bigFont   = new Font("微軟正黑體", Font.BOLD, 48);
        medFont   = new Font("微軟正黑體", Font.BOLD, 24);
        smallFont = new Font("微軟正黑體", Font.PLAIN, 14);
        // fallback
        if (!bigFont.getFamily().equals("微軟正黑體")) {
            bigFont   = new Font("Dialog", Font.BOLD, 48);
            medFont   = new Font("Dialog", Font.BOLD, 24);
            smallFont = new Font("Dialog", Font.PLAIN, 14);
        }
    }

    // ── Start / Reset ──────────────────────────────────────────────────────────
    public void startGame() {
        for (Plant[] row : grid) Arrays.fill(row, null);
        zombies.clear(); peas.clear(); suns.clear(); explosions.clear(); floatTexts.clear();
        sunCount       = Constants.START_SUN;
        wave           = 1;
        zombiesKilled  = 0;
        zombiesSpawnedThisWave = 0;
        lastSunFall    = System.currentTimeMillis();
        lastZombieSpawn= System.currentTimeMillis() + 3000;
        lastUpdate     = System.currentTimeMillis();
        gameStartTime  = System.currentTimeMillis();
        totalPauseTime = 0;
        selectedPlant  = null;
        // clear end-screen and pause-menu button bounds
        endBtnRestartBounds = null;
        endBtnMenuBounds = null;
        pauseBtnContinueBounds = null;
        pauseBtnRestartBounds = null;
        pauseBtnEndBounds = null;
        state          = State.PLAYING;
        timer.start();
    }

    // ── Main update ────────────────────────────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        if (state != State.PLAYING) { repaint(); return; }

        long now = System.currentTimeMillis();
        double dt = (now - lastUpdate) / 1000.0;
        if (dt > 0.1) dt = 0.1;
        lastUpdate = now;

        updateSuns(dt, now);
        updateZombieSpawn(now);
        updateZombies(dt, now);
        updatePeas(dt);
        updatePlants(now);
        updateExplosions(dt);
        updateFloatTexts();
        checkCollisions();
        checkWinLose(now);

        repaint();
    }

    private void updateSuns(double dt, long now) {
        // Sky sun drops
        if (now - lastSunFall > Constants.SUN_FALL_INTERVAL) {
            lastSunFall = now;
            double sx = 80 + Math.random() * (Constants.WINDOW_WIDTH - 160);
            double ty = 80 + Math.random() * (Constants.GRID_Y + Constants.ROWS * Constants.CELL_H - 160);
            suns.add(new Sun(sx, -30, ty));
        }
        // Update
        for (Sun s : suns) s.update(dt);
        suns.removeIf(s -> s.collected || s.isExpired());
    }

    private void updateZombieSpawn(long now) {
        if (wave > maxWaves) return;
        int totalThisWave = zombiesPerWave + (wave - 1) * 2;
        if (zombiesSpawnedThisWave >= totalThisWave) {
            // Wait for all zombies to die, then next wave
            if (zombies.isEmpty()) {
                wave++;
                zombiesSpawnedThisWave = 0;
                // Every 3 waves, increase all zombies' base HP by 80
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
                // wave >= 5: 15% Cherry, 55% Normal, 25% Fast, 5% Tank
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
            int level = 1 + (int)(Math.random() * maxLevel);
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
            // Check if zombie has a plant to attack
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
                            // Explode!
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

        // Damage all zombies in radius
        Iterator<Zombie> it = zombies.iterator();
        while (it.hasNext()) {
            Zombie z = it.next();
            int zx = (int)z.x + Zombie.ZOMBIE_W / 2;
            int zy = z.getPixelY() + Zombie.ZOMBIE_H / 2;
            double dist = Math.hypot(zx - cx, zy - cy);
            if (dist <= Constants.CHERRY_RADIUS) {
                z.hp -= Constants.CHERRY_DMG;
                if (z.isDead()) {
                    // handle special death effects (e.g., cherry zombie explosion)
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
                        // trigger death effects (like cherry zombie explosion)
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
            // Explosion visual
            explosions.add(new Explosion(zx, zy, Constants.CHERRY_ZOMBIE_RADIUS));
            addFloatText("💥 BOOM!", zx, zy - 30, new Color(255, 120, 0), 28, 1200);

            // Damage nearby plants
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

    private void checkWinLose(long now) {
        // Lose: zombie reaches left side
        for (Zombie z : zombies) {
            if (z.x < Constants.GRID_X - 40) {
                if (state != State.LOSE) {
                    state = State.LOSE;
                    timer.stop();
                    int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
                    endBtnRestartBounds = new Rectangle(bx, Constants.END_BTN_RESTART_Y, Constants.END_BTN_W, Constants.END_BTN_H);
                    endBtnMenuBounds = new Rectangle(bx, Constants.END_BTN_MENU_Y, Constants.END_BTN_W, Constants.END_BTN_H);
                    // ensure the UI updates immediately
                    repaint();
                }
                return;
            }
        }
        // Win: survived all waves and no zombies left
        if (wave > maxWaves && zombies.isEmpty()) {
            if (state != State.WIN) {
                state = State.WIN;
                timer.stop();
                int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
                endBtnRestartBounds = new Rectangle(bx, Constants.END_BTN_RESTART_Y, Constants.END_BTN_W, Constants.END_BTN_H);
                endBtnMenuBounds = new Rectangle(bx, Constants.END_BTN_MENU_Y, Constants.END_BTN_W, Constants.END_BTN_H);
                repaint();
            }
        }
    }

    // ── Painting ───────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (state) {
            case MENU    -> drawMenu(g);
            case PLAYING, PAUSED -> { drawGame(g); if (state == State.PAUSED) drawPauseOverlay(g); }
            case WIN     -> { drawGame(g); drawEndScreen(g, true); }
            case LOSE    -> { drawGame(g); drawEndScreen(g, false); }
        }
    }

    private void drawMenu(Graphics2D g) {
        // Background
        drawSky(g);
        drawGrassLawn(g);

        // Title
        g.setFont(new Font("Dialog", Font.BOLD, 56));
        String title = "🌻 植物大戰殭屍";
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(title);
        // Shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(title, (Constants.WINDOW_WIDTH - tw) / 2 + 3, 180 + 3);
        // Gradient text
        g.setColor(new Color(80, 200, 80));
        g.drawString(title, (Constants.WINDOW_WIDTH - tw) / 2, 180);

        // Decorative plants
        drawDecorativePlants(g);

        // Start button
        drawButton(g, Constants.WINDOW_WIDTH / 2 - 100, 250, 200, 55,
                new Color(60, 180, 60), new Color(40, 140, 40), "開始遊戲", Color.WHITE, 22);

        // Instructions
        g.setFont(new Font("Dialog", Font.PLAIN, 15));
        g.setColor(new Color(240, 240, 240));
        String[] tips = {
            "• 點擊底部植物欄選擇植物，再點擊草坪種植",
            "• 收集陽光以購買植物（點擊太陽收集）",
            "• 向日葵可持續產生陽光",
            "• 豌豆射手可攻擊殭屍",
            "• 堅果牆可阻擋殭屍",
            "• 寒冰射手會減慢殭屍速度",
            "• 櫻桃炸彈會在2秒後爆炸，傷害範圍內的所有殭屍",
            "• 共 10 波殭屍，全部消滅即可獲勝！"
        };
        int ty = 340;
        for (String tip : tips) {
            int tw2 = g.getFontMetrics().stringWidth(tip);
            g.drawString(tip, (Constants.WINDOW_WIDTH - tw2) / 2, ty);
            ty += 22;
        }
    }

    private void drawDecorativePlants(Graphics2D g) {
        // Draw sample plants for decoration
        Sunflower sf = new Sunflower(0, 0);
        sf.draw(g); // sample sunflower (may overlap grid)
        // Simple flower left
        g.setColor(new Color(255, 220, 0));
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            g.fillOval((int)(80 + Math.cos(a) * 18) - 8, (int)(230 + Math.sin(a) * 18) - 8, 16, 16);
        }
        g.setColor(new Color(140, 80, 20));
        g.fillOval(68, 218, 24, 24);
        // Simple flower right
        g.setColor(new Color(255, 220, 0));
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            g.fillOval((int)(820 + Math.cos(a) * 18) - 8, (int)(230 + Math.sin(a) * 18) - 8, 16, 16);
        }
        g.setColor(new Color(140, 80, 20));
        g.fillOval(808, 218, 24, 24);
    }

    private void drawGame(Graphics2D g) {
        drawSky(g);
        drawGrassLawn(g);
        drawGrid(g);
        // Selected-tile highlight (under plants) — shown when clicked and mouse still over tile
        drawSelectedHighlight(g);
        drawShop(g);
        drawHUD(g);

        // Plants
        for (int r = 0; r < Constants.ROWS; r++)
            for (int c = 0; c < Constants.COLS; c++)
                if (grid[r][c] != null) grid[r][c].draw(g);

        // Remove-button overlay for selected deployed tile
        drawRemoveButton(g);

        // Peas
        for (Pea p : peas) p.draw(g);

        // Zombies
        for (Zombie z : zombies) z.draw(g);

        // Suns
        for (Sun s : suns) s.draw(g);

        // Explosions
        for (Explosion ex : explosions) ex.draw(g);

        // Float texts
        for (FloatText ft : floatTexts) ft.draw(g);

        // Hover preview
        drawHoverPreview(g);

        // Danger indicator
        drawDangerZone(g);
    }

    private void drawSky(Graphics2D g) {
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235), 0, 120, new Color(180, 230, 180));
        g.setPaint(sky);
        g.fillRect(0, 0, Constants.WINDOW_WIDTH, 120);
    }

    private void drawGrassLawn(Graphics2D g) {
        // Dirt base
        g.setColor(new Color(120, 80, 40));
        g.fillRect(Constants.GRID_X, Constants.GRID_Y,
                Constants.COLS * Constants.CELL_W, Constants.ROWS * Constants.CELL_H);

        // Alternating grass cells
        for (int r = 0; r < Constants.ROWS; r++) {
            for (int c = 0; c < Constants.COLS; c++) {
                int x = Constants.GRID_X + c * Constants.CELL_W;
                int y = Constants.GRID_Y + r * Constants.CELL_H;
                Color grass = (r + c) % 2 == 0 ? new Color(90, 160, 60) : new Color(80, 150, 55);
                g.setColor(grass);
                g.fillRect(x, y, Constants.CELL_W, Constants.CELL_H);
            }
        }
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 40));
        g.setStroke(new BasicStroke(1));
        for (int r = 0; r <= Constants.ROWS; r++) {
            int y = Constants.GRID_Y + r * Constants.CELL_H;
            g.drawLine(Constants.GRID_X, y, Constants.GRID_X + Constants.COLS * Constants.CELL_W, y);
        }
        for (int c = 0; c <= Constants.COLS; c++) {
            int x = Constants.GRID_X + c * Constants.CELL_W;
            g.drawLine(x, Constants.GRID_Y, x, Constants.GRID_Y + Constants.ROWS * Constants.CELL_H);
        }

        // Hover highlight
        if (hoverCol >= 0 && hoverRow >= 0 && selectedPlant != null) {
            int hx = Constants.GRID_X + hoverCol * Constants.CELL_W;
            int hy = Constants.GRID_Y + hoverRow * Constants.CELL_H;
            boolean canPlace = grid[hoverRow][hoverCol] == null && sunCount >= selectedPlant.cost;
            g.setColor(canPlace ? new Color(255, 255, 100, 80) : new Color(255, 60, 60, 80));
            g.fillRect(hx, hy, Constants.CELL_W, Constants.CELL_H);
            g.setColor(canPlace ? new Color(255, 255, 0, 160) : new Color(255, 0, 0, 160));
            g.setStroke(new BasicStroke(2));
            g.drawRect(hx, hy, Constants.CELL_W, Constants.CELL_H);
            g.setStroke(new BasicStroke(1));
        }
    }

    private void drawShop(Graphics2D g) {
        // Shop background
        g.setColor(new Color(40, 80, 30));
        g.fillRoundRect(120, 8, 640, 110, 16, 16);
        g.setColor(new Color(80, 160, 60));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(120, 8, 640, 110, 16, 16);
        g.setStroke(new BasicStroke(1));

        // Sun counter (left panel)
        g.setColor(new Color(30, 60, 20));
        g.fillRoundRect(4, 8, 110, 110, 12, 12);
        g.setColor(new Color(255, 220, 30));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(4, 8, 110, 110, 12, 12);
        g.setStroke(new BasicStroke(1));
        // Sun icon
        g.setColor(new Color(255, 230, 30));
        g.fillOval(18, 18, 36, 36);
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine((int)(36 + Math.cos(a) * 18), (int)(36 + Math.sin(a) * 18),
                       (int)(36 + Math.cos(a) * 25), (int)(36 + Math.sin(a) * 25));
            g.setStroke(new BasicStroke(1));
        }
        g.setColor(new Color(255, 220, 30));
        g.setFont(new Font("Dialog", Font.BOLD, 22));
        String sunStr = String.valueOf(sunCount);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(sunStr, 59 - fm.stringWidth(sunStr) / 2, 90);
        g.setFont(new Font("Dialog", Font.PLAIN, 11));
        g.setColor(new Color(200, 200, 180));
        g.drawString("陽光", 42, 108);

        // Plant items
        PlantType[] types = PlantType.values();
        for (int i = 0; i < types.length; i++) {
            PlantType pt = types[i];
            int ix = Constants.SHOP_START_X + i * (Constants.SHOP_ITEM_W + 8);
            int iy = Constants.SHOP_Y + 5;
            boolean selected  = selectedPlant == pt;
            boolean affordable = sunCount >= pt.cost;
            // Card
            Color cardBg = selected ? new Color(255, 240, 100) : affordable ? new Color(60, 100, 50) : new Color(40, 60, 35);
            g.setColor(cardBg);
            g.fillRoundRect(ix, iy, Constants.SHOP_ITEM_W, Constants.SHOP_ITEM_H, 10, 10);
            g.setColor(selected ? new Color(220, 160, 0) : affordable ? new Color(100, 180, 80) : new Color(60, 80, 50));
            g.setStroke(new BasicStroke(selected ? 3 : 1.5f));
            g.drawRoundRect(ix, iy, Constants.SHOP_ITEM_W, Constants.SHOP_ITEM_H, 10, 10);
            g.setStroke(new BasicStroke(1));

            // Plant mini icon
            drawMiniPlant(g, pt, ix + Constants.SHOP_ITEM_W / 2, iy + 32, affordable);

            // Name
            g.setFont(new Font("Dialog", Font.BOLD, 10));
            g.setColor(affordable ? Color.WHITE : new Color(120, 120, 100));
            String name = pt.name;
            FontMetrics fm2 = g.getFontMetrics();
            g.drawString(name, ix + (Constants.SHOP_ITEM_W - fm2.stringWidth(name)) / 2, iy + 63);

            // Cost
            g.setFont(new Font("Dialog", Font.BOLD, 12));
            g.setColor(affordable ? new Color(255, 230, 50) : new Color(150, 120, 60));
            String cost = "☀" + pt.cost;
            FontMetrics fm3 = g.getFontMetrics();
            g.drawString(cost, ix + (Constants.SHOP_ITEM_W - fm3.stringWidth(cost)) / 2, iy + 80);

            // Cooldown overlay if not affordable
            if (!affordable) {
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRoundRect(ix, iy, Constants.SHOP_ITEM_W, Constants.SHOP_ITEM_H, 10, 10);
            }
        }

        // Right side: wave info
        g.setColor(new Color(255, 220, 150));
        g.setFont(new Font("Dialog", Font.BOLD, 14));
        String waveStr = "第 " + Math.min(wave, maxWaves) + "/" + maxWaves + " 波";
        g.drawString(waveStr, 780, 40);
        g.setColor(new Color(180, 220, 150));
        g.setFont(new Font("Dialog", Font.PLAIN, 13));
        g.drawString("消滅: " + zombiesKilled, 780, 60);

        // ESC hint
        g.setColor(new Color(180, 200, 170));
        g.setFont(new Font("Dialog", Font.PLAIN, 12));
        g.drawString("[ESC] 暫停", 780, 95);
    }

    private void drawMiniPlant(Graphics2D g, PlantType pt, int cx, int cy, boolean color) {
        Color c  = color ? pt.color     : pt.color.darker().darker();
        Color c2 = color ? pt.darkColor : pt.darkColor.darker().darker();
        switch (pt) {
            case SUNFLOWER -> {
                g.setColor(c);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60);
                    g.fillOval((int)(cx + Math.cos(a) * 10) - 5, (int)(cy + Math.sin(a) * 10) - 5, 10, 10);
                }
                g.setColor(c2);
                g.fillOval(cx - 8, cy - 8, 16, 16);
            }
            case PEASHOOTER, SNOWPEA -> {
                g.setColor(c);
                g.fillOval(cx - 11, cy - 12, 22, 22);
                g.setColor(c2);
                g.fillRoundRect(cx + 8, cy - 4, 14, 8, 4, 4);
            }
            case WALLNUT -> {
                g.setColor(c);
                g.fillOval(cx - 13, cy - 16, 26, 32);
                g.setColor(c2);
                g.drawOval(cx - 13, cy - 16, 26, 32);
            }
            case CHERRYBOMB -> {
                g.setColor(c);
                g.fillOval(cx - 14, cy - 8, 14, 14);
                g.fillOval(cx,      cy - 8, 14, 14);
                g.setColor(c2);
                g.setStroke(new BasicStroke(2));
                g.drawLine(cx - 7, cy - 8, cx - 9, cy - 18);
                g.drawLine(cx + 7, cy - 8, cx + 9, cy - 18);
                g.setStroke(new BasicStroke(1));
            }
        }
    }

    private void drawHUD(Graphics2D g) {
        // Bottom bar
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRect(0, Constants.WINDOW_HEIGHT - 30, Constants.WINDOW_WIDTH, 30);
        g.setFont(new Font("Dialog", Font.PLAIN, 13));
        g.setColor(new Color(220, 220, 200));
        if (selectedPlant != null) {
            g.drawString("已選擇: " + selectedPlant.name + "（花費 " + selectedPlant.cost + " 陽光）  右鍵取消選擇", 10, Constants.WINDOW_HEIGHT - 10);
        } else {
            g.drawString("點擊頂部植物欄選擇植物，再點擊草坪種植。點擊太陽收集陽光！", 10, Constants.WINDOW_HEIGHT - 10);
        }
    }

    private void drawHoverPreview(Graphics2D g) {
        if (selectedPlant == null) return;
        // Show mini preview near cursor
        int mx = mousePos.x, my = mousePos.y;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        drawMiniPlant(g, selectedPlant, mx, my - 20, true);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawRemoveButton(Graphics2D g) {
        if (selectedTileCol < 0 || selectedTileRow < 0) {
            removeBtnBounds = null;
            return;
        }
        // If tile no longer has a plant, hide
        if (grid[selectedTileRow][selectedTileCol] == null) {
            selectedTileCol = selectedTileRow = -1;
            removeBtnBounds = null;
            return;
        }
        int tx = Constants.GRID_X + selectedTileCol * Constants.CELL_W;
        int ty = Constants.GRID_Y + selectedTileRow * Constants.CELL_H;
        int bw = 20, bh = 20;
        int bx = tx + Constants.CELL_W - bw - 6;
        int by = ty + 6;
        // Button background
        g.setColor(new Color(200, 50, 50, 220));
        g.fillRoundRect(bx, by, bw, bh, 6, 6);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        // Draw X
        g.drawLine(bx + 4, by + 4, bx + bw - 5, by + bh - 5);
        g.drawLine(bx + bw - 5, by + 4, bx + 4, by + bh - 5);
        g.setStroke(new BasicStroke(1));
        removeBtnBounds = new Rectangle(bx, by, bw, bh);
    }

    private void drawSelectedHighlight(Graphics2D g) {
        if (selectedTileCol < 0 || selectedTileRow < 0) return;
        // Only draw highlight while the mouse remains over the same tile
        if (hoverCol != selectedTileCol || hoverRow != selectedTileRow) return;
        int tx = Constants.GRID_X + selectedTileCol * Constants.CELL_W;
        int ty = Constants.GRID_Y + selectedTileRow * Constants.CELL_H;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g.setColor(new Color(255, 255, 255));
        g.fillRect(tx, ty, Constants.CELL_W, Constants.CELL_H);
        g.setComposite(old);
    }

    private void drawDangerZone(Graphics2D g) {
        // Red line on left side
        g.setColor(new Color(220, 30, 30, 120));
        g.setStroke(new BasicStroke(3));
        g.drawLine(Constants.GRID_X, Constants.GRID_Y,
                   Constants.GRID_X, Constants.GRID_Y + Constants.ROWS * Constants.CELL_H);
        g.setStroke(new BasicStroke(1));
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);
        g.setFont(new Font("Dialog", Font.BOLD, 48));
        g.setColor(Color.WHITE);
        String txt = "⏸ 遊戲暫停";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(txt, (Constants.WINDOW_WIDTH - fm.stringWidth(txt)) / 2, 260);
        g.setFont(new Font("Dialog", Font.PLAIN, 20));
        g.setColor(new Color(200, 220, 200));
        String hint = "選擇一項操作";
        fm = g.getFontMetrics();
        g.drawString(hint, (Constants.WINDOW_WIDTH - fm.stringWidth(hint)) / 2, 300);

        int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
        // ensure pause button rects exist (created when pausing, but fallback here)
        if (pauseBtnContinueBounds == null) {
            pauseBtnContinueBounds = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y, Constants.END_BTN_W, Constants.END_BTN_H);
            pauseBtnRestartBounds = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP, Constants.END_BTN_W, Constants.END_BTN_H);
            pauseBtnEndBounds     = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP * 2, Constants.END_BTN_W, Constants.END_BTN_H);
        }

        // Continue
        Color contFill = pauseBtnContinueBounds.contains(mousePos) ? new Color(90, 210, 90) : new Color(60, 160, 60);
        drawButton(g, pauseBtnContinueBounds.x, pauseBtnContinueBounds.y, pauseBtnContinueBounds.width, pauseBtnContinueBounds.height,
            contFill, new Color(40, 120, 40), "繼續遊戲", Color.WHITE, 20);

        // Restart
        Color rstFill = pauseBtnRestartBounds.contains(mousePos) ? new Color(255, 200, 80) : new Color(200, 140, 0);
        drawButton(g, pauseBtnRestartBounds.x, pauseBtnRestartBounds.y, pauseBtnRestartBounds.width, pauseBtnRestartBounds.height,
            rstFill, new Color(150, 100, 10), "重新開始", Color.WHITE, 20);

        // End Game
        Color endFill = pauseBtnEndBounds.contains(mousePos) ? new Color(240, 100, 100) : new Color(200, 60, 60);
        drawButton(g, pauseBtnEndBounds.x, pauseBtnEndBounds.y, pauseBtnEndBounds.width, pauseBtnEndBounds.height,
            endFill, new Color(140, 30, 30), "結束遊戲", Color.WHITE, 20);
    }

    private void drawEndScreen(Graphics2D g, boolean win) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT);

        g.setFont(new Font("Dialog", Font.BOLD, 56));
        String title = win ? "🎉 你贏了！" : "💀 遊戲結束";
        g.setColor(win ? new Color(100, 255, 100) : new Color(255, 80, 80));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (Constants.WINDOW_WIDTH - fm.stringWidth(title)) / 2, 230);

        g.setFont(new Font("Dialog", Font.PLAIN, 22));
        g.setColor(new Color(220, 220, 200));
        String sub = win ? "所有殭屍已被消滅！你的花園得救了！" : "殭屍衝進了你的花園...";
        fm = g.getFontMetrics();
        g.drawString(sub, (Constants.WINDOW_WIDTH - fm.stringWidth(sub)) / 2, 285);

        String stat = "消滅殭屍: " + zombiesKilled + " 個  |  到達第 " + Math.min(wave, maxWaves) + " 波";
        g.setFont(new Font("Dialog", Font.PLAIN, 18));
        fm = g.getFontMetrics();
        g.drawString(stat, (Constants.WINDOW_WIDTH - fm.stringWidth(stat)) / 2, 325);

        int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
        drawButton(g, bx, Constants.END_BTN_RESTART_Y, Constants.END_BTN_W, Constants.END_BTN_H,
            new Color(60, 160, 60), new Color(40, 120, 40), "再玩一次", Color.WHITE, 22);
        drawButton(g, bx, Constants.END_BTN_MENU_Y, Constants.END_BTN_W, Constants.END_BTN_H,
            new Color(100, 60, 160), new Color(70, 40, 120), "返回主選單", Color.WHITE, 22);
    }

    private void drawButton(Graphics2D g, int x, int y, int w, int h,
                            Color fill, Color border, String text, Color textColor, int fontSize) {
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 14, 14);
        g.setColor(border);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, w, h, 14, 14);
        g.setStroke(new BasicStroke(1));
        g.setFont(new Font("Dialog", Font.BOLD, fontSize));
        g.setColor(textColor);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x + (w - fm.stringWidth(text)) / 2, y + h / 2 + fm.getAscent() / 2 - 2);
    }

    // ── Mouse events ───────────────────────────────────────────────────────────
    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        // Menu start
        if (state == State.MENU) {
            if (mx >= Constants.WINDOW_WIDTH / 2 - 100 && mx <= Constants.WINDOW_WIDTH / 2 + 100
                    && my >= 250 && my <= 305) {
                startGame();
            }
            return;
        }

        // Pause menu clicks
        if (state == State.PAUSED) {
            if (pauseBtnContinueBounds != null && pauseBtnContinueBounds.contains(mx, my)) {
                state = State.PLAYING;
                totalPauseTime += System.currentTimeMillis() - pauseStart;
                lastUpdate = System.currentTimeMillis();
                pauseBtnContinueBounds = null; pauseBtnRestartBounds = null; pauseBtnEndBounds = null;
                repaint();
            } else if (pauseBtnRestartBounds != null && pauseBtnRestartBounds.contains(mx, my)) {
                startGame();
                repaint();
            } else if (pauseBtnEndBounds != null && pauseBtnEndBounds.contains(mx, my)) {
                state = State.LOSE;
                timer.stop();
                int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
                endBtnRestartBounds = new Rectangle(bx, Constants.END_BTN_RESTART_Y, Constants.END_BTN_W, Constants.END_BTN_H);
                endBtnMenuBounds = new Rectangle(bx, Constants.END_BTN_MENU_Y, Constants.END_BTN_W, Constants.END_BTN_H);
                pauseBtnContinueBounds = null; pauseBtnRestartBounds = null; pauseBtnEndBounds = null;
                repaint();
            }
            return;
        }

        // End screen buttons (use drawn bounds for accuracy)
        if (state == State.WIN || state == State.LOSE) {
            if (endBtnRestartBounds != null && endBtnRestartBounds.contains(mx, my)) {
                startGame();
                repaint();
            } else if (endBtnMenuBounds != null && endBtnMenuBounds.contains(mx, my)) {
                state = State.MENU;
                timer.stop();
                // clear button bounds when returning to menu
                endBtnRestartBounds = null;
                endBtnMenuBounds = null;
                repaint();
            }
            return;
        }
    }
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        mousePos = e.getPoint();
        int mx = e.getX(), my = e.getY();
        int newCol = (mx - Constants.GRID_X) / Constants.CELL_W;
        int newRow = (my - Constants.GRID_Y) / Constants.CELL_H;
        if (newCol < 0 || newCol >= Constants.COLS || newRow < 0 || newRow >= Constants.ROWS) {
            hoverCol = hoverRow = -1;
            // if we had a selected tile, cancel it when mouse leaves grid
            if (selectedTileCol >= 0) {
                selectedTileCol = selectedTileRow = -1;
                removeBtnBounds = null;
            }
            return;
        }
        hoverCol = newCol;
        hoverRow = newRow;
        // If we've previously clicked to show the overlay and now moved outside that tile, cancel it
        if (selectedTileCol >= 0 && (hoverCol != selectedTileCol || hoverRow != selectedTileRow)) {
            selectedTileCol = selectedTileRow = -1;
            removeBtnBounds = null;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        // menu and end-screen clicks handled in mouseClicked to ensure clicks register

        if (state == State.PAUSED) {
            // Do not auto-resume on arbitrary clicks; pause menu handles clicks explicitly
            return;
        }

        if (state != State.PLAYING) return;

        // Right click: deselect
        if (e.getButton() == MouseEvent.BUTTON3) {
            selectedPlant = null;
            return;
        }

        // If remove-button is visible and clicked -> remove plant
        if (removeBtnBounds != null && removeBtnBounds.contains(mx, my)) {
            if (selectedTileRow >= 0 && selectedTileCol >= 0 && grid[selectedTileRow][selectedTileCol] != null) {
                grid[selectedTileRow][selectedTileCol] = null;
                addFloatText("已移除植物", mx, my - 10, new Color(255, 200, 100), 16, 800);
            }
            selectedTileRow = selectedTileCol = -1;
            removeBtnBounds = null;
            return;
        }

        // Check sun collection
        for (Sun s : suns) {
            if (s.getBounds().contains(mx, my)) {
                s.collected = true;
                sunCount += Constants.SUN_VALUE;
                addFloatText("+25 ☀", mx, my, new Color(255, 230, 50), 18, 900);
                return;
            }
        }

        // Check shop clicks
        PlantType[] types = PlantType.values();
        for (int i = 0; i < types.length; i++) {
            int ix = Constants.SHOP_START_X + i * (Constants.SHOP_ITEM_W + 8);
            int iy = Constants.SHOP_Y + 5;
            if (mx >= ix && mx <= ix + Constants.SHOP_ITEM_W && my >= iy && my <= iy + Constants.SHOP_ITEM_H) {
                if (sunCount >= types[i].cost) {
                    selectedPlant = types[i];
                }
                return;
            }
        }

        // Grid placement (hover controls remove-overlay visibility)
        int clickedCol = (mx - Constants.GRID_X) / Constants.CELL_W;
        int clickedRow = (my - Constants.GRID_Y) / Constants.CELL_H;
        boolean clickedOnGrid = clickedCol >= 0 && clickedCol < Constants.COLS && clickedRow >= 0 && clickedRow < Constants.ROWS;

        if (selectedPlant != null && clickedOnGrid) {
            if (grid[clickedRow][clickedCol] == null && sunCount >= selectedPlant.cost) {
                Plant newPlant = switch (selectedPlant) {
                    case SUNFLOWER -> new Sunflower(clickedCol, clickedRow);
                    case PEASHOOTER -> new Peashooter(clickedCol, clickedRow);
                    case WALLNUT -> new Wallnut(clickedCol, clickedRow);
                    case SNOWPEA -> new SnowPea(clickedCol, clickedRow);
                    case CHERRYBOMB -> new CherryBomb(clickedCol, clickedRow);
                };
                grid[clickedRow][clickedCol] = newPlant;
                sunCount -= selectedPlant.cost;
                addFloatText("-" + selectedPlant.cost + " ☀", mx, my - 20,
                        new Color(220, 180, 50), 16, 800);
                // Keep selected for rapid planting (deselect cherry bomb after place)
                if (selectedPlant == PlantType.CHERRYBOMB) selectedPlant = null;
            }
        } else if (clickedOnGrid) {
            // Click-to-show overlay: if clicking an occupied tile, show remove/X button.
            if (grid[clickedRow][clickedCol] != null) {
                selectedTileCol = clickedCol;
                selectedTileRow = clickedRow;
            } else {
                // clicked empty tile -> cancel any shown overlay
                selectedTileCol = selectedTileRow = -1;
                removeBtnBounds = null;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    // ── Keyboard ───────────────────────────────────────────────────────────────
    public void handleKey(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (state == State.PLAYING) {
                // Enter pause menu
                state = State.PAUSED;
                pauseStart = System.currentTimeMillis();
                int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
                pauseBtnContinueBounds = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y, Constants.END_BTN_W, Constants.END_BTN_H);
                pauseBtnRestartBounds  = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP, Constants.END_BTN_W, Constants.END_BTN_H);
                pauseBtnEndBounds      = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP * 2, Constants.END_BTN_W, Constants.END_BTN_H);
                repaint();
            } else if (state == State.PAUSED) {
                // Resume game
                state = State.PLAYING;
                totalPauseTime += System.currentTimeMillis() - pauseStart;
                lastUpdate = System.currentTimeMillis();
                pauseBtnContinueBounds = null;
                pauseBtnRestartBounds = null;
                pauseBtnEndBounds = null;
                repaint();
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void addFloatText(String text, int x, int y, Color color, int size, int duration) {
        floatTexts.add(new FloatText(text, x, y, color, size,
                System.currentTimeMillis() + duration));
    }

    // ── Inner: FloatText ───────────────────────────────────────────────────────
    static class FloatText {
        String text; double x, y; Color color; int size; long expireTime;
        FloatText(String text, double x, double y, Color color, int size, long expireTime) {
            this.text = text; this.x = x; this.y = y;
            this.color = color; this.size = size; this.expireTime = expireTime;
        }
        void draw(Graphics2D g) {
            long remaining = expireTime - System.currentTimeMillis();
            float alpha = remaining / 400f;
            alpha = Math.max(0f, Math.min(1f, alpha));
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setFont(new Font("Dialog", Font.BOLD, size));
            int alphaInt = Math.max(0, Math.min(255, (int)(120 * alpha)));
            g.setColor(new Color(0, 0, 0, alphaInt));
            g.drawString(text, (int) x + 2, (int) y + 2);
            g.setColor(color);
            g.drawString(text, (int) x, (int) y);
            g.setComposite(old);
        }
    }
}

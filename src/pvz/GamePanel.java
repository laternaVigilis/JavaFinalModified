package pvz;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import pvz.Plant.*;
import pvz.Zombie.*;

public class GamePanel extends JPanel {

    // ── Game state ─────────────────────────────────────────────────────────────
    public enum State { MENU, PLAYING, PAUSED, WIN, LOSE }
    private State state = State.MENU;

    // ── Game model (moved to GameWorld) ─────────────────────────────────────────
    private final GameWorld world = new GameWorld();

    // ── Timing / UI ────────────────────────────────────────────────────────────
    private long lastUpdate;
    private long gameStartTime;
    private long pauseStart;
    private long totalPauseTime;

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

    // ── Game engine (controls timer & update loop) ─────────────────────────────
    private GameEngine engine;

    // ── Fonts ──────────────────────────────────────────────────────────────────
    private Font bigFont, medFont, smallFont;

    // ── Window (merged from GameWindow) ────────────────────────────────────────
    private JFrame frame;

    public GamePanel() {
        setPreferredSize(new Dimension(Constants.WINDOW_WIDTH, Constants.WINDOW_HEIGHT));
        setBackground(new Color(80, 140, 60));
        // allow keyboard focus so ESC and other keys work
        setFocusable(true);
        // attach extracted input listener
        GameInputListener inputListener = new GameInputListener(this, world);
        addMouseListener(inputListener);
        addMouseMotionListener(inputListener);

        // Keyboard input
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e);
            }
        });

        // engine created after world is initialized
        engine = new GameEngine(this, world);
        initFonts();
        initWindow();
    }

    private void initWindow() {
        frame = new JFrame("🌻 植物大戰殭屍 - Java Edition");
        frame.add(this);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
    }

    public JFrame getFrame() {
        return frame;
    }

    // Exposed for GameEngine to inspect/control the state and timing
    public State getState() { return state; }
    public void setState(State s) { this.state = s; }
    public GameWorld getWorld() { return world; }
    public synchronized long getLastUpdate() { return lastUpdate; }
    public synchronized void setLastUpdate(long v) { lastUpdate = v; }
    // Expose small accessors used by input listener
    public PlantType getSelectedPlant() { return selectedPlant; }
    public void setSelectedPlant(PlantType p) { selectedPlant = p; }
    public int getHoverCol() { return hoverCol; }
    public int getHoverRow() { return hoverRow; }
    public void setHoverColRow(int c, int r) { hoverCol = c; hoverRow = r; }
    public int getSelectedTileCol() { return selectedTileCol; }
    public int getSelectedTileRow() { return selectedTileRow; }
    public void setSelectedTile(int c, int r) { selectedTileCol = c; selectedTileRow = r; }
    public Rectangle getRemoveBtnBounds() { return removeBtnBounds; }
    public void setRemoveBtnBounds(Rectangle r) { removeBtnBounds = r; }
    public Point getMousePos() { return mousePos; }
    public void setMousePos(Point p) { mousePos = p; }
    public void stopEngine() { if (engine != null) engine.stop(); }
    // prepare end-screen button rectangles (used by engine when game ends)
    public void prepareEndScreenButtons() {
        int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
        endBtnRestartBounds = new Rectangle(bx, Constants.END_BTN_RESTART_Y, Constants.END_BTN_W, Constants.END_BTN_H);
        endBtnMenuBounds = new Rectangle(bx, Constants.END_BTN_MENU_Y, Constants.END_BTN_W, Constants.END_BTN_H);
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
        world.start();
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
        engine.start();
        // ensure this panel has keyboard focus so ESC is received
        javax.swing.SwingUtilities.invokeLater(() -> requestFocusInWindow());
    }

    // Game updates are driven by GameEngine -> see src/pvz/GameEngine.java

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
                if (world.grid[r][c] != null) world.grid[r][c].draw(g);

        // Remove-button overlay for selected deployed tile
        drawRemoveButton(g);

        // Peas
        for (Pea p : world.peas) p.draw(g);

        // Zombies
        for (Zombie z : world.zombies) z.draw(g);

        // Suns
        for (Sun s : world.suns) s.draw(g);

        // Explosions
        for (Explosion ex : world.explosions) ex.draw(g);

        // Float texts
        for (FloatText ft : world.floatTexts) ft.draw(g);

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
            boolean canPlace = world.grid[hoverRow][hoverCol] == null && world.sunCount >= selectedPlant.cost;
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
        String sunStr = String.valueOf(world.sunCount);
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
            boolean affordable = world.sunCount >= pt.cost;
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
        g.setColor(new Color(0, 100, 0));
        g.setFont(new Font("Dialog", Font.BOLD, 14));
        String waveStr = "第 " + Math.min(world.wave, world.maxWaves) + "/" + world.maxWaves + " 波";
        g.drawString(waveStr, 780, 40);
        g.setColor(new Color(0, 100, 0));
        g.setFont(new Font("Dialog", Font.PLAIN, 13));
        g.drawString("消滅: " + world.zombiesKilled, 780, 60);

        // ESC hint
        g.setColor(new Color(0, 100, 0));
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
        if (world.grid[selectedTileRow][selectedTileCol] == null) {
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

        String stat = "消滅殭屍: " + world.zombiesKilled + " 個  |  到達第 " + Math.min(world.wave, world.maxWaves) + " 波";
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

    // Mouse input handling has been moved to pvz.GameInputListener

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

    // Helper methods for the input listener to control pause/resume flows
    public void enterPauseMenu() {
        state = State.PAUSED;
        pauseStart = System.currentTimeMillis();
        int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
        pauseBtnContinueBounds = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y, Constants.END_BTN_W, Constants.END_BTN_H);
        pauseBtnRestartBounds  = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP, Constants.END_BTN_W, Constants.END_BTN_H);
        pauseBtnEndBounds      = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP * 2, Constants.END_BTN_W, Constants.END_BTN_H);
        repaint();
    }

    public void resumeFromPause() {
        state = State.PLAYING;
        totalPauseTime += System.currentTimeMillis() - pauseStart;
        lastUpdate = System.currentTimeMillis();
        pauseBtnContinueBounds = null; pauseBtnRestartBounds = null; pauseBtnEndBounds = null;
        repaint();
    }

    public void endGameFromPauseAsLose() {
        state = State.LOSE;
        if (engine != null) engine.stop();
        prepareEndScreenButtons();
        pauseBtnContinueBounds = null; pauseBtnRestartBounds = null; pauseBtnEndBounds = null;
        repaint();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private void addFloatText(String text, int x, int y, Color color, int size, int duration) {
        world.addFloatText(text, x, y, color, size, duration);
    }

    // FloatText was moved to its own file: pvz/FloatText.java
}

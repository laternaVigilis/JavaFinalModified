package pvz;

import java.awt.*;

public class Plant {
    public final PlantType type;
    public final int col, row;
    public int hp;
    public long lastShootTime;
    public long lastSunTime;
    public long placedTime;
    public boolean exploding;      // cherry bomb
    public long   explodeStartTime;

    public Plant(PlantType type, int col, int row) {
        this.type      = type;
        this.col       = col;
        this.row       = row;
        this.hp        = type.maxHp;
        this.placedTime = System.currentTimeMillis();
    }

    public boolean isDead() {
        return hp <= 0;
    }

    /** Pixel center X */
    public int cx() {
        return Constants.GRID_X + col * Constants.CELL_W + Constants.CELL_W / 2;
    }

    /** Pixel center Y */
    public int cy() {
        return Constants.GRID_Y + row * Constants.CELL_H + Constants.CELL_H / 2;
    }

    public void draw(Graphics2D g) {
        int x = Constants.GRID_X + col * Constants.CELL_W;
        int y = Constants.GRID_Y + row * Constants.CELL_H;
        int w = Constants.CELL_W;
        int h = Constants.CELL_H;
        int cx = x + w / 2;
        int cy = y + h / 2;

        switch (type) {
            case SUNFLOWER   -> drawSunflower(g, cx, cy);
            case PEASHOOTER  -> drawPeashooter(g, cx, cy);
            case WALLNUT     -> drawWallnut(g, cx, cy);
            case SNOWPEA     -> drawSnowPea(g, cx, cy);
            case CHERRYBOMB  -> drawCherryBomb(g, cx, cy);
        }

        // HP bar
        int barW = w - 10;
        int barH = 6;
        int bx   = x + 5;
        int by   = y + h - 12;
        g.setColor(Color.DARK_GRAY);
        g.fillRect(bx, by, barW, barH);
        float ratio = (float) hp / type.maxHp;
        g.setColor(ratio > 0.5f ? new Color(80, 200, 80)
                 : ratio > 0.25f ? new Color(220, 180, 0)
                 : new Color(200, 60, 60));
        g.fillRect(bx, by, (int)(barW * ratio), barH);
        g.setColor(Color.BLACK);
        g.drawRect(bx, by, barW, barH);
    }

    private void drawSunflower(Graphics2D g, int cx, int cy) {
        // Petals
        g.setColor(new Color(255, 200, 0));
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            int px = (int)(cx + Math.cos(angle) * 20);
            int py = (int)(cy + Math.sin(angle) * 20);
            g.fillOval(px - 8, py - 8, 16, 16);
        }
        // Center
        g.setColor(new Color(140, 80, 20));
        g.fillOval(cx - 14, cy - 14, 28, 28);
        // Face
        g.setColor(Color.BLACK);
        g.fillOval(cx - 5, cy - 5, 4, 4);
        g.fillOval(cx + 1, cy - 5, 4, 4);
        g.drawArc(cx - 5, cy, 10, 6, 0, -180);
        // Stem
        g.setColor(new Color(60, 160, 60));
        g.setStroke(new BasicStroke(4));
        g.drawLine(cx, cy + 14, cx, cy + 40);
        g.setStroke(new BasicStroke(1));
    }

    private void drawPeashooter(Graphics2D g, int cx, int cy) {
        // Body
        g.setColor(new Color(60, 180, 60));
        g.fillOval(cx - 18, cy - 20, 36, 40);
        g.setColor(new Color(30, 140, 30));
        g.drawOval(cx - 18, cy - 20, 36, 40);
        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(cx - 10, cy - 15, 10, 10);
        g.fillOval(cx + 0,  cy - 15, 10, 10);
        g.setColor(Color.BLACK);
        g.fillOval(cx - 7, cy - 12, 5, 5);
        g.fillOval(cx + 3, cy - 12, 5, 5);
        // Mouth / barrel
        g.setColor(new Color(20, 120, 20));
        g.fillRoundRect(cx + 12, cy - 5, 22, 12, 6, 6);
        g.setColor(Color.BLACK);
        g.drawRoundRect(cx + 12, cy - 5, 22, 12, 6, 6);
        // Leaf
        g.setColor(new Color(80, 200, 80));
        int[] lx = {cx - 5, cx - 25, cx - 10};
        int[] ly = {cy + 10, cy + 20, cy + 25};
        g.fillPolygon(lx, ly, 3);
    }

    private void drawWallnut(Graphics2D g, int cx, int cy) {
        float ratio = (float) hp / type.maxHp;
        Color base = ratio > 0.5f ? new Color(180, 120, 50)
                   : ratio > 0.25f ? new Color(160, 100, 40)
                   : new Color(120, 70, 30);
        // Shell
        g.setColor(base);
        g.fillOval(cx - 22, cy - 26, 44, 52);
        g.setColor(new Color(100, 60, 20));
        g.drawOval(cx - 22, cy - 26, 44, 52);
        // Crack lines at low HP
        if (ratio < 0.5f) {
            g.setColor(new Color(80, 40, 10));
            g.setStroke(new BasicStroke(2));
            g.drawLine(cx - 5, cy - 20, cx + 5, cy);
            g.drawLine(cx + 3, cy - 5, cx - 3, cy + 15);
            g.setStroke(new BasicStroke(1));
        }
        // Face
        g.setColor(Color.BLACK);
        g.fillOval(cx - 9, cy - 10, 6, 7);
        g.fillOval(cx + 3, cy - 10, 6, 7);
        if (ratio > 0.25f) {
            g.drawArc(cx - 7, cy + 2, 14, 8, 0, -180);
        } else {
            g.drawArc(cx - 7, cy + 4, 14, 8, 0, 180); // sad face when dying
        }
    }

    private void drawSnowPea(Graphics2D g, int cx, int cy) {
        // Body (icy blue)
        g.setColor(new Color(100, 200, 255));
        g.fillOval(cx - 18, cy - 20, 36, 40);
        g.setColor(new Color(50, 150, 220));
        g.drawOval(cx - 18, cy - 20, 36, 40);
        // Ice crystals
        g.setColor(new Color(200, 240, 255, 180));
        for (int i = 0; i < 3; i++) {
            int ix = cx - 12 + i * 10;
            int iy = cy - 8;
            g.fillPolygon(new int[]{ix, ix - 4, ix + 4}, new int[]{iy - 8, iy + 4, iy + 4}, 3);
        }
        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval(cx - 10, cy - 12, 10, 10);
        g.fillOval(cx + 0,  cy - 12, 10, 10);
        g.setColor(new Color(0, 80, 160));
        g.fillOval(cx - 7, cy - 9, 5, 5);
        g.fillOval(cx + 3, cy - 9, 5, 5);
        // Barrel
        g.setColor(new Color(50, 150, 210));
        g.fillRoundRect(cx + 12, cy - 5, 22, 12, 6, 6);
        g.setColor(Color.WHITE);
        g.drawRoundRect(cx + 12, cy - 5, 22, 12, 6, 6);
    }

    private void drawCherryBomb(Graphics2D g, int cx, int cy) {
        long now = System.currentTimeMillis();
        boolean blink = ((now - placedTime) / 300) % 2 == 0;

        // Two cherries
        Color c1 = blink ? new Color(255, 60, 60) : new Color(200, 30, 30);
        Color c2 = blink ? new Color(220, 30, 30) : new Color(170, 10, 10);
        g.setColor(c1);
        g.fillOval(cx - 20, cy - 15, 24, 24);
        g.setColor(c2);
        g.fillOval(cx - 4,  cy - 15, 24, 24);
        // Stems
        g.setColor(new Color(60, 120, 30));
        g.setStroke(new BasicStroke(2));
        g.drawLine(cx - 8, cy - 14, cx - 12, cy - 30);
        g.drawLine(cx + 8, cy - 14, cx + 12, cy - 30);
        g.setStroke(new BasicStroke(1));
        // Leaves
        g.setColor(new Color(80, 180, 40));
        g.fillOval(cx - 14, cy - 32, 14, 8);
        // Fuse spark
        if (blink) {
            g.setColor(Color.YELLOW);
            g.fillOval(cx - 4, cy - 34, 8, 8);
        }
        // Faces
        g.setColor(Color.BLACK);
        g.fillOval(cx - 15, cy - 10, 3, 3);
        g.fillOval(cx - 9,  cy - 10, 3, 3);
        g.fillOval(cx + 1,  cy - 10, 3, 3);
        g.fillOval(cx + 7,  cy - 10, 3, 3);
    }
}

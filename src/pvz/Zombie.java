package pvz;

import java.awt.*;

public abstract class Zombie {
    // Template IDs
    public static final int TEMPLATE_NORMAL = 0;
    public static final int TEMPLATE_FAST   = 1;
    public static final int TEMPLATE_TANK   = 2;
    public static final int TEMPLATE_CHERRY = 3;
    // Global HP bonus applied every 3 waves
    private static int globalHpBonus = 0;

    public static void addGlobalHpBonus(int amount) { globalHpBonus += amount; }
    public static int getGlobalHpBonus() { return globalHpBonus; }

    public int row;
    public double x;          // pixel X
    public int hp;
    public int maxHp;
    public final int templateId; // identifies the subclass/template
    public final char idChar;    // display char (e.g. 'N','F','T')
    public final int level;
    public final int baseHp;
    public final double baseSpeed;
    public final int baseAttack;
    public long lastAttackTime;
    public boolean attacking;
    public double speedFactor; // base speed multiplier, slowed when frozen
    public long slowUntil;
    protected long animTick;
    protected double legPhase;

    public static final int ZOMBIE_W = 50;
    public static final int ZOMBIE_H = 70;

    protected Zombie(int row, int level, int templateId, char idChar, int baseHp, double baseSpeed, int baseAttack) {
        this.row         = row;
        this.x           = Constants.WINDOW_WIDTH + 20;
        this.templateId  = templateId;
        this.idChar      = idChar;
        this.level       = level;
        this.baseHp      = baseHp;
        this.baseSpeed   = baseSpeed;
        this.baseAttack  = baseAttack;
        this.maxHp       = calculateHp(baseHp, level);
        this.hp          = maxHp;
        this.speedFactor = baseSpeed;
        this.legPhase    = 0;
    }

    private static int calculateHp(int baseHp, int level) {
        int effectiveBase = baseHp + globalHpBonus;
        return (int) (effectiveBase * (1 + (level - 1) * Constants.ZOMBIE_LEVEL_HP_MULT));
    }

    public boolean isDead() { return hp <= 0; }

    public int getPixelY() {
        return Constants.GRID_Y + row * Constants.CELL_H;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, getPixelY(), ZOMBIE_W, ZOMBIE_H);
    }

    public void update(double dt) {
        animTick++;
        if (System.currentTimeMillis() > slowUntil) speedFactor = baseSpeed;
        if (!attacking) {
            x -= Constants.ZOMBIE_SPEED * speedFactor * dt;
            legPhase += 4 * speedFactor * dt;
        }
    }

    // Shared drawing helpers (subclasses should call these)
    protected void drawShadow(Graphics2D g, int px, int py) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(px + 5, py + ZOMBIE_H - 5, ZOMBIE_W - 10, 10);
    }

    protected void drawLegsAndShoes(Graphics2D g, int px, int py, Color pantsColor, int legW) {
        int legOff = (int)(Math.sin(legPhase) * 8);
        g.setColor(pantsColor);
        g.fillRect(px + 12, py + 45, legW, 22 + legOff);
        g.fillRect(px + 28, py + 45, legW, 22 - legOff);
        // Shoes
        g.setColor(new Color(60, 40, 20));
        g.fillRoundRect(px + 9,  py + 65 + legOff,  16, 8, 4, 4);
        g.fillRoundRect(px + 25, py + 65 - legOff,  16, 8, 4, 4);
    }

    protected void drawHpBar(Graphics2D g, int px, int py) {
        int barW = ZOMBIE_W;
        int barH = 6 + (level - 1) * 2;
        int barY = py - 12 - barH;
        g.setColor(Color.DARK_GRAY);
        g.fillRect(px, barY, barW, barH);
        float ratio = (float) hp / maxHp;
        g.setColor(ratio > 0.5f ? new Color(80, 200, 80) : ratio > 0.25f ? new Color(220, 180, 0) : new Color(200, 60, 60));
        g.fillRect(px, barY, (int)(barW * ratio), barH);
        g.setColor(Color.BLACK);
        g.drawRect(px, barY, barW, barH);
        g.setFont(new Font("Dialog", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        g.drawString(idChar + "" + level, px + barW / 2 - 6, barY - 4);
    }

    // Subclasses must implement their own visual appearance
    public abstract void draw(Graphics2D g);

    // Return attack power (template stat)
    public int attackPower() { return baseAttack; }
}

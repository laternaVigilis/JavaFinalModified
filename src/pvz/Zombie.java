package pvz;

import java.awt.*;

public class Zombie {
    public enum Type { NORMAL, FAST, TANK }

    public int row;
    public double x;          // pixel X
    public int hp;
    public int maxHp;
    public final Type type;
    public final int level;
    public final double baseSpeed;
    public long lastAttackTime;
    public boolean attacking;
    public double speedFactor; // base speed multiplier, slowed when frozen
    public long slowUntil;
    private long animTick;
    private double legPhase;

    public static final int ZOMBIE_W = 50;
    public static final int ZOMBIE_H = 70;

    public Zombie(int row, Type type, int level) {
        this.row         = row;
        this.x           = Constants.WINDOW_WIDTH + 20;
        this.type        = type;
        this.level       = level;
        if (type == Type.FAST) {
            this.baseSpeed = 1.4;
        } else if (type == Type.TANK) {
            this.baseSpeed = 0.75;
        } else {
            this.baseSpeed = 1.0;
        }
        this.maxHp       = calculateHp(type, level);
        this.hp          = maxHp;
        this.speedFactor = baseSpeed;
        this.legPhase    = 0;
    }

    private static int calculateHp(Type type, int level) {
        int baseHp;
        if (type == Type.FAST) {
            baseHp = Constants.ZOMBIE_FAST_HP;
        } else if (type == Type.TANK) {
            baseHp = Constants.ZOMBIE_TANK_HP;
        } else {
            baseHp = Constants.ZOMBIE_HP;
        }
        return (int) (baseHp * (1 + (level - 1) * Constants.ZOMBIE_LEVEL_HP_MULT));
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

    public void draw(Graphics2D g) {
        int px = (int) x;
        int py = getPixelY() + 5;

        boolean slowed = System.currentTimeMillis() < slowUntil;
        Color skinColor  = slowed ? new Color(140, 200, 230) : new Color(150, 200, 120);
        Color shirtColor = slowed ? new Color(80, 120, 180)  : new Color(80, 120, 60);
        Color pantsColor = slowed ? new Color(60, 90, 160)   : new Color(60, 90, 40);

        // Shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(px + 5, py + ZOMBIE_H - 5, ZOMBIE_W - 10, 10);

        // Legs (animated)
        int legOff = (int)(Math.sin(legPhase) * 8);
        g.setColor(pantsColor);
        g.fillRect(px + 12, py + 45, 10, 22 + legOff);
        g.fillRect(px + 28, py + 45, 10, 22 - legOff);
        // Shoes
        g.setColor(new Color(60, 40, 20));
        g.fillRoundRect(px + 9,  py + 65 + legOff,  16, 8, 4, 4);
        g.fillRoundRect(px + 25, py + 65 - legOff,  16, 8, 4, 4);

        // Body / shirt
        g.setColor(shirtColor);
        g.fillRoundRect(px + 8, py + 28, 34, 22, 8, 8);
        // Tattered effect
        g.setColor(shirtColor.darker());
        g.fillRect(px + 10, py + 44, 6, 6);
        g.fillRect(px + 28, py + 44, 8, 4);

        // Arms
        g.setColor(skinColor);
        if (attacking) {
            // Arms stretched forward (to the left)
            g.fillRoundRect(px - 10, py + 28, 22, 10, 5, 5);
            g.fillRoundRect(px - 10, py + 38, 20, 10, 5, 5);
        } else {
            int armSwing = (int)(Math.sin(legPhase + Math.PI / 2) * 5);
            g.fillRoundRect(px + 2,  py + 30, 10, 18 + armSwing, 5, 5);
            g.fillRoundRect(px + 38, py + 30, 10, 18 - armSwing, 5, 5);
        }

        // Head
        g.setColor(skinColor);
        g.fillOval(px + 10, py + 2, 30, 28);
        g.setColor(skinColor.darker());
        g.drawOval(px + 10, py + 2, 30, 28);

        // Hair (messy)
        g.setColor(new Color(60, 40, 20));
        g.fillRect(px + 12, py + 2, 26, 8);
        g.fillOval(px + 8,  py,     12, 10);
        g.fillOval(px + 30, py,     14, 8);

        // Eyes (zombie: lopsided)
        g.setColor(new Color(200, 30, 30));
        g.fillOval(px + 14, py + 12, 7, 8);
        g.fillOval(px + 27, py + 14, 7, 6);
        g.setColor(Color.BLACK);
        g.fillOval(px + 16, py + 14, 4, 5);
        g.fillOval(px + 29, py + 15, 4, 4);

        // Mouth
        g.setColor(new Color(80, 20, 20));
        g.fillArc(px + 16, py + 22, 16, 6, 0, -180);
        // Drool
        g.setColor(new Color(100, 180, 80, 180));
        g.fillOval(px + 22, py + 28, 5, 7);

        // HP bar
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
        g.drawString(type.name().charAt(0) + "" + level, px + barW / 2 - 6, barY - 4);
    }
}

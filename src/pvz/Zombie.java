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
    private long animTick;
    private double legPhase;

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

    public void draw(Graphics2D g) {
        int px = (int) x;
        int py = getPixelY() + 5;

        boolean slowed = System.currentTimeMillis() < slowUntil;

        // Choose colors/visuals based on templateId + level so each zombie looks appropriate
        Color skinColor;
        Color shirtColor;
        Color pantsColor;
        int bodyW = 34; // base body width
        int bodyH = 22; // base body height
        int legW = 10;   // base leg width

        switch (templateId) {
            case TEMPLATE_FAST -> {
                skinColor = slowed ? new Color(160, 210, 240) : new Color(180, 220, 160);
                Color[] shirts = new Color[]{ new Color(200, 60, 60), new Color(220, 80, 80), new Color(240, 120, 80), new Color(220, 60, 60) };
                int idx = Math.max(0, Math.min(level - 1, shirts.length - 1));
                shirtColor = slowed ? shirts[idx].brighter() : shirts[idx];
                pantsColor  = slowed ? new Color(50, 60, 70).brighter() : new Color(50, 60, 70);
                bodyW = 30; bodyH = 20; legW = 8; // slimmer
            }
            case TEMPLATE_TANK -> {
                skinColor = slowed ? new Color(120, 170, 180) : new Color(120, 160, 120);
                Color[] shirts = new Color[]{ new Color(110, 110, 120), new Color(120, 120, 130), new Color(140, 130, 120), new Color(90, 90, 100) };
                int idx = Math.max(0, Math.min(level - 1, shirts.length - 1));
                shirtColor = slowed ? shirts[idx].brighter() : shirts[idx];
                pantsColor  = slowed ? new Color(60, 70, 80).brighter() : new Color(60, 70, 80);
                bodyW = 40; bodyH = 26; legW = 12; // bulkier
            }
            default -> {
                // Normal
                skinColor = slowed ? new Color(140, 200, 230) : new Color(150, 200, 120);
                Color[] shirts = new Color[]{ new Color(80, 120, 60), new Color(100, 140, 80), new Color(120, 160, 90), new Color(140, 110, 70) };
                Color[] pants  = new Color[]{ new Color(60, 90, 40),  new Color(60, 95, 45),  new Color(60,100,50),  new Color(60,85,35) };
                int idx = Math.max(0, Math.min(level - 1, shirts.length - 1));
                shirtColor = slowed ? shirts[idx].brighter() : shirts[idx];
                pantsColor  = slowed ? pants[idx].brighter()  : pants[idx];
                bodyW = 34; bodyH = 22; legW = 10;
            }
        }

        // Shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(px + 5, py + ZOMBIE_H - 5, ZOMBIE_W - 10, 10);

        // Legs (animated)
        int legOff = (int)(Math.sin(legPhase) * 8);
        g.setColor(pantsColor);
        g.fillRect(px + 12, py + 45, legW, 22 + legOff);
        g.fillRect(px + 28, py + 45, legW, 22 - legOff);
        // Shoes
        g.setColor(new Color(60, 40, 20));
        g.fillRoundRect(px + 9,  py + 65 + legOff,  16, 8, 4, 4);
        g.fillRoundRect(px + 25, py + 65 - legOff,  16, 8, 4, 4);

        // Body / shirt (size varies by template)
        g.setColor(shirtColor);
        int bodyX = px + 8 - (bodyW - 34) / 2;
        g.fillRoundRect(bodyX, py + 28, bodyW, bodyH, 8, 8);
        // Tattered effect (slightly different for tank)
        g.setColor(shirtColor.darker());
        g.fillRect(bodyX + 2, py + 28 + bodyH - 6, Math.max(6, bodyW/6), 6);
        if (templateId == TEMPLATE_NORMAL) {
            g.fillRect(bodyX + bodyW - 8, py + 28 + bodyH - 6, Math.max(6, bodyW/5), 4);
        }

        // Arms (vary thickness for tank/fast)
        g.setColor(skinColor);
        int armW = (templateId == TEMPLATE_TANK) ? 14 : (templateId == TEMPLATE_FAST) ? 8 : 10;
        if (attacking) {
            // Arms stretched forward (to the left)
            g.fillRoundRect(px - 10, py + 28, 22, armW, 5, 5);
            g.fillRoundRect(px - 10, py + 38, 20, armW, 5, 5);
        } else {
            int armSwing = (int)(Math.sin(legPhase + Math.PI / 2) * 5);
            g.fillRoundRect(px + 2,  py + 30, armW, 18 + armSwing, 5, 5);
            g.fillRoundRect(px + 38, py + 30, armW, 18 - armSwing, 5, 5);
        }

        // Head (cherry variant replaces normal head)
        if (templateId == TEMPLATE_CHERRY) {
            // Cherry head: two cherries with stems
            int leftX = px + 10;
            int topY  = py - 6;
            g.setColor(new Color(200, 30, 30));
            g.fillOval(leftX, topY, 20, 20);
            g.fillOval(leftX + 16, topY, 20, 20);
            // Highlights
            g.setColor(new Color(255, 200, 200, 160));
            g.fillOval(leftX + 4, topY + 4, 6, 6);
            g.fillOval(leftX + 20, topY + 4, 6, 6);
            // Stems
            g.setColor(new Color(60, 120, 40));
            g.setStroke(new BasicStroke(2));
            g.drawLine(leftX + 10, topY + 4, leftX + 8, topY - 12);
            g.drawLine(leftX + 30, topY + 4, leftX + 34, topY - 12);
            g.setStroke(new BasicStroke(1));
        } else {
            // Normal head
            g.setColor(skinColor);
            g.fillOval(px + 10, py + 2, 30, 28);
            g.setColor(skinColor.darker());
            g.drawOval(px + 10, py + 2, 30, 28);

            // Hair / helmet variants
            if (templateId == TEMPLATE_TANK) {
                // Helmet
                g.setColor(new Color(50, 50, 60));
                g.fillArc(px + 8, py - 2, 34, 18, 0, 180);
                g.setColor(new Color(40, 40, 50));
                g.drawArc(px + 8, py - 2, 34, 18, 0, 180);
            } else {
                // Hair (messy)
                g.setColor(new Color(60, 40, 20));
                g.fillRect(px + 12, py + 2, 26, 8);
                g.fillOval(px + 8,  py,     12, 10);
                g.fillOval(px + 30, py,     14, 8);
            }
        }

        // Eyes (zombie: lopsided) and face details per template
        g.setColor(new Color(200, 30, 30));
        g.fillOval(px + 14, py + 12, 7, 8);
        g.fillOval(px + 27, py + 14, 7, 6);
        g.setColor(Color.BLACK);
        g.fillOval(px + 16, py + 14, 4, 5);
        g.fillOval(px + 29, py + 15, 4, 4);
        if (templateId == TEMPLATE_FAST) {
            // scar or extra eye mark
            g.setColor(new Color(180, 20, 20));
            g.drawLine(px + 12, py + 18, px + 20, py + 16);
        }

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
        g.drawString(idChar + "" + level, px + barW / 2 - 6, barY - 4);
    }

    // Return attack power (template stat)
    public int attackPower() { return baseAttack; }
}

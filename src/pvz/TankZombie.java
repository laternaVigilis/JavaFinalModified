package pvz;

import java.awt.*;

public class TankZombie extends Zombie {
    public TankZombie(int row, int level) {
        super(row, level, TEMPLATE_TANK, 'T', Constants.ZOMBIE_TANK_HP, 0.75, Constants.ZOMBIE_DMG);
    }

    @Override
    public void draw(Graphics2D g) {
        int px = (int) x;
        int py = getPixelY() + 5;
        boolean slowed = System.currentTimeMillis() < slowUntil;

        Color skinColor = slowed ? new Color(120, 170, 180) : new Color(120, 160, 120);
        Color[] shirts = new Color[]{ new Color(110, 110, 120), new Color(120, 120, 130), new Color(140, 130, 120), new Color(90, 90, 100) };
        int idx = Math.max(0, Math.min(level - 1, shirts.length - 1));
        Color shirtColor = slowed ? shirts[idx].brighter() : shirts[idx];
        Color pantsColor  = slowed ? new Color(60, 70, 80).brighter() : new Color(60, 70, 80);
        int bodyW = 40, bodyH = 26, legW = 12;

        drawShadow(g, px, py);
        drawLegsAndShoes(g, px, py, pantsColor, legW);

        // Body
        g.setColor(shirtColor);
        int bodyX = px + 8 - (bodyW - 34) / 2;
        g.fillRoundRect(bodyX, py + 28, bodyW, bodyH, 8, 8);
        g.setColor(shirtColor.darker());
        g.fillRect(bodyX + 2, py + 28 + bodyH - 6, Math.max(6, bodyW/6), 6);

        // Arms (thicker)
        g.setColor(skinColor);
        int armW = 14;
        if (attacking) {
            g.fillRoundRect(px - 10, py + 28, 22, armW, 5, 5);
            g.fillRoundRect(px - 10, py + 38, 20, armW, 5, 5);
        } else {
            int armSwing = (int)(Math.sin(legPhase + Math.PI / 2) * 5);
            g.fillRoundRect(px + 2,  py + 30, armW, 18 + armSwing, 5, 5);
            g.fillRoundRect(px + 38, py + 30, armW, 18 - armSwing, 5, 5);
        }

        // Head with helmet
        g.setColor(skinColor);
        g.fillOval(px + 10, py + 2, 30, 28);
        g.setColor(skinColor.darker());
        g.drawOval(px + 10, py + 2, 30, 28);
        g.setColor(new Color(50, 50, 60));
        g.fillArc(px + 8, py - 2, 34, 18, 0, 180);
        g.setColor(new Color(40, 40, 50));
        g.drawArc(px + 8, py - 2, 34, 18, 0, 180);

        // Face
        g.setColor(new Color(200, 30, 30));
        g.fillOval(px + 14, py + 12, 7, 8);
        g.fillOval(px + 27, py + 14, 7, 6);
        g.setColor(Color.BLACK);
        g.fillOval(px + 16, py + 14, 4, 5);
        g.fillOval(px + 29, py + 15, 4, 4);

        // Mouth and drool
        g.setColor(new Color(80, 20, 20));
        g.fillArc(px + 16, py + 22, 16, 6, 0, -180);
        g.setColor(new Color(100, 180, 80, 180));
        g.fillOval(px + 22, py + 28, 5, 7);

        drawHpBar(g, px, py);
    }
}

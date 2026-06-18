package pvz;

import java.awt.*;

public class NormalZombie extends Zombie {
    public NormalZombie(int row, int level) {
        super(row, level, TEMPLATE_NORMAL, 'N', Constants.ZOMBIE_HP, 1.0, Constants.ZOMBIE_DMG);
    }

    @Override
    public void draw(Graphics2D g) {
        int px = (int) x;
        int py = getPixelY() + 5;
        boolean slowed = System.currentTimeMillis() < slowUntil;

        Color skinColor = slowed ? new Color(140, 200, 230) : new Color(150, 200, 120);
        Color[] shirts = new Color[]{ new Color(80, 120, 60), new Color(100, 140, 80), new Color(120, 160, 90), new Color(140, 110, 70) };
        Color[] pants  = new Color[]{ new Color(60, 90, 40),  new Color(60, 95, 45),  new Color(60,100,50),  new Color(60,85,35) };
        int idx = Math.max(0, Math.min(level - 1, shirts.length - 1));
        Color shirtColor = slowed ? shirts[idx].brighter() : shirts[idx];
        Color pantsColor  = slowed ? pants[idx].brighter()  : pants[idx];
        int bodyW = 34, bodyH = 22, legW = 10;

        drawShadow(g, px, py);
        drawLegsAndShoes(g, px, py, pantsColor, legW);

        // Body
        g.setColor(shirtColor);
        int bodyX = px + 8 - (bodyW - 34) / 2;
        g.fillRoundRect(bodyX, py + 28, bodyW, bodyH, 8, 8);
        g.setColor(shirtColor.darker());
        g.fillRect(bodyX + 2, py + 28 + bodyH - 6, Math.max(6, bodyW/6), 6);
        g.fillRect(bodyX + bodyW - 8, py + 28 + bodyH - 6, Math.max(6, bodyW/5), 4);

        // Arms
        g.setColor(skinColor);
        int armW = 10;
        if (attacking) {
            g.fillRoundRect(px - 10, py + 28, 22, armW, 5, 5);
            g.fillRoundRect(px - 10, py + 38, 20, armW, 5, 5);
        } else {
            int armSwing = (int)(Math.sin(legPhase + Math.PI / 2) * 5);
            g.fillRoundRect(px + 2,  py + 30, armW, 18 + armSwing, 5, 5);
            g.fillRoundRect(px + 38, py + 30, armW, 18 - armSwing, 5, 5);
        }

        // Head/hair
        g.setColor(skinColor);
        g.fillOval(px + 10, py + 2, 30, 28);
        g.setColor(skinColor.darker());
        g.drawOval(px + 10, py + 2, 30, 28);
        g.setColor(new Color(60, 40, 20));
        g.fillRect(px + 12, py + 2, 26, 8);
        g.fillOval(px + 8,  py,     12, 10);
        g.fillOval(px + 30, py,     14, 8);

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

package pvz;

import java.awt.*;

public class CherryBomb extends Plant {
    public CherryBomb(int col, int row) {
        super(PlantType.CHERRYBOMB, col, row, PlantType.CHERRYBOMB.maxHp, Constants.CHERRY_DMG, 'C');
    }

    @Override
    public void draw(Graphics2D g) {
        int x = Constants.GRID_X + col * Constants.CELL_W;
        int y = Constants.GRID_Y + row * Constants.CELL_H;
        int w = Constants.CELL_W;
        int h = Constants.CELL_H;
        int cx = x + w / 2;
        int cy = y + h / 2;

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
}

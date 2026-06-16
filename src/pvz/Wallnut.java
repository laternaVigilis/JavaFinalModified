package pvz;

import java.awt.*;

public class Wallnut extends Plant {
    public Wallnut(int col, int row) {
        super(PlantType.WALLNUT, col, row, PlantType.WALLNUT.maxHp, 0, 'W');
    }

    @Override
    public void draw(Graphics2D g) {
        int x = Constants.GRID_X + col * Constants.CELL_W;
        int y = Constants.GRID_Y + row * Constants.CELL_H;
        int w = Constants.CELL_W;
        int h = Constants.CELL_H;
        int cx = x + w / 2;
        int cy = y + h / 2;

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

        // HP bar
        int barW = w - 10;
        int barH = 6;
        int bx   = x + 5;
        int by   = y + h - 12;
        g.setColor(Color.DARK_GRAY);
        g.fillRect(bx, by, barW, barH);
        float ratio2 = (float) hp / type.maxHp;
        g.setColor(ratio2 > 0.5f ? new Color(80, 200, 80)
                 : ratio2 > 0.25f ? new Color(220, 180, 0)
                 : new Color(200, 60, 60));
        g.fillRect(bx, by, (int)(barW * ratio2), barH);
        g.setColor(Color.BLACK);
        g.drawRect(bx, by, barW, barH);
    }
}

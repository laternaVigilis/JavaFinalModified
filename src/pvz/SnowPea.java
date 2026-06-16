package pvz;

import java.awt.*;

public class SnowPea extends Plant {
    public SnowPea(int col, int row) {
        super(PlantType.SNOWPEA, col, row, PlantType.SNOWPEA.maxHp, Constants.SNOW_PEA_DMG, 'X');
    }

    @Override
    public void draw(Graphics2D g) {
        int x = Constants.GRID_X + col * Constants.CELL_W;
        int y = Constants.GRID_Y + row * Constants.CELL_H;
        int w = Constants.CELL_W;
        int h = Constants.CELL_H;
        int cx = x + w / 2;
        int cy = y + h / 2;

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

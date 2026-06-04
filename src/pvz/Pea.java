package pvz;

import java.awt.*;

public class Pea {
    public double x, y;
    public final int row;
    public final boolean frozen; // snow pea
    public boolean dead;

    public Pea(double x, double y, int row, boolean frozen) {
        this.x      = x;
        this.y      = y;
        this.row    = row;
        this.frozen = frozen;
    }

    public void update(double dt) {
        x += Constants.PEA_SPEED * dt;
        if (x > Constants.WINDOW_WIDTH + 20) dead = true;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - 7, (int) y - 7, 14, 14);
    }

    public void draw(Graphics2D g) {
        if (frozen) {
            // Ice pea
            g.setColor(new Color(180, 230, 255));
            g.fillOval((int) x - 7, (int) y - 7, 14, 14);
            g.setColor(new Color(100, 180, 240));
            g.drawOval((int) x - 7, (int) y - 7, 14, 14);
            // Shine
            g.setColor(new Color(255, 255, 255, 180));
            g.fillOval((int) x - 4, (int) y - 5, 5, 5);
        } else {
            // Normal pea
            g.setColor(new Color(60, 180, 60));
            g.fillOval((int) x - 7, (int) y - 7, 14, 14);
            g.setColor(new Color(30, 130, 30));
            g.drawOval((int) x - 7, (int) y - 7, 14, 14);
            g.setColor(new Color(120, 230, 120, 180));
            g.fillOval((int) x - 4, (int) y - 5, 5, 5);
        }
    }
}

package pvz;

import java.awt.*;

public class Sun {
    public double x, y;
    public double targetY;
    public boolean falling;
    public boolean collected;
    public long spawnTime;
    private double bobOffset;

    public static final int SIZE   = 36;
    public static final int EXPIRE = 8000; // ms before disappearing

    public Sun(double x, double startY, double targetY) {
        this.x         = x;
        this.y         = startY;
        this.targetY   = targetY;
        this.falling   = true;
        this.spawnTime = System.currentTimeMillis();
    }

    // Sun produced by sunflower (appears above plant)
    public Sun(double x, double y) {
        this.x         = x;
        this.y         = y;
        this.targetY   = y;
        this.falling   = false;
        this.spawnTime = System.currentTimeMillis();
    }

    public void update(double dt) {
        if (falling && y < targetY) {
            y += 80 * dt;
            if (y >= targetY) { y = targetY; falling = false; }
        }
        bobOffset = Math.sin(System.currentTimeMillis() * 0.003) * 3;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > EXPIRE;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - SIZE / 2, (int) (y - SIZE / 2 + bobOffset), SIZE, SIZE);
    }

    public void draw(Graphics2D g) {
        int cx = (int) x;
        int cy = (int) (y + bobOffset);
        long elapsed = System.currentTimeMillis() - spawnTime;
        float alpha = elapsed > EXPIRE - 1500
                ? Math.max(0f, 1f - (elapsed - (EXPIRE - 1500)) / 1500f)
                : 1f;
        Composite oldC = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Rays
        g.setColor(new Color(255, 220, 50));
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45 + (System.currentTimeMillis() * 0.05));
            int rx1 = (int)(cx + Math.cos(angle) * 16);
            int ry1 = (int)(cy + Math.sin(angle) * 16);
            int rx2 = (int)(cx + Math.cos(angle) * 24);
            int ry2 = (int)(cy + Math.sin(angle) * 24);
            g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(rx1, ry1, rx2, ry2);
            g.setStroke(new BasicStroke(1));
        }
        // Main circle
        g.setColor(new Color(255, 230, 30));
        g.fillOval(cx - 15, cy - 15, 30, 30);
        g.setColor(new Color(240, 180, 0));
        g.drawOval(cx - 15, cy - 15, 30, 30);
        // Face
        g.setColor(new Color(180, 120, 0));
        g.fillOval(cx - 6, cy - 5, 4, 4);
        g.fillOval(cx + 2, cy - 5, 4, 4);
        g.drawArc(cx - 5, cy + 1, 10, 6, 0, -180);

        g.setComposite(oldC);
    }
}

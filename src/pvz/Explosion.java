package pvz;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Explosion {
    public double x, y;
    public long startTime;
    public int radius;
    public boolean done;
    private List<Particle> particles = new ArrayList<>();
    private static final Random RND = new Random();

    public Explosion(double x, double y, int radius) {
        this.x         = x;
        this.y         = y;
        this.radius    = radius;
        this.startTime = System.currentTimeMillis();
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle(x, y));
        }
    }

    public void update(double dt) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 800) { done = true; return; }
        for (Particle p : particles) p.update(dt);
    }

    public void draw(Graphics2D g) {
        long elapsed = System.currentTimeMillis() - startTime;
        float prog  = elapsed / 800f;
        float alpha = Math.max(0, 1f - prog);

        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        // Shockwave ring
        int r = (int)(radius * prog);
        g.setColor(new Color(255, 180, 50));
        g.setStroke(new BasicStroke(4 * (1 - prog)));
        g.drawOval((int) x - r, (int) y - r, r * 2, r * 2);
        g.setStroke(new BasicStroke(1));

        // Inner glow
        if (prog < 0.3f) {
            int gr = (int)(radius * 0.6 * (prog / 0.3));
            g.setColor(new Color(255, 255, 200, (int)(200 * (1 - prog / 0.3))));
            g.fillOval((int) x - gr, (int) y - gr, gr * 2, gr * 2);
        }

        // Particles
        for (Particle p : particles) p.draw(g);

        g.setComposite(old);
    }

    private static class Particle {
        double x, y, vx, vy;
        Color  color;
        int    size;

        Particle(double ox, double oy) {
            double angle = RND.nextDouble() * Math.PI * 2;
            double speed = 80 + RND.nextDouble() * 160;
            vx    = Math.cos(angle) * speed;
            vy    = Math.sin(angle) * speed;
            x     = ox;
            y     = oy;
            size  = 4 + RND.nextInt(8);
            int r = 200 + RND.nextInt(55);
            int gr = RND.nextInt(150);
            color = new Color(r, gr, 0);
        }

        void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            vy += 200 * dt;
        }

        void draw(Graphics2D g) {
            g.setColor(color);
            g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }
    }
}

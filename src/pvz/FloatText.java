package pvz;

import java.awt.*;

public class FloatText {
    String text;
    double x, y;
    Color color;
    int size;
    long expireTime;

    public FloatText(String text, double x, double y, Color color, int size, long expireTime) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.size = size;
        this.expireTime = expireTime;
    }

    public void draw(Graphics2D g) {
        long remaining = expireTime - System.currentTimeMillis();
        float alpha = remaining / 400f;
        alpha = Math.max(0f, Math.min(1f, alpha));
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setFont(new Font("Dialog", Font.BOLD, size));
        int alphaInt = Math.max(0, Math.min(255, (int) (120 * alpha)));
        g.setColor(new Color(0, 0, 0, alphaInt));
        g.drawString(text, (int) x + 2, (int) y + 2);
        g.setColor(color);
        g.drawString(text, (int) x, (int) y);
        g.setComposite(old);
    }
}

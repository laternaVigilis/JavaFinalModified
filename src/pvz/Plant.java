package pvz;

import java.awt.*;

/**
 * Abstract Plant base class with template stats: baseHp, baseAttack, idChar
 * Concrete plant types should extend this and implement draw-specific behavior
 */
public abstract class Plant {
    public final PlantType type; // keeps shop/display info
    public final int col, row;
    public int hp;
    public final int baseHp;
    public final int baseAttack;
    public final char idChar; // short id for display (e.g. 'S','P','W','C')
    public long lastShootTime;
    public long lastSunTime;
    public long placedTime;
    public boolean exploding;      // cherry bomb
    public long   explodeStartTime;

    protected Plant(PlantType type, int col, int row, int baseHp, int baseAttack, char idChar) {
        this.type = type;
        this.col = col;
        this.row = row;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.idChar = idChar;
        this.hp = baseHp;
        this.placedTime = System.currentTimeMillis();
    }

    public boolean isDead() { return hp <= 0; }

    public int cx() { return Constants.GRID_X + col * Constants.CELL_W + Constants.CELL_W / 2; }
    public int cy() { return Constants.GRID_Y + row * Constants.CELL_H + Constants.CELL_H / 2; }

    public abstract void draw(Graphics2D g);
}

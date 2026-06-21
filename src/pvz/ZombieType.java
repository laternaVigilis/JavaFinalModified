package pvz;

import pvz.Zombie.*;

public enum ZombieType {
    NORMAL('N', Constants.ZOMBIE_HP, 1.0, Constants.ZOMBIE_DMG) {
        @Override public Zombie create(int row, int level) { return new NormalZombie(row, level); }
    },
    FAST('F', Constants.ZOMBIE_FAST_HP, 1.4, Constants.ZOMBIE_DMG) {
        @Override public Zombie create(int row, int level) { return new FastZombie(row, level); }
    },
    TANK('T', Constants.ZOMBIE_TANK_HP, 0.75, Constants.ZOMBIE_DMG) {
        @Override public Zombie create(int row, int level) { return new TankZombie(row, level); }
    },
    CHERRY('C', 250, 1.0, Constants.ZOMBIE_DMG) {
        @Override public Zombie create(int row, int level) { return new CherryZombie(row, level); }
    };

    public final char idChar;
    public final int baseHp;
    public final double baseSpeed;
    public final int baseAttack;

    ZombieType(char idChar, int baseHp, double baseSpeed, int baseAttack) {
        this.idChar = idChar;
        this.baseHp = baseHp;
        this.baseSpeed = baseSpeed;
        this.baseAttack = baseAttack;
    }

    public abstract Zombie create(int row, int level);
}

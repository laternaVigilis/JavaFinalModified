package pvz;

public class TankZombie extends Zombie {
    public TankZombie(int row, int level) {
        super(row, level, TEMPLATE_TANK, 'T', Constants.ZOMBIE_TANK_HP, 0.75, Constants.ZOMBIE_DMG);
    }
}

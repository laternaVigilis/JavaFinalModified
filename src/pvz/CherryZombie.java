package pvz;

public class CherryZombie extends Zombie {
    public CherryZombie(int row, int level) {
        super(row, level, TEMPLATE_CHERRY, 'C', 250, 1.0, Constants.ZOMBIE_DMG);
    }
}

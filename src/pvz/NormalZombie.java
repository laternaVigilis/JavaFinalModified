package pvz;

public class NormalZombie extends Zombie {
    public NormalZombie(int row, int level) {
        super(row, level, TEMPLATE_NORMAL, 'N', Constants.ZOMBIE_HP, 1.0, Constants.ZOMBIE_DMG);
    }
}

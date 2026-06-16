package pvz;

public class FastZombie extends Zombie {
    public FastZombie(int row, int level) {
        super(row, level, TEMPLATE_FAST, 'F', Constants.ZOMBIE_FAST_HP, 1.4, Constants.ZOMBIE_DMG);
    }
}

package pvz;

public class Constants {
    // Window
    public static final int WINDOW_WIDTH  = 900;
    public static final int WINDOW_HEIGHT = 620;

    // Grid
    public static final int COLS         = 9;
    public static final int ROWS         = 5;
    public static final int CELL_W       = 80;
    public static final int CELL_H       = 90;
    public static final int GRID_X       = 120;
    public static final int GRID_Y       = 130;

    // Sun
    public static final int SUN_FALL_INTERVAL   = 7000;  // ms between sun drops
    public static final int SUN_VALUE           = 25;
    public static final int START_SUN           = 150;

    // Zombie spawn
    public static final int ZOMBIE_SPAWN_BASE   = 8000;  // ms
    public static final int ZOMBIE_SPEED        = 40;    // px/sec

    // Plant costs
    public static final int SUNFLOWER_COST      = 50;
    public static final int PEASHOOTER_COST     = 100;
    public static final int WALLNUT_COST        = 50;
    public static final int SNOWPEA_COST        = 175;
    public static final int CHERRYBOMB_COST     = 150;

    // Plant stats
    public static final int SUNFLOWER_HP        = 300;
    public static final int PEASHOOTER_HP       = 300;
    public static final int WALLNUT_HP          = 1500;
    public static final int SNOWPEA_HP          = 300;
    public static final int CHERRYBOMB_HP       = 250;

    // Zombie stats
    public static final int ZOMBIE_HP           = 200;
    public static final int ZOMBIE_FAST_HP      = 150;
    public static final int ZOMBIE_TANK_HP      = 320;
    public static final double ZOMBIE_LEVEL_HP_MULT = 0.25; // per level above 1
    public static final int ZOMBIE_DMG          = 100;   // per attack interval
    public static final int ZOMBIE_ATTACK_RATE  = 1000;  // ms

    // Pea
    public static final int PEA_SPEED           = 250;   // px/sec
    public static final int PEA_DMG             = 40;   // increased pea damage
    public static final int SHOOT_INTERVAL      = 1500;  // ms

    // Snow pea
    public static final int SNOW_PEA_DMG        = 40;   // increased snow pea damage
    public static final double SNOW_SLOW_FACTOR = 0.4;
    public static final int SLOW_DURATION       = 4000;  // ms

    // Cherry bomb
    public static final int CHERRY_DMG          = 1800;
    public static final int CHERRY_RADIUS       = 120;
    public static final int CHERRY_FUSE         = 2000;  // ms

    // Cherry zombie explosion
    public static final int CHERRY_ZOMBIE_RADIUS = 120;
    public static final int CHERRY_ZOMBIE_DMG    = 1800;

    // Sunflower sun production
    public static final int SF_SUN_INTERVAL     = 8000;  // ms
    public static final int SF_SUN_VALUE        = 25;

    // UI
    public static final int SHOP_Y              = 20;
    public static final int SHOP_ITEM_W         = 70;
    public static final int SHOP_ITEM_H         = 90;
    public static final int SHOP_START_X        = 130;

    // End-screen buttons
    public static final int END_BTN_HALF_WIDTH  = 110; // half of button width
    public static final int END_BTN_W           = 220;
    public static final int END_BTN_H           = 55;
    public static final int END_BTN_RESTART_Y   = 360;
    public static final int END_BTN_MENU_Y      = 430;

    // Pause-menu button layout
    public static final int PAUSE_BTN_FIRST_Y   = 300;
    public static final int PAUSE_BTN_GAP       = 70;
}

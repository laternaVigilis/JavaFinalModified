package pvz;

import java.awt.*;

public enum PlantType {
    SUNFLOWER("向日葵", Constants.SUNFLOWER_COST, Constants.SUNFLOWER_HP,
            new Color(255, 220, 0), new Color(255, 180, 0)),
    PEASHOOTER("豌豆射手", Constants.PEASHOOTER_COST, Constants.PEASHOOTER_HP,
            new Color(60, 180, 60), new Color(30, 140, 30)),
    WALLNUT("堅果牆", Constants.WALLNUT_COST, Constants.WALLNUT_HP,
            new Color(180, 120, 50), new Color(140, 90, 30)),
    SNOWPEA("寒冰射手", Constants.SNOWPEA_COST, Constants.SNOWPEA_HP,
            new Color(100, 200, 255), new Color(50, 150, 220)),
    CHERRYBOMB("櫻桃炸彈", Constants.CHERRYBOMB_COST, Constants.CHERRYBOMB_HP,
            new Color(220, 50, 50), new Color(180, 20, 20));

    public final String name;
    public final int cost;
    public final int maxHp;
    public final Color color;
    public final Color darkColor;

    PlantType(String name, int cost, int maxHp, Color color, Color darkColor) {
        this.name      = name;
        this.cost      = cost;
        this.maxHp     = maxHp;
        this.color     = color;
        this.darkColor = darkColor;
    }
}

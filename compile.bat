@echo off
echo Compiling Plants vs Zombies...
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src ^
    "src\pvz\Constants.java" ^
    "src\pvz\Explosion.java" ^
    "src\pvz\GamePanel.java" ^
    "src\pvz\Main.java" ^
    "src\pvz\Pea.java" ^
    "src\pvz\PlantType.java" ^
    "src\pvz\Sun.java" ^
    "src\pvz\FloatText.java" ^
    "src\pvz\Plant\CherryBomb.java" ^
    "src\pvz\Plant\Peashooter.java" ^
    "src\pvz\Plant\Plant.java" ^
    "src\pvz\Plant\SnowPea.java" ^
    "src\pvz\Plant\Sunflower.java" ^
    "src\pvz\Plant\Wallnut.java" ^
    "src\pvz\Zombie\CherryZombie.java" ^
    "src\pvz\Zombie\FastZombie.java" ^
    "src\pvz\Zombie\NormalZombie.java" ^
    "src\pvz\Zombie\TankZombie.java" ^
    "src\pvz\Zombie\Zombie.java"
if %errorlevel% == 0 (
    echo Compilation successful!
    echo Running game...
    java -cp out pvz.Main
) else (
    echo Compilation failed. Please check the error messages above.
    pause
)

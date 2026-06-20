@echo off
echo Compiling Plants vs Zombies...
if not exist out mkdir out

REM Compile all Java sources under src (including subpackages)
javac -encoding UTF-8 -d out -sourcepath src src\pvz\*.java src\pvz\Plant\*.java src\pvz\Zombie\*.java

if %errorlevel% == 0 (
    echo Compilation successful!
    echo Running game...
    java -cp out pvz.Main
) else (
    echo Compilation failed. Please check the error messages above.
    pause
)

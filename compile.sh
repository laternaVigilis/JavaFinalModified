#!/bin/bash
echo "Compiling Plants vs Zombies..."
mkdir -p out
javac -encoding UTF-8 -d out -sourcepath src src/pvz/Main.java
if [ $? -eq 0 ]; then
    echo "Compilation successful! Running game..."
    java -cp out pvz.Main
else
    echo "Compilation failed."
fi

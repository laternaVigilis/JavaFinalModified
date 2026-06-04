=====================================================
  🌻 植物大戰殭屍 - Java Edition
  Plants vs Zombies Clone in Pure Java
=====================================================

【系統需求】
- Java JDK 8 或以上（需包含 javac）
- 可至 https://adoptium.net 免費下載

【編譯方式】
Windows:
  compile.bat

macOS / Linux:
  chmod +x compile.sh
  ./compile.sh

【直接執行】
編譯後執行：
  java -cp out pvz.Main

【遊戲說明】
1. 點擊頂部植物欄選擇植物
2. 點擊草坪格子種植（需足夠陽光）
3. 點擊飄落的太陽收集陽光
4. 右鍵或選擇相同植物可取消選擇
5. 按 ESC 暫停遊戲

【植物介紹】
🌻 向日葵 (50☀)  - 定期產生陽光，是基礎植物
🌿 豌豆射手(100☀) - 向右攻擊殭屍
🌰 堅果牆 (50☀)  - 高HP，阻擋殭屍前進
❄ 寒冰射手(175☀) - 攻擊並減速殭屍
🍒 櫻桃炸彈(150☀)- 2秒後爆炸，範圍傷害

【勝利條件】
消滅全部 10 波殭屍即可獲勝！

【檔案結構】
src/pvz/
  Main.java       - 程式進入點
  GameWindow.java - 視窗 (JFrame)
  GamePanel.java  - 主遊戲邏輯與繪圖
  Plant.java      - 植物類別
  PlantType.java  - 植物類型列舉
  Zombie.java     - 殭屍類別
  Pea.java        - 豌豆子彈
  Sun.java        - 陽光拾取物
  Explosion.java  - 爆炸特效
  Constants.java  - 遊戲常數

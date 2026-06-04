package pvz;

import javax.swing.*;
import java.awt.event.*;

public class GameWindow extends JFrame {

    private final GamePanel gamePanel;

    public GameWindow() {
        super("🌻 植物大戰殭屍 - Java Edition");
        gamePanel = new GamePanel();
        add(gamePanel);
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Keyboard input
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gamePanel.handleKey(e);
            }
        });
        gamePanel.setFocusable(true);
        gamePanel.requestFocus();
    }
}

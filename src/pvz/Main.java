package pvz;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel gamePanel = new GamePanel();
            gamePanel.getFrame().setVisible(true);
            gamePanel.requestFocusInWindow();
        });
    }
}

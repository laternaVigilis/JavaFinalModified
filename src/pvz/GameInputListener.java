package pvz;

import java.awt.*;
import java.awt.event.*;
import pvz.Plant.*;
import pvz.Zombie.*;

public class GameInputListener implements MouseListener, MouseMotionListener {
    private final GamePanel panel;
    private final GameWorld world;

    public GameInputListener(GamePanel panel, GameWorld world) {
        this.panel = panel;
        this.world = world;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        GamePanel.State state = panel.getState();

        // Menu start
        if (state == GamePanel.State.MENU) {
            if (mx >= Constants.WINDOW_WIDTH / 2 - 100 && mx <= Constants.WINDOW_WIDTH / 2 + 100
                    && my >= 250 && my <= 305) {
                panel.startGame();
            }
            return;
        }

        // Pause menu clicks - compute bounds locally (consistent with GamePanel)
        if (state == GamePanel.State.PAUSED) {
            int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
            Rectangle cont = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y, Constants.END_BTN_W, Constants.END_BTN_H);
            Rectangle rst  = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP, Constants.END_BTN_W, Constants.END_BTN_H);
            Rectangle end  = new Rectangle(bx, Constants.PAUSE_BTN_FIRST_Y + Constants.PAUSE_BTN_GAP * 2, Constants.END_BTN_W, Constants.END_BTN_H);
            if (cont.contains(mx, my)) {
                panel.resumeFromPause();
            } else if (rst.contains(mx, my)) {
                panel.startGame();
            } else if (end.contains(mx, my)) {
                panel.endGameFromPauseAsLose();
            }
            return;
        }

        // End screen buttons
        if (state == GamePanel.State.WIN || state == GamePanel.State.LOSE) {
            int bx = Constants.WINDOW_WIDTH / 2 - Constants.END_BTN_HALF_WIDTH;
            Rectangle restart = new Rectangle(bx, Constants.END_BTN_RESTART_Y, Constants.END_BTN_W, Constants.END_BTN_H);
            Rectangle menuBtn  = new Rectangle(bx, Constants.END_BTN_MENU_Y, Constants.END_BTN_W, Constants.END_BTN_H);
            if (restart.contains(mx, my)) {
                panel.startGame();
            } else if (menuBtn.contains(mx, my)) {
                panel.setState(GamePanel.State.MENU);
                panel.stopEngine();
                // clear any end screen state silently
                panel.repaint();
            }
            return;
        }
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {
        panel.setMousePos(e.getPoint());
        int mx = e.getX(), my = e.getY();
        int newCol = (mx - Constants.GRID_X) / Constants.CELL_W;
        int newRow = (my - Constants.GRID_Y) / Constants.CELL_H;
        if (newCol < 0 || newCol >= Constants.COLS || newRow < 0 || newRow >= Constants.ROWS) {
            panel.setHoverColRow(-1, -1);
            // if we had a selected tile, cancel it when mouse leaves grid
            if (panel.getSelectedTileCol() >= 0) {
                panel.setSelectedTile(-1, -1);
                panel.setRemoveBtnBounds(null);
            }
            return;
        }
        panel.setHoverColRow(newCol, newRow);
        // If we've previously clicked to show the overlay and now moved outside that tile, cancel it
        if (panel.getSelectedTileCol() >= 0 && (panel.getHoverCol() != panel.getSelectedTileCol() || panel.getHoverRow() != panel.getSelectedTileRow())) {
            panel.setSelectedTile(-1, -1);
            panel.setRemoveBtnBounds(null);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        GamePanel.State state = panel.getState();

        if (state == GamePanel.State.PAUSED) {
            // Do not auto-resume on arbitrary clicks; pause menu handles clicks explicitly
            return;
        }

        if (state != GamePanel.State.PLAYING) return;

        // Right click: deselect
        if (e.getButton() == MouseEvent.BUTTON3) {
            panel.setSelectedPlant(null);
            return;
        }

        // If remove-button is visible and clicked -> remove plant
        Rectangle rb = panel.getRemoveBtnBounds();
        if (rb != null && rb.contains(mx, my)) {
            int sr = panel.getSelectedTileRow();
            int sc = panel.getSelectedTileCol();
            if (sr >= 0 && sc >= 0 && world.grid[sr][sc] != null) {
                world.grid[sr][sc] = null;
                world.addFloatText("已移除植物", mx, my - 10, new Color(255, 200, 100), 16, 800);
            }
            panel.setSelectedTile(-1, -1);
            panel.setRemoveBtnBounds(null);
            return;
        }

        // Check sun collection
        for (Sun s : world.suns) {
            if (s.getBounds().contains(mx, my)) {
                s.collected = true;
                world.sunCount += Constants.SUN_VALUE;
                world.addFloatText("+25 ☀", mx, my, new Color(255, 230, 50), 18, 900);
                return;
            }
        }

        // Check shop clicks
        PlantType[] types = PlantType.values();
        for (int i = 0; i < types.length; i++) {
            int ix = Constants.SHOP_START_X + i * (Constants.SHOP_ITEM_W + 8);
            int iy = Constants.SHOP_Y + 5;
            if (mx >= ix && mx <= ix + Constants.SHOP_ITEM_W && my >= iy && my <= iy + Constants.SHOP_ITEM_H) {
                if (world.sunCount >= types[i].cost) {
                    panel.setSelectedPlant(types[i]);
                }
                return;
            }
        }

        // Grid placement (hover controls remove-overlay visibility)
        int clickedCol = (mx - Constants.GRID_X) / Constants.CELL_W;
        int clickedRow = (my - Constants.GRID_Y) / Constants.CELL_H;
        boolean clickedOnGrid = clickedCol >= 0 && clickedCol < Constants.COLS && clickedRow >= 0 && clickedRow < Constants.ROWS;

        PlantType sel = panel.getSelectedPlant();
        if (sel != null && clickedOnGrid) {
            if (world.grid[clickedRow][clickedCol] == null && world.sunCount >= sel.cost) {
                Plant newPlant = switch (sel) {
                    case SUNFLOWER -> new Sunflower(clickedCol, clickedRow);
                    case PEASHOOTER -> new Peashooter(clickedCol, clickedRow);
                    case WALLNUT -> new Wallnut(clickedCol, clickedRow);
                    case SNOWPEA -> new SnowPea(clickedCol, clickedRow);
                    case CHERRYBOMB -> new CherryBomb(clickedCol, clickedRow);
                };
                world.grid[clickedRow][clickedCol] = newPlant;
                world.sunCount -= sel.cost;
                world.addFloatText("-" + sel.cost + " ☀", mx, my - 20,
                        new Color(220, 180, 50), 16, 800);
                // Keep selected for rapid planting (deselect cherry bomb after place)
                if (sel == PlantType.CHERRYBOMB) panel.setSelectedPlant(null);
            }
        } else if (clickedOnGrid) {
            // Click-to-show overlay: if clicking an occupied tile, show remove/X button.
            if (world.grid[clickedRow][clickedCol] != null) {
                panel.setSelectedTile(clickedCol, clickedRow);
            } else {
                // clicked empty tile -> cancel any shown overlay
                panel.setSelectedTile(-1, -1);
                panel.setRemoveBtnBounds(null);
            }
        }
    }

    @Override public void mouseReleased(MouseEvent e) {}
}



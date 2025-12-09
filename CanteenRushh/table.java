package CanteenRushh;

import javax.swing.*;
import java.awt.*;

public class table {

    private int x, y;
    private Image tableImage;

    public table(int x, int y) {
        this.x = x;
        this.y = y;

        // LOAD TABLE IMAGE
        tableImage = new ImageIcon(getClass().getResource("/tableres/tablechair.png")).getImage();
    }

    public void draw(Graphics g) {
        // Preferred size for your game (scaled)
        int width = 140;
        int height = 110;

        g.drawImage(tableImage, x, y, width, height, null);
    }
}

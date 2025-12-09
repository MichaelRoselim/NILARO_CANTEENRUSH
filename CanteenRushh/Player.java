package CanteenRushh;

import javax.swing.*;
import java.awt.*;

public class Player {

    public int x = 200, y = 200;
    private int targetX = 200, targetY = 200;
    private int speed = 5;
    private boolean moving = false;

    private ImageIcon currentSprite, idle, walkUp, walkDown, walkLeft, walkRight;

    public Player() {
        walkUp = new ImageIcon(getClass().getResource("/tindera_gif/walk_backwards.gif"));
        walkDown = new ImageIcon(getClass().getResource("/tindera_gif/walk_forward.gif"));
        walkLeft = new ImageIcon(getClass().getResource("/tindera_gif/walk_left.gif"));
        walkRight = new ImageIcon(getClass().getResource("/tindera_gif/walk_right.gif"));
        idle = new ImageIcon(getClass().getResource("/tindera_gif/walk_still.png"));

        currentSprite = idle;
    }

    public void moveTo(int mx, int my) {
        targetX = mx - 20;
        targetY = my - 20;
        moving = true;
    }

    public void update() {
        if (!moving) return;

        int dx = targetX - x;
        int dy = targetY - y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > speed) {
            x += speed * dx / dist;
            y += speed * dy / dist;

            if (Math.abs(dx) > Math.abs(dy))
                currentSprite = (dx > 0) ? walkRight : walkLeft;
            else
                currentSprite = (dy > 0) ? walkDown : walkUp;
        } else {
            moving = false;
            currentSprite = idle;
        }
    }

    public void draw(Graphics g) {
        currentSprite.paintIcon(null, g, x, y);
    }

    // ✅ FIXED: Proper getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

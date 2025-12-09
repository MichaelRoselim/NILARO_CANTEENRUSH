package CanteenRushh;

import javax.swing.JFrame;

public class CanteenRushMain {
    public static void main(String[] args) {

        JFrame frame = new JFrame("Canteen Rush");
        GamePanel panel = new GamePanel();

        frame.add(panel);
        frame.setSize(800, 600);          // FIXED WINDOW SIZE
        frame.setResizable(false);        // SO IT DOESN'T RESIZE
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

package CanteenRushh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

import customers.CUSTOMER;
import customers.CustomerQueue;

public class GamePanel extends JPanel implements ActionListener, MouseListener {

    private Timer timer;
    private Player player;

    private Image background;
    private Image counterImage;
    private Image tableImage;

    private ArrayList<Rectangle> tables = new ArrayList<>();

    private CustomerQueue customerQueue = new CustomerQueue();

    public GamePanel() {

        try {
            background = new ImageIcon(getClass().getResource("/tile/bg (2).png")).getImage();
            counterImage = new ImageIcon(getClass().getResource("/tile/COUNTERNI.png")).getImage();
            tableImage = new ImageIcon(getClass().getResource("/tableres/tablechair.png")).getImage();
        } catch (Exception e) {
            System.out.println("Missing background assets!");
        }

        player = new Player();

        // TABLE POSITIONS (LIKE YOUR IMAGE)
        tables.add(new Rectangle(200, 350, 180, 120));
        tables.add(new Rectangle(500, 350, 180, 120));
        tables.add(new Rectangle(200, 550, 180, 120));
        tables.add(new Rectangle(500, 550, 180, 120));

        // QUEUE POSITION (LEFT SIDE)
        customerQueue.setQueueStart(60, 180);

        addMouseListener(this);
        setFocusable(true);

        timer = new Timer(20, this);
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // BACKGROUND
        g.drawImage(background, 0, 0, getWidth(), getHeight(), null);

        // COUNTER
        g.drawImage(counterImage, 100, 90, 700, 150, null);

        // TABLES
        for (Rectangle r : tables) {
            g.drawImage(tableImage, r.x, r.y, r.width, r.height, null);
        }

        // CUSTOMERS
        for (CUSTOMER c : customerQueue.all()) {
            c.draw(g);
        }

        // PLAYER
        player.draw(g);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        player.update();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        player.moveTo(e.getX(), e.getY());
    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    // ✅ CALLED BY MAIN LAUNCHER
    public void spawnCustomerByName(String name) {
        CUSTOMER c = new CUSTOMER(name, 0, 0);
        customerQueue.enqueue(c);
        repaint();
    }
}

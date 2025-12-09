package CanteenRushh;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException; 
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * customer - visual Customer used by CustomerQueue.
 * - Uses PNG standing sprites for WAITING / AT_COUNTER
 * - Uses animated GIF for MOVING_TO_COUNTER (walk)
 */
public class Customer {
    public enum State { WAITING, MOVING_TO_COUNTER, AT_COUNTER }

    public final int id;
    private static final Random RNG = new Random();

    // visual & motion
    public double x, y;
    private Point target;
    private final int width = 48, height = 64;
    private final double speed = 120.0; // pixels per second

    // sprites: standing PNGs and walking GIFs
    private Image spriteStanding;    // static PNG for WAITING and AT_COUNTER
    private Image spriteWalking;     // animated GIF for MOVING_TO_COUNTER

    // filenames (must be placed in same package/resource path or assets/)
    private static final String[] WALKING_GIFS = {
        "walking-talikod-keren.gif",
        "walking-talikod-matcha.gif",
        "walking-talikod-miki.gif",
        "walking-talikod-rov.gif",
        "walking-talikod-jea.gif"
    };

    private static final String[] STANDING_PNGS = {
        "standing-keren1.png",
        "standing-matcha1.png",
        "standing-miki1.png",
        "standing-rov1.png",
        "standing-jea1.png"
    };

    // state & gameplay
    private State state = State.WAITING;
    public boolean paid = true; // default true until set to unpaid at counter
    public String menuRequest = "";
    public int maxPatience = 15; // seconds of patience total
    public double patienceRemaining; // seconds
    private double patienceAccumulator = 0.0; // accumulate dt for 1s ticks
    private final String[] SAMPLE_MENU = {"Burger","Fries","Soda","Coffee","Nuggets","Wrap"};

    // locking
    private boolean lockedToCounter = false;

    public Customer(int id, Point spawnPos) {
        this.id = id;
        this.x = spawnPos.x;
        this.y = spawnPos.y;
        this.target = new Point((int)x, (int)y);
        this.patienceRemaining = maxPatience;

        // pick a character index and load matching standing and walking sprites
        int idx = RNG.nextInt(Math.min(WALKING_GIFS.length, STANDING_PNGS.length));
        loadSprites(idx);
    }

    private void loadSprites(int idx) {
        String walkFile = WALKING_GIFS[idx];
        String standFile = STANDING_PNGS[idx];

        // Try classpath resource for walking GIF (animated)
        try {
            java.net.URL res = getClass().getResource(walkFile);
            if (res == null) res = getClass().getResource("/" + walkFile);
            if (res != null) {
                spriteWalking = new ImageIcon(res).getImage();
            }
        } catch (Exception e) {
            spriteWalking = null;
        }

        // Try classpath resource for standing PNG
        try {
            java.net.URL res2 = getClass().getResource(standFile);
            if (res2 == null) res2 = getClass().getResource("/" + standFile);
            if (res2 != null) {
                spriteStanding = ImageIO.read(res2);
            }
        } catch (IOException e) {
            spriteStanding = null;
        }

        // Fallback to assets/ folder in working directory for walking GIF
        if (spriteWalking == null) {
            try {
                File f = new File("assets/" + walkFile);
                if (f.exists()) spriteWalking = new ImageIcon(f.getPath()).getImage();
            } catch (Exception ignored) {}
        }

        // Fallback to assets/ folder for standing PNG
        if (spriteStanding == null) {
            try {
                File f = new File("assets/" + standFile);
                if (f.exists()) spriteStanding = ImageIO.read(f);
            } catch (IOException ignored) {}
        }

        // Last fallback: placeholder images if both missing
        if (spriteStanding == null) spriteStanding = createPlaceholderImage();
        if (spriteWalking == null) spriteWalking = spriteStanding; // if no gif, use static image
    }

    private Image createPlaceholderImage() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(180, 130, 100));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(0, 0, width - 1, height - 1);
        g.dispose();
        return img;
    }

    public State getState() { return state; }
    public void setState(State s) { state = s; }

    public void setTarget(Point t) {
        this.target = new Point(t);
    }

    public Point getTarget() { return target; }

    public boolean isLockedToCounter() { return lockedToCounter; }
    public void lockToCounter() { lockedToCounter = true; }

    public boolean isAtCounter() { return state == State.AT_COUNTER; }

    /**
     * Update customer's movement + patience.
     * @param dtSeconds delta time in seconds
     */
    public void update(double dtSeconds) {
        // Movement: linear movement toward target
        double dx = target.x - x;
        double dy = target.y - y;
        double dist = Math.hypot(dx, dy);
        double step = speed * dtSeconds;
        if (dist > 0.5) {
            double nx = x + dx / dist * Math.min(step, dist);
            double ny = y + dy / dist * Math.min(step, dist);
            x = nx; y = ny;
        } else {
            // snap to target
            x = target.x; y = target.y;
        }

        // Patience tick: accumulate and reduce once per second
        patienceAccumulator += dtSeconds;
        while (patienceAccumulator >= 1.0) {
            applyPatienceTick();
            patienceAccumulator -= 1.0;
        }
    }

    private void applyPatienceTick() {
        // Reduce patience differently depending on state
        double reduction = 1.0; // waiting baseline
        if (state == State.MOVING_TO_COUNTER) reduction = 1.0;
        if (state == State.AT_COUNTER) {
            reduction = paid ? 1.5 : 3.0; // unpaid reduces faster
        }
        patienceRemaining -= reduction;
        if (patienceRemaining < 0) patienceRemaining = 0;
    }

    public boolean isAngry() { return patienceRemaining <= 0; }

    public void arriveAtCounterAndChooseRequest() {
        this.state = State.AT_COUNTER;
        this.menuRequest = SAMPLE_MENU[RNG.nextInt(SAMPLE_MENU.length)];
        this.paid = false;
        lockToCounter();
    }

    // Simple draw - chooses standing vs walking sprite based on state
    public void draw(Graphics2D g) {
        int ix = (int) Math.round(x - width/2.0);
        int iy = (int) Math.round(y - height);

        // Select sprite: walking when MOVING_TO_COUNTER, standing otherwise
        Image toDraw = (state == State.MOVING_TO_COUNTER) ? spriteWalking : spriteStanding;

        if (toDraw != null) {
            g.drawImage(toDraw, ix, iy, width, height, null);
        } else {
            // fallback body if image missing
            RoundRectangle2D.Double body = new RoundRectangle2D.Double(ix, iy, width, height, 10, 10);
            g.setColor(new Color(220, 180, 150));
            g.fill(body);
            g.setColor(Color.DARK_GRAY);
            g.draw(body);
        }

        // speech bubble / request (only at counter)
        if (state == State.AT_COUNTER) {
            g.setColor(Color.WHITE);
            g.fillRoundRect(ix - 6, iy - 26, 60, 18, 8, 8);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(menuRequest, ix - 2, iy - 12);
        }

        // patience bar (above head)
        int barW = width;
        int barH = 6;
        double frac = Math.max(0, Math.min(1.0, patienceRemaining / maxPatience));
        g.setColor(Color.DARK_GRAY);
        g.fillRect(ix, iy - 14, barW, barH);
        g.setColor(frac > 0.5 ? Color.GREEN : (frac > 0.2 ? Color.ORANGE : Color.RED));
        g.fillRect(ix + 1, iy - 13, (int) ((barW - 2) * frac), barH - 2);

        // pay indicator (at counter)
        if (state == State.AT_COUNTER) {
            g.setColor(paid ? Color.BLUE : Color.RED);
            g.fillOval(ix + width - 12, iy - 6, 10, 10);
        }
    }
}

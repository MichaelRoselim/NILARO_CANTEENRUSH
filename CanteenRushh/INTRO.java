package CanteenRushh;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.*;
import java.io.File;

/**
 * INTRO - story panel for Canteen Rush
 *
 * This implementation provides:
 *  - loadAssets()/getFileURL() so image/audio files can be resolved
 *  - a simple IntroScene inner class with the expected API used by MainLauncher
 *  - startGameThread()/run() loop similar to your original
 *  - safe audio preload/play/stop methods
 */
public class INTRO extends JPanel implements Runnable {

    private static final long serialVersionUID = 1L;

    final int initialScreenWidth = 1000;
    final int initialScreenHeight = 750;
    final Dimension preferredSize = new Dimension(initialScreenWidth, initialScreenHeight);

    Thread gameThread;
    final int FPS = 60;

    private IntroScene dialogManager;
    private int sceneIndex = 0;

    private Runnable storyFinishedListener;

    public void setStoryFinishedListener(Runnable r) {
        this.storyFinishedListener = r;
    }

    private final Map<String, BufferedImage> backgrounds = new HashMap<>();
    private BufferedImage currentBackground;

    static class Scene {
        final String backgroundFile;
        final String speaker;
        final String text;

        public Scene(String bg, String speaker, String text) {
            this.backgroundFile = bg;
            this.speaker = speaker;
            this.text = text;
        }
    }

    private final List<Scene> STORY = new ArrayList<>();

    // BACKGROUND FILES (relative to classpath or filesystem)
    private static final String BACKGROUND_S1 = "SNB/W1.png";
    private static final String BACKGROUND_S2 = "SNB/W2.png";
    private static final String BACKGROUND_S3 = "SNB/W3.png";
    private static final String BACKGROUND_S4 = "SNB/W4.png";
    private static final String BACKGROUND_S5 = "SNB/W5.png";
    private static final String BACKGROUND_S6 = "SNB/W6.png";
    private static final String BACKGROUND_S7 = "SNB/W7.png";
    private static final String BACKGROUND_S8 = "SNB/W8.png";
    private static final String BACKGROUND_S9 = "SNB/W9.png";
    private static final String BACKGROUND_S10 = "SNB/W10.png";
    private static final String BACKGROUND_S11 = "SNB/W11.png";
    // You can add BACKGROUND_S12 if needed

    // ---------------- AUDIO CLIPS ----------------
    private Clip introClip;       // S1–S5
    private Clip intenseClip;     // S6–S9
    private Clip entranceClip;    // S10
    private Clip lastClip;        // S11

    private static final String INTRO_MUSIC    = "storyline_intro.wav";
    private static final String INTENSE_MUSIC  = "storyline_intense.wav";
    private static final String ENTRANCE_MUSIC = "storyline_studentEntrance.wav";
    private static final String LAST_MUSIC     = "storyline_lastPart.wav";

    public INTRO() {
        this.setPreferredSize(preferredSize);
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(new KeyInputHandler());
        this.setDoubleBuffered(true);

        dialogManager = new IntroScene();

        loadStory();
        loadAssets();

        // Preload all music files (if present)
        preloadMusic(INTRO_MUSIC, "intro");
        preloadMusic(INTENSE_MUSIC, "intense");
        preloadMusic(ENTRANCE_MUSIC, "entrance");
        preloadMusic(LAST_MUSIC, "last");

        showNextScene();

        // Request focus so KeyListener works when panel is shown
        this.addHierarchyListener(e -> {
            if (isDisplayable()) {
                requestFocusInWindow();
            }
        });
    }

    // ---------------- STORY CONTENT ----------------
    private void loadStory() {
        STORY.add(new Scene(BACKGROUND_S1, "Alex", "Hi, I’m Alex — a working student at Bukidnon State University."));
        STORY.add(new Scene(BACKGROUND_S2, "Alex", "Class just ended, but there’s no time to breathe."));
        STORY.add(new Scene(BACKGROUND_S3, "Alex", "I need to go straight to the cafeteria for my shift."));
        STORY.add(new Scene(BACKGROUND_S4, "Alex", "Even though I'm tired, the register is already waiting — and customers don’t wait."));
        STORY.add(new Scene(BACKGROUND_S5, "Alex", "I wipe the counter one final time."));

        STORY.add(new Scene(BACKGROUND_S6, "Narrator", "As everything is still."));
        STORY.add(new Scene(BACKGROUND_S7, "Narrator", "Too still."));
        STORY.add(new Scene(BACKGROUND_S8, "Alex", "Then I hear it —"));
        STORY.add(new Scene(BACKGROUND_S9, "Narrator", "Footsteps. Lots of them. It’s getting louder... and louder..."));

        STORY.add(new Scene(BACKGROUND_S10, "Narrator", "And it happens."));
        STORY.add(new Scene(BACKGROUND_S10, "Narrator", "The lunch rush slams into the cafeteria!"));

        STORY.add(new Scene(BACKGROUND_S11, "Narrator", "Customers are starving. The line is growing.\nServe fast. Stay sharp."));
    }

    // ---------------- ASSET LOADING ----------------
    private void loadAssets() {
        try {
            for (Scene s : STORY) {
                if (s.backgroundFile == null) continue;
                if (!backgrounds.containsKey(s.backgroundFile)) {
                    URL u = getFileURL(s.backgroundFile);
                    if (u != null) {
                        backgrounds.put(s.backgroundFile, ImageIO.read(u));
                    } else {
                        // try to load with ImageIO directly from file path
                        try {
                            backgrounds.put(s.backgroundFile, ImageIO.read(new File(s.backgroundFile)));
                        } catch (Exception ex) {
                            backgrounds.put(s.backgroundFile, null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("FAILED TO LOAD IMAGES: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Attempts to resolve a resource path either from the classpath or as a file path.
     * Returns null if not found.
     */
    private URL getFileURL(String f) {
        if (f == null) return null;
        // try with a leading slash (classpath)
        URL url = getClass().getResource("/" + f);
        if (url == null) {
            // try without leading slash
            url = getClass().getResource(f);
        }
        if (url == null) {
            try {
                File file = new File(f);
                if (file.exists()) return file.toURI().toURL();
            } catch (Exception ignored) {}
        }
        return url;
    }

    // ---------------- MUSIC PRELOAD ----------------
    private void preloadMusic(String file, String type) {
        try {
            URL url = getFileURL(file);
            if (url == null) return;

            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);

            switch (type) {
                case "intro": introClip = clip; break;
                case "intense": intenseClip = clip; break;
                case "entrance": entranceClip = clip; break;
                case "last": lastClip = clip; break;
            }

        } catch (Exception e) {
            System.out.println("Failed to load audio: " + file + " -> " + e.getMessage());
        }
    }

    private void playClip(Clip c) {
        if (c == null) return;
        try {
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.loop(Clip.LOOP_CONTINUOUSLY);
            c.start();
        } catch (Throwable t) {
            // ignore playback errors
        }
    }

    private void stopClip(Clip c) {
        if (c == null) return;
        try {
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
        } catch (Throwable t) {
            // ignore
        }
    }

    // ---------------- MUSIC CONDITIONS ----------------
    private boolean isIntroScene(String bg) {
        return bg != null && (bg.equals(BACKGROUND_S1) ||
               bg.equals(BACKGROUND_S2) ||
               bg.equals(BACKGROUND_S3) ||
               bg.equals(BACKGROUND_S4) ||
               bg.equals(BACKGROUND_S5));
    }

    private boolean isIntenseScene(String bg) {
        return bg != null && (bg.equals(BACKGROUND_S6) ||
               bg.equals(BACKGROUND_S7) ||
               bg.equals(BACKGROUND_S8) ||
               bg.equals(BACKGROUND_S9));
    }

    private boolean isEntranceScene(String bg) {
        return bg != null && bg.equals(BACKGROUND_S10);
    }

    private boolean isLastScene(String bg) {
        return bg != null && bg.equals(BACKGROUND_S11);
    }

    // ---------------- SCENE TRANSITION ----------------
    private void showNextScene() {

        if (sceneIndex >= STORY.size()) {
            stopClip(introClip);
            stopClip(intenseClip);
            stopClip(entranceClip);
            stopClip(lastClip);

            // notify story finished on EDT
            if (storyFinishedListener != null) {
                SwingUtilities.invokeLater(storyFinishedListener);
            }
            gameThread = null;
            return;
        }

        Scene s = STORY.get(sceneIndex);
        currentBackground = backgrounds.get(s.backgroundFile);
        dialogManager.startDialog(s.text, s.speaker);
        sceneIndex++;

        // Stop all music before playing new one
        stopClip(introClip);
        stopClip(intenseClip);
        stopClip(entranceClip);
        stopClip(lastClip);

        // Decide music
        if (isIntroScene(s.backgroundFile)) {
            playClip(introClip);
        } 
        else if (isIntenseScene(s.backgroundFile)) {
            playClip(intenseClip);
        } 
        else if (isEntranceScene(s.backgroundFile)) {
            playClip(entranceClip);
        } 
        else if (isLastScene(s.backgroundFile)) {
            playClip(lastClip);
        }
    }

    // ---------------- GAME LOOP ----------------
    public void startGameThread() {
        if (gameThread != null && gameThread.isAlive()) return;
        gameThread = new Thread(this, "INTRO-Thread");
        gameThread.start();
    }

    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long last = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / drawInterval;
            last = now;

            if (delta >= 1) {
                dialogManager.update();
                repaint();
                delta--;
            }

            // slight sleep to avoid tight loop when not necessary
            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }
    }

    // ---------------- RENDER ----------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (currentBackground != null)
            g2.drawImage(currentBackground, 0, 0, getWidth(), getHeight(), null);
        else {
            // placeholder background
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        dialogManager.draw(g2, getWidth(), getHeight());
    }

    // ---------------- INPUT ----------------
    private class KeyInputHandler implements KeyListener {

        public void keyTyped(KeyEvent e) {}

        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE ||
                e.getKeyCode() == KeyEvent.VK_ENTER) {

                if (!dialogManager.isFinished()) {
                    dialogManager.skipDialog();
                } else {
                    showNextScene();
                }
            }
        }

        public void keyReleased(KeyEvent e) {}
    }

    // -----------------------
    // Minimal IntroScene implementation that matches expected API
    // -----------------------
    private class IntroScene {
        private String speaker = "";
        private String fullText = "";
        private String shownText = "";
        private int charIndex = 0;
        private int charPerUpdate = 1;
        private boolean finished = true;
        private int waitTicksAfterFinish = 40; // show finished state a bit then auto-advance
        private int waitCounter = 0;

        public IntroScene() {}

        public void startDialog(String fullText, String speaker) {
            this.speaker = speaker == null ? "" : speaker;
            this.fullText = fullText == null ? "" : fullText;
            this.shownText = "";
            this.charIndex = 0;
            this.finished = false;
            this.waitCounter = 0;
        }

        /**
         * Update typing effect and auto-advance after full text shown.
         */
        public void update() {
            if (finished) {
                if (waitCounter > 0) {
                    waitCounter--;
                    if (waitCounter == 0) {
                        // after small pause when finished, no auto-advance here to let user press key
                    }
                }
                return;
            }

            // reveal characters gradually
            if (charIndex < fullText.length()) {
                int reveal = Math.min(charPerUpdate, fullText.length() - charIndex);
                charIndex += reveal;
                shownText = fullText.substring(0, charIndex);
            } else {
                // finished typing
                finished = true;
                waitCounter = waitTicksAfterFinish;
            }
        }

        public void draw(Graphics2D g2, int w, int h) {
            // draw speaker and dialog box
            int boxH = 160;
            int boxY = h - boxH - 20;
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(20, boxY, w - 40, boxH);

            g2.setColor(Color.WHITE);
            Font f = g2.getFont().deriveFont(Font.BOLD, 18f);
            g2.setFont(f);
            g2.drawString(speaker, 30, boxY + 28);

            Font tf = g2.getFont().deriveFont(Font.PLAIN, 16f);
            g2.setFont(tf);
            // simple word-wrap
            drawStringWrapped(g2, shownText, 30, boxY + 56, w - 60, 20);
        }

        private void drawStringWrapped(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
            if (text == null) return;
            String[] words = text.split("\\s+");
            String line = "";
            for (String w : words) {
                String test = line.isEmpty() ? w : line + " " + w;
                int tw = g2.getFontMetrics().stringWidth(test);
                if (tw > maxWidth && !line.isEmpty()) {
                    g2.drawString(line, x, y);
                    y += lineHeight;
                    line = w;
                } else {
                    line = test;
                }
            }
            if (!line.isEmpty()) {
                g2.drawString(line, x, y);
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public void skipDialog() {
            // show full text immediately
            this.charIndex = fullText.length();
            this.shownText = fullText;
            this.finished = true;
            this.waitCounter = waitTicksAfterFinish;
        }
    }
}

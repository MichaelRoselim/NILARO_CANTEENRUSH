package CanteenRushh;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.*;
import java.io.File;
import GameLauncher.MainLauncher;

public class CanteenMenu extends JFrame {

    private Image backgroundImage;
    private Clip backgroundMusic;
    private FloatControl volumeControl;
    private boolean isMuted = false;
    private float previousVolume = 1.0f;

    // Preloaded hover clip to avoid latency
    private Clip hoverClip;
    private FloatControl hoverVolumeControl;

    // ================== ADDED AS REQUESTED ====================
    private Runnable startGameListener;

    public void addStartGameListener(Runnable r) {
        this.startGameListener = r;
    }
    // ==========================================================

    public CanteenMenu() {
        setTitle("Canteen Rush");

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        backgroundImage = new ImageIcon("canteen_bg.png").getImage();

        // Load and start background music
        playMusic("menu_music.wav");

        // Preload hover sound into memory to eliminate delay
        loadHoverSound("hover.wav");

        showMainMenu();

        // ===== ESC KEY with Confirmation Dialog =====
        getRootPane().registerKeyboardAction(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to exit the game?",
                    "Exit Confirmation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                dispose(); // properly close clips
                System.exit(0);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ================= LOAD PIXEL FONT =================
    private Font loadPixelFont(float size) {
        try {
            Font pixelFont = Font.createFont(Font.TRUETYPE_FONT, new File("Courier New"));
            return pixelFont.deriveFont(size);
        } catch (Exception e) {
            System.out.println("Pixel font not found, using default.");
            return new Font("Monospaced", Font.BOLD, (int) size);
        }
    }

    // ================= PRELOAD HOVER SOUND =================
    private void loadHoverSound(String hoverFilePath) {
        try {
            File soundFile = new File(hoverFilePath);
            if (!soundFile.exists()) {
                System.out.println("Hover sound file not found: " + hoverFilePath);
                return;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            hoverClip = AudioSystem.getClip();
            hoverClip.open(ais);

            // Try to get volume control for hover clip
            try {
                hoverVolumeControl = (FloatControl) hoverClip.getControl(FloatControl.Type.MASTER_GAIN);
            } catch (Exception ex) {
                hoverVolumeControl = null;
            }

            // Do not start it; just keep it open and ready.
        } catch (Exception e) {
            e.printStackTrace();
            hoverClip = null;
            hoverVolumeControl = null;
        }
    }

    // ================= PLAY PRELOADED HOVER SOUND (NO LAG) =================
    private void playHoverSound() {
        if (isMuted || previousVolume <= 0f) return;
        if (hoverClip == null) return;

        // If clip is playing, rewind and restart for instant retrigger
        if (hoverClip.isRunning()) {
            hoverClip.stop();
        }

        hoverClip.setFramePosition(0); // rewind

        // Update hover volume control to match current previousVolume
        if (hoverVolumeControl != null) {
            float min = hoverVolumeControl.getMinimum();
            float max = hoverVolumeControl.getMaximum();
            float gain = min + (max - min) * previousVolume;
            hoverVolumeControl.setValue(gain);
        }

        // Start (non-blocking)
        hoverClip.start();
    }

    // ================= CUSTOM OVAL BUTTON =================
    private JButton createOvalButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(255, 215, 0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 50, 50);

                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(0, 0, getWidth(), getHeight(), 50, 50);

                super.paintComponent(g);
            }
        };

        // ===== BASE SIZE =====
        int baseWidth = 300;
        int baseHeight = 70;
        Dimension baseSize = new Dimension(baseWidth, baseHeight);
        button.setPreferredSize(baseSize);
        button.setMinimumSize(baseSize);
        button.setMaximumSize(baseSize);

        // ===== FONT =====
        button.setFont(loadPixelFont(32f));
        button.setForeground(Color.BLACK);

        // ===== STYLE =====
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // ===== HOVER EFFECT =====
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                int newWidth = (int)(baseWidth * 1.1);
                int newHeight = (int)(baseHeight * 1.1);
                button.setPreferredSize(new Dimension(newWidth, newHeight));
                button.setFont(loadPixelFont(36f)); // slightly bigger font
                button.revalidate();

                // Play preloaded hover sound (instant)
                playHoverSound();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setPreferredSize(baseSize);
                button.setFont(loadPixelFont(32f)); // reset font
                button.revalidate();
            }
        });

        return button;
    }

    // ================= MAIN MENU =================
    private void showMainMenu() {
        getContentPane().removeAll();
        repaint();

        JPanel backgroundPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        backgroundPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;

        JButton startButton = createOvalButton("Start Game");
        JButton optionsButton = createOvalButton("Options");
        JButton creditsButton = createOvalButton("Credits");

        startButton.addActionListener(e -> {
            if (startGameListener != null) {
                startGameListener.run();
            } else {
                showDifficultyMenu();
            }
        });

        optionsButton.addActionListener(e -> showOptionsWindow());
        creditsButton.addActionListener(e -> showCreditsWindow());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        optionsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        creditsButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        buttonPanel.add(startButton);
        buttonPanel.add(Box.createVerticalStrut(30));
        buttonPanel.add(optionsButton);
        buttonPanel.add(Box.createVerticalStrut(30));
        buttonPanel.add(creditsButton);

        backgroundPanel.add(buttonPanel, gbc);
        add(backgroundPanel);

        revalidate();
        repaint();
    }

    // ================= DIFFICULTY MENU =================
    private void showDifficultyMenu() {
        getContentPane().removeAll();
        repaint();

        JPanel difficultyPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        difficultyPanel.setLayout(null);

        JButton easyBtn = createOvalButton("EASY");
        JButton mediumBtn = createOvalButton("MEDIUM");
        JButton hardBtn = createOvalButton("HARD");

        // =============================== 
        // >>> CONNECT TO MAIN LAUNCHER <<<
        // ===============================

        easyBtn.addActionListener(e -> {
            MainLauncher.startCanteenRushGame(1);
            dispose();
        });

        mediumBtn.addActionListener(e -> {
            MainLauncher.startCanteenRushGame(2);
            dispose();
        });

        hardBtn.addActionListener(e -> {
            MainLauncher.startCanteenRushGame(3);
            dispose();
        });

        int btnWidth = 300;
        int btnHeight = 70;

        SwingUtilities.invokeLater(() -> {
            int cx = (getWidth() - btnWidth) / 2;
            int yStart = (getHeight() / 2) - 150;

            easyBtn.setBounds(cx, yStart, btnWidth, btnHeight);
            mediumBtn.setBounds(cx, yStart + 100, btnWidth, btnHeight);
            hardBtn.setBounds(cx, yStart + 200, btnWidth, btnHeight);
        });

        difficultyPanel.add(easyBtn);
        difficultyPanel.add(mediumBtn);
        difficultyPanel.add(hardBtn);

        JButton backButton = createOvalButton("Back");
        backButton.setFont(loadPixelFont(20f));
        backButton.setBounds(30, 30, 150, 50);
        backButton.addActionListener(e -> showMainMenu());
        difficultyPanel.add(backButton);

        add(difficultyPanel);
        revalidate();
        repaint();
    }

    public void showDifficultyMenuDirect() {
        showDifficultyMenu();
    }

    // ================= MUSIC CONTROL =================

    private void playMusic(String musicFilePath) {
        try {
            File musicFile = new File(musicFilePath);
            if (!musicFile.exists()) {
                System.out.println("Music file not found!");
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioStream);

            try {
                volumeControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
            } catch (Exception ex) {
                volumeControl = null;
            }

            // Apply volume BEFORE starting
            isMuted = false;
            setVolume(previousVolume);

            // IMPORTANT FIX:
            // Run start in a short delay to ensure JFrame is visible
            new Thread(() -> {
                try {
                    Thread.sleep(300);        // Small delay ensures playback stability
                    backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

                    if (!backgroundMusic.isRunning()) {
                        backgroundMusic.start(); // Force start
                    }

                    System.out.println("Background music started.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setVolume(float volume) {
        if (volumeControl != null) {
            float min = volumeControl.getMinimum();
            float max = volumeControl.getMaximum();
            float gain = min + (max - min) * volume;
            volumeControl.setValue(gain);
        }
        // also update hover clip volume so it matches immediately
        if (hoverVolumeControl != null) {
            float min = hoverVolumeControl.getMinimum();
            float max = hoverVolumeControl.getMaximum();
            float gain = min + (max - min) * volume;
            hoverVolumeControl.setValue(gain);
        }
    }

    // ================= OPTIONS WINDOW =================

    private void showOptionsWindow() {
        // Directly open the main volume settings window
        showSettingsWindow();
    }

    // ================= SETTINGS WINDOW =================
    private void showSettingsWindow() {
        // Undecorated, modal dialog
        JDialog settingsDialog = new JDialog(this, "Settings", true);
        settingsDialog.setSize(600, 400);
        settingsDialog.setUndecorated(true);
        settingsDialog.setLocationRelativeTo(this);
        settingsDialog.setResizable(false);

        // Make dialog background fully transparent
        settingsDialog.setBackground(new Color(0, 0, 0, 0));

        // Main panel for custom drawing
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Semi-transparent yellow box with rounded corners
                g2.setColor(new Color(255, 230, 0, 210)); // 220 alpha for opacity
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); // 30px corner radius

                // Optional black border
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
        };

        panel.setOpaque(false);
        panel.setLayout(null);

        // Volume label
        JLabel volumeLabel = new JLabel("Volume:");
        volumeLabel.setFont(loadPixelFont(24f));
        volumeLabel.setBounds(100, 100, 150, 40);
        panel.add(volumeLabel);

        // ------------------ OVAL SLIDER APPLIED HERE -------------------
        // Volume slider
        JSlider volumeSlider = new JSlider(0, 100, (int) (previousVolume * 100));
        // Apply custom oval UI
        volumeSlider.setUI(new OvalSliderUI(volumeSlider));
        volumeSlider.setOpaque(false);
        volumeSlider.setBounds(220, 100, 250, 50);
        panel.add(volumeSlider);
        // ---------------------------------------------------------------

        // Mute button
        JButton muteButton = createOvalButton(isMuted ? "Unmute" : "Mute");
        muteButton.setBounds(220, 180, 150, 50);
        panel.add(muteButton);

        // Back button
        JButton backButton = createOvalButton("Back");
        backButton.setBounds(220, 260, 150, 50);
        panel.add(backButton);

        // Slider change listener
        volumeSlider.addChangeListener(e -> {
            if (!isMuted) {
                previousVolume = volumeSlider.getValue() / 100f;
                setVolume(previousVolume);
            } else {
                previousVolume = volumeSlider.getValue() / 100f;
            }
        });

        // Mute toggle
        muteButton.addActionListener(e -> {
            if (isMuted) {
                isMuted = false;
                muteButton.setText("Mute");
                setVolume(previousVolume);
            } else {
                isMuted = true;
                muteButton.setText("Unmute");
                setVolume(0f);
            }
        });

        // Close dialog with Back
        backButton.addActionListener(e -> settingsDialog.dispose());

        settingsDialog.add(panel);
        settingsDialog.setVisible(true);
    }

    // ================= FULLSCREEN CREDITS WINDOW =================
    private void showCreditsWindow() {
        JFrame creditsFrame = new JFrame("Credits");

        creditsFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        creditsFrame.setUndecorated(true);
        creditsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Image creditsBackground = new ImageIcon("canteen_bg.png").getImage();

        JPanel creditsPanel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(creditsBackground, 0, 0, getWidth(), getHeight(), this);

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // === Box size ===
                int boxWidth = 550;
                int boxHeight = 380;

                // === Center the box ===
                int boxX = (getWidth() - boxWidth) / 2;
                int boxY = (getHeight() - boxHeight) / 2;

                // Draw yellow box
                g2.setColor(new Color(255, 230, 0, 210)); // semi-transparent
                g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 40, 40);

                // Border
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, 40, 40);

                // === Text ===
                g.setFont(loadPixelFont(36f));
                g.setColor(Color.BLACK);

                int textX = boxX + 40;

                g.drawString("Developed by:", textX, boxY + 70);
                g.drawString("John Michael", textX, boxY + 140);
                g.drawString("Zhaharra", textX, boxY + 190);
                g.drawString("Jea Nicole", textX, boxY + 240);
                g.drawString("Keren Sole", textX, boxY + 290);

                g.setFont(loadPixelFont(32f));
                g.drawString("Thank you for playing!", textX, boxY + 350);
            }
        };

        creditsPanel.setLayout(null);

        JButton backButton = createOvalButton("Back");
        backButton.setFont(loadPixelFont(24f));
        creditsPanel.add(backButton);

        // === PLACE BACK BUTTON IN UPPER-LEFT CORNER ===
        SwingUtilities.invokeLater(() -> {
            int btnWidth = 150;
            int btnHeight = 60;

            backButton.setBounds(
                    30,   // X position (left side)
                    30,   // Y position (top)
                    btnWidth,
                    btnHeight
            );
        });

        backButton.addActionListener(e -> creditsFrame.dispose());

        creditsFrame.add(creditsPanel);
        creditsFrame.setVisible(true);

    }

    // Ensure clips are closed when disposing the frame
    @Override
    public void dispose() {
        try {
            if (backgroundMusic != null) {
                backgroundMusic.stop();
                backgroundMusic.close();
            }
        } catch (Exception ignored) {}
        try {
            if (hoverClip != null) {
                hoverClip.stop();
                hoverClip.close();
            }
        } catch (Exception ignored) {}
        super.dispose();
    }

    // ================= MAIN =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CanteenMenu().setVisible(true));
    }

    // ================= CUSTOM OVAL SLIDER UI (INNER CLASS) =================
    // This must be inside the CanteenMenu class but outside any method.
    private static class OvalSliderUI extends javax.swing.plaf.basic.BasicSliderUI {

        public OvalSliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int trackHeight = 12;
            int trackY = trackRect.y + (trackRect.height - trackHeight) / 2;

            // Background Track (rounded)
            g2.setColor(new Color(255, 225, 90));   // light yellow track
            g2.fillRoundRect(trackRect.x, trackY, trackRect.width, trackHeight, 20, 20);

            // Outline
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(trackRect.x, trackY, trackRect.width, trackHeight, 20, 20);
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = thumbRect.width;
            int h = thumbRect.height;

            // Thumb fill
            g2.setColor(new Color(255, 245, 120));
            g2.fillOval(thumbRect.x, thumbRect.y, w, h);

            // Thumb outline
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(thumbRect.x, thumbRect.y, w, h);
        }

        @Override
        protected Dimension getThumbSize() {
            return new Dimension(22, 22); // circular thumb size
        }
    }
}

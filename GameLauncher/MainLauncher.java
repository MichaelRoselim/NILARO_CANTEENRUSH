package GameLauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import CanteenRushh.CanteenMenu;
import CanteenRushh.INTRO;
import CanteenRushh.GamePanel;

/**
 * MainLauncher - robust window switching for Canteen Rush
 *
 * Handles:
 *  - components that might be JFrame or JPanel (wraps JPanel in a JFrame)
 *  - per-window translucency only when supported (safe fallback)
 *  - avoids lambdas for Java 7 compatibility
 *  - catches exceptions from callbacks so EDT doesn't die silently
 */
public class MainLauncher {

    private static Window activeWindow = null;

    public static void main(String[] args) {
        // Always start UI on EDT
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                startMainMenu();
            }
        });
    }

    public static void startMainMenu() {
        // create menu; could be JFrame or JPanel - we treat generically
        final Object menuObj = new CanteenMenu();

        // attach start listener if available (wrap in try/catch to avoid EDT crash)
        try {
            if (menuObj instanceof CanteenMenu) {
                ((CanteenMenu) menuObj).addStartGameListener(new Runnable() {
                    public void run() {
                        try {
                            fadeDispose(activeWindow);
                            startIntroScene();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                });
            }
        } catch (Throwable t) {
            System.out.println("WARNING: could not register start listener: " + t.getMessage());
            t.printStackTrace();
        }

        showNewWindowFor(menuObj);
    }

    public static void startIntroScene() {
        final INTRO introPanel = new INTRO();

        final JFrame storyFrame = new JFrame();
        storyFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        storyFrame.setUndecorated(true);
        storyFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        storyFrame.setContentPane(introPanel);

        // register story-finished callback if available
        try {
            introPanel.setStoryFinishedListener(new Runnable() {
                public void run() {
                    try {
                        fadeDispose(activeWindow);
                        startDifficultyMenu();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
            try {
                introPanel.startGameThread();
            } catch (Throwable t) {
                System.out.println("WARNING: startGameThread threw: " + t.getMessage());
                t.printStackTrace();
            }
        } catch (Throwable t) {
            System.out.println("WARNING: intro callbacks missing or failed: " + t.getMessage());
            t.printStackTrace();
        }

        showNewWindowFor(storyFrame);
    }

    public static void startDifficultyMenu() {
        final Object menuObj = new CanteenMenu();

        try {
            if (menuObj instanceof CanteenMenu) {
                ((CanteenMenu) menuObj).showDifficultyMenuDirect();
            }
        } catch (Throwable t) {
            System.out.println("WARNING: showDifficultyMenuDirect missing or threw: " + t.getMessage());
            t.printStackTrace();
        }

        showNewWindowFor(menuObj);
    }

    // ===================== UPDATED VERSION =====================
    public static void startCanteenRushGame(final int difficulty) {
        final GamePanel panel = new GamePanel();

        final JFrame gameFrame = new JFrame("Canteen Rush - Difficulty: " + difficulty);
        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameFrame.setUndecorated(true);
        gameFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        gameFrame.getContentPane().add(panel);

        // Use the central fade/window logic
        showNewWindowFor(gameFrame);

        // OPTIONAL: spawn customers when game starts (after small delay)
        Timer customerTimer = new Timer(300, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((Timer) e.getSource()).stop();
                try {
                    panel.spawnCustomerByName("Miki");
                    panel.spawnCustomerByName("Jea");
                    panel.spawnCustomerByName("Rov");
                    panel.spawnCustomerByName("Keren");
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
        customerTimer.setRepeats(false);
        customerTimer.start();
    }
    // ============================================================

    // ============================================================
    // WINDOW SWITCHING / FADE
    // ============================================================
    private static void showNewWindowFor(Object source) {
        final Window newWindow = toWindow(source);
        if (newWindow == null) {
            System.out.println("ERROR: showNewWindowFor: could not create a Window for: " + source);
            return;
        }

        if (activeWindow != null) {
            fadeDispose(activeWindow);
        }

        activeWindow = newWindow;

        if (!isTranslucencySupported()) {
            try {
                prepareAndShowWindow(activeWindow);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return;
        }

        try {
            if (activeWindow instanceof Frame) {
                Frame f = (Frame) activeWindow;
                if (f.isDisplayable()) {
                    prepareAndShowWindow(activeWindow);
                    return;
                } else {
                    f.setUndecorated(true);
                }
            }

            setWindowOpacitySafe(activeWindow, 0f);
            prepareAndShowWindow(activeWindow);
            fadeIn(activeWindow);
        } catch (Throwable t) {
            System.out.println("WARNING: translucency/fade failed - showing normally: " + t.getMessage());
            t.printStackTrace();
            try { prepareAndShowWindow(activeWindow); } catch (Throwable ignore) {}
        }
    }

    private static Window toWindow(Object source) {
        if (source == null) return null;

        if (source instanceof Window) {
            return (Window) source;
        }

        if (source instanceof Component) {
            Component comp = (Component) source;
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.getContentPane().removeAll();
            frame.getContentPane().add(comp);
            frame.pack();
            return frame;
        }

        System.out.println("toWindow: unknown source type: " + source.getClass().getName());
        return null;
    }

    private static void prepareAndShowWindow(final Window w) {
        if (w == null) return;

        if (w instanceof Frame) {
            Frame f = (Frame) w;
            if (f.getWidth() == 0 || f.getHeight() == 0) {
                f.pack();
            }
            f.setExtendedState(Frame.MAXIMIZED_BOTH);
        } else if (w instanceof Dialog) {
            Dialog d = (Dialog) w;
            if (d.getWidth() == 0 || d.getHeight() == 0) {
                d.pack();
            }
        }

        w.setVisible(true);
    }

    private static boolean isTranslucencySupported() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            return gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void setWindowOpacitySafe(Window w, float opacity) {
        try {
            w.setOpacity(Math.max(0f, Math.min(1f, opacity)));
        } catch (Throwable t) {
            // ignore - not supported
        }
    }

    private static void fadeDispose(final Window window) {
        if (window == null) return;

        if (!isTranslucencySupported()) {
            try { window.dispose(); } catch (Throwable ignore) {}
            return;
        }

        final Timer timer = new Timer(10, null);
        timer.addActionListener(new ActionListener() {
            float opacity = 1f;

            public void actionPerformed(ActionEvent e) {
                try {
                    opacity -= 0.05f;
                    if (opacity <= 0f) {
                        timer.stop();
                        try { window.dispose(); } catch (Throwable ignore) {}
                    } else {
                        setWindowOpacitySafe(window, opacity);
                    }
                } catch (Throwable t) {
                    timer.stop();
                    try { window.dispose(); } catch (Throwable ignore) {}
                }
            }
        });
        timer.start();
    }

    private static void fadeIn(final Window window) {
        if (window == null) return;
        if (!isTranslucencySupported()) return;

        final Timer timer = new Timer(10, null);
        timer.addActionListener(new ActionListener() {
            float opacity = 0f;

            public void actionPerformed(ActionEvent e) {
                try {
                    opacity += 0.05f;
                    if (opacity >= 1f) {
                        timer.stop();
                        setWindowOpacitySafe(window, 1f);
                    } else {
                        setWindowOpacitySafe(window, opacity);
                    }
                } catch (Throwable t) {
                    timer.stop();
                }
            }
        });
        timer.start();
    }
}

package CanteenRushh;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Main application entry point for Canteen Rush.
 * This class creates the main window and runs the INTRO panel.
 */
public class Main {
    
    /**
     * The main entry point for the Canteen Rush game.
     * This method initializes the JFrame and the primary game panel (INTRO).
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {

            JFrame window = new JFrame("CANTEEN RUSH: Storyline");

            // Close when window is closed
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // ===== MAKE FULLSCREEN =====
            window.setExtendedState(JFrame.MAXIMIZED_BOTH);  // fills whole laptop screen
            window.setUndecorated(true);                     // remove title bar
            window.setResizable(false);                      // lock fullscreen size

            // Load the INTRO panel
            INTRO gamePanel = new INTRO();
            window.add(gamePanel);

            // Center is not needed for fullscreen, but harmless
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            // Start the game loop
            gamePanel.startGameThread();
        });
    }
}

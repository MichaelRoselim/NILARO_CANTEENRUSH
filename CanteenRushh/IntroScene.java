package CanteenRushh;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages dialogues and rendering for INTRO scenes.
 */
public class IntroScene {

    private String currentText = "";
    private String currentSpeaker = "";
    private boolean finished = true;

    // Dialog box appearance
    private final Color boxColor = Color.decode("#f2c515");
    private final Color borderColor = Color.BLACK;
    private final int bottomMargin = 20; // 20 pixels from bottom
    private final int sideMargin = 20;   // 20 pixels from left and right

    private boolean skipAnimation = false;
    private int charIndex = 0;

    private Timer typingTimer;

    public void startDialog(String text, String speaker) {
        this.currentText = text;
        this.currentSpeaker = speaker;
        this.finished = false;
        this.charIndex = 0;
        this.skipAnimation = false;

        if (typingTimer != null) {
            typingTimer.cancel();
        }

        typingTimer = new Timer();
        typingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!skipAnimation && charIndex < currentText.length()) {
                    charIndex++;
                } else {
                    finished = true;
                    typingTimer.cancel();
                }
            }
        }, 0, 30); // Typing speed
    }

    public void skipDialog() {
        skipAnimation = true;
        charIndex = currentText.length();
        finished = true;
        if (typingTimer != null) typingTimer.cancel();
    }

    public boolean isFinished() {
        return finished;
    }

    public void update() {
        // Could add animations here
    }

    public void draw(Graphics2D g2, int panelWidth, int panelHeight) {
        if (currentText.isEmpty()) return;

        // Box size
        int boxWidth = panelWidth - 2 * sideMargin;
        int boxHeight = 140; // Slightly taller to fit text nicely
        int x = sideMargin;
        int y = panelHeight - boxHeight - bottomMargin;

        // Fill box
        g2.setColor(boxColor);
        g2.fillRect(x, y, boxWidth, boxHeight);

        // Border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(x, y, boxWidth, boxHeight);

        // Speaker
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString(currentSpeaker + ":", x + 15, y + 30);

        // Text (partial for typing effect)
        g2.setFont(new Font("Courier New", Font.BOLD, 25));
        String displayText = currentText.substring(0, Math.min(charIndex, currentText.length()));
        drawStringMultiLine(g2, displayText, x + 15, y + 60, boxWidth - 30);
    }

    private void drawStringMultiLine(Graphics2D g2, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g2.getFontMetrics();
        int lineHeight = fm.getHeight();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String testLine = line + word + " ";
            if (fm.stringWidth(testLine) > maxWidth) {
                g2.drawString(line.toString(), x, y);
                line = new StringBuilder(word + " ");
                y += lineHeight;
            } else {
                line.append(word).append(" ");
            }
        }
        g2.drawString(line.toString(), x, y);
    }
}

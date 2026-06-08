package io.github.javainvaders;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Handles score calcualtion and level bonuses.
 * Kinda simple but keeps the scoring logic out of Main.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Score {

    /** Points given for a type-0 (basic) alien kill. */
    public static final int PTS_TYPE0 = 10;

    /** Points for a type-1 (medium) alien kill. */
    public static final int PTS_TYPE1 = 20;

    /** Points for a type-2 (tough) alien kill. */
    public static final int PTS_TYPE2 = 30;

    /**
     * Returns how many points an alien of the given type is worth.
     * Types above 2 fallback to 10 just in case.
     *
     * @param type the alien type (0, 1 or 2)
     * @return point value
     */
    public static int pointsForAlien(int type) {
        if (type == 2) return PTS_TYPE2;
        if (type == 1) return PTS_TYPE1;
        return PTS_TYPE0;
    }

    /**
     * Calculates the level-completion bonus based on the current level.
     * Bonus grows with each level so players have reason to keep going.
     *
     * @param level current level number (1-based)
     * @return bonus points to award
     */
    public static int levelBonus(int level) {
        return 500 + level * 250;
    }

    /**
     * Awards the level-completion bonus to any player that still has
     * lives or is alive. Stores the amounts in bonusAwarded for the UI.
     *
     * @param p1           player one
     * @param p2           player two
     * @param level        current level — determines bonus size
     * @param bonusAwarded two-element array that gets filled with [p1bonus, p2bonus]
     */
    public static void awardLevelBonus(Player p1, Player p2, int level, int[] bonusAwarded) {
        int bonus = levelBonus(level);
        if (p1.lives > 0 || p1.alive) { p1.score += bonus; bonusAwarded[0] = bonus; }
        if (p2.lives > 0 || p2.alive) { p2.score += bonus; bonusAwarded[1] = bonus; }
    }

    /**
     * Draws the HUD score text for both players and the current level label.
     * Also handles the fading "Saved!" feedback message.
     *
     * @param batch             active SpriteBatch (already begun)
     * @param font              regular font
     * @param p1                player one
     * @param p2                player two
     * @param level             current level
     * @param saveFeedbackTimer seconds left on the "Saved!" fade — 0 means dont draw it
     * @param screenW           game screen width for right-side positioning
     * @param screenH           game screen height for top positioning
     */
    public static void drawHUD(SpriteBatch batch, BitmapFont font,
                                Player p1, Player p2, int level,
                                float saveFeedbackTimer, int screenW, int screenH) {
        font.setColor(com.badlogic.gdx.graphics.Color.CYAN);
        font.draw(batch, "P1  " + p1.score, 20, screenH - 20);

        font.setColor(com.badlogic.gdx.graphics.Color.LIME);
        font.draw(batch, "P2  " + p2.score, screenW - 200, screenH - 20);

        font.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        font.draw(batch, "LEVEL " + level, screenW / 2f - 50, screenH - 20);

        font.setColor(com.badlogic.gdx.graphics.Color.DARK_GRAY);
        font.draw(batch, "F5: Save", screenW / 2f - 40, 30);

        if (saveFeedbackTimer > 0) {
            float alpha = Math.min(1f, saveFeedbackTimer);
            font.setColor(0f, 1f, 0f, alpha);
            font.draw(batch, "Saved!", screenW / 2f + 60, 30);
        }
    }
}

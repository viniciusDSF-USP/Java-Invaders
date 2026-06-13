package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for Score.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class ScoreTest {

    // pointsForAlien tests

    /**
     * Type 0 should return the base point value.
     */
    @Test
    public void pointsForAlien_type0_returns10() {
        assertEquals(Score.PTS_TYPE0, Score.pointsForAlien(0));
    }

    /**
     * Type 1 should return the mid-tier point value.
     */
    @Test
    public void pointsForAlien_type1_returns20() {
        assertEquals(Score.PTS_TYPE1, Score.pointsForAlien(1));
    }

    /**
     * Type 2 should return the highest point value.
     */
    @Test
    public void pointsForAlien_type2_returns30() {
        assertEquals(Score.PTS_TYPE2, Score.pointsForAlien(2));
    }

    /**
     * Unknown types should fall back to the type-0 value.
     */
    @Test
    public void pointsForAlien_unknownType_fallsBackTo10() {
        assertEquals(Score.PTS_TYPE0, Score.pointsForAlien(99));
    }

    // levelBonus tests

    /**
     * Level 1 bonus should equal 750.
     */
    @Test
    public void levelBonus_level1_returns750() {
        assertEquals(750, Score.levelBonus(1));
    }

    /**
     * Bonus should increase with each level.
     */
    @Test
    public void levelBonus_growsWithLevel() {
        int bonus1 = Score.levelBonus(1);
        int bonus2 = Score.levelBonus(2);
        int bonus3 = Score.levelBonus(3);

        assertTrue("Level-2 bonus should exceed level-1", bonus2 > bonus1);
        assertTrue("Level-3 bonus should exceed level-2", bonus3 > bonus2);
    }

    // awardLevelBonus tests

    /**
     * Both alive players should receive the full level bonus.
     */
    @Test
    public void awardLevelBonus_addsBonusToBothAlive_players() {
        Player p1           = new Player(1, 300f);
        Player p2           = new Player(2, 600f);
        int[]  bonusAwarded = new int[2];
        int    expected     = Score.levelBonus(1);

        Score.awardLevelBonus(p1, p2, 1, bonusAwarded);

        assertEquals(expected, p1.score);
        assertEquals(expected, p2.score);
        assertEquals(expected, bonusAwarded[0]);
        assertEquals(expected, bonusAwarded[1]);
    }

    /**
     * A dead player with no lives left should not recieve any bonus.
     */
    @Test
    public void awardLevelBonus_skipsDeadPlayerWithNoLives() {
        Player p1           = new Player(1, 300f);
        Player p2           = new Player(2, 600f);
        int[]  bonusAwarded = new int[2];
        p2.alive = false;
        p2.lives = 0;

        Score.awardLevelBonus(p1, p2, 1, bonusAwarded);

        assertEquals(Score.levelBonus(1), p1.score);
        assertEquals(0, p2.score);
        assertEquals(0, bonusAwarded[1]);
    }

    /**
     * A dead player who still has lives should still get the bonus.
     */
    @Test
    public void awardLevelBonus_awardsDeadPlayerIfHasRemainingLives() {
        Player p1           = new Player(1, 300f);
        Player p2           = new Player(2, 600f);
        int[]  bonusAwarded = new int[2];
        p1.alive = false;
        p1.lives = 1; // one life remaining

        Score.awardLevelBonus(p1, p2, 2, bonusAwarded);

        assertEquals(Score.levelBonus(2), p1.score);
        assertEquals(Score.levelBonus(2), bonusAwarded[0]);
    }
}

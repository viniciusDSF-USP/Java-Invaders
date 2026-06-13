package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Unit tests for Alien and its static helpers.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienTest {

    // Screen width passed to movement methods
    private static final int SCREEN_W = 1280;

    /**
     * Restores default step and drop values before each test.
     */
    @Before
    public void resetStaticDefaults() {
        Alien.ALIEN_STEP = 12f;
        Alien.ALIEN_DROP = 18f;
    }

    // Constructor tests

    /**
     * Position, type and alive flag should be set on construction.
     */
    @Test
    public void constructor_setsFieldsCorrectly() {
        Alien a = new Alien(100f, 200f, 1);

        assertEquals(100f, a.x,    0.001f);
        assertEquals(200f, a.y,    0.001f);
        assertEquals(1,    a.type);
        assertTrue("Alien should start alive", a.alive);
    }

    // Rect tests

    /**
     * Bounding box should be centered on the alien position.
     */
    @Test
    public void rect_returnsCorrectBoundingBox() {
        Alien     a = new Alien(100f, 200f, 0);
        Rectangle r = a.rect();

        assertEquals(100f - Alien.ALIEN_W / 2, r.x,      0.001f);
        assertEquals(200f - Alien.ALIEN_H / 2, r.y,      0.001f);
        assertEquals(Alien.ALIEN_W,             r.width,  0.001f);
        assertEquals(Alien.ALIEN_H,             r.height, 0.001f);
    }

    // allAliensDead tests

    /**
     * Should return false when at least one alien is still alive.
     */
    @Test
    public void allAliensDead_returnsFalse_whenAtLeastOneIsAlive() {
        Array<Alien> aliens = buildGrid();
        assertFalse(Alien.allAliensDead(aliens));
    }

    /**
     * Should return true once every alien is marked dead.
     */
    @Test
    public void allAliensDead_returnsTrue_afterAllKilled() {
        Array<Alien> aliens = buildGrid();
        for (Alien a : aliens) a.alive = false;

        assertTrue(Alien.allAliensDead(aliens));
    }

    /**
     * An empty array should be treated as all dead.
     */
    @Test
    public void allAliensDead_returnsTrue_onEmptyArray() {
        assertTrue(Alien.allAliensDead(new Array<>()));
    }

    // moveAliens lateral tests

    /**
     * Aliens should shift right by one step when direction is positive.
     */
    @Test
    public void moveAliens_movesAliensRightByStep_whenDirectionIsPositive() {
        Array<Alien>  aliens = new Array<>();
        Alien         a      = new Alien(200f, 400f, 0);
        AlienMoveState state = new AlienMoveState(1f);
        aliens.add(a);

        Alien.moveAliens(aliens, state, 1, SCREEN_W);

        assertEquals(200f + Alien.ALIEN_STEP, a.x, 0.001f);
    }

    /**
     * Aliens should shift left by one step when direction is negative.
     */
    @Test
    public void moveAliens_movesAliensLeftByStep_whenDirectionIsNegative() {
        Array<Alien>   aliens = new Array<>();
        Alien          a      = new Alien(600f, 400f, 0);
        AlienMoveState state  = new AlienMoveState(-1f);
        aliens.add(a);

        Alien.moveAliens(aliens, state, 1, SCREEN_W);

        assertEquals(600f - Alien.ALIEN_STEP, a.x, 0.001f);
    }

    /**
     * Dead aliens should not be moved.
     */
    @Test
    public void moveAliens_skipsDeadAliens() {
        Array<Alien>   aliens = new Array<>();
        Alien          dead   = new Alien(200f, 400f, 0);
        AlienMoveState state  = new AlienMoveState(1f);
        dead.alive = false;
        aliens.add(dead);

        Alien.moveAliens(aliens, state, 1, SCREEN_W);

        // Position must not change for dead aliens
        assertEquals(200f, dead.x, 0.001f);
    }

    // moveAliens descend tests

    /**
     * When descend is flagged, aliens drop and direction flips.
     */
    @Test
    public void moveAliens_dropsAliens_whenDescendFlagIsTrue() {
        Array<Alien>   aliens = new Array<>();
        Alien          a      = new Alien(400f, 400f, 0);
        AlienMoveState state  = new AlienMoveState(1f);
        aliens.add(a);
        state.alienDescend = true;

        Alien.moveAliens(aliens, state, 1, SCREEN_W);

        assertEquals(400f - Alien.ALIEN_DROP, a.y, 0.001f);
        assertFalse("alienDescend must be cleared after drop", state.alienDescend);
        assertEquals(-1f, state.alienDirX, 0.001f); // direction flips after drop
    }

    /**
     * Reaching the right edge should set the descend flag.
     */
    @Test
    public void moveAliens_setsDescendFlag_whenRightEdgeReached() {
        Array<Alien>   aliens = new Array<>();
        AlienMoveState state  = new AlienMoveState(1f);

        // Place alien just close enough to trigger the edge check
        float edgeX = SCREEN_W - 20 - Alien.ALIEN_W / 2 - 1f;
        Alien a     = new Alien(edgeX, 400f, 0);
        aliens.add(a);

        Alien.moveAliens(aliens, state, 1, SCREEN_W);

        assertTrue("alienDescend should be set when right edge is reached", state.alienDescend);
    }

    // Level speed bonus tests

    /**
     * Level 2 and above should move aliens further than level 1.
     */
    @Test
    public void moveAliens_appliesExtraStep_forLevel2AndAbove() {
        Array<Alien> aliens1 = new Array<>();
        Array<Alien> aliens2 = new Array<>();
        Alien        a1      = new Alien(200f, 400f, 0);
        Alien        a2      = new Alien(200f, 400f, 0);
        aliens1.add(a1);
        aliens2.add(a2);

        Alien.moveAliens(aliens1, new AlienMoveState(1f), 1, SCREEN_W);
        Alien.moveAliens(aliens2, new AlienMoveState(1f), 2, SCREEN_W);

        assertTrue("Level-2 aliens should move further than level-1", a2.x > a1.x);
    }

    // Helpers

    /** Builds a small 2x2 alien grid for convenience. */
    private Array<Alien> buildGrid() {
        Array<Alien> list = new Array<>();
        list.add(new Alien(100f, 500f, 0));
        list.add(new Alien(150f, 500f, 0));
        list.add(new Alien(100f, 460f, 1));
        list.add(new Alien(150f, 460f, 1));
        return list;
    }
}

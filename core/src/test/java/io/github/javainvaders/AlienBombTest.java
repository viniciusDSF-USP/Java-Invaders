package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.math.Rectangle;

/**
 * Unit tests for Alien.Bomb and Alien.RadialBomb.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienBombTest {

    // Default speed values used across tests
    private static final float DEFAULT_BOMB_SPEED   = 220f;
    private static final float DEFAULT_RADIAL_SPEED = 500f;

    /**
     * Resets static speed fields before each test.
     */
    @Before
    public void resetStaticDefaults() {
        Alien.Bomb.BOMB_SPEED         = DEFAULT_BOMB_SPEED;
        Alien.RadialBomb.RADIAL_SPEED = DEFAULT_RADIAL_SPEED;
    }

    // Bomb tests

    /**
     * Checks that the bomb stores its spawn position.
     */
    @Test
    public void bomb_constructor_setsPositionCorrectly() {
        Alien.Bomb b = new Alien.Bomb(150f, 300f);
        assertEquals(150f, b.x, 0.001f);
        assertEquals(300f, b.y, 0.001f);
    }

    /**
     * Verifies the hitbox origin and dimensions match the bomb constants.
     */
    @Test
    public void bomb_rect_returnsCorrectHitbox() {
        Alien.Bomb b = new Alien.Bomb(150f, 300f);
        Rectangle  r = b.rect();

        // X is centered on the bomb position
        assertEquals(150f - Alien.Bomb.BOMB_W / 2, r.x,      0.001f);
        assertEquals(300f,                          r.y,      0.001f);
        assertEquals(Alien.Bomb.BOMB_W,             r.width,  0.001f);
        assertEquals(Alien.Bomb.BOMB_H,             r.height, 0.001f);
    }

    // RadialBomb tests

    /**
     * Confirms position and direction vector are set from the given angle.
     */
    @Test
    public void radialBomb_constructor_setsPositionAndDirection() {
        double            angle = Math.PI / 4; // 45 degrees
        Alien.RadialBomb  rb    = new Alien.RadialBomb(200f, 400f, (float) angle);

        assertEquals(200f,                    rb.x,  0.001f);
        assertEquals(400f,                    rb.y,  0.001f);
        assertEquals((float) Math.cos(angle), rb.vx, 0.001f);
        assertEquals((float) Math.sin(angle), rb.vy, 0.001f);
    }

    /**
     * Angle zero should produce a rightward unit vector.
     */
    @Test
    public void radialBomb_firingRight_hasPositiveVxAndZeroVy() {
        Alien.RadialBomb rb = new Alien.RadialBomb(0f, 0f, 0f);
        assertEquals( 1f, rb.vx, 0.001f);
        assertEquals( 0f, rb.vy, 0.001f);
    }

    /**
     * Negative PI/2 should point straight downward.
     */
    @Test
    public void radialBomb_firingDown_hasNegativeVy() {
        // Angle -PI/2 points straight down (negative y axis)
        Alien.RadialBomb rb = new Alien.RadialBomb(0f, 0f, (float) (-Math.PI / 2));
        assertEquals( 0f, rb.vx, 0.001f);
        assertEquals(-1f, rb.vy, 0.001f);
    }

    /**
     * Ensures RadialBomb is a subtype of Bomb.
     */
    @Test
    public void radialBomb_isInstanceOfBomb() {
        Alien.RadialBomb rb = new Alien.RadialBomb(0f, 0f, 0f);
        assertTrue(rb instanceof Alien.Bomb);
    }
}

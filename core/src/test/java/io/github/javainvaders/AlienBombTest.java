package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.math.Rectangle;

/**
 * Unit tests for {@link Alien.Bomb} and {@link Alien.RadialBomb}.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienBombTest {

    @Before
    public void resetStaticDefaults() {
        Alien.Bomb.BOMB_SPEED          = 220f;
        Alien.RadialBomb.RADIAL_SPEED  = 500f;
    }

    // Bomb constructor

    @Test
    public void bomb_constructor_setsPositionCorrectly() {
        Alien.Bomb b = new Alien.Bomb(150f, 300f);

        assertEquals(150f, b.x, 0.001f);
        assertEquals(300f, b.y, 0.001f);
    }

    // Bomb rect

    @Test
    public void bomb_rect_returnsCorrectHitbox() {
        Alien.Bomb b = new Alien.Bomb(150f, 300f);
        Rectangle r = b.rect();

        assertEquals(150f - Alien.Bomb.BOMB_W / 2, r.x,      0.001f);
        assertEquals(300f,                          r.y,      0.001f);
        assertEquals(Alien.Bomb.BOMB_W,             r.width,  0.001f);
        assertEquals(Alien.Bomb.BOMB_H,             r.height, 0.001f);
    }

    // RadialBomb constructor

    @Test
    public void radialBomb_constructor_setsPositionAndDirection() {
        double angle = Math.PI / 4; // 45 degrees
        Alien.RadialBomb rb = new Alien.RadialBomb(200f, 400f, (float) angle);

        assertEquals(200f, rb.x, 0.001f);
        assertEquals(400f, rb.y, 0.001f);
        assertEquals((float) Math.cos(angle), rb.vx, 0.001f);
        assertEquals((float) Math.sin(angle), rb.vy, 0.001f);
    }

    @Test
    public void radialBomb_firingRight_hasPositiveVxAndZeroVy() {
        Alien.RadialBomb rb = new Alien.RadialBomb(0f, 0f, 0f); // angle 0 = right

        assertEquals(1f,  rb.vx, 0.001f);
        assertEquals(0f,  rb.vy, 0.001f);
    }

    @Test
    public void radialBomb_firingDown_hasNegativeVy() {
        // angle = -PI/2 points straight down (negative y)
        Alien.RadialBomb rb = new Alien.RadialBomb(0f, 0f, (float) (-Math.PI / 2));

        assertEquals(0f,  rb.vx, 0.001f);
        assertEquals(-1f, rb.vy, 0.001f);
    }

    // RadialBomb extends Bomb

    @Test
    public void radialBomb_isInstanceOfBomb() {
        Alien.RadialBomb rb = new Alien.RadialBomb(0f, 0f, 0f);
        assertTrue(rb instanceof Alien.Bomb);
    }
}

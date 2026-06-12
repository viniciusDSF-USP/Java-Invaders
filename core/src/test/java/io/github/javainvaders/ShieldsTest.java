package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Unit tests for {@link Shields}.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class ShieldsTest {

    private static final int SCREEN_W = 1280;

    // constructor

    @Test
    public void constructor_setsInitialState() {
        Shields s = new Shields(400f, 150f);

        assertEquals(400f, s.x,    0.001f);
        assertEquals(150f, s.y,    0.001f);
        assertEquals(0,    s.hits);
        assertTrue("Shield should start alive", s.alive);
        assertEquals(0f, s.breakTimer, 0.001f);
    }

    // rect

    @Test
    public void rect_returnsCorrectHitbox() {
        Shields s = new Shields(400f, 150f);
        Rectangle r = s.rect();

        assertEquals(400f - Shields.SHIELD_W / 2, r.x,      0.001f);
        assertEquals(150f - Shields.SHIELD_H / 2, r.y,      0.001f);
        assertEquals(Shields.SHIELD_W,             r.width,  0.001f);
        assertEquals(Shields.SHIELD_H,             r.height, 0.001f);
    }

    // hitByBomb

    @Test
    public void hitByBomb_incrementsHitsCounter() {
        Shields s = new Shields(400f, 150f);
        Shields.hitByBomb(s);

        assertEquals(1, s.hits);
        assertTrue("Shield should still be alive after 1 hit", s.alive);
    }

    @Test
    public void hitByBomb_startsBreakAnimation_whenMaxHitsReached() {
        Shields s = new Shields(400f, 150f);
        for (int i = 0; i < Shields.MAX_HITS; i++) {
            Shields.hitByBomb(s);
        }

        assertFalse("Shield must not be alive after MAX_HITS", s.alive);
        assertTrue("breakTimer must be positive after break starts", s.breakTimer > 0f);
    }

    @Test
    public void hitByBomb_ignoresDeadShield() {
        Shields s = new Shields(400f, 150f);
        s.alive = false;

        Shields.hitByBomb(s);

        assertEquals(0, s.hits); // no change
    }

    // destroyInstantly

    @Test
    public void destroyInstantly_setsAliveToFalseAndStartsAnimation() {
        Shields s = new Shields(400f, 150f);

        Shields.destroyInstantly(s);

        assertFalse(s.alive);
        assertTrue(s.breakTimer > 0f);
        assertEquals(Shields.MAX_HITS, s.hits);
    }

    // isGone

    @Test
    public void isGone_returnsFalse_whenShieldIsAlive() {
        Shields s = new Shields(400f, 150f);
        assertFalse(s.isGone());
    }

    @Test
    public void isGone_returnsTrue_whenDeadAndTimerExpired() {
        Shields s = new Shields(400f, 150f);
        s.alive      = false;
        s.breakTimer = 0f;

        assertTrue(s.isGone());
    }

    // createForLevel

    @Test
    public void createForLevel_level1_creates3Shields() {
        Array<Shields> list = Shields.createForLevel(1, SCREEN_W);
        assertEquals(3, list.size);
    }

    @Test
    public void createForLevel_level2_creates2Shields() {
        Array<Shields> list = Shields.createForLevel(2, SCREEN_W);
        assertEquals(2, list.size);
    }

    @Test
    public void createForLevel_level3_creates1Shield() {
        Array<Shields> list = Shields.createForLevel(3, SCREEN_W);
        assertEquals(1, list.size);
    }

    @Test
    public void createForLevel_level4_createsNoShields() {
        Array<Shields> list = Shields.createForLevel(4, SCREEN_W);
        assertEquals(0, list.size);
    }

    // update

    @Test
    public void update_removesExpiredShields() {
        Array<Shields> list = new Array<>();
        Shields s = new Shields(400f, 150f);
        s.alive      = false;
        s.breakTimer = 0f;
        list.add(s);

        Shields.update(list, 0.1f);

        assertEquals(0, list.size);
    }

    @Test
    public void update_keepsShieldWhileBreakTimerPositive() {
        Array<Shields> list = new Array<>();
        Shields s = new Shields(400f, 150f);
        s.alive      = false;
        s.breakTimer = 0.5f;
        list.add(s);

        Shields.update(list, 0.1f); // timer goes to 0.4

        assertEquals(1, list.size);
    }
}

package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Alien.Boss}.
 *
 * NOTE: Boss.update() uses LibGDX rendering internals (ShapeRenderer).
 * These tests cover only the pure-logic methods (constructor, hit, etc.)
 * that have no rendering dependency.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienBossTest {
    private static final int SCREEN_W = 1280;

    @Before
    public void resetStaticDefaults() {
        Alien.Boss.BOSS_MAX_HP       = 50;
        Alien.Boss.BOSS_SPEED_X      = 100f;
        Alien.Boss.BOSS_FIRE_INTERVAL = 2.5f;
        Alien.Boss.BOSS_BURST_COUNT  = 24;
    }

    // constructor

    @Test
    public void constructor_bossStartsAliveAndEntering() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);

        assertTrue("Boss must start alive", boss.alive);
        assertEquals(Alien.Boss.BOSS_MAX_HP, boss.hp);
    }

    // hit

    @Test
    public void hit_decrementsBossHP() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);
        int hpBefore = boss.hp;

        boss.hit();

        assertEquals(hpBefore - 1, boss.hp);
    }

    @Test
    public void hit_returnsFalse_whenBossStillAlive() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);

        boolean killed = boss.hit();

        assertFalse(killed);
        assertTrue(boss.alive);
    }

    @Test
    public void hit_returnsTrueAndStartsDeathSequence_whenHPReachesZero() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);
        // Drain HP to 1 without triggering death
        for (int i = 0; i < Alien.Boss.BOSS_MAX_HP - 1; i++) {
            boss.hit();
        }

        boolean killed = boss.hit(); // this one brings HP to 0

        assertTrue("hit() should return true on killing blow", killed);
        assertFalse("Boss.alive must be false after death", boss.alive);
    }

    @Test
    public void hit_ignoresCallsAfterDeath() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);
        // Kill the boss
        for (int i = 0; i < Alien.Boss.BOSS_MAX_HP; i++) boss.hit();

        boolean result = boss.hit(); // should be ignored

        assertFalse("hit() on a dead boss must return false", result);
    }

    @Test
    public void hit_hpNeverGoesBelowZero() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);
        for (int i = 0; i < Alien.Boss.BOSS_MAX_HP + 10; i++) boss.hit();

        assertTrue("HP should not be negative", boss.hp <= 0);
    }
}

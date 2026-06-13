package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for Alien.Boss pure-logic methods.
 *
 * Note: Boss.update() depends on LibGDX rendering internals (ShapeRenderer)
 * and is not covered here.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienBossTest {

    // Screen width passed to the Boss constructor
    private static final int SCREEN_W = 1280;

    /**
     * Restores static boss fields before each test.
     */
    @Before
    public void resetStaticDefaults() {
        Alien.Boss.BOSS_MAX_HP        = 50;
        Alien.Boss.BOSS_SPEED_X       = 100f;
        Alien.Boss.BOSS_FIRE_INTERVAL = 2.5f;
        Alien.Boss.BOSS_BURST_COUNT   = 24;
    }

    // Constructor tests

    /**
     * Boss should start alive with full HP.
     */
    @Test
    public void constructor_bossStartsAliveAndEntering() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);
        assertTrue("Boss must start alive", boss.alive);
        assertEquals(Alien.Boss.BOSS_MAX_HP, boss.hp);
    }

    // Hit tests

    /**
     * Each hit reduces HP by one.
     */
    @Test
    public void hit_decrementsBossHP() {
        Alien.Boss boss    = new Alien.Boss(SCREEN_W);
        int        hpBefore = boss.hp;
        boss.hit();
        assertEquals(hpBefore - 1, boss.hp);
    }

    /**
     * A non-fatal hit returns false and leaves the boss alive.
     */
    @Test
    public void hit_returnsFalse_whenBossStillAlive() {
        Alien.Boss boss    = new Alien.Boss(SCREEN_W);
        boolean    killed  = boss.hit();
        assertFalse(killed);
        assertTrue(boss.alive);
    }

    /**
     * The killing blow returns true and marks the boss as dead.
     */
    @Test
    public void hit_returnsTrueAndStartsDeathSequence_whenHPReachesZero() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);

        // Drain HP down to 1 without triggering death
        for (int i = 0; i < Alien.Boss.BOSS_MAX_HP - 1; i++) {
            boss.hit();
        }

        boolean killed = boss.hit(); // brings HP to zero
        assertTrue("hit() should return true on killing blow", killed);
        assertFalse("Boss.alive must be false after death", boss.alive);
    }

    /**
     * Calling hit on a dead boss should have no effect.
     */
    @Test
    public void hit_ignoresCallsAfterDeath() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);

        // Kill the boss first
        for (int i = 0; i < Alien.Boss.BOSS_MAX_HP; i++) boss.hit();

        boolean result = boss.hit(); // extra hit on dead boss
        assertFalse("hit() on a dead boss must return false", result);
    }

    /**
     * HP should never go negative regardless of extra hits.
     */
    @Test
    public void hit_hpNeverGoesBelowZero() {
        Alien.Boss boss = new Alien.Boss(SCREEN_W);

        // Overshoot the kill count on purpose
        for (int i = 0; i < Alien.Boss.BOSS_MAX_HP + 10; i++) boss.hit();

        assertTrue("HP should not be negative", boss.hp <= 0);
    }
}

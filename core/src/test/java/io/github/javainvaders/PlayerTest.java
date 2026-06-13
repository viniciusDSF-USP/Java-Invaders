package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Unit tests for Player and its inner Player.Bullet class.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class PlayerTest {

    /**
     * Restores default speed and cooldown values before each test.
     */
    @Before
    public void resetStaticDefaults() {
        Player.SHIP_SPEED          = 320f;
        Player.SHOOT_COOLDOWN      = 0.35f;
        Player.Bullet.BULLET_SPEED = 320f;
    }

    // Constructor tests

    /**
     * Player should start with correct index, position, lives and score.
     */
    @Test
    public void constructor_setsInitialState() {
        Player p = new Player(1, 300f);

        assertEquals(1,    p.index);
        assertEquals(300f, p.x,     0.001f);
        assertEquals(60f,  p.y,     0.001f);
        assertEquals(3,    p.lives);
        assertEquals(0,    p.score);
        assertTrue("Player should start alive", p.alive);
    }

    // Rect tests

    /**
     * Hitbox should be centered on the player position.
     */
    @Test
    public void rect_returnsCorrectHitbox() {
        Player    p = new Player(1, 200f);
        Rectangle r = p.rect();

        assertEquals(200f - Player.SHIP_W / 2, r.x,      0.001f);
        assertEquals(60f  - Player.SHIP_H / 2, r.y,      0.001f);
        assertEquals(Player.SHIP_W,             r.width,  0.001f);
        assertEquals(Player.SHIP_H,             r.height, 0.001f);
    }

    // killPlayer tests

    /**
     * Killing a player should mark it dead and start respawn timers.
     */
    @Test
    public void killPlayer_marksPlayerDeadAndStartsTimers() {
        Player                p          = new Player(1, 300f);
        Array<Main.Explosion> explosions = new Array<>();

        Player.killPlayer(p, explosions);

        assertFalse("Player should be dead after killPlayer", p.alive);
        assertTrue("respawnTimer must be positive", p.respawnTimer > 0);
        assertTrue("blinkTimer must be positive",   p.blinkTimer   > 0);
        assertEquals(1, explosions.size);
    }

    /**
     * The explosion should be spawned at the player's position.
     */
    @Test
    public void killPlayer_addsExplosionAtPlayerPosition() {
        Player                p          = new Player(1, 400f);
        Array<Main.Explosion> explosions = new Array<>();
        p.y = 75f; // non-default y

        Player.killPlayer(p, explosions);

        Main.Explosion ex = explosions.first();
        assertEquals(400f, ex.x, 0.001f);
        assertEquals(75f,  ex.y, 0.001f);
    }

    // allPlayersDead tests

    /**
     * Should return true when both players are dead with no lives left.
     */
    @Test
    public void allPlayersDead_returnsTrue_whenBothDeadNoLives() {
        Player p1 = makeDeadPlayer(1);
        Player p2 = makeDeadPlayer(2);
        assertTrue(Player.allPlayersDead(p1, p2));
    }

    /**
     * Should return false when one player still has lives.
     */
    @Test
    public void allPlayersDead_returnsFalse_whenOnePlayerHasLives() {
        Player p1 = makeDeadPlayer(1);
        Player p2 = new Player(2, 500f); // still alive
        assertFalse(Player.allPlayersDead(p1, p2));
    }

    /**
     * Should return false when both players are alive.
     */
    @Test
    public void allPlayersDead_returnsFalse_whenBothAlive() {
        Player p1 = new Player(1, 300f);
        Player p2 = new Player(2, 600f);
        assertFalse(Player.allPlayersDead(p1, p2));
    }

    // Bullet constructor tests

    /**
     * Bullet should store position and owner index on construction.
     */
    @Test
    public void bullet_constructor_setsFields() {
        Player.Bullet b = new Player.Bullet(100f, 80f, 1);

        assertEquals(100f, b.x,     0.001f);
        assertEquals(80f,  b.y,     0.001f);
        assertEquals(1,    b.owner);
    }

    // Bullet rect tests

    /**
     * Bullet hitbox should be centered horizontally on its position.
     */
    @Test
    public void bullet_rect_returnsCorrectHitbox() {
        Player.Bullet b = new Player.Bullet(100f, 80f, 1);
        Rectangle     r = b.rect();

        assertEquals(100f - Player.Bullet.BULLET_W / 2, r.x,      0.001f);
        assertEquals(80f,                                r.y,      0.001f);
        assertEquals(Player.Bullet.BULLET_W,             r.width,  0.001f);
        assertEquals(Player.Bullet.BULLET_H,             r.height, 0.001f);
    }

    // Helpers

    /** Creates a player with no lives and alive set to false. */
    private Player makeDeadPlayer(int index) {
        Player p = new Player(index, 300f);
        p.alive = false;
        p.lives = 0;
        return p;
    }
}

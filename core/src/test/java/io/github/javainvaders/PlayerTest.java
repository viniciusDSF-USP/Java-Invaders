package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Unit tests for {@link Player} and its inner {@link Player.Bullet} class.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class PlayerTest {

    // setup

    @Before
    public void resetStaticDefaults() {
        Player.SHIP_SPEED     = 320f;
        Player.SHOOT_COOLDOWN = 0.35f;
        Player.Bullet.BULLET_SPEED = 320f;
    }

    // constructor

    @Test
    public void constructor_setsInitialState() {
        Player p = new Player(1, 300f);

        assertEquals(1,     p.index);
        assertEquals(300f,  p.x,      0.001f);
        assertEquals(60f,   p.y,      0.001f);
        assertEquals(3,     p.lives);
        assertEquals(0,     p.score);
        assertTrue("Player should start alive", p.alive);
    }

    // rect

    @Test
    public void rect_returnsCorrectHitbox() {
        Player p = new Player(1, 200f);
        Rectangle r = p.rect();

        assertEquals(200f - Player.SHIP_W / 2, r.x,      0.001f);
        assertEquals(60f  - Player.SHIP_H / 2, r.y,      0.001f);
        assertEquals(Player.SHIP_W,             r.width,  0.001f);
        assertEquals(Player.SHIP_H,             r.height, 0.001f);
    }

    // killPlayer

    @Test
    public void killPlayer_marksPlayerDeadAndStartsTimers() {
        Player p = new Player(1, 300f);
        Array<Main.Explosion> explosions = new Array<>();

        Player.killPlayer(p, explosions);

        assertFalse("Player should be dead after killPlayer", p.alive);
        assertTrue("respawnTimer must be positive",  p.respawnTimer > 0);
        assertTrue("blinkTimer must be positive",    p.blinkTimer   > 0);
        assertEquals(1, explosions.size);
    }

    @Test
    public void killPlayer_addsExplosionAtPlayerPosition() {
        Player p = new Player(1, 400f);
        p.y = 75f; // non-default y
        Array<Main.Explosion> explosions = new Array<>();

        Player.killPlayer(p, explosions);

        Main.Explosion ex = explosions.first();
        assertEquals(400f, ex.x, 0.001f);
        assertEquals(75f,  ex.y, 0.001f);
    }

    // allPlayersDead

    @Test
    public void allPlayersDead_returnsTrue_whenBothDeadNoLives() {
        Player p1 = makeDeadPlayer(1);
        Player p2 = makeDeadPlayer(2);

        assertTrue(Player.allPlayersDead(p1, p2));
    }

    @Test
    public void allPlayersDead_returnsFalse_whenOnePlayerHasLives() {
        Player p1 = makeDeadPlayer(1);
        Player p2 = new Player(2, 500f); // still alive

        assertFalse(Player.allPlayersDead(p1, p2));
    }

    @Test
    public void allPlayersDead_returnsFalse_whenBothAlive() {
        Player p1 = new Player(1, 300f);
        Player p2 = new Player(2, 600f);

        assertFalse(Player.allPlayersDead(p1, p2));
    }

    // Bullet constructor

    @Test
    public void bullet_constructor_setsFields() {
        Player.Bullet b = new Player.Bullet(100f, 80f, 1);

        assertEquals(100f, b.x,     0.001f);
        assertEquals(80f,  b.y,     0.001f);
        assertEquals(1,    b.owner);
    }

    // Bullet rect

    @Test
    public void bullet_rect_returnsCorrectHitbox() {
        Player.Bullet b = new Player.Bullet(100f, 80f, 1);
        Rectangle r = b.rect();

        assertEquals(100f - Player.Bullet.BULLET_W / 2, r.x,      0.001f);
        assertEquals(80f,                                r.y,      0.001f);
        assertEquals(Player.Bullet.BULLET_W,             r.width,  0.001f);
        assertEquals(Player.Bullet.BULLET_H,             r.height, 0.001f);
    }

    // helpers

    private Player makeDeadPlayer(int index) {
        Player p = new Player(index, 300f);
        p.alive = false;
        p.lives = 0;
        return p;
    }
}

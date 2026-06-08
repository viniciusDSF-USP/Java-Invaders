package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Represents a player ship in the game. Holds position, lives,
 * score and shooting state. Also handles blink logic wen the player dies.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Player {

    /** How wide the ship is, in pixels. */
    public static final float SHIP_W = 40f;

    /** How tall the ship is. */
    public static final float SHIP_H = 24f;

    /** Movement speed of the ship (pixels per second). */
    public static final float SHIP_SPEED = 320f;

    /** Min time between shots. */
    public static final float SHOOT_COOLDOWN = 0.35f;

    /** Horizontal position of the ship center. */
    public float x;

    /** Vertical position. Stays at 60 most of the time. */
    public float y;

    /** How many lives this player still has. */
    public int lives;

    /** Current score for this player. */
    public int score;

    /** Countdown before player can shoot again. */
    public float shootTimer;

    /** Wether the ship is currently on screen and active. */
    public boolean alive;

    /** Time left before the player respawns after dying. */
    public float respawnTimer;

    /** Which player this is: 1 or 2. */
    public int index;

    /** Total time the blink animation still runs for. */
    public float blinkTimer = 0f;

    /** Controls how fast the ship flickers on/off. */
    public float blinkInterval = 0f;

    /** Wether the ship is visible during a blink frame. */
    public boolean blinkVisible = true;

    /**
     * Sets up a new player at the given x position.
     *
     * @param index player number (1 or 2)
     * @param x     starting horizontal position
     */
    public Player(int index, float x) {
        this.index = index;
        this.x = x;
        this.y = 60f;
        this.lives = 3;
        this.score = 0;
        this.shootTimer = 0f;
        this.alive = true;
        this.respawnTimer = 0f;
    }

    /**
     * Returns the hitbox rectangle used for collision detection.
     *
     * @return a Rectangle centered on the ship
     */
    public Rectangle rect() {
        return new Rectangle(x - SHIP_W / 2, y - SHIP_H / 2, SHIP_W, SHIP_H);
    }

    // Static gameplay helpers

    /**
     * Marks a player as dead and kicks off the respawn/blink animation.
     * Adds an explosion at the player's position too.
     *
     * @param p          the player to kill
     * @param explosions list to push the new explosion into
     */
    public static void killPlayer(Player p, Array<Main.Explosion> explosions) {
        p.alive         = false;
        p.respawnTimer  = 2f;
        p.blinkTimer    = 2f;
        p.blinkInterval = 0f;
        p.blinkVisible  = true;
        explosions.add(new Main.Explosion(p.x, p.y));
    }

    /**
     * Returns true when both players are dead with no lives remaining.
     *
     * @param p1 player one
     * @param p2 player two
     * @return true if the game is over for both
     */
    public static boolean allPlayersDead(Player p1, Player p2) {
        return (!p1.alive && p1.lives <= 0) && (!p2.alive && p2.lives <= 0);
    }

    /**
     * Updates a single player: movement input, shoot cooldown, and respawn blink.
     * Fires a bullet into the bullets list when the shoot key is held.
     *
     * @param p                the player to update
     * @param dt               delta time
     * @param left             key code for moving left
     * @param right            key code for moving right
     * @param shoot            key code for shooting
     * @param bullets          list to push new bullets into
     * @param screenW          game screen width for clamping position
     * @param initialShootTimer grace period at level start — no shots if > 0
     */
    public static void updatePlayer(Player p, float dt, int left, int right, int shoot,
                                    Array<Bullet> bullets, int screenW, float initialShootTimer) {
        if (!p.alive) {
            p.respawnTimer -= dt;

            if (p.blinkTimer > 0) {
                p.blinkTimer    -= dt;
                p.blinkInterval -= dt;
                if (p.blinkInterval <= 0) {
                    p.blinkVisible  = !p.blinkVisible;
                    p.blinkInterval = 0.33f;
                }
            }

            if (p.respawnTimer <= 0) {
                p.lives--;
                if (p.lives > 0) {
                    p.alive = true;
                }
            }
            return;
        }

        if (Gdx.input.isKeyPressed(left))  p.x -= SHIP_SPEED * dt;
        if (Gdx.input.isKeyPressed(right)) p.x += SHIP_SPEED * dt;
        p.x = Math.max(SHIP_W / 2, Math.min(screenW - SHIP_W / 2, p.x));

        p.shootTimer -= dt;
        if (Gdx.input.isKeyPressed(shoot) && p.shootTimer <= 0 && initialShootTimer <= 0) {
            bullets.add(new Bullet(p.x, p.y + SHIP_H / 2, p.index));
            p.shootTimer = SHOOT_COOLDOWN;
        }
    }

    /**
     * Draws the ship body and wings using shape primitives.
     * Skips drawing if the player is in a blink-off frame.
     *
     * @param p      the player whose ship to draw
     * @param c      color of the ship
     * @param shapes active ShapeRenderer (already begun)
     */
    public static void drawShip(Player p, Color c, ShapeRenderer shapes) {
        // respect blink visibility during respawn
        if (!p.blinkVisible && p.blinkTimer > 0) return;

        shapes.setColor(c);
        shapes.rect(p.x - SHIP_W / 2, p.y - SHIP_H / 2, SHIP_W, SHIP_H);
        shapes.triangle(
            p.x - 8, p.y + SHIP_H / 2,
            p.x + 8, p.y + SHIP_H / 2,
            p.x,     p.y + SHIP_H / 2 + 14
        );
        shapes.rect(p.x - SHIP_W / 2 - 8, p.y - SHIP_H / 2, 8, SHIP_H / 2);
        shapes.rect(p.x + SHIP_W / 2,      p.y - SHIP_H / 2, 8, SHIP_H / 2);
    }

    /**
     * Draws small ship icons representing remaining lives.
     * Also draws a blinking icon while the player is respawning.
     *
     * @param p      player whose lives to draw
     * @param x      left edge where the icons start
     * @param y      vertical position of the icons
     * @param c      color for the icons
     * @param shapes active ShapeRenderer (already begun)
     */
    public static void drawLives(Player p, float x, float y, Color c, ShapeRenderer shapes) {
        shapes.setColor(c);
        int lives = Math.max(0, p.lives - (p.alive ? 0 : 1));
        for (int i = 0; i < lives; i++) {
            float lx = x + i * 24f;
            shapes.triangle(lx, y - 10, lx + 6, y, lx + 12, y - 10);
            shapes.rect(lx + 2, y - 14, 8, 4);
        }
        if (p.blinkTimer > 0 && p.blinkVisible) {
            float lx = x + lives * 24f;
            shapes.triangle(lx, y - 10, lx + 6, y, lx + 12, y - 10);
            shapes.rect(lx + 2, y - 14, 8, 4);
        }
    }

    // Bullet

    /**
     * A bullet fired by a player ship. Moves upward and dies when it
     * leaves the screen or hits an alien.
     */
    public static class Bullet {

        /** Width of the bullet sprite. */
        public static final float BULLET_W = 4f;

        /** Heigh of the bullet sprite. */
        public static final float BULLET_H = 16f;

        /** How fast bullets travell upward. */
        public static final float BULLET_SPEED = 320f;

        /** Horizontal center of the bullet. */
        public float x;

        /** Bottom edge of the bullet. */
        public float y;

        /** Which player shot this (1 or 2) — used for scoring. */
        public int owner;

        /**
         * Creates a bullet at the given position for the given player.
         *
         * @param x     horizontal center
         * @param y     starting y (usually top of the ship)
         * @param owner player index who fired
         */
        public Bullet(float x, float y, int owner) {
            this.x = x;
            this.y = y;
            this.owner = owner;
        }

        /**
         * Hitbox for collision checks.
         *
         * @return bounding rectangle of this bullet
         */
        public Rectangle rect() {
            return new Rectangle(x - BULLET_W / 2, y, BULLET_W, BULLET_H);
        }
    }
}

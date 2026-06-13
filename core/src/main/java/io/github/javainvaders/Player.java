package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Represents a player ship. Holds position, lives, score and shooting state.
 * Also handles the blink animation when the player dies and respawns.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Player {

    // Sprite dimensions

    /** Ship width in pixels. */
    public static final float SHIP_W = 40f;

    /** Ship height in pixels. */
    public static final float SHIP_H = 24f;

    // Configurable stats

    /** Movement speed in pixels per second. */
    public static float SHIP_SPEED = 320f;

    /** Min time between shots in seconds. */
    public static float SHOOT_COOLDOWN = 0.35f;

    // Identity

    /** Which player this is: 1 or 2. */
    public int index;

    // Position

    /** Horizontal center of the ship. */
    public float x;

    /** Vertical center — stays near 60 most of the time. */
    public float y;

    // State

    /** Whether the ship is currently active on screen. */
    public boolean alive;

    /** How many lives this player still has. */
    public int lives;

    /** Current score. */
    public int score;

    // Timers

    /** Countdown before the player can shoot again. */
    public float shootTimer;

    /** Countdown before the player respawns after dying. */
    public float respawnTimer;

    // Blink animation

    /** Total time the blink animation still runs for. */
    public float blinkTimer = 0f;

    /** Controls how fast the ship flickers on and off. */
    public float blinkInterval = 0f;

    /** Whether the ship is visible on the current blink frame. */
    public boolean blinkVisible = true;

    /**
     * Creates a new player at the given x position.
     *
     * @param index player number (1 or 2)
     * @param x     starting horizontal center
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
     * Loads player config values from JSON data.
     *
     * @param config the JSON section containing player settings
     */
    public static void loadConfig(JsonValue config) {
        if (config.has("SHIP_SPEED"))     SHIP_SPEED     = config.getFloat("SHIP_SPEED");
        if (config.has("SHOOT_COOLDOWN")) SHOOT_COOLDOWN = config.getFloat("SHOOT_COOLDOWN");
    }

    /**
     * Returns the hitbox used for collision detection.
     *
     * @return a rectangle centered on the ship
     */
    public Rectangle rect() {
        return new Rectangle(x - SHIP_W / 2, y - SHIP_H / 2, SHIP_W, SHIP_H);
    }

    // Static gameplay helpers

    /**
     * Marks a player as dead and starts the respawn blink animation.
     * Also pushes an explosion at the player's position.
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
        SoundManager.get().playPlayerHit();
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
     * Updates a single player: handles movement input, shoot cooldown and
     * respawn blink. Fires a bullet when the shoot key is held.
     *
     * @param p                 the player to update
     * @param dt                delta time in seconds
     * @param left              key code for moving left
     * @param right             key code for moving right
     * @param shoot             key code for shooting
     * @param bullets           list to push new bullets into
     * @param screenW           screen width used to clamp position
     * @param initialShootTimer grace period at level start — no shots if > 0
     */
    public static void updatePlayer(Player p, float dt, int left, int right, int shoot,
                                    Array<Bullet> bullets, int screenW, float initialShootTimer) {
        if (!p.alive) {
            p.respawnTimer -= dt;

            // Blink while waiting to respawn
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
                if (p.lives > 0) p.alive = true;
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
            SoundManager.get().playShoot();
        }
    }

    /**
     * Draws the ship body and wings using shape primitives.
     * Skips drawing on blink-off frames.
     *
     * @param p      the player to draw
     * @param c      ship color
     * @param shapes active ShapeRenderer (already begun)
     */
    public static void drawShip(Player p, Color c, ShapeRenderer shapes) {
        // Skip on blink-off frames during respawn
        if (!p.blinkVisible && p.blinkTimer > 0) return;

        shapes.setColor(c);
        shapes.rect(p.x - SHIP_W / 2, p.y - SHIP_H / 2, SHIP_W, SHIP_H);
        shapes.triangle(
            p.x - 8, p.y + SHIP_H / 2,
            p.x + 8, p.y + SHIP_H / 2,
            p.x,     p.y + SHIP_H / 2 + 14
        );
        // Side wings
        shapes.rect(p.x - SHIP_W / 2 - 8, p.y - SHIP_H / 2, 8, SHIP_H / 2);
        shapes.rect(p.x + SHIP_W / 2,      p.y - SHIP_H / 2, 8, SHIP_H / 2);
    }

    /**
     * Draws small ship icons for each remaining life. Also shows a blinking
     * icon while the player is waiting to respawn.
     *
     * @param p      player whose lives to draw
     * @param x      left edge where icons start
     * @param y      vertical position of the icons
     * @param c      icon color
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
        // Blinking icon slot while respawning
        if (p.blinkTimer > 0 && p.blinkVisible) {
            float lx = x + lives * 24f;
            shapes.triangle(lx, y - 10, lx + 6, y, lx + 12, y - 10);
            shapes.rect(lx + 2, y - 14, 8, 4);
        }
    }

    // Bullet

    /**
     * A bullet fired by a player ship. Moves upward and is removed when
     * it leaves the screen or hits an alien.
     *
     * @author Larissa R. G.; Vinicius S. F.
     */
    public static class Bullet {

        // Sprite dimensions

        /** Bullet width in pixels. */
        public static final float BULLET_W = 4f;

        /** Bullet height in pixels. */
        public static final float BULLET_H = 16f;

        // Configurable stats

        /** Upward travel speed in pixels per second. */
        public static float BULLET_SPEED = 320f;

        // Instance fields

        /** Horizontal center of the bullet. */
        public float x;

        /** Bottom edge of the bullet. */
        public float y;

        /** Player index who fired this bullet — used for scoring. */
        public int owner;

        /**
         * Creates a bullet at the given position for the given player.
         *
         * @param x     horizontal center
         * @param y     starting y (usually the top of the ship)
         * @param owner player index who fired
         */
        public Bullet(float x, float y, int owner) {
            this.x = x;
            this.y = y;
            this.owner = owner;
        }

        /**
         * Loads bullet config values from JSON data.
         *
         * @param config the JSON section containing bullet settings
         */
        public static void loadConfig(JsonValue config) {
            if (config.has("BULLET_SPEED")) BULLET_SPEED = config.getFloat("BULLET_SPEED");
        }

        /**
         * Returns the hitbox for collision checks.
         *
         * @return bounding rectangle of this bullet
         */
        public Rectangle rect() {
            return new Rectangle(x - BULLET_W / 2, y, BULLET_W, BULLET_H);
        }
    }
}

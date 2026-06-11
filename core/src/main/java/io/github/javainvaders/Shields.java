package io.github.javainvaders;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Protective shields placed between players and the alien grid.
 * Each shield absorbs up to 5 bomb hits before breaking; aliens that
 * collide with a shield destroy it instantly. When a shield breaks it
 * blinks 3 times then disappears.
 *
 * Color transitions from WHITE (full health) to RED (1 hit left) as the
 * shield takes damage.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Shields {

    // ------------------------------------------------------------------ sizes

    /** Width of a shield block. */
    public static final float SHIELD_W = 80f;

    /** Height of a shield block. */
    public static final float SHIELD_H = 24f;

    /** Y position (bottom edge) of all shields. */
    public static final float SHIELD_Y = 150f;

    /** How many bomb hits a shield can absorb before breaking. */
    public static final int MAX_HITS = 5;

    // ------------------------------------------------------------------ state

    /** Horizontal center of this shield. */
    public float x;

    /** Vertical center of this shield. */
    public float y;

    /** Number of hits already received (0 = pristine, MAX_HITS = destroyed). */
    public int hits;

    /** Whether this shield is still active (not yet fully removed). */
    public boolean alive;

    /**
     * When > 0 the shield is in its break-blink animation.
     * Counts down to 0, then the shield is removed.
     */
    public float breakTimer;

    /**
     * Controls the on/off cadence of the blink animation.
     * Decrements each frame; flips visibility when it hits 0.
     */
    public float blinkInterval;

    /** Whether the shield is currently visible during a blink frame. */
    public boolean blinkVisible;

    // Total duration of the break animation (3 blinks × 2 states × ~0.1 s).
    private static final float BREAK_DURATION = 0.6f;

    // ------------------------------------------------------------------ ctor

    /**
     * Creates a shield centered at (x, y).
     *
     * @param x horizontal center
     * @param y vertical center
     */
    public Shields(float x, float y) {
        this.x            = x;
        this.y            = y;
        this.hits         = 0;
        this.alive        = true;
        this.breakTimer   = 0f;
        this.blinkInterval = 0f;
        this.blinkVisible  = true;
    }

    // ------------------------------------------------------------------ rect

    /**
     * Hitbox used for collision checks against bombs and aliens.
     *
     * @return bounding rectangle centered on this shield
     */
    public Rectangle rect() {
        return new Rectangle(x - SHIELD_W / 2, y - SHIELD_H / 2, SHIELD_W, SHIELD_H);
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Returns true when this shield is completely gone (no longer rendered
     * or used in collision checks).
     *
     * @return true if the shield should be removed from the list
     */
    public boolean isGone() {
        return !alive && breakTimer <= 0f;
    }

    // ------------------------------------------------------------------ static API

    /**
     * Builds the shield list for the given level.
     * Level 1 → 3 shields, level 2 → 2 shields, level 3 → 1 shield.
     *
     * @param level current level number
     * @param screenW game screen width, used to space shields evenly
     * @return a new Array populated with the correct shields
     */
    public static Array<Shields> createForLevel(int level, int screenW) {
        Array<Shields> list = new Array<>();
        int count = Math.max(0, 4 - level); // 3, 2, 1 for levels 1-3
        if (count == 0) return list;

        float spacing = screenW / (count + 1f);
        for (int i = 1; i <= count; i++) {
            list.add(new Shields(spacing * i, SHIELD_Y));
        }
        return list;
    }

    /**
     * Registers a bomb hit on a shield. Increments the hit counter and
     * starts the break animation once the shield reaches MAX_HITS.
     *
     * @param s the shield that was hit
     */
    public static void hitByBomb(Shields s) {
        if (!s.alive || s.breakTimer > 0f) return;
        s.hits++;
        if (s.hits >= MAX_HITS) {
            startBreak(s);
        }
    }

    /**
     * Destroys a shield immediately (e.g. when an alien collides with it).
     *
     * @param s the shield to destroy
     */
    public static void destroyInstantly(Shields s) {
        if (!s.alive || s.breakTimer > 0f) return;
        s.hits = MAX_HITS;
        startBreak(s);
    }

    /** Kicks off the blink-then-remove animation. */
    private static void startBreak(Shields s) {
        s.alive        = false;
        s.breakTimer   = BREAK_DURATION;
        s.blinkInterval = 0.1f;
        s.blinkVisible  = true;
    }

    /**
     * Ticks the break animation for every shield in the list and removes
     * fully expired shields.
     *
     * @param shields list of shields to update
     * @param dt      delta time in seconds
     */
    public static void update(Array<Shields> shields, float dt) {
        for (Shields s : shields) {
            if (s.breakTimer > 0f) {
                s.breakTimer   -= dt;
                s.blinkInterval -= dt;
                if (s.blinkInterval <= 0f) {
                    s.blinkVisible  = !s.blinkVisible;
                    s.blinkInterval = 0.1f;
                }
            }
        }
        // Remove fully expired shields
        for (int i = shields.size - 1; i >= 0; i--) {
            if (shields.get(i).isGone()) shields.removeIndex(i);
        }
    }

    /**
     * Draws all shields. Color interpolates from WHITE (0 hits) to RED
     * (MAX_HITS - 1 hits). Blink animation is respected during break.
     *
     * @param shields list of shields to draw
     * @param shapes  active ShapeRenderer (already begun)
     */
    public static void draw(Array<Shields> shields, ShapeRenderer shapes) {
        for (Shields s : shields) {
            // During break animation, respect blink visibility
            if (!s.alive) {
                if (!s.blinkVisible) continue;
                shapes.setColor(Color.RED);
            } else {
                // Lerp: 0 hits = WHITE, MAX_HITS-1 hits = RED
                float t = (float) s.hits / (MAX_HITS - 1);
                float r = 1f;
                float g = 1f - t;
                float b = 1f - t;
                shapes.setColor(r, g, b, 1f);
            }
            shapes.rect(s.x - SHIELD_W / 2, s.y - SHIELD_H / 2, SHIELD_W, SHIELD_H);

            // Draw a simple "crack" overlay to hint at damage level
            if (s.alive && s.hits > 0) {
                shapes.setColor(0f, 0f, 0f, 0.35f * s.hits);
                float segW = SHIELD_W / MAX_HITS;
                for (int i = 0; i < s.hits; i++) {
                    shapes.rect(
                        s.x - SHIELD_W / 2 + i * segW + 2,
                        s.y - SHIELD_H / 2 + 4,
                        segW - 4,
                        SHIELD_H - 8
                    );
                }
            }
        }
    }
}

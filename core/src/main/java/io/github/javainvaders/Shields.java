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
 * Color transitions from white (full health) to red (1 hit left) as
 * the shield takes damage.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Shields {

    // Sprite dimensions and placement

    /** Width of a shield block. */
    public static final float SHIELD_W = 80f;

    /** Height of a shield block. */
    public static final float SHIELD_H = 24f;

    /** Y position (bottom edge) shared by all shields. */
    public static final float SHIELD_Y = 150f;

    // Hit limit

    /** How many bomb hits a shield can absorb before breaking. */
    public static final int MAX_HITS = 5;

    // Break animation duration

    // Total duration of the break animation (3 blinks x 2 states x ~0.1 s)
    private static final float BREAK_DURATION = 0.6f;

    // Instance fields — position

    /** Horizontal center of this shield. */
    public float x;

    /** Vertical center of this shield. */
    public float y;

    // Instance fields — state

    /** Whether this shield is still active. */
    public boolean alive;

    /** Number of hits already received (0 = pristine, MAX_HITS = destroyed). */
    public int hits;

    // Instance fields — break animation

    /**
     * When > 0 the shield is in its break-blink animation.
     * Counts down to 0, then the shield is removed.
     */
    public float breakTimer;

    /** Controls the on/off cadence of the blink — flips visibility when it hits 0. */
    public float blinkInterval;

    /** Whether the shield is visible on the current blink frame. */
    public boolean blinkVisible;

    /**
     * Creates a shield centered at (x, y).
     *
     * @param x horizontal center
     * @param y vertical center
     */
    public Shields(float x, float y) {
        this.x             = x;
        this.y             = y;
        this.hits          = 0;
        this.alive         = true;
        this.breakTimer    = 0f;
        this.blinkInterval = 0f;
        this.blinkVisible  = true;
    }

    /**
     * Returns the hitbox for collision checks against bombs and aliens.
     *
     * @return bounding rectangle centered on this shield
     */
    public Rectangle rect() {
        return new Rectangle(x - SHIELD_W / 2, y - SHIELD_H / 2, SHIELD_W, SHIELD_H);
    }

    /**
     * Returns true when the shield is fully gone and can be removed from the list.
     *
     * @return true if no longer rendered or collidable
     */
    public boolean isGone() {
        return !alive && breakTimer <= 0f;
    }

    // Static API

    /**
     * Builds the shield list for the given level.
     * Level 1 gives 3 shields, level 2 gives 2, level 3 gives 1.
     *
     * @param level   current level number
     * @param screenW screen width used to space shields evenly
     * @return new array populated with the right shields
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
     * starts the break animation once MAX_HITS is reached.
     *
     * @param s the shield that was hit
     */
    public static void hitByBomb(Shields s) {
        if (!s.alive || s.breakTimer > 0f) return;
        s.hits++;
        if (s.hits >= MAX_HITS) {
            startBreak(s);
            SoundManager.get().playShieldBreak();
        } else {
            SoundManager.get().playShieldHit();
        }
    }

    /**
     * Destroys a shield instantly — used when an alien walks into it.
     *
     * @param s the shield to destroy
     */
    public static void destroyInstantly(Shields s) {
        if (!s.alive || s.breakTimer > 0f) return;
        s.hits = MAX_HITS;
        startBreak(s);
        SoundManager.get().playShieldBreak();
    }

    /** Starts the blink-then-remove animation. */
    private static void startBreak(Shields s) {
        s.alive        = false;
        s.breakTimer   = BREAK_DURATION;
        s.blinkInterval = 0.1f;
        s.blinkVisible  = true;
    }

    /**
     * Ticks the break animation for every shield and removes fully expired ones.
     *
     * @param shields list of shields to update
     * @param dt      delta time in seconds
     */
    public static void update(Array<Shields> shields, float dt) {
        for (Shields s : shields) {
            if (s.breakTimer > 0f) {
                s.breakTimer    -= dt;
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
     * Draws all shields. Color lerps from white (0 hits) to red (MAX_HITS - 1).
     * Respects the blink visibility during the break animation.
     *
     * @param shields list of shields to draw
     * @param shapes  active ShapeRenderer (already begun)
     */
    public static void draw(Array<Shields> shields, ShapeRenderer shapes) {
        for (Shields s : shields) {
            if (!s.alive) {
                // Skip on blink-off frames
                if (!s.blinkVisible) continue;
                shapes.setColor(Color.RED);
            } else {
                // Lerp from white to red as damage accumulates
                float t = (float) s.hits / (MAX_HITS - 1);
                shapes.setColor(1f, 1f - t, 1f - t, 1f);
            }
            shapes.rect(s.x - SHIELD_W / 2, s.y - SHIELD_H / 2, SHIELD_W, SHIELD_H);

            // Crack overlay darkens damaged segments
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

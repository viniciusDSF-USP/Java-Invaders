package io.github.javainvaders;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Represents one alien enemy on the grid. Tracks position, type
 * and whether its still alive. Also holds the Bomb and Boss inner classes.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Alien {

    // Grid constants

    /** Number of columns in the alien grid. */
    public static final int ALIEN_COLS = 11;

    /** Number of rows in the alien grid. */
    public static final int ALIEN_ROWS = 5;

    // Sprite dimensions

    /** Width of the alien sprite box. */
    public static final float ALIEN_W = 36f;

    /** Height of the alien sprite box. */
    public static final float ALIEN_H = 24f;

    // Movement

    /** Horizontal pixels moved per movement tick. */
    public static float ALIEN_STEP = 12f;

    /** Pixels droped down when aliens reach the edge. */
    public static float ALIEN_DROP = 18f;

    // Instance fields

    /** Horizontal center of this alien. */
    public float x;

    /** Vertical center of this alien. */
    public float y;

    /**
     * Visual and point type: 0 = basic, 1 = medium, 2 = tough.
     * Higher type means more points and fancier sprite details.
     */
    public int type;

    /** Whether this alien is still in play. */
    public boolean alive;

    /**
     * Creates an alien at the given position with the given type.
     *
     * @param x    horizontal center
     * @param y    vertical center
     * @param type 0, 1, or 2 - controls appearance and score value
     */
    public Alien(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.alive = true;
    }

    /**
     * Loads alien config values from JSON data.
     *
     * @param config the JSON section containing alien settings
     */
    public static void loadConfig(JsonValue config) {
        if (config.has("ALIEN_STEP")) ALIEN_STEP = config.getFloat("ALIEN_STEP");
        if (config.has("ALIEN_DROP")) ALIEN_DROP = config.getFloat("ALIEN_DROP");
    }

    /**
     * Returns the hitbox for collision checks against bullets or the player.
     *
     * @return bounding rectangle centered on this alien
     */
    public Rectangle rect() {
        return new Rectangle(x - ALIEN_W / 2, y - ALIEN_H / 2, ALIEN_W, ALIEN_H);
    }

    // Static helpers

    /**
     * Returns true when every alien in the list is dead.
     *
     * @param aliens the current alien list
     * @return true if none are alive
     */
    public static boolean allAliensDead(Array<Alien> aliens) {
        for (Alien a : aliens) if (a.alive) return false;
        return true;
    }

    /**
     * Moves every alive alien one step sideways, or drops them down if
     * alienDescend is true. Flips direction after a descent.
     * Updates alienDescend and alienDirX on the state object.
     *
     * @param aliens  the alien list
     * @param state   mutable movement state (dir, descend flag)
     * @param level   current level - higher levels move an extra bit
     * @param screenW game screen width used for edge detection
     */
    public static void moveAliens(Array<Alien> aliens, AlienMoveState state, int level, int screenW) {
        if (state.alienDescend) {
            for (Alien a : aliens) if (a.alive) a.y -= ALIEN_DROP;
            state.alienDescend = false;
            state.alienDirX = -state.alienDirX;
            return;
        }

        float step = ALIEN_STEP * state.alienDirX;
        float minX = screenW, maxX = 0;
        for (Alien a : aliens) {
            if (!a.alive) continue;
            a.x += step;
            minX = Math.min(minX, a.x);
            maxX = Math.max(maxX, a.x);
        }

        // Extra speed boost starting from level 2
        if (level >= 2) {
            float extraStep = (level - 1) * 3f * state.alienDirX;
            for (Alien a : aliens) if (a.alive) a.x += extraStep;
            maxX += (level - 1) * 3f * Math.signum(state.alienDirX);
            minX += (level - 1) * 3f * Math.signum(state.alienDirX);
        }

        if (state.alienDirX > 0 && maxX + ALIEN_W / 2 >= screenW - 20) state.alienDescend = true;
        if (state.alienDirX < 0 && minX - ALIEN_W / 2 <= 20)           state.alienDescend = true;
    }

    /**
     * Draws an alien using shape primitives. Higher types get extra
     * decorative bits like antennas and a top knob.
     *
     * @param a      the alien to draw
     * @param shapes active ShapeRenderer (already begun)
     */
    public static void drawAlien(Alien a, ShapeRenderer shapes) {
        // Pick color by type
        Color c = a.type == 2 ? Color.MAGENTA : a.type == 1 ? Color.ORANGE : Color.WHITE;
        shapes.setColor(c);

        float hw = ALIEN_W / 2, hh = ALIEN_H / 2;
        shapes.rect(a.x - hw + 4, a.y - hh, ALIEN_W - 8, ALIEN_H);
        shapes.rect(a.x - hw, a.y - hh + 6, ALIEN_W, ALIEN_H - 12);
        if (a.type >= 1) {
            // Side antenna blobs for medium and tough types
            shapes.rect(a.x - hw - 6, a.y, 6, 6);
            shapes.rect(a.x + hw,     a.y, 6, 6);
        }
        if (a.type == 2) {
            // Top knob only on the toughest type
            shapes.rect(a.x - 4, a.y + hh, 8, 8);
        }
        shapes.rect(a.x - 10, a.y - hh - 6, 6, 6);
        shapes.rect(a.x +  4, a.y - hh - 6, 6, 6);
    }

    // Boss

    /**
     * The level-3 boss. Appears after all regular aliens are dead,
     * descends from the top, then drifts sideways and fires radial
     * bursts of bombs every few seconds. Takes several hits to kill
     * and flashes briefly on each hit.
     *
     * @author Larissa R. G.; Vinicius S. F.
     */
    public static class Boss {

        // Sprite dimensions

        /** Width of the boss sprite - roughly 13.5x a normal alien. */
        public static final float BOSS_W = 36f * 13.5f;

        /** Height of the boss sprite. */
        public static final float BOSS_H = 24f * 13.5f;

        // Scoring

        /** Points awarded for killing the boss. */
        public static final int BOSS_SCORE = 1000;

        // Configurable stats

        /** How many bullets it takes to destroy the boss. */
        public static int BOSS_MAX_HP = 50;

        /** Horizontal drift speed once it reaches its target Y. */
        public static float BOSS_SPEED_X = 100f;

        /** Seconds between radial bomb bursts. */
        public static float BOSS_FIRE_INTERVAL = 2.5f;

        /** Number of bombs in each radial burst. */
        public static int BOSS_BURST_COUNT = 24;

        // Movement constants

        /** Downward entry speed from the top of the screen. */
        public static final float BOSS_ENTRY_SPEED = 250f;

        /** Y position where the boss stops descending. */
        public static final float BOSS_TARGET_Y = Main.H - BOSS_H * 0.65f;

        // Bob animation

        /** Amplitude of the vertical bob in pixels. */
        public static final float BOSS_BOB_AMP = 18f;

        /** Full bob cycles per second. */
        public static final float BOSS_BOB_SPEED = 0.8f;

        // Death sequence

        /** How many times the boss flashes during its death sequence. */
        public static final int BOSS_DEATH_FLASHES = 5;

        /** Duration of each flash half-cycle in seconds. */
        public static final float BOSS_DEATH_FLASH_INTERVAL = 0.12f;

        /** Seconds the boss holds as a white silhouette before the final explosion. */
        public static final float BOSS_DEATH_FREEZE_DURATION = 2f;

        // Instance fields - position and health

        /** Horizontal center of the boss. */
        public float x;

        /** Vertical center of the boss. */
        public float y;

        /** Remaining hit points. */
        public int hp;

        /** Whether the boss is still alive. */
        public boolean alive;

        // Movement state

        /** True while the boss is still entering from the top. */
        public boolean entering;

        /** Current horizontal movement direction, +1 or -1. */
        public float dirX;

        // Fire state

        /** Countdown to the next bomb burst. */
        public float fireTimer;

        // Visual timers

        /** Timer for the hit-flash effect. */
        public float hitFlashTimer;

        /** Accumulated time used to drive the sine-wave vertical bob. */
        public float bobTimer;

        /** Vertical offset on top of BOSS_TARGET_Y - oscillates via sine. */
        public float bobOffset;

        // Death sequence state

        /**
         * True once the boss reaches zero HP and the death sequence begins.
         * While dying is true, alive is false but the boss is still rendered
         * and updated (flashing, shaking, freeze, final explosion).
         */
        public boolean dying;

        /** How many flash half-cycles remain in the death sequence. */
        public int deathFlashesLeft;

        /** Countdown for each individual flash half-cycle. */
        public float deathFlashTimer;

        /** Whether the boss sprite is visible during a death-flash off-frame. */
        public boolean deathVisible;

        /** Horizontal shake offset applied while dying. */
        public float shakeOffsetX;

        /**
         * True when all flashes are done and the boss is held as a solid white
         * silhouette for BOSS_DEATH_FREEZE_DURATION before the final explosion.
         */
        public boolean deathFrozen;

        /** Countdown for the frozen-silhouette phase. */
        public float deathFreezeTimer;

        /**
         * True once the death sequence is fully over and the game can award
         * the score, spawn the explosion and proceed to YOU WIN.
         */
        public boolean deathSequenceDone;

        /** Shared RNG instance used to vary the burst count each fire cycle. */
        private static final Random RNG = new Random();

        /**
         * Creates a boss that starts above the visible screen and
         * descends to its patrol height.
         *
         * @param screenW game screen width, used to center the boss
         */
        public Boss(int screenW) {
            this.x          = screenW / 2f;
            // Start just above the top edge so it slides in
            this.y          = Main.H + BOSS_H / 2 + 10f;
            this.hp         = BOSS_MAX_HP;
            this.alive      = true;
            this.entering   = true;
            this.dirX       = 1f;
            this.fireTimer  = BOSS_FIRE_INTERVAL;
            this.hitFlashTimer = 0f;
            this.bobTimer   = 0f;
            this.bobOffset  = 0f;
            this.dying      = false;
            this.deathFlashesLeft  = BOSS_DEATH_FLASHES * 2; // on+off pairs
            this.deathFlashTimer   = 0f;
            this.deathVisible      = true;
            this.shakeOffsetX      = 0f;
            this.deathFrozen       = false;
            this.deathFreezeTimer  = 0f;
            this.deathSequenceDone = false;
        }

        /**
         * Loads boss config values from JSON data.
         *
         * @param config the JSON section containing boss settings
         */
        public static void loadConfig(JsonValue config) {
            if (config.has("BOSS_MAX_HP"))         BOSS_MAX_HP        = config.getInt("BOSS_MAX_HP");
            if (config.has("BOSS_SPEED_X"))        BOSS_SPEED_X       = config.getFloat("BOSS_SPEED_X");
            if (config.has("BOSS_FIRE_INTERVAL"))  BOSS_FIRE_INTERVAL = config.getFloat("BOSS_FIRE_INTERVAL");
            if (config.has("BOSS_BURST_COUNT"))    BOSS_BURST_COUNT   = config.getInt("BOSS_BURST_COUNT");
        }

        /**
         * Returns the hitbox for collision checks against player bullets.
         *
         * @return bounding rectangle centered on this boss
         */
        public Rectangle rect() {
            return new Rectangle(x - BOSS_W / 2, y - BOSS_H / 2, BOSS_W, BOSS_H);
        }

        /**
         * Updates boss movement, entry descent and the fire timer.
         * Also drives the vertical bob, the death-sequence animation and
         * pushes new bombs into the provided list when it fires.
         *
         * @param dt      delta time in seconds
         * @param bombs   bomb list to append radial burst into
         * @param screenW game screen width for wall bounce
         */
        public void update(float dt, Array<Bomb> bombs, int screenW) {
            // Death sequence - runs even though alive == false
            if (dying) {
                if (deathFrozen) {
                    // Hold white silhouette, then signal the game to finish
                    deathFreezeTimer -= dt;
                    if (deathFreezeTimer <= 0f) {
                        deathSequenceDone = true;
                        dying = false;
                    }
                    return;
                }

                // Flash on/off while shaking horizontally
                deathFlashTimer -= dt;
                if (deathFlashTimer <= 0f) {
                    deathVisible = !deathVisible;
                    deathFlashTimer = BOSS_DEATH_FLASH_INTERVAL;
                    deathFlashesLeft--;

                    // Random shake direction each half-cycle
                    shakeOffsetX = (RNG.nextFloat() - 0.5f) * 300f;

                    if (deathFlashesLeft <= 0) {
                        // All flashes done - freeze as solid white
                        deathVisible      = true;
                        deathFrozen       = true;
                        deathFreezeTimer  = BOSS_DEATH_FREEZE_DURATION;
                        shakeOffsetX      = 0f;
                    }
                }
                return;
            }

            if (!alive) return;

            if (hitFlashTimer > 0) hitFlashTimer -= dt;

            if (entering) {
                y -= BOSS_ENTRY_SPEED * dt;
                if (y <= BOSS_TARGET_Y) {
                    y        = BOSS_TARGET_Y;
                    entering = false;
                }
                return; // Dont shoot or drift sideways while entering
            }

            // Horizontal drift - bounce off walls
            x += BOSS_SPEED_X * dirX * dt;
            if (x + BOSS_W / 2 >= screenW - 20) { x = screenW - 20 - BOSS_W / 2; dirX = -1f; }
            if (x - BOSS_W / 2 <= 20)            { x = 20 + BOSS_W / 2;            dirX =  1f; }

            // Vertical bob - sine wave on top of the patrol height
            bobTimer += dt;
            bobOffset = (float) Math.sin(bobTimer * BOSS_BOB_SPEED * 2 * Math.PI) * BOSS_BOB_AMP;

            // Radial burst fire - count varies by +-{0,1,2} to break safe zones
            fireTimer -= dt;
            if (fireTimer <= 0f) {
                fireTimer = BOSS_FIRE_INTERVAL;
                fireBurst(bombs);
            }
        }

        // Fires BOSS_BURST_COUNT + random[0,5] bombs spread evenly around a full circle.
        // The varying count rotates the gap position each volley so there are no permanent safe zones.
        private void fireBurst(Array<Bomb> bombs) {
            int count = BOSS_BURST_COUNT + RNG.nextInt(6); // [0, 5] extra bombs
            float step = (float)(2 * Math.PI) / count;
            for (int i = 0; i < count; i++) {
                float angle = i * step;
                RadialBomb rb = new RadialBomb(x, y + bobOffset, angle);
                bombs.add(rb);
            }
        }

        /**
         * Registers a bullet hit. Decrements HP and triggers the hit flash.
         * When HP reaches zero, starts the death sequence (flashes, shake and
         * freeze) instead of removing the boss immediately. Call
         * {@link #deathSequenceDone} to know when the game can proceed.
         *
         * @return true if this hit reduced HP to zero (death sequence started)
         */
        public boolean hit() {
            if (!alive || dying) return false;
            hp--;
            hitFlashTimer = 0.12f;
            if (hp <= 0) {
                alive             = false;
                dying             = true;
                deathFlashesLeft  = BOSS_DEATH_FLASHES * 2; // on + off for each flash
                deathFlashTimer   = BOSS_DEATH_FLASH_INTERVAL;
                deathVisible      = true;
                shakeOffsetX      = 0f;
                deathFrozen       = false;
                deathFreezeTimer  = 0f;
                deathSequenceDone = false;
                return true;
            }
            return false;
        }

        /**
         * Draws the boss using shape primitives. Behavior differs by phase:
         * normal patrol is gold/yellow with bob offset and flashes white on hit;
         * death sequence alternates visible/invisible while shaking sideways;
         * death freeze shows a solid white silhouette with no shake.
         *
         * @param shapes active ShapeRenderer (already begun)
         */
        public void draw(ShapeRenderer shapes) {
            // Only render during active life or death sequence
            if (!alive && !dying) return;

            // During death flashes, respect the on/off visibility flag
            if (dying && !deathFrozen && !deathVisible) return;

            // Color: white during any death phase or hit-flash, gold otherwise
            if (dying) {
                shapes.setColor(Color.WHITE);
            } else if (hitFlashTimer > 0) {
                shapes.setColor(Color.WHITE);
            } else {
                shapes.setColor(Color.YELLOW);
            }

            // Apply vertical bob during patrol; shake sideways while dying
            float drawX = x + (dying ? shakeOffsetX : 0f);
            float drawY = y + ((!dying && !entering) ? bobOffset : 0f);

            float hw = BOSS_W / 2, hh = BOSS_H / 2;

            // Main body
            shapes.rect(drawX - hw + 10, drawY - hh,       BOSS_W - 20, BOSS_H);
            shapes.rect(drawX - hw,      drawY - hh + 18,  BOSS_W,      BOSS_H - 36);

            // Side antenna blobs
            shapes.rect(drawX - hw - 14, drawY,      14, 14);
            shapes.rect(drawX + hw,      drawY,      14, 14);

            // Top knob
            shapes.rect(drawX - 10, drawY + hh,      20, 20);

            // Bottom legs
            shapes.rect(drawX - 28, drawY - hh - 14, 14, 14);
            shapes.rect(drawX - 6,  drawY - hh - 14, 14, 14);
            shapes.rect(drawX + 14, drawY - hh - 14, 14, 14);

            // HP bar - hidden during death sequence
            if (!dying) {
                // Background (dark red)
                shapes.setColor(0.35f, 0f, 0f, 1f);
                shapes.rect(drawX - hw, drawY + hh + 28, BOSS_W, 8);

                // Fill (bright red, shrinks as damage accumulates)
                float hpFrac = (float) hp / BOSS_MAX_HP;
                shapes.setColor(Color.RED);
                shapes.rect(drawX - hw, drawY + hh + 28, BOSS_W * hpFrac, 8);
            }
        }
    }

    // RadialBomb

    /**
     * A bomb fired by the boss that travels outward at a fixed angle
     * instead of straight down. Rotates visually as it moves.
     *
     * @author Larissa R. G.; Vinicius S. F.
     */
    public static class RadialBomb extends Bomb {

        /** Travel speed in pixels per second. */
        public static float RADIAL_SPEED = 500f;

        /** Normalised X component of the travel direction. */
        public final float vx;

        /** Normalised Y component of the travel direction. */
        public final float vy;

        /** Accumulated rotation angle for the spin visual. */
        public float angle;

        /**
         * Creates a radial bomb fired from (x, y) in the given direction.
         *
         * @param x     origin X (boss center)
         * @param y     origin Y (boss center)
         * @param angle direction in radians (0 = right, PI/2 = up)
         */
        public RadialBomb(float x, float y, float angle) {
            super(x, y);
            this.vx    = (float) Math.cos(angle);
            this.vy    = (float) Math.sin(angle);
            this.angle = angle;
        }

        /**
         * Loads radial bomb config values from JSON data.
         *
         * @param config the JSON section containing radial bomb settings
         */
        public static void loadConfig(JsonValue config) {
            if (config.has("RADIAL_SPEED")) RADIAL_SPEED = config.getFloat("RADIAL_SPEED");
        }
    }

    // Bomb

    /**
     * A bomb droped by an alien. Falls straight down and kills the
     * player on contact.
     *
     * @author Larissa R. G.; Vinicius S. F.
     */
    public static class Bomb {

        // Sprite dimensions

        /** Width of the bomb sprite. */
        public static final float BOMB_W = 6f;

        /** Height of the bomb sprite. */
        public static final float BOMB_H = 14f;

        // Movement

        /** How fast bombs fall downward in pixels per second. */
        public static float BOMB_SPEED = 220f;

        // Instance fields

        /** Horizontal center of the bomb. */
        public float x;

        /** Bottom edge of the bomb. */
        public float y;

        /**
         * Creates a bomb at the given position (usually the bottom of an alien).
         *
         * @param x horizontal center
         * @param y starting Y position
         */
        public Bomb(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Loads bomb config values from JSON data.
         *
         * @param config the JSON section containing bomb settings
         */
        public static void loadConfig(JsonValue config) {
            if (config.has("BOMB_SPEED")) BOMB_SPEED = config.getFloat("BOMB_SPEED");
        }

        /**
         * Returns the hitbox for collision checks against player ships.
         *
         * @return bounding rectangle of this bomb
         */
        public Rectangle rect() {
            return new Rectangle(x - BOMB_W / 2, y, BOMB_W, BOMB_H);
        }
    }
}

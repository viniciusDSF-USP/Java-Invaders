package io.github.javainvaders;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

/**
 * Represents one alien enemy on the grid. Tracks position, type
 * and wether its still alive. Also contains the Bomb class that
 * aliens drop on players.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Alien {

    /** Width of the alien sprite box. */
    public static final float ALIEN_W = 36f;

    /** Height of the alien sprite box. */
    public static final float ALIEN_H = 24f;

    /** Horizontal pixels moved per movement tick. */
    public static final float ALIEN_STEP = 12f;

    /** Pixels dropped down when aliens reach the edge. */
    public static final float ALIEN_DROP = 18f;

    /** Number of columns in the alien grid. */
    public static final int ALIEN_COLS = 11;

    /** Number of rows in the alien grid. */
    public static final int ALIEN_ROWS = 5;

    /** Horizontal center of this alien. */
    public float x;

    /** Vertical center of this alien. */
    public float y;

    /**
     * Visual/point type: 0 = basic, 1 = medium, 2 = tough.
     * Higher type = more points and fancier sprite details.
     */
    public int type;

    /** Wether this alien is still in play. */
    public boolean alive;

    /**
     * Creates an alien at a given position with a given type.
     *
     * @param x    horizontal center
     * @param y    vertical center
     * @param type 0, 1, or 2 — controls appearance and score value
     */
    public Alien(float x, float y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.alive = true;
    }

    /**
     * Hitbox for collision checks against bullets or players.
     *
     * @return bounding rectangle centered on this alien
     */
    public Rectangle rect() {
        return new Rectangle(x - ALIEN_W / 2, y - ALIEN_H / 2, ALIEN_W, ALIEN_H);
    }

    // Static helpers

    /**
     * Returns true when every alien in the given array is dead.
     *
     * @param aliens the current alien list
     * @return true if none are alive
     */
    public static boolean allAliensDead(Array<Alien> aliens) {
        for (Alien a : aliens) if (a.alive) return false;
        return true;
    }

    /**
     * Moves every alive alien one step sideways (or drops them down if
     * alienDescend is true). Flips direction after a descent.
     * Updates alienDescend and alienDirX on the passed-in state object.
     *
     * @param aliens        the alien list
     * @param state         mutable movement state (dir, descend flag)
     * @param level         current level — higher levels move an extra bit
     * @param screenW       game screen width used for edge detection
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
     * Draws an alien using shape primitives. Different types get extra
     * decorative bits (antennas, top knob).
     *
     * @param a      the alien to draw
     * @param shapes active ShapeRenderer (already begun)
     */
    public static void drawAlien(Alien a, ShapeRenderer shapes) {
        // pick color by type
        Color c = a.type == 2 ? Color.MAGENTA : a.type == 1 ? Color.ORANGE : Color.WHITE;
        shapes.setColor(c);

        float hw = ALIEN_W / 2, hh = ALIEN_H / 2;
        shapes.rect(a.x - hw + 4, a.y - hh, ALIEN_W - 8, ALIEN_H);
        shapes.rect(a.x - hw, a.y - hh + 6, ALIEN_W, ALIEN_H - 12);
        if (a.type >= 1) {
            shapes.rect(a.x - hw - 6, a.y, 6, 6);
            shapes.rect(a.x + hw,     a.y, 6, 6);
        }
        if (a.type == 2) {
            shapes.rect(a.x - 4, a.y + hh, 8, 8);
        }
        shapes.rect(a.x - 10, a.y - hh - 6, 6, 6);
        shapes.rect(a.x +  4, a.y - hh - 6, 6, 6);
    }

    // Bomb

    /**
     * A bomb droped by an alien. Falls down and kills players on contact.
     */
    public static class Bomb {

        /** Width of the bomb sprite. */
        public static final float BOMB_W = 6f;

        /** Height of the bomb sprite. */
        public static final float BOMB_H = 14f;

        /** How fast bombs fall downward. */
        public static final float BOMB_SPEED = 220f;

        /** Horizontal center of the bomb. */
        public float x;

        /** Bottom edge of the bomb. */
        public float y;

        /**
         * Creates a bomb at the given position (usually bottom of an alien).
         *
         * @param x horizontal center
         * @param y starting y position
         */
        public Bomb(float x, float y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Hitbox for collision checks agaisnt player ships.
         *
         * @return bounding rectangle of this bomb
         */
        public Rectangle rect() {
            return new Rectangle(x - BOMB_W / 2, y, BOMB_W, BOMB_H);
        }
    }

    // Movement state helper

    /**
     * Tiny bag of mutable alien movement state. Kept separate so Game
     * doesnt have to expose raw fields and Alien.moveAliens can mutate it cleanly.
     */
    public static class AlienMoveState {

        /** Current horizontal movement direction, +1 or -1. */
        public float alienDirX;

        /** True when aliens should drop down on the next tick. */
        public boolean alienDescend;

        /**
         * Creates the state with an initial direction.
         *
         * @param alienDirX starting dir, usually +1
         */
        public AlienMoveState(float alienDirX) {
            this.alienDirX = alienDirX;
            this.alienDescend = false;
        }
    }
}

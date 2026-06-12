package io.github.javainvaders;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;

/**
 * Core gameplay class. Owns the entity lists, runs the update loop,
 * handles collisions and draws every gameplay frame.
 * Menu transitions and save/load are delegated to Screen and Save.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Game {

    // Render resources (shared from Main)

    /** Batch for drawing text on top of the game. */
    final SpriteBatch batch;

    /** Renderer for ships, aliens, bullets, etc. */
    final ShapeRenderer shapes;

    /** Regular font for HUD text. */
    final BitmapFont font;

    // Game entities — package-accessible so Save can reach them

    /** Player one (cyan ship). */
    Player p1;

    /** Player two (lime ship). */
    Player p2;

    /** All bullets currently flying. */
    Array<Player.Bullet> bullets;

    /** All bombs currently falling. */
    Array<Alien.Bomb> bombs;

    /** The alien grid. */
    Array<Alien> aliens;

    /** Shields protecting players from bombs. */
    Array<Shields> shields;

    /** Active explosion animations. */
    Array<Main.Explosion> explosions;

    /** Boss for level 3 — null until all normal aliens are dead. */
    Alien.Boss boss;

    /** Positions of explosion on boss death */
    final float[][] BOSS_EXPLOSION_OFFSETS = {
        { 0,  0}, {-150, 50}, {150, -100}, {-150, -150},
        {150,  50}, {-150, 100}, {100, -100}, {100,  150}
    };

    /** Index of explosion on boss death to look on offset */
    int bossExplosionIndex;

    /** Timer to make the animation of boss death */
    float bossExplosionTimer;

    // Level state

    /** Current level number (1-3). */
    int level;

    /** Countdown until next bomb is droped. */
    private float bombTimer;

    /** How often (seconds) a bomb drops. Decreases with level. */
    private float bombInterval;

    /** Countdown until aliens take another step. */
    private float alienMoveTimer;

    /** Time between alien movement ticks. Decreases with level. */
    private float alienMoveInterval;

    /** Mutable alien movement state (direction and descend flag). */
    AlienMoveState alienMoveState;

    /** Countdown shown on the level-complete screen. */
    float levelCompleteTimer;

    /** Bonus points awarded to each player at level end. */
    int[] bonusAwarded;

    /** Timer driving the "Saved!" feedback text fade. */
    float saveFeedbackTimer;

    /**
     * Set during loadGame so initLevel doesnt reset the loaded player state.
     */
    boolean loaded;

    /**
     * Short grace period at level start so holding shoot doesnt
     * instantly fire a bullet.
     */
    private float initialShootTimer;

    /** Reference back to main so we can flip currentScreen. */
    final Main main;

    /** Which screen is active — written by Screen callbacks too. */
    Screen.State currentScreen;

    /**
     * Creates the Game and grabs the shared rendering resources from Main.
     *
     * @param main   the Main instance (used for screen-state transitions)
     * @param batch  shared SpriteBatch
     * @param shapes shared ShapeRenderer
     * @param font   shared regular BitmapFont
     */
    public Game(Main main, SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        this.main   = main;
        this.batch  = batch;
        this.shapes = shapes;
        this.font   = font;
    }

    // Game setup

    /** Starts a fresh game from level 1 with full lives. */
    public void startNewGame() {
        level = 1;
        p1 = new Player(1, Main.W / 2f - 120);
        p2 = new Player(2, Main.W / 2f + 120);
        initLevel();
        currentScreen = Screen.State.GAMEPLAY;
    }

    /**
     * Initialises (or re-initialises) the current level.
     * If loaded is true, player stats are left untouched so the loaded
     * save data persists through the call.
     */
    void initLevel() {
        bullets      = new Array<>();
        bombs        = new Array<>();
        explosions   = new Array<>();
        aliens       = new Array<>();
        shields = Shields.createForLevel(level, Main.W);
        bonusAwarded = new int[]{0, 0};
        boss = null; // spawned later when all aliens die on level 3

        if (!loaded) {
            p1.alive = true; p1.respawnTimer = 0f; p1.x = Main.W / 2f - 120;
            p2.alive = true; p2.respawnTimer = 0f; p2.x = Main.W / 2f + 120;
            p1.lives = 3;
            p2.lives = 3;
        }

        float startX = 120f;
        float startY = Main.H - 160f;
        for (int row = 0; row < Alien.ALIEN_ROWS; row++) {
            for (int col = 0; col < Alien.ALIEN_COLS; col++) {
                float ax   = startX + col * 56f;
                float ay   = startY - row * 44f;
                int   type = row < 1 ? 2 : (row < 3 ? 1 : 0);
                aliens.add(new Alien(ax, ay, type));
            }
        }

        float speedMult   = 10f + (level - 1) * 5f;
        alienMoveInterval = Math.max(0.04f, 0.8f / speedMult);
        bombInterval      = Math.max(0.4f,  8f  / speedMult);
        alienMoveTimer    = 0f;
        bombTimer         = 1f;
        alienMoveState    = new AlienMoveState(1f);
        initialShootTimer = 0.2f;
    }

    // Gameplay update

    /**
     * Main update step while in the GAMEPLAY state.
     * Delegates movement, shooting, alien ticks and collision to helpers.
     *
     * @param dt delta time in seconds
     */
    public void updateGame(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            main.screenManager.resetPauseSelection();
            currentScreen = Screen.State.PAUSE_MENU;
            return;
        }

        if (initialShootTimer > 0) initialShootTimer -= dt;

        Player.updatePlayer(p1, dt, Input.Keys.A,    Input.Keys.D,     Input.Keys.SPACE,
                            bullets, Main.W, initialShootTimer);
        Player.updatePlayer(p2, dt, Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.ENTER,
                            bullets, Main.W, initialShootTimer);

        // move bullets and bombs
        for (Player.Bullet b : bullets) b.y += Player.Bullet.BULLET_SPEED * dt;
        for (Alien.Bomb b : bombs) {
            if (b == null) continue;
            if (b instanceof Alien.RadialBomb) {
                Alien.RadialBomb rb = (Alien.RadialBomb) b;
                rb.x     += rb.vx * Alien.RadialBomb.RADIAL_SPEED * dt;
                rb.y     += rb.vy * Alien.RadialBomb.RADIAL_SPEED * dt;
                rb.angle += dt * 4f; // spin for visual flair
            } else {
                b.y -= Alien.Bomb.BOMB_SPEED * dt;
            }
        }

        // boss tick (level 3 only, spawned once normal aliens are all dead).
        // update runs even while dying so the death sequence animation plays out.
        if (boss != null && (boss.alive || boss.dying)) boss.update(dt, bombs, Main.W);

        // alien movement tick
        alienMoveTimer += dt;
        if (alienMoveTimer >= alienMoveInterval) {
            alienMoveTimer = 0f;
            Alien.moveAliens(aliens, alienMoveState, level, Main.W);
        }

        // bomb drop tick
        bombTimer += dt;
        if (bombTimer >= bombInterval) {
            bombTimer = 0f;
            dropBomb();
        }

        // tick explosion timers
        for (Main.Explosion e : explosions) e.timer -= dt;

        Shields.update(shields, dt);

        checkCollisions();
        cleanUp();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) Save.saveGame(this);

        if (saveFeedbackTimer > 0) saveFeedbackTimer -= dt;

        // check win/lose transitions
        if (Alien.allAliensDead(aliens)) {
            if (level == 3) {
                // spawn boss once all regular aliens die, if not already done
                if (boss == null) boss = new Alien.Boss(Main.W);

                // timer to wait explosion animations
                bossExplosionTimer -= dt;

                // fire explosions while the boss is in its white-flash dying state
                if (boss != null && boss.dying && !boss.deathSequenceDone && bossExplosionTimer <= 0f && bossExplosionIndex < BOSS_EXPLOSION_OFFSETS.length) {
                    float[] off = BOSS_EXPLOSION_OFFSETS[bossExplosionIndex];
                    explosions.add(new Main.Explosion(boss.x + off[0], boss.y + off[1]));
                    bossExplosionIndex++;
                    bossExplosionTimer = Alien.Boss.BOSS_DEATH_FREEZE_DURATION/5f;
                }

                // death sequence done: fire the final explosion once
                if (boss != null && boss.deathSequenceDone && bossExplosionIndex == BOSS_EXPLOSION_OFFSETS.length) {
                    explosions.add(new Main.Explosion(boss.x, boss.y));
                    boss.dying = false; // hide boss sprite
                    bossExplosionTimer = 0.8f;
                    bossExplosionIndex++; // increment so this block never runs again
                }

                // once the death sequence ends, do the final explosion and transition
                if (boss != null && boss.deathSequenceDone && bossExplosionTimer <= 0f && bossExplosionIndex > BOSS_EXPLOSION_OFFSETS.length) {
                    Score.awardLevelBonus(p1, p2, level, bonusAwarded);
                    levelCompleteTimer = 3f;
                    currentScreen = Screen.State.LEVEL_COMPLETE;
                }
            } else {
                Score.awardLevelBonus(p1, p2, level, bonusAwarded);
                levelCompleteTimer = 3f;
                currentScreen = Screen.State.LEVEL_COMPLETE;
            }
        }

        if (Player.allPlayersDead(p1, p2)) {
            currentScreen = Screen.State.GAME_OVER;
        }
    }

    /** Picks a random alive alien to drop a bomb. Does nothing if none alive. */
    private void dropBomb() {
        Array<Alien> alive = new Array<>();
        for (Alien a : aliens) if (a.alive) alive.add(a);
        if (alive.size == 0) return;
        Alien shooter = alive.get((int)(Math.random() * alive.size));
        bombs.add(new Alien.Bomb(shooter.x, shooter.y - Alien.ALIEN_H / 2));
    }

    /** Checks bullet/alien, bomb/player, and alien/player collisions. */
    private void checkCollisions() {
        // bullets vs aliens
        for (Iterator<Player.Bullet> bi = bullets.iterator(); bi.hasNext(); ) {
            Player.Bullet b = bi.next();
            boolean hit = false;

            // check vs boss first
            if (boss != null && boss.alive && b.rect().overlaps(boss.rect())) {
                boolean killed = boss.hit();
                if (killed) explosions.add(new Main.Explosion(boss.x, boss.y));
                int pts = killed ? Alien.Boss.BOSS_SCORE : Alien.Boss.BOSS_SCORE / Alien.Boss.BOSS_MAX_HP;
                if (b.owner == 1) p1.score += pts;
                else              p2.score += pts;
                hit = true;
            }

            if (!hit) {
                for (Alien a : aliens) {
                    if (!a.alive) continue;
                    if (b.rect().overlaps(a.rect())) {
                        a.alive = false;
                        explosions.add(new Main.Explosion(a.x, a.y));
                        int pts = Score.pointsForAlien(a.type);
                        if (b.owner == 1) p1.score += pts;
                        else              p2.score += pts;
                        hit = true;
                        break;
                    }
                }
            }

            if (b.y > Main.H) hit = true;
            if (hit) bi.remove();
        }

        // bombs vs players
        for (Iterator<Alien.Bomb> bi = bombs.iterator(); bi.hasNext(); ) {
            Alien.Bomb b = bi.next();
            boolean hit = false;
            if (p1.alive && b.rect().overlaps(p1.rect())) { Player.killPlayer(p1, explosions); hit = true; }
            if (p2.alive && b.rect().overlaps(p2.rect())) { Player.killPlayer(p2, explosions); hit = true; }

            for (Shields s : shields) {
                if (s.alive && b.rect().overlaps(s.rect())) {
                    Shields.hitByBomb(s);
                    hit = true;
                    break;
                }
            }

            // radial bombs leave the field from any edge; normal bombs only from the bottom
            if (b != null){
                boolean outOfBounds = (b instanceof Alien.RadialBomb)
                    ? (b.x < -50 || b.x > Main.W + 50 || b.y < -50 || b.y > Main.H + 50)
                    : (b.y < 0);
                if (outOfBounds) hit = true;
            }
            if (hit) bi.remove();
        }

        // aliens touching players or reaching the floor
        for (Alien a : aliens) {
            if (!a.alive) continue;
            if (p1.alive && a.rect().overlaps(p1.rect())) Player.killPlayer(p1, explosions);
            if (p2.alive && a.rect().overlaps(p2.rect())) Player.killPlayer(p2, explosions);

            for (Shields s : shields) {
                if (s.alive && a.rect().overlaps(s.rect())) {
                    Shields.destroyInstantly(s);
                }
            }

            if (a.y - Alien.ALIEN_H / 2 <= 50) currentScreen = Screen.State.GAME_OVER;
        }
    }

    /** Removes any explosions whose timer has run out. */
    private void cleanUp() {
        for (Iterator<Main.Explosion> ei = explosions.iterator(); ei.hasNext(); )
            if (ei.next().timer <= 0) ei.remove();
    }

    /** Handles the level-complete countdown and transitions to next level or game-over. */
    public void updateLevelComplete(float dt) {
        levelCompleteTimer -= dt;
        if (levelCompleteTimer <= 0) {
            level++;
            if (level > 3) {
                currentScreen = Screen.State.GAME_OVER;
            } else {
                initLevel();
                currentScreen = Screen.State.GAMEPLAY;
            }
        }
    }

    // Rendering

    /** Draws the entire gameplay frame: ships, aliens, bullets, bombs, HUD. */
    public void renderGame() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // ships — drawShip now handles blink internally
        if (p1.alive || p1.blinkTimer > 0) Player.drawShip(p1, Color.CYAN, shapes);
        if (p2.alive || p2.blinkTimer > 0) Player.drawShip(p2, Color.LIME, shapes);

        // player bullets
        for (Player.Bullet b : bullets) {
            shapes.setColor(b.owner == 1 ? Color.CYAN : Color.LIME);
            shapes.rect(b.x - Player.Bullet.BULLET_W / 2, b.y, Player.Bullet.BULLET_W, Player.Bullet.BULLET_H);
        }

        // alien bombs (normal straight-falling ones only; radial handled above)
        for (Alien.Bomb b : bombs) {
            if (b instanceof Alien.RadialBomb || b == null) continue;
            shapes.setColor(Color.RED);
            shapes.rect(b.x - Alien.Bomb.BOMB_W / 2, b.y, Alien.Bomb.BOMB_W, Alien.Bomb.BOMB_H);
        }

        // aliens
        for (Alien a : aliens) {
            if (!a.alive) continue;
            Alien.drawAlien(a, shapes);
        }

        // boss (level 3)
        if (boss != null) boss.draw(shapes);

        // radial bombs get a spinning square visual instead of a plain rect
        for (Alien.Bomb b : bombs) {
            if (b instanceof Alien.RadialBomb) {
                Alien.RadialBomb rb = (Alien.RadialBomb) b;
                shapes.setColor(Color.RED);
                // rotate around its own center using a tiny offset quad
                float s = (float) Math.sin(rb.angle) * 5f;
                float c2 = (float) Math.cos(rb.angle) * 5f;
                shapes.triangle(rb.x + c2, rb.y + s, rb.x - s, rb.y + c2, rb.x - c2, rb.y - s);
                shapes.triangle(rb.x + s,  rb.y - c2, rb.x + c2, rb.y + s, rb.x - c2, rb.y - s);
            }
        }

        // shields
        Shields.draw(shields, shapes);

        // explosions — simple growing circle that fades out
        for (Main.Explosion e : explosions) {
            float r = e.timer / 0.4f;
            shapes.setColor(1f, 0.5f * r, 0f, r);
            shapes.circle(e.x, e.y, 18 * (1 - r) + 6);
        }

        // floor divider line
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(0, 48, Main.W, 2);

        // lives icons
        Player.drawLives(p1, 20,          Main.H - 50, Color.CYAN, shapes);
        Player.drawLives(p2, Main.W - 200, Main.H - 50, Color.LIME, shapes);

        shapes.end();

        // HUD text (scores, level, save hint)
        batch.begin();
        Score.drawHUD(batch, font, p1, p2, level, saveFeedbackTimer, Main.W, Main.H);
        batch.end();
    }
}

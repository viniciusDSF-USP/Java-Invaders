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

    // Shared rendering resources

    /** Batch for drawing HUD text. */
    final SpriteBatch batch;

    /** Renderer for ships, aliens, bullets, etc. */
    final ShapeRenderer shapes;

    /** Regular font for HUD text. */
    final BitmapFont font;

    // Navigation

    /** Reference back to main so we can flip currentScreen. */
    final Main main;

    /** Which screen is currently active - also written by Screen callbacks. */
    Screen.State currentScreen;

    // Game entities - package-accessible so Save can reach them

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

    /** Boss for level 3 - null until all normal aliens are dead. */
    Alien.Boss boss;

    // Boss death sequence

    /** Explosion offsets used during the boss death animation. */
    final float[][] BOSS_EXPLOSION_OFFSETS = {
        { 0,  0}, {-150, 50}, {150, -100}, {-150, -150},
        {150,  50}, {-150, 100}, {100, -100}, {100,  150}
    };

    /** Index of the next offset to use in the boss death explosion sequence. */
    int bossExplosionIndex;

    /** Timer that paces explosions during the boss death animation. */
    float bossExplosionTimer;

    // Level state

    /** Current level number (1-3). */
    int level;

    /** Mutable alien movement state (direction and descend flag). */
    AlienMoveState alienMoveState;

    /** Countdown until aliens take another step. */
    private float alienMoveTimer;

    /** Time between alien movement ticks - decreases with level. */
    private float alienMoveInterval;

    /** Countdown until the next bomb is droped. */
    private float bombTimer;

    /** How often a bomb drops in seconds - decreases with level. */
    private float bombInterval;

    /** Short grace period at level start so holding shoot doesnt instantly fire. */
    private float initialShootTimer;

    // Post-level state

    /** Countdown shown on the level-complete screen. */
    float levelCompleteTimer;

    /** Bonus points awarded to each player at level end. */
    int[] bonusAwarded;

    // UI feedback

    /** Timer driving the "Saved!" feedback text fade. */
    float saveFeedbackTimer;

    // Save flag

    /** Set during loadGame so initLevel doesnt reset the loaded player state. */
    boolean loaded;

    /**
     * Creates the Game and grabs the shared rendering resources from Main.
     *
     * @param main   the Main instance (used for screen-state transitions)
     * @param batch  shared SpriteBatch
     * @param shapes shared ShapeRenderer
     * @param font   shared BitmapFont
     */
    public Game(Main main, SpriteBatch batch, ShapeRenderer shapes, BitmapFont font) {
        this.main   = main;
        this.batch  = batch;
        this.shapes = shapes;
        this.font   = font;
    }

    // Setup

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
     * If loaded is true, player stats are left untouched so save data persists.
     */
    void initLevel() {
        bullets      = new Array<>();
        bombs        = new Array<>();
        explosions   = new Array<>();
        aliens       = new Array<>();
        shields      = Shields.createForLevel(level, Main.W);
        bonusAwarded = new int[]{0, 0};
        boss         = null; // spawned later when all aliens die on level 3
        bossExplosionIndex = 0;
        bossExplosionTimer = 0f;

        if (!loaded) {
            p1.alive = true; p1.respawnTimer = 0f; p1.x = Main.W / 2f - 120;
            p2.alive = true; p2.respawnTimer = 0f; p2.x = Main.W / 2f + 120;
            p1.lives = 3;
            p2.lives = 3;
        }

        // Build the alien grid
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

        // Speed scales with level
        float speedMult   = 10f + (level - 1) * 5f;
        alienMoveInterval = Math.max(0.04f, 0.8f / speedMult);
        bombInterval      = Math.max(0.4f,  8f  / speedMult);
        alienMoveTimer    = 0f;
        bombTimer         = 1f;
        alienMoveState    = new AlienMoveState(1f);
        initialShootTimer = 0.2f;
    }

    // Update

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

        // Move bullets upward
        for (Player.Bullet b : bullets) b.y += Player.Bullet.BULLET_SPEED * dt;

        // Move bombs - radial ones travel on an angle, normal ones fall straight down
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

        // Boss tick - runs even while dying so the death animation plays out
        if (boss != null && (boss.alive || boss.dying)) boss.update(dt, bombs, Main.W);

        // Alien movement tick
        alienMoveTimer += dt;
        if (alienMoveTimer >= alienMoveInterval) {
            alienMoveTimer = 0f;
            Alien.moveAliens(aliens, alienMoveState, level, Main.W);
        }

        // Bomb drop tick
        bombTimer += dt;
        if (bombTimer >= bombInterval) {
            bombTimer = 0f;
            dropBomb();
        }

        // Tick explosion timers
        for (Main.Explosion e : explosions) e.timer -= dt;

        Shields.update(shields, dt);

        checkCollisions();
        cleanUp();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) Save.saveGame(this);

        if (saveFeedbackTimer > 0) saveFeedbackTimer -= dt;

        // Win/lose transitions
        if (Alien.allAliensDead(aliens)) {
            if (level == 3) {
                // Spawn boss once all regular aliens are gone
                if (boss == null) {
                    boss = new Alien.Boss(Main.W);
                    SoundManager.get().playBossMusic();
                }

                bossExplosionTimer -= dt;

                // Fire explosions while the boss is in its white-flash dying state
                if (boss != null && boss.dying && !boss.deathSequenceDone
                        && bossExplosionTimer <= 0f
                        && bossExplosionIndex < BOSS_EXPLOSION_OFFSETS.length) {
                    float[] off = BOSS_EXPLOSION_OFFSETS[bossExplosionIndex];
                    explosions.add(new Main.Explosion(boss.x + off[0], boss.y + off[1]));
                    SoundManager.get().playExplosion();
                    bossExplosionIndex++;
                    bossExplosionTimer = Alien.Boss.BOSS_DEATH_FREEZE_DURATION / 5f;
                }

                // Final explosion once all offset explosions have fired
                if (boss != null && boss.deathSequenceDone
                        && bossExplosionIndex == BOSS_EXPLOSION_OFFSETS.length) {
                    explosions.add(new Main.Explosion(boss.x, boss.y));
                    SoundManager.get().playExplosion();
                    boss.dying = false; // hide boss sprite
                    bossExplosionTimer = 0.8f;
                    bossExplosionIndex++; // prevent this block from running again
                }

                // Transition after the final explosion settles
                if (boss != null && boss.deathSequenceDone
                        && bossExplosionTimer <= 0f
                        && bossExplosionIndex > BOSS_EXPLOSION_OFFSETS.length) {
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
            SoundManager.get().playMenuMusic();
            currentScreen = Screen.State.GAME_OVER;
        }
    }

    /** Picks a random alive alien to drop a bomb. Does nothing if none are alive. */
    private void dropBomb() {
        Array<Alien> alive = new Array<>();
        for (Alien a : aliens) if (a.alive) alive.add(a);
        if (alive.size == 0) return;
        Alien shooter = alive.get((int)(Math.random() * alive.size));
        bombs.add(new Alien.Bomb(shooter.x, shooter.y - Alien.ALIEN_H / 2));
    }

    /** Checks bullet/alien, bomb/player and alien/player collisions. */
    private void checkCollisions() {
        // Bullets vs aliens (and boss)
        for (Iterator<Player.Bullet> bi = bullets.iterator(); bi.hasNext(); ) {
            Player.Bullet b = bi.next();
            boolean hit = false;

            // Check vs boss first
            if (boss != null && boss.alive && b.rect().overlaps(boss.rect())) {
                boolean killed = boss.hit();
                SoundManager.get().playExplosion();
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
                        SoundManager.get().playExplosion();
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

        // Bombs vs players and shields
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

            // Radial bombs leave from any edge; normal bombs only from the bottom
            if (b != null) {
                boolean outOfBounds = (b instanceof Alien.RadialBomb)
                    ? (b.x < -50 || b.x > Main.W + 50 || b.y < -50 || b.y > Main.H + 50)
                    : (b.y < 0);
                if (outOfBounds) hit = true;
            }
            if (hit) bi.remove();
        }

        // Aliens touching players or reaching the floor
        for (Alien a : aliens) {
            if (!a.alive) continue;
            if (p1.alive && a.rect().overlaps(p1.rect())) Player.killPlayer(p1, explosions);
            if (p2.alive && a.rect().overlaps(p2.rect())) Player.killPlayer(p2, explosions);

            for (Shields s : shields) {
                if (s.alive && a.rect().overlaps(s.rect())) Shields.destroyInstantly(s);
            }

            if (a.y - Alien.ALIEN_H / 2 <= 50) currentScreen = Screen.State.GAME_OVER;
        }
    }

    /** Removes explosions whose timer has run out. */
    private void cleanUp() {
        for (Iterator<Main.Explosion> ei = explosions.iterator(); ei.hasNext(); )
            if (ei.next().timer <= 0) ei.remove();
    }

    /** Handles the level-complete countdown and transitions to the next level or game over. */
    public void updateLevelComplete(float dt) {
        levelCompleteTimer -= dt;
        if (levelCompleteTimer <= 0) {
            level++;
            if (level > 3) {
                SoundManager.get().playMenuMusic();
                currentScreen = Screen.State.GAME_OVER;
            } else {
                SoundManager.get().playGameplayMusic();
                initLevel();
                currentScreen = Screen.State.GAMEPLAY;
            }
        }
    }

    // Rendering

    /** Draws the full gameplay frame: ships, aliens, bullets, bombs, HUD. */
    public void renderGame() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Ships - drawShip handles the respawn blink internally
        if (p1.alive || p1.blinkTimer > 0) Player.drawShip(p1, Color.CYAN, shapes);
        if (p2.alive || p2.blinkTimer > 0) Player.drawShip(p2, Color.LIME, shapes);

        // Player bullets
        for (Player.Bullet b : bullets) {
            shapes.setColor(b.owner == 1 ? Color.CYAN : Color.LIME);
            shapes.rect(b.x - Player.Bullet.BULLET_W / 2, b.y, Player.Bullet.BULLET_W, Player.Bullet.BULLET_H);
        }

        // Normal (straight-falling) bombs
        for (Alien.Bomb b : bombs) {
            if (b instanceof Alien.RadialBomb || b == null) continue;
            shapes.setColor(Color.RED);
            shapes.rect(b.x - Alien.Bomb.BOMB_W / 2, b.y, Alien.Bomb.BOMB_W, Alien.Bomb.BOMB_H);
        }

        // Aliens
        for (Alien a : aliens) {
            if (!a.alive) continue;
            Alien.drawAlien(a, shapes);
        }

        // Boss (level 3 only)
        if (boss != null) boss.draw(shapes);

        // Radial bombs - spinning triangle visual instead of a plain rect
        for (Alien.Bomb b : bombs) {
            if (b instanceof Alien.RadialBomb) {
                Alien.RadialBomb rb = (Alien.RadialBomb) b;
                shapes.setColor(Color.RED);
                float s  = (float) Math.sin(rb.angle) * 5f;
                float c2 = (float) Math.cos(rb.angle) * 5f;
                shapes.triangle(rb.x + c2, rb.y + s, rb.x - s, rb.y + c2, rb.x - c2, rb.y - s);
                shapes.triangle(rb.x + s,  rb.y - c2, rb.x + c2, rb.y + s, rb.x - c2, rb.y - s);
            }
        }

        // Shields
        Shields.draw(shields, shapes);

        // Explosions - growing circle that fades out
        for (Main.Explosion e : explosions) {
            float r = e.timer / 0.4f;
            shapes.setColor(1f, 0.5f * r, 0f, r);
            shapes.circle(e.x, e.y, 18 * (1 - r) + 6);
        }

        // Floor divider
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(0, 48, Main.W, 2);

        // Lives icons
        Player.drawLives(p1, 20,           Main.H - 50, Color.CYAN, shapes);
        Player.drawLives(p2, Main.W - 200, Main.H - 50, Color.LIME, shapes);

        shapes.end();

        // HUD text (scores, level, save hint)
        batch.begin();
        Score.drawHUD(batch, font, p1, p2, level, saveFeedbackTimer, Main.W, Main.H);
        batch.end();
    }
}

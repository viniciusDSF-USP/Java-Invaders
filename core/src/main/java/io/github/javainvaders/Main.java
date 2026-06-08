package io.github.javainvaders;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Main entry point for Java Invaders. Sets up shared render resources,
 * owns the Screen manager and hands each frame to Game or Screen depending
 * on the active state. The actual gameplay logic lives in Game.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Main extends ApplicationAdapter {

    // Constants

    /** Game window width. */
    public static final int W = 1280;

    /** Game window height. */
    public static final int H = 720;

    // Render resources

    /** Batch for drawing sprites and text. */
    private SpriteBatch batch;

    /** Renderer for shapes (ships, aliens, bullets, etc). */
    private ShapeRenderer shapes;

    /** Regular-size font. */
    private BitmapFont font;

    /** Big font for titles and game-over messages. */
    private BitmapFont bigFont;

    /** Orthographic camera that maps game coords to screen coords. */
    private OrthographicCamera camera;

    // Managers

    /** Handles all menu/pause rendering and their input. */
    Screen screenManager;

    /** Owns the gameplay loop, entities and rendering. */
    private Game game;

    // Lifecycle

    @Override
    public void create() {
        batch  = new SpriteBatch();
        shapes = new ShapeRenderer();

        font = new BitmapFont();
        font.getData().setScale(1.6f);

        bigFont = new BitmapFont();
        bigFont.getData().setScale(3.2f);

        camera = new OrthographicCamera(W, H);
        camera.position.set(W / 2f, H / 2f, 0);
        camera.update();

        screenManager = new Screen(font, bigFont, batch, shapes);
        game = new Game(this, batch, shapes, font);
        game.currentScreen = Screen.State.MAIN_MENU;
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        float dt = Gdx.graphics.getDeltaTime();

        switch (game.currentScreen) {
            case MAIN_MENU:
                // Screen.handleMenu does input + render + calls game.startNewGame when needed
                screenManager.handleMenu(dt, game);
                break;
            case GAMEPLAY:
                game.updateGame(dt);
                game.renderGame();
                break;
            case PAUSE_MENU:
                // Screen.handlePause does input + render (with frozen game in background)
                screenManager.handlePause(dt, game);
                break;
            case LEVEL_COMPLETE:
                game.updateLevelComplete(dt);
                screenManager.renderLevelComplete(game.level, game.bonusAwarded, game.levelCompleteTimer);
                break;
            case GAME_OVER:
                if (screenManager.updateGameOver(dt)) {
                    screenManager.resetMenuSelection();
                    game.currentScreen = Screen.State.MAIN_MENU;
                }
                boolean won = game.level > 3 && Alien.allAliensDead(game.aliens);
                screenManager.renderGameOver(won,
                    game.p1 != null ? game.p1.score : 0,
                    game.p2 != null ? game.p2.score : 0);
                break;
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        bigFont.dispose();
    }

    // Explosion helper class

    /**
     * Tiny data class for a timed explosion animation.
     * Just holds position and a countdown.
     */
    public static class Explosion {

        /** X center of the explosion. */
        float x;

        /** Y center of the explosion. */
        float y;

        /** Time remaining before the explosion dissapears. */
        float timer;

        /**
         * Creates an explosion at the given position.
         *
         * @param x horizontal center
         * @param y vertical center
         */
        Explosion(float x, float y) {
            this.x = x;
            this.y = y;
            this.timer = 0.4f;
        }
    }
}

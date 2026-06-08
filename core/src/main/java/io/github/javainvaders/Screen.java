package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Manages all non-gameplay screens: the main menu, pause menu,
 * level-complete screen and game-over screen. Rendering and input
 * for thse screens live here so Main stays clean.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Screen {

    /** All posible game states / screens. */
    public enum State {
        MAIN_MENU, GAMEPLAY, PAUSE_MENU, LEVEL_COMPLETE, GAME_OVER
    }

    /** Screen width in pixels. */
    private static final int W = Main.W;

    /** Screen height in pixels. */
    private static final int H = Main.H;

    /** Currently highlighted option in the main menu (0-2). */
    private int menuSelection;

    /** Currently highlighted option in the pause menu (0-2). */
    private int pauseSelection;

    /** Font used for most text. */
    private final BitmapFont font;

    /** Bigger font for titles and big messages. */
    private final BitmapFont bigFont;

    /** Batch used for drawing text. */
    private final SpriteBatch batch;

    /** Shape renderer used for the pause overlay. */
    private final ShapeRenderer shapes;

    /**
     * Creates the screen manager. Takes refs to the shared render resources
     * so we dont have to create new ones.
     *
     * @param font     regular-sized font
     * @param bigFont  large font for headings
     * @param batch    sprite batch for text drawing
     * @param shapes   shape renderer for the pause overlay
     */
    public Screen(BitmapFont font, BitmapFont bigFont, SpriteBatch batch, ShapeRenderer shapes) {
        this.font = font;
        this.bigFont = bigFont;
        this.batch = batch;
        this.shapes = shapes;
        this.menuSelection = 0;
        this.pauseSelection = 0;
    }

    // Main menu

    /**
     * Handles keyboard input on the main menu.
     * Returns a MenuAction telling Main what to do next.
     *
     * @param dt delta time (not really used here but kept for symetry)
     * @return action triggered by the player, or NONE
     */
    public MenuAction updateMenu(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))   menuSelection = (menuSelection + 2) % 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) menuSelection = (menuSelection + 1) % 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (menuSelection == 0) return MenuAction.NEW_GAME;
            if (menuSelection == 1) return MenuAction.LOAD_GAME;
            return MenuAction.EXIT;
        }
        return MenuAction.NONE;
    }

    /** Draws the main menu with title and 3 options. */
    public void renderMenu() {
        batch.begin();
        bigFont.setColor(Color.GREEN);
        bigFont.draw(batch, "JAVA INVADERS", W / 2f - 220, H - 160);

        font.setColor(menuSelection == 0 ? Color.YELLOW : Color.WHITE);
        font.draw(batch, (menuSelection == 0 ? "> " : "  ") + "NEW GAME",  W / 2f - 80, H / 2f + 60);

        font.setColor(menuSelection == 1 ? Color.YELLOW : Color.WHITE);
        font.draw(batch, (menuSelection == 1 ? "> " : "  ") + "LOAD GAME", W / 2f - 80, H / 2f);

        font.setColor(menuSelection == 2 ? Color.YELLOW : Color.WHITE);
        font.draw(batch, (menuSelection == 2 ? "> " : "  ") + "EXIT",      W / 2f - 80, H / 2f - 60);

        font.setColor(Color.GRAY);
        font.draw(batch, "P1: A/D move  SPACE shoot    P2: LEFT/RIGHT move  ENTER shoot", 60, 40);
        batch.end();
    }

    /**
     * Handles the full main-menu frame: reads input and calls startNewGame
     * or loadGame on the game instance depending on what the player chose.
     * Also calls renderMenu so Game doesnt have to.
     *
     * @param dt   delta time
     * @param game the Game instance — needed to call startNewGame / loadGame
     */
    public void handleMenu(float dt, Game game) {
        MenuAction action = updateMenu(dt);
        if (action == MenuAction.NEW_GAME)       game.startNewGame();
        else if (action == MenuAction.LOAD_GAME) Save.loadGame(game);
        else if (action == MenuAction.EXIT)      Gdx.app.exit();
        renderMenu();
    }

    // Pause menu

    /**
     * Handles keyboard input while paused.
     * Returns a PauseAction so Main knows wether to resume, go to menu, or quit.
     *
     * @param dt delta time
     * @return action chosen by the player, or NONE
     */
    public PauseAction updatePause(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP))   pauseSelection = (pauseSelection + 2) % 3;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) pauseSelection = (pauseSelection + 1) % 3;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) return PauseAction.RESUME;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (pauseSelection == 0) return PauseAction.RESUME;
            if (pauseSelection == 1) return PauseAction.EXIT_TO_MENU;
            return PauseAction.EXIT_GAME;
        }
        return PauseAction.NONE;
    }

    /**
     * Renders the pause overlay on top of the frozen game frame.
     * Calls gameRenderCallback first so the background isnt black.
     *
     * @param gameRenderCallback a Runnable that draws the current game state
     */
    public void renderPause(Runnable gameRenderCallback) {
        // draw frozen game in background
        gameRenderCallback.run();

        // semi-transparent dark overlay
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.7f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        bigFont.setColor(Color.RED);
        bigFont.draw(batch, "PAUSED", W / 2f - 110, H - 200);

        font.setColor(pauseSelection == 0 ? Color.YELLOW : Color.WHITE);
        font.draw(batch, (pauseSelection == 0 ? "> " : "  ") + "RESUME",       W / 2f - 100, H / 2f + 40);

        font.setColor(pauseSelection == 1 ? Color.YELLOW : Color.WHITE);
        font.draw(batch, (pauseSelection == 1 ? "> " : "  ") + "EXIT TO MENU", W / 2f - 100, H / 2f - 20);

        font.setColor(pauseSelection == 2 ? Color.YELLOW : Color.WHITE);
        font.draw(batch, (pauseSelection == 2 ? "> " : "  ") + "EXIT GAME",    W / 2f - 100, H / 2f - 80);
        batch.end();
    }

    /**
     * Handles the full pause frame: reads input, transitions state on Game,
     * and draws the pause overlay on top of the frozen game.
     *
     * @param dt   delta time
     * @param game the Game instance so we can flip its currentScreen
     */
    public void handlePause(float dt, Game game) {
        PauseAction action = updatePause(dt);
        switch (action) {
            case RESUME:
                game.currentScreen = State.GAMEPLAY;
                break;
            case EXIT_TO_MENU:
                resetMenuSelection();
                game.currentScreen = State.MAIN_MENU;
                break;
            case EXIT_GAME:
                Gdx.app.exit();
                break;
            default:
                break;
        }
        renderPause(game::renderGame);
    }

    // Level complete screen

    /**
     * Draws the level-complete screen with bonus info and countdown.
     *
     * @param level            the level that was just cleared
     * @param bonusAwarded     array of size 2 with bonus points for P1 and P2
     * @param levelCompleteTimer seconds remaining on the countdown
     */
    public void renderLevelComplete(int level, int[] bonusAwarded, float levelCompleteTimer) {
        batch.begin();
        bigFont.setColor(Color.YELLOW);
        bigFont.draw(batch, "LEVEL COMPLETE!", W / 2f - 280, H / 2f + 60);

        font.setColor(Color.WHITE);
        font.draw(batch, "Level " + level + " cleared!", W / 2f - 100, H / 2f);

        if (bonusAwarded[0] > 0)
            font.draw(batch, "P1 Bonus: +" + bonusAwarded[0], W / 2f - 100, H / 2f - 50);
        if (bonusAwarded[1] > 0)
            font.draw(batch, "P2 Bonus: +" + bonusAwarded[1], W / 2f - 100, H / 2f - 90);

        font.setColor(Color.GRAY);
        font.draw(batch, "Next level in " + (int)(levelCompleteTimer + 1) + "...", W / 2f - 100, H / 2f - 150);
        batch.end();
    }

    // Game over screen

    /**
     * Handles input on the game-over screen. Returns true when the player
     * presses ENTER or ESC to go back to the main menu.
     *
     * @param dt delta time
     * @return true if the player wants to return to the main menu
     */
    public boolean updateGameOver(float dt) {
        return Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
    }

    /**
     * Draws the game-over (or win) screen with final scores.
     *
     * @param won    true if all aliens were defeated (game beaten)
     * @param p1Score score for player 1
     * @param p2Score score for player 2
     */
    public void renderGameOver(boolean won, int p1Score, int p2Score) {
        batch.begin();
        bigFont.setColor(Color.RED);
        bigFont.draw(batch, won ? "YOU WIN!" : "GAME OVER",
                     won ? W / 2f - 140 : W / 2f - 170,
                     H / 2f + 80);

        font.setColor(Color.WHITE);
        font.draw(batch, "P1 Score: " + p1Score, W / 2f - 100, H / 2f);
        font.draw(batch, "P2 Score: " + p2Score, W / 2f - 100, H / 2f - 50);

        font.setColor(Color.GRAY);
        font.draw(batch, "Press ENTER to return to menu", W / 2f - 180, H / 2f - 120);
        batch.end();
    }

    // Reset helpers

    /** Resets the main menu cursor to the top. Usefull when returning from a game. */
    public void resetMenuSelection() {
        menuSelection = 0;
    }

    /** Resets the pause cursor to "RESUME". Called each time the game is paused. */
    public void resetPauseSelection() {
        pauseSelection = 0;
    }

    // Enums for actions

    /** Actions the main menu can trigger. */
    public enum MenuAction {
        NONE, NEW_GAME, LOAD_GAME, EXIT
    }

    /** Actions the pause menu can trigger. */
    public enum PauseAction {
        NONE, RESUME, EXIT_TO_MENU, EXIT_GAME
    }
}

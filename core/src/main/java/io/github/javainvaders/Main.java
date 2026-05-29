package io.github.javainvaders;


import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.utils.ScreenUtils;

/*
Required Features:
  - [X] Game Setup.
  - [] Main menu with “New Game”, “Load Game”, and “Exit”.
  - [] LibGDX screen manager to transition between menus and gameplay.
  - [] Core Game Mechanics.
  - [] Players move their ships left/right and fire projectiles upward.
  - [] Alien formations move toward the players and drop bombs periodically.
  - [] Collision detection: projectiles destroy aliens; alien bombs or bodies destroy player ships.
  - [] Two-Player Co-op.
  - [] Two players share the screen using different keyboard keys.
  - [] Both ships have individual lives; the game ends when all lives are lost.
  - [] Levels and Difficulty.
  - [] At least 3 levels with progressively faster aliens and/or more complex movement patterns.
  - [] Brief “Level Complete” screen between levels.
  - [] Scoring and Lives.
  - [] Points for each alien destroyed; bonus points for level completion.
  - [] Each player starts with 3 lives.
  - [] Real-time score and lives are displayed on screen.
  - [] Save and Load.
  - [] Save current game progress (level, scores, lives) to a file.
  - [] Load a previously saved game from the main menu.

Optional Enhancements:
  - [] Power-ups: rapid fire, shield, bomb drop.
  - [] Network multiplayer using LibGDX networking (players on separate computers).
  - [] Boss enemy at the end of each level.
*/

public class Main extends ApplicationAdapter {
    static final int W = 1280;
    static final int H = 720;

    @Override
    public void create() {

    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);
    }

    @Override
    public void dispose() {

    }
}
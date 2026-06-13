package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Persists and restores game state to/from a local file.
 * Format is a simple comma-separated string so theres no extra deps.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Save {

    /** File name written to the app's local storage directory. */
    private static String SAVE_FILE = "javainvaders.sav";

    /**
     * Loads save config values from JSON data.
     *
     * @param config the JSON section containing save settings
     */
    public static void loadConfig(JsonValue config) {
        if (config.has("SAVE_FILE")) SAVE_FILE = config.getString("SAVE_FILE") + ".sav";
    }

    /**
     * Returns true when the game state is safe to persist.
     * Blocks saving if the boss is active or any player is dying on their last life.
     *
     * @param game the running Game instance to inspect
     * @return true if saving is allowed
     */
    private static boolean canSave(Game game) {
        // Block while the boss is on screen
        if (game.boss != null && (game.boss.alive || game.boss.dying)) return false;

        // Block while any player is dying on their last life
        boolean p1DyingLastLife = !game.p1.alive && game.p1.lives <= 1 && game.p1.respawnTimer > 0;
        boolean p2DyingLastLife = !game.p2.alive && game.p2.lives <= 1 && game.p2.respawnTimer > 0;

        return !(p1DyingLastLife || p2DyingLastLife);
    }

    /**
     * Writes the current game state to the save file.
     * Does nothing silently if p1/p2 are null or saving is blocked.
     * Sets game.saveFeedbackTimer on success so the HUD shows "Saved!".
     *
     * @param game the running Game instance to read state from
     */
    public static void saveGame(Game game) {
        if (game.p1 == null || game.p2 == null) return;
        if (!canSave(game)) return;

        StringBuilder data = new StringBuilder();

        // Level
        data.append(game.level).append(",");

        // Player 1 state
        data.append(game.p1.score).append(",")
            .append(game.p1.lives).append(",")
            .append(game.p1.x).append(",")
            .append(game.p1.alive ? "1" : "0").append(",")
            .append(game.p1.respawnTimer).append(",");

        // Player 2 state
        data.append(game.p2.score).append(",")
            .append(game.p2.lives).append(",")
            .append(game.p2.x).append(",")
            .append(game.p2.alive ? "1" : "0").append(",")
            .append(game.p2.respawnTimer).append(",");

        // Alien movement direction
        data.append(game.alienMoveState.alienDirX).append(",");

        // Alien grid
        for (Alien a : game.aliens) {
            data.append(a.x).append(",")
                .append(a.y).append(",")
                .append(a.alive ? "1" : "0").append(",");
        }

        // Shields
        data.append(game.shields.size).append(",");
        for (Shields s : game.shields) {
            data.append(s.x).append(",")
                .append(s.y).append(",")
                .append(s.hits).append(",")
                .append(s.alive ? "1" : "0").append(",");
        }

        try {
            FileHandle file = Gdx.files.local(SAVE_FILE);
            file.writeString(data.toString(), false);
            game.saveFeedbackTimer = 1.5f;
        } catch (Exception ignored) {}
    }

    /**
     * Reads the save file and restores game state into the given Game instance.
     * Falls back to startNewGame if the file is missing or the data looks corrupt.
     *
     * @param game the Game instance to restore state into
     */
    public static void loadGame(Game game) {
        try {
            FileHandle file = Gdx.files.local(SAVE_FILE);
            if (!file.exists()) { game.startNewGame(); return; }

            String[] parts = file.readString().trim().split(",");
            if (parts.length < 8) { game.startNewGame(); return; }

            int idx = 0;

            // Level
            game.level = Integer.parseInt(parts[idx++]);

            // Player 1
            game.p1 = new Player(1, Main.W / 2f - 120);
            game.p1.score        = Integer.parseInt(parts[idx++]);
            game.p1.lives        = Integer.parseInt(parts[idx++]);
            game.p1.x            = Float.parseFloat(parts[idx++]);
            game.p1.alive        = parts[idx++].equals("1");
            game.p1.respawnTimer = Float.parseFloat(parts[idx++]);

            // If p1 was mid-respawn, resolve it now rather than restoring a dead state
            if (!game.p1.alive) {
                if (game.p1.lives > 1) { game.p1.lives--; game.p1.alive = true; }
                else                   { game.p1.lives = 0; }
                game.p1.respawnTimer = 0f;
            }

            // Player 2
            game.p2 = new Player(2, Main.W / 2f + 120);
            game.p2.score        = Integer.parseInt(parts[idx++]);
            game.p2.lives        = Integer.parseInt(parts[idx++]);
            game.p2.x            = Float.parseFloat(parts[idx++]);
            game.p2.alive        = parts[idx++].equals("1");
            game.p2.respawnTimer = Float.parseFloat(parts[idx++]);

            // Same mid-respawn resolution for p2
            if (!game.p2.alive) {
                if (game.p2.lives > 1) { game.p2.lives--; game.p2.alive = true; }
                else                   { game.p2.lives = 0; }
                game.p2.respawnTimer = 0f;
            }

            // Save direction before initLevel so we can patch it back in after
            float savedDirX = Float.parseFloat(parts[idx++]);

            game.loaded = true;
            game.initLevel();
            game.loaded = false;

            // Patch direction back - initLevel resets it to +1
            game.alienMoveState.alienDirX = savedDirX;

            // Restore individual alien positions and alive flags
            for (int i = 0; i < game.aliens.size; i++) {
                if (idx + 2 < parts.length) {
                    Alien a = game.aliens.get(i);
                    a.x     = Float.parseFloat(parts[idx++]);
                    a.y     = Float.parseFloat(parts[idx++]);
                    a.alive = parts[idx++].equals("1");
                }
            }

            // Restore shields
            game.shields = new Array<>();
            if (idx < parts.length) {
                int shieldCount = Integer.parseInt(parts[idx++]);
                for (int i = 0; i < shieldCount; i++) {
                    if (idx + 3 < parts.length) {
                        float   sx     = Float.parseFloat(parts[idx++]);
                        float   sy     = Float.parseFloat(parts[idx++]);
                        int     shits  = Integer.parseInt(parts[idx++]);
                        boolean salive = parts[idx++].equals("1");
                        if (salive) {
                            Shields s = new Shields(sx, sy);
                            s.hits = shits;
                            game.shields.add(s);
                        }
                    }
                }
            }

            game.currentScreen = Screen.State.GAMEPLAY;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | com.badlogic.gdx.utils.GdxRuntimeException e) {
            game.startNewGame();
        }
    }
}

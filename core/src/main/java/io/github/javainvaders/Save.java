package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

/**
 * Handles persisting and restoring game state to/from a local file.
 * Format is a simple comma-separated string so theres no extra deps.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Save {

    /** File name written to the app's local storage directory. */
    private static final String SAVE_FILE = "javainvaders.sav";

    /**
     * Writes the current game state to the save file.
     * Silently does nothing if p1/p2 are null (no game running).
     * Sets game.saveFeedbackTimer on success so the HUD shows "Saved!".
     *
     * @param game the running Game instance to read state from
     */
    public static void saveGame(Game game) {
        if (game.p1 == null || game.p2 == null) return;

        StringBuilder data = new StringBuilder();
        data.append(game.level).append(",");

        data.append(game.p1.score).append(",")
            .append(game.p1.lives).append(",")
            .append(game.p1.x).append(",")
            .append(game.p1.alive ? "1" : "0").append(",")
            .append(game.p1.respawnTimer).append(",");
        
        data.append(game.p2.score).append(",")
            .append(game.p2.lives).append(",")
            .append(game.p2.x).append(",")
            .append(game.p2.alive ? "1" : "0").append(",")
            .append(game.p2.respawnTimer).append(",");

        data.append(game.alienMoveState.alienDirX).append(",");

        for (Alien a : game.aliens) {
            data.append(a.x).append(",")
                .append(a.y).append(",")
                .append(a.alive ? "1" : "0").append(",");
        }

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
     * Falls back to startNewGame if the file is missing or the data looks bad.
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
            game.level = Integer.parseInt(parts[idx++]);

            game.p1 = new Player(1, Main.W / 2f - 120);
            game.p1.score        = Integer.parseInt(parts[idx++]);
            game.p1.lives        = Integer.parseInt(parts[idx++]);
            game.p1.x            = Float.parseFloat(parts[idx++]);
            game.p1.alive        = parts[idx++].equals("1");
            game.p1.respawnTimer = Float.parseFloat(parts[idx++]);

            if (!game.p1.alive) {
                if (game.p1.lives > 1) {
                    game.p1.lives--;
                    game.p1.alive = true;
                } else {
                    game.p1.lives = 0;
                    game.p1.alive = false;
                }
                game.p1.respawnTimer = 0f;
            }

            game.p2 = new Player(2, Main.W / 2f + 120);
            game.p2.score        = Integer.parseInt(parts[idx++]);
            game.p2.lives        = Integer.parseInt(parts[idx++]);
            game.p2.x            = Float.parseFloat(parts[idx++]);
            game.p2.alive        = parts[idx++].equals("1");
            game.p2.respawnTimer = Float.parseFloat(parts[idx++]);

            if (!game.p2.alive) {
                if (game.p2.lives > 1) {
                    game.p2.lives--;
                    game.p2.alive = true;
                } else {
                    game.p2.lives = 0;
                    game.p2.alive = false;
                }
                game.p2.respawnTimer = 0f;
            }


            // restore alien direction before initLevel so it survives
            float savedDirX = Float.parseFloat(parts[idx++]);

            game.loaded = true;
            game.initLevel();
            game.loaded = false;

            // patch in direction after init
            game.alienMoveState.alienDirX = savedDirX;

            // restore individual alien positions and alive flags
            for (int i = 0; i < game.aliens.size; i++) {
                if (idx + 2 < parts.length) {
                    Alien a = game.aliens.get(i);
                    a.x     = Float.parseFloat(parts[idx++]);
                    a.y     = Float.parseFloat(parts[idx++]);
                    a.alive = parts[idx++].equals("1");
                }
            }

            // restore individual shields positions and lives
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
        } catch (Exception e) {
            game.startNewGame();
        }
    }
}

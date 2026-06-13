package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Loads game configuration from a JSON file and pushes values
 * into the relevant classes. Does nothing if "default" is true.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Config {

    /**
     * Reads config.json and applies custom values to Player, Alien,
     * SoundManager and Save. Skips everything if the default flag is set.
     */
    public static void loadFile() {
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("config.json"));

            // Skip if the file is flagged to use defaults
            if (root.getBoolean("default", true)) return;

            // Player and bullet settings
            if (root.has("Player")) {
                Player.loadConfig(root.get("Player"));
                Player.Bullet.loadConfig(root.get("Player"));
            }

            // Alien, bomb and radial bomb settings
            if (root.has("Alien")) {
                Alien.loadConfig(root.get("Alien"));
                Alien.Bomb.loadConfig(root.get("Alien"));
                Alien.RadialBomb.loadConfig(root.get("Alien"));
            }

            // Sound settings
            if (root.has("SoundManager")) {
                SoundManager.loadConfig(root.get("SoundManager"));
            }

            // Save settings
            if (root.has("Save")) {
                Save.loadConfig(root.get("Save"));
            }

        } catch (Exception e) {
            System.err.println("Error reading config.json: " + e.getMessage());
        }
    }
}

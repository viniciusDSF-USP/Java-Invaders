package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Manages game configurations by reading a JSON file.
 * 
 * @author Larissa R. G.; Vinicius S. F.
 */
public class Config {
    /**
     * It loads settings for players, aliens and save if custom values are enabled.
     */
    public static void loadFile() {
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("config.json"));

            // Verify default
            boolean useDefault = root.getBoolean("default", true);
            if (useDefault) {
                return;
            }

            // Defines consts in Player
            if (root.has("Player")) {
                Player.loadConfig(root.get("Player"));
                Player.Bullet.loadConfig(root.get("Player"));
            }

            // Defines consts in Alien
            if (root.has("Alien")) {
                Alien.loadConfig(root.get("Alien"));
                Alien.Bomb.loadConfig(root.get("Alien"));
                Alien.RadialBomb.loadConfig(root.get("Alien"));
            }

            // Defines consts in Save
            if (root.has("Save")) {
                Save.loadConfig(root.get("Save"));
            }

        } catch (Exception e) {
            System.err.println("Error on reading config.json: " + e.getMessage());
        }
    }
}
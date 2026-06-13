package io.github.javainvaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Centralises all audio: sound effects and background music.
 * Sounds are generated procedurally via PCM synthesis so no asset files
 * are required. Each buffer is written to a temporary .wav file on disk
 * (deleted on JVM exit) so libGDX can identify the format by extension.
 *
 * Call create() once during ApplicationAdapter.create(), then use the
 * play() helpers wherever game events occur. Call dispose() inside
 * ApplicationAdapter.dispose().
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class SoundManager {

    // Singleton

    /** Singleton instance so any class can reach it without passing refs. */
    private static SoundManager instance;

    /** Returns the singleton, creating it first if needed. */
    public static SoundManager get() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    private SoundManager() {}

    // Volume

    /** Master volume aplied to every generated sample. */
    private static double VOLUME = 0.2f;

    // Custom music filenames
    //
    // Place .wav (or .mp3 / .ogg) files inside the assets folder and set
    // the names below. If a file is not found, the procedurally generated
    // track is used as fallback so the game still runs.
    //
    // Example:
    //   assets/
    //     music_menu.wav
    //     music_gameplay.wav
    //     music_boss.wav

    /** Asset file for the main-menu music. Empty = use generated track. */
    private static final String MUSIC_MENU_FILE     = "music_menu.wav";

    /** Asset file for the gameplay music. Empty = use generated track. */
    private static final String MUSIC_GAMEPLAY_FILE = "music_gameplay.wav";

    /** Asset file for the boss-fight music. Empty = use generated track. */
    private static final String MUSIC_BOSS_FILE     = "music_boss.wav";

    // Sound effects

    /** Played when the player fires a bullet. */
    private Sound shootSound;

    /** Played when the player takes damage or dies. */
    private Sound playerHitSound;

    /** Played when a shield absorbs a bomb hit. */
    private Sound shieldHitSound;

    /** Played when a shield breaks. */
    private Sound shieldBreakSound;

    /** Played when any explosion occurs (alien, player or boss). */
    private Sound explosionSound;

    /** Played when the cursor moves between menu options. */
    private Sound menuSelectSound;

    // Music tracks

    /** Background music for the main menu. */
    private Music menuMusic;

    /** Background music during normal gameplay (levels 1-2). */
    private Music gameplayMusic;

    /** Background music during the boss fight. */
    private Music bossMusic;

    /** Currently playing music track. */
    private Music currentMusic;

    // PCM constants
    //
    // All audio is synthesised from simple waveforms into a short[] buffer,
    // then written to a temp .wav so libGDX can load it by extension.
    // Temp files are registered for deleteOnExit().
    //
    // WAV layout: 44-byte PCM header, mono, 44100 Hz, 16-bit signed LE.

    /** Sample rate used for all generated audio. */
    private static final int SAMPLE_RATE = 44100;

    // Lifecycle

    /**
     * Loads or generates all sounds and music tracks. Must be called from
     * the GL thread (inside ApplicationAdapter.create()).
     * Music is loaded from asset files when they exist; falls back to
     * procedural generation otherwise.
     */
    public void create() {
        shootSound       = generateShoot();
        playerHitSound   = generatePlayerHit();
        shieldHitSound   = generateShieldHit();
        shieldBreakSound = generateShieldBreak();
        explosionSound   = generateExplosion();
        menuSelectSound  = generateMenuSelect();

        menuMusic     = loadMusicOrGenerate(MUSIC_MENU_FILE,     SoundManager::generateMenuMusic);
        gameplayMusic = loadMusicOrGenerate(MUSIC_GAMEPLAY_FILE, SoundManager::generateGameplayMusic);
        bossMusic     = loadMusicOrGenerate(MUSIC_BOSS_FILE,     SoundManager::generateBossMusic);
    }

    /**
     * Loads sound config values from JSON data.
     *
     * @param config the JSON section containing sound settings
     */
    public static void loadConfig(JsonValue config) {
        if (config.has("VOLUME")) VOLUME = config.getFloat("VOLUME");
    }

    /**
     * Tries to load a music track from the assets folder.
     * Falls back to the provided generator if the file does not exist.
     *
     * @param filename  asset filename (e.g. "music_menu.wav")
     * @param generator procedural fallback, called only when file is missing
     * @return a ready-to-use Music instance
     */
    private static Music loadMusicOrGenerate(String filename,
                                             java.util.function.Supplier<Music> generator) {
        if (!filename.isEmpty() && Gdx.files.internal(filename).exists()) {
            return Gdx.audio.newMusic(Gdx.files.internal(filename));
        }
        return generator.get();
    }

    /** Releases all audio resources. */
    public void dispose() {
        shootSound.dispose();
        playerHitSound.dispose();
        shieldHitSound.dispose();
        shieldBreakSound.dispose();
        explosionSound.dispose();
        menuSelectSound.dispose();
        menuMusic.dispose();
        gameplayMusic.dispose();
        bossMusic.dispose();
    }

    // Sound playback

    /** Plays the shoot sound. */
    public void playShoot()       { if (shootSound != null) shootSound.play(0.4f); }

    /** Plays the player-hit / death sound. */
    public void playPlayerHit()   { if (playerHitSound != null) playerHitSound.play(0.7f); }

    /** Plays the shield-hit sound. */
    public void playShieldHit()   { if (shieldHitSound != null) shieldHitSound.play(0.5f); }

    /** Plays the shield-break sound. */
    public void playShieldBreak() { if (shieldBreakSound != null) shieldBreakSound.play(0.6f); }

    /** Plays the explosion sound. */
    public void playExplosion()   { if (explosionSound != null) explosionSound.play(0.5f); }

    /** Plays the menu-cursor-move sound. */
    public void playMenuSelect()  { if (menuSelectSound != null) menuSelectSound.play(0.5f); }

    // Music control

    /**
     * Switches to the menu music, stopping whatever is currently playing.
     * Safe to call repeatedley - does nothing if already on menu music.
     */
    public void playMenuMusic()     { switchMusic(menuMusic);     }

    /** Switches to the gameplay music. Safe to call repeatedley. */
    public void playGameplayMusic() { switchMusic(gameplayMusic); }

    /** Switches to the boss-fight music. Safe to call repeatedley. */
    public void playBossMusic()     { switchMusic(bossMusic);     }

    /** Stops all music immediately. */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    private void switchMusic(Music next) {
        if (currentMusic == next) return;
        if (currentMusic != null) currentMusic.stop();
        currentMusic = next;
        currentMusic.setLooping(true);
        currentMusic.setVolume(0.9f);
        currentMusic.play();
    }

    // PCM helpers

    /**
     * Writes samples to a temp .wav file and loads it as a Sound.
     * The file is deleted when the JVM exits.
     */
    private static Sound pcmToSound(short[] samples) {
        try {
            java.io.File tmp = java.io.File.createTempFile("jiSfx_", ".wav");
            tmp.deleteOnExit();
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                fos.write(buildWav(samples, 1, SAMPLE_RATE, 16));
            }
            return Gdx.audio.newSound(new com.badlogic.gdx.files.FileHandle(tmp));
        } catch (java.io.IOException e) {
            throw new com.badlogic.gdx.utils.GdxRuntimeException(
                "SoundManager: failed to create temp sound WAV", e);
        }
    }

    /**
     * Writes samples to a temp .wav file and loads it as a Music.
     * The file is deleted when the JVM exits.
     */
    private static Music pcmToMusic(short[] samples) {
        try {
            java.io.File tmp = java.io.File.createTempFile("jiMus_", ".wav");
            tmp.deleteOnExit();
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp)) {
                fos.write(buildWav(samples, 1, SAMPLE_RATE, 16));
            }
            return Gdx.audio.newMusic(new com.badlogic.gdx.files.FileHandle(tmp));
        } catch (java.io.IOException e) {
            throw new com.badlogic.gdx.utils.GdxRuntimeException(
                "SoundManager: failed to create temp music WAV", e);
        }
    }

    /** Builds a standard 44-byte PCM WAV header followed by the raw sample bytes. */
    private static byte[] buildWav(short[] samples, int channels, int sampleRate, int bitsPerSample) {
        int dataLen  = samples.length * 2;
        int totalLen = 36 + dataLen;
        byte[] buf   = new byte[44 + dataLen];
        int i = 0;
        // RIFF chunk descriptor
        buf[i++]='R'; buf[i++]='I'; buf[i++]='F'; buf[i++]='F';
        writeInt(buf, i, totalLen); i += 4;
        buf[i++]='W'; buf[i++]='A'; buf[i++]='V'; buf[i++]='E';
        // fmt sub-chunk
        buf[i++]='f'; buf[i++]='m'; buf[i++]='t'; buf[i++]=' ';
        writeInt(buf, i, 16); i += 4;
        writeShort(buf, i, (short) 1); i += 2;                          // PCM
        writeShort(buf, i, (short) channels); i += 2;
        writeInt(buf, i, sampleRate); i += 4;
        writeInt(buf, i, sampleRate * channels * bitsPerSample / 8); i += 4;
        writeShort(buf, i, (short)(channels * bitsPerSample / 8)); i += 2;
        writeShort(buf, i, (short) bitsPerSample); i += 2;
        // data sub-chunk
        buf[i++]='d'; buf[i++]='a'; buf[i++]='t'; buf[i++]='a';
        writeInt(buf, i, dataLen); i += 4;
        for (short s : samples) {
            buf[i++] = (byte)(s & 0xFF);
            buf[i++] = (byte)((s >> 8) & 0xFF);
        }
        return buf;
    }

    private static void writeInt(byte[] b, int off, int v) {
        b[off]   = (byte) v;
        b[off+1] = (byte)(v >> 8);
        b[off+2] = (byte)(v >> 16);
        b[off+3] = (byte)(v >> 24);
    }

    private static void writeShort(byte[] b, int off, short v) {
        b[off]   = (byte) v;
        b[off+1] = (byte)(v >> 8);
    }

    /** Clamps f to [-1, 1] and scales to a signed 16-bit sample. */
    private static short f2s(float f) {
        f = Math.max(-1f, Math.min(1f, f)) * (float) VOLUME;
        return (short)(f * 32767f);
    }

    // Sound generators

    /**
     * Short rising blip - laser-gun feel.
     * Square wave, pitch sweeps 400 Hz to 900 Hz over 0.08 s.
     */
    private static Sound generateShoot() {
        int len = (int)(SAMPLE_RATE * 0.08f);
        short[] s = new short[len];
        for (int i = 0; i < len; i++) {
            float t    = (float) i / SAMPLE_RATE;
            float freq = 400f + 500f * (t / 0.08f);
            float env  = 1f - (t / 0.08f);
            float wave = (Math.sin(2 * Math.PI * freq * t) > 0) ? 1f : -1f;
            s[i] = f2s(wave * env * 0.6f);
        }
        return pcmToSound(s);
    }

    /**
     * Low rumble and noise burst - conveys impact and death.
     * White noise plus descending sine, 0.4 s.
     */
    private static Sound generatePlayerHit() {
        int len = (int)(SAMPLE_RATE * 0.4f);
        short[] s = new short[len];
        java.util.Random rng = new java.util.Random(1L);
        for (int i = 0; i < len; i++) {
            float t     = (float) i / SAMPLE_RATE;
            float env   = (float) Math.exp(-t * 6f);
            float noise = rng.nextFloat() * 2f - 1f;
            float tone  = (float) Math.sin(2 * Math.PI * (200f - 100f * t / 0.4f) * t);
            s[i] = f2s((noise * 0.5f + tone * 0.5f) * env);
        }
        return pcmToSound(s);
    }

    /**
     * Mid thud - shield absorbs a hit.
     * Short sawtooth at 180 Hz, 0.12 s.
     */
    private static Sound generateShieldHit() {
        int len = (int)(SAMPLE_RATE * 0.12f);
        short[] s = new short[len];
        for (int i = 0; i < len; i++) {
            float t   = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 20f);
            float saw = 2f * ((t * 180f) % 1f) - 1f;
            s[i] = f2s(saw * env * 0.7f);
        }
        return pcmToSound(s);
    }

    /**
     * Crunch and falling pitch - shield shatters.
     * Noise with exponential decay plus descending tone, 0.35 s.
     */
    private static Sound generateShieldBreak() {
        int len = (int)(SAMPLE_RATE * 0.35f);
        short[] s = new short[len];
        java.util.Random rng = new java.util.Random(42L);
        for (int i = 0; i < len; i++) {
            float t     = (float) i / SAMPLE_RATE;
            float env   = (float) Math.exp(-t * 9f);
            float noise = rng.nextFloat() * 2f - 1f;
            float freq  = 300f - 200f * (t / 0.35f);
            float tone  = (float) Math.sin(2 * Math.PI * freq * t);
            s[i] = f2s((noise * 0.6f + tone * 0.4f) * env);
        }
        return pcmToSound(s);
    }

    /**
     * Boomy low-frequency burst - classic arcade explosion.
     * Noise plus sub-bass sine, 0.6 s.
     */
    private static Sound generateExplosion() {
        int len = (int)(SAMPLE_RATE * 0.6f);
        short[] s = new short[len];
        java.util.Random rng = new java.util.Random(13L);
        for (int i = 0; i < len; i++) {
            float t = (float) i / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 12f);
            float freq = 60f * (1f - t * 2f);
            if (freq < 20f) freq = 20f;
            float phase  = (freq * t) % 1f;
            float square = phase < 0.5f ? 1f : -1f;
            float noise  = rng.nextBoolean() ? 1f : -1f;
            float sample = square * env * 0.5f + noise * env * 0.7f;
            sample = Math.round(sample * 8f) / 8f;
            s[i] = f2s(sample);
        }
        return pcmToSound(s);
    }

    /**
     * Quick high-pitched blip pair - menu cursor movement.
     * Two-tone: 880 Hz then 1100 Hz, 0.06 s each.
     */
    private static Sound generateMenuSelect() {
        int len = (int)(SAMPLE_RATE * 0.06f);
        short[] s = new short[len];
        for (int i = 0; i < len; i++) {
            float t    = (float) i / SAMPLE_RATE;
            float env  = (float) Math.exp(-t * 30f);
            float freq = (i < len / 2) ? 880f : 1100f;
            float wave = (float) Math.sin(2 * Math.PI * freq * t);
            s[i] = f2s(wave * env * 0.5f);
        }
        return pcmToSound(s);
    }

    // Music generators
    //
    // Music is built as a long PCM buffer using a tiny step-sequencer.
    // Notes are (frequency_Hz, duration_seconds) pairs; freq=0 means rest.

    /** Synthesises one note into dst starting at offset and returns the next offset. */
    private static int addNote(short[] dst, int offset, float freq, float dur,
                               float vol, boolean square) {
        int samples = (int)(SAMPLE_RATE * dur);
        for (int i = 0; i < samples && offset + i < dst.length; i++) {
            float t    = (float) i / SAMPLE_RATE;
            float env  = (float) Math.exp(-t * (1.5f / dur));
            float wave = square
                ? (Math.sin(2 * Math.PI * freq * t) > 0 ? 1f : -1f)
                : (float) Math.sin(2 * Math.PI * freq * t);
            dst[offset + i] = f2s(wave * env * vol);
        }
        return offset + samples;
    }

    /**
     * Arpeggiated chiptune melody - bouncy space-game menu feel.
     * Four-bar loop in A minor.
     */
    private static Music generateMenuMusic() {
        float[][] melody = {
            {523.3f, 0.15f}, {392.0f, 0.15f}, {329.6f, 0.15f}, {392.0f, 0.15f},
            {523.3f, 0.20f}, {659.3f, 0.20f}, {783.9f, 0.40f}, {0, 0.20f},
            {349.2f, 0.15f}, {440.0f, 0.15f}, {523.3f, 0.15f}, {440.0f, 0.15f},
            {392.0f, 0.40f}, {329.6f, 0.40f}, {261.6f, 0.40f}, {0, 0.40f}
        };
        float totalDur = 0;
        for (float[] n : melody) totalDur += n[1];
        short[] pcm = new short[(int)(SAMPLE_RATE * totalDur)];
        int off = 0;
        for (float[] n : melody) {
            if (n[0] > 0) off = addNote(pcm, off, n[0], n[1], 0.3f, true);
            else          off += (int)(SAMPLE_RATE * n[1]);
        }
        return pcmToMusic(pcm);
    }

    /**
     * Driving pulse-wave bass line with a secondary melody - tense gameplay feel.
     */
    private static Music generateGameplayMusic() {
        float[][] bass = {
            {65.4f, 0.2f}, {65.4f, 0.2f}, {77.8f, 0.2f}, {65.4f, 0.2f},
            {87.3f, 0.2f}, {65.4f, 0.2f}, {98.0f, 0.2f}, {87.3f, 0.2f},
            {55.0f, 0.2f}, {55.0f, 0.2f}, {65.4f, 0.2f}, {55.0f, 0.2f},
            {73.4f, 0.2f}, {55.0f, 0.2f}, {82.4f, 0.2f}, {73.4f, 0.2f}
        };
        float[][] lead = {
            {0, 0.4f}, {523.3f, 0.2f}, {392.0f, 0.2f}, {311.1f, 0.4f},
            {0, 0.4f}, {392.0f, 0.2f}, {523.3f, 0.2f}, {587.3f, 0.6f},
            {0, 0.6f}
        };
        float totalBass = 0; for (float[] n : bass) totalBass += n[1];
        float totalLead = 0; for (float[] n : lead) totalLead += n[1];
        int totalSamples = (int)(SAMPLE_RATE * Math.max(totalBass, totalLead));
        short[] pcm = new short[totalSamples];

        // Layer bass (square wave)
        int off = 0;
        for (float[] n : bass) {
            if (n[0] > 0) {
                int len = (int)(SAMPLE_RATE * n[1]);
                for (int i = 0; i < len && off + i < pcm.length; i++) {
                    float t    = (float) i / SAMPLE_RATE;
                    float env  = (float) Math.exp(-t * 3f);
                    float wave = (Math.sin(2 * Math.PI * n[0] * t) > 0) ? 1f : -1f;
                    pcm[off + i] = f2s(wave * env * 0.25f);
                }
                off += len;
            } else {
                off += (int)(SAMPLE_RATE * n[1]);
            }
        }

        // Mix lead (sine) on top
        off = 0;
        for (float[] n : lead) {
            if (n[0] > 0) {
                int len = (int)(SAMPLE_RATE * n[1]);
                for (int i = 0; i < len && off + i < pcm.length; i++) {
                    float t    = (float) i / SAMPLE_RATE;
                    float env  = (float) Math.exp(-t * (2f / n[1]));
                    float wave = (float) Math.sin(2 * Math.PI * n[0] * t);
                    float cur  = pcm[off + i] / 32767f;
                    pcm[off + i] = f2s(cur + wave * env * 0.2f);
                }
                off += len;
            } else {
                off += (int)(SAMPLE_RATE * n[1]);
            }
        }
        return pcmToMusic(pcm);
    }

    /**
     * Heavy descending ostinato - ominous boss-fight feel.
     * Low square bass with a dissonant tritone melody.
     */
    private static Music generateBossMusic() {
        float[][] bass = {
            {82.4f, 0.5f}, {0, 0.1f}, {82.4f, 0.2f}, {77.8f, 0.8f},
            {73.4f, 0.5f}, {0, 0.1f}, {73.4f, 0.2f}, {65.4f, 0.8f}
        };
        float[][] lead = {
            {659.3f, 0.4f}, {622.3f, 0.4f}, {466.2f, 0.6f}, {0, 0.2f},
            {587.3f, 0.4f}, {554.4f, 0.4f}, {415.3f, 0.6f}, {0, 0.2f}
        };
        float totalBass = 0; for (float[] n : bass) totalBass += n[1];
        float totalLead = 0; for (float[] n : lead) totalLead += n[1];
        int totalSamples = (int)(SAMPLE_RATE * Math.max(totalBass, totalLead));
        short[] pcm = new short[totalSamples];

        // Layer bass (square wave)
        int off = 0;
        for (float[] n : bass) {
            int len = (int)(SAMPLE_RATE * n[1]);
            for (int i = 0; i < len && off + i < pcm.length; i++) {
                float t    = (float) i / SAMPLE_RATE;
                float wave = (Math.sin(2 * Math.PI * n[0] * t) > 0) ? 1f : -1f;
                pcm[off + i] = f2s(wave * 0.3f);
            }
            off += len;
        }

        // Mix lead (sine) on top
        off = 0;
        for (float[] n : lead) {
            if (n[0] > 0) {
                int len = (int)(SAMPLE_RATE * n[1]);
                for (int i = 0; i < len && off + i < pcm.length; i++) {
                    float t    = (float) i / SAMPLE_RATE;
                    float env  = (float) Math.exp(-t * (1.5f / n[1]));
                    float wave = (float) Math.sin(2 * Math.PI * n[0] * t);
                    float cur  = pcm[off + i] / 32767f;
                    pcm[off + i] = f2s(cur + wave * env * 0.22f);
                }
                off += len;
            } else {
                off += (int)(SAMPLE_RATE * n[1]);
            }
        }
        return pcmToMusic(pcm);
    }
}

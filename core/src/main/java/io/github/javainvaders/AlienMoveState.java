package io.github.javainvaders;

// Movement state helper

/**
 * Tiny bag of mutable alien movement state. Kept separate so Game
 * doesnt have to expose raw fields and Alien.moveAliens can mutate it cleanly.
 * 
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienMoveState {

    /** Current horizontal movement direction, +1 or -1. */
    public float alienDirX;

    /** True when aliens should drop down on the next tick. */
    public boolean alienDescend;

    /**
     * Creates the state with an initial direction.
     *
     * @param alienDirX starting dir, usually +1
     */
    public AlienMoveState(float alienDirX) {
        this.alienDirX = alienDirX;
        this.alienDescend = false;
    }
}
package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for AlienMoveState.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienMoveStateTest {

    // Constructor tests

    /**
     * Direction should be stored and descend should default to false.
     */
    @Test
    public void constructor_setsDirectionAndDescendFalse() {
        AlienMoveState state = new AlienMoveState(1f);
        assertEquals(1f, state.alienDirX, 0.001f);
        assertFalse("alienDescend must start as false", state.alienDescend);
    }

    /**
     * Constructor should also accept a negative direction value.
     */
    @Test
    public void constructor_acceptsNegativeDirection() {
        AlienMoveState state = new AlienMoveState(-1f);
        assertEquals(-1f, state.alienDirX, 0.001f);
        assertFalse(state.alienDescend);
    }

    // Mutation tests

    /**
     * Public fields should be directly writable after construction.
     */
    @Test
    public void fields_areMutableDirectly() {
        AlienMoveState state = new AlienMoveState(1f);

        // Flip direction and trigger descend
        state.alienDirX    = -1f;
        state.alienDescend = true;

        assertEquals(-1f, state.alienDirX, 0.001f);
        assertTrue(state.alienDescend);
    }
}

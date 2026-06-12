package io.github.javainvaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit tests for {@link AlienMoveState}.
 *
 * @author Larissa R. G.; Vinicius S. F.
 */
public class AlienMoveStateTest {

    // constructor

    @Test
    public void constructor_setsDirectionAndDescendFalse() {
        AlienMoveState state = new AlienMoveState(1f);

        assertEquals(1f, state.alienDirX, 0.001f);
        assertFalse("alienDescend must start as false", state.alienDescend);
    }

    @Test
    public void constructor_acceptsNegativeDirection() {
        AlienMoveState state = new AlienMoveState(-1f);

        assertEquals(-1f, state.alienDirX, 0.001f);
        assertFalse(state.alienDescend);
    }

    // mutation

    @Test
    public void fields_areMutableDirectly() {
        AlienMoveState state = new AlienMoveState(1f);
        state.alienDirX   = -1f;
        state.alienDescend = true;

        assertEquals(-1f, state.alienDirX, 0.001f);
        assertTrue(state.alienDescend);
    }
}

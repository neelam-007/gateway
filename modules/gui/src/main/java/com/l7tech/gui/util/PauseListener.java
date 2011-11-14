package com.l7tech.gui.util;

import javax.swing.text.JTextComponent;

/**
 * @author alex
 * @version $Revision$
 */
public interface PauseListener {
    /**
     * Invoked on the swing thread.
     *
     * @param component Registered component
     * @param msecs how long since last pause event
     */
    void textEntryPaused( JTextComponent component, long msecs );

    /**
     * Invoked on the swing thread.
     *
     * @param component Registered component.
     */
    void textEntryResumed( JTextComponent component );
}

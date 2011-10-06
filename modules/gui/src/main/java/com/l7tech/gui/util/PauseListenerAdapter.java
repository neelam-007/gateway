package com.l7tech.gui.util;

import javax.swing.text.JTextComponent;

/**
 * Adapter for PauseListener implementations
 */
public abstract class PauseListenerAdapter implements PauseListener {
    @Override
    public void textEntryPaused( final JTextComponent component, final long msecs ) {
    }

    @Override
    public void textEntryResumed( final JTextComponent component ) {
    }
}

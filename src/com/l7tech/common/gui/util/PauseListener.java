package com.l7tech.common.gui.util;

import javax.swing.text.JTextComponent;

/**
 * @author alex
 * @version $Revision$
 */
public interface PauseListener {
    void textEntryPaused( JTextComponent component, long msecs );
    void textEntryResumed( JTextComponent component );
}

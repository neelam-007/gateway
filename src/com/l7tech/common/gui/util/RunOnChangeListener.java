package com.l7tech.common.gui.util;

import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeListener;

/**
 * Utility listener that runs the given Runnable whenever it receives a change event.
 *
 * @author $Author$
 * @version $Revision$
 */
public class RunOnChangeListener implements ChangeListener, DocumentListener {

    //- PUBLIC

    /**
     *
     */
    public RunOnChangeListener(Runnable runme) {
        if(runme==null) throw new NullPointerException("runme parameter must not be null");
        this.runnable = runme;
    }

    /**
     * @see ChangeListener
     */
    public void stateChanged(ChangeEvent e) {
        runnable.run();
    }

    /**
     * @see DocumentListener
     */
    public void changedUpdate(DocumentEvent e) {
        runnable.run();
    }

    /**
     * @see DocumentListener
     */
    public void insertUpdate(DocumentEvent e) {
        runnable.run();
    }

    /**
     * @see DocumentListener
     */
    public void removeUpdate(DocumentEvent e) {
        runnable.run();
    }

    //- PRIVATE

    private final Runnable runnable;
}

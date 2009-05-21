package com.l7tech.gui.util;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * Utility listener that runs the given Runnable whenever it receives a change event.
 *
 * @author $Author$
 * @version $Revision$
 */
public class RunOnChangeListener implements ActionListener, ChangeListener, DocumentListener, ItemListener, ListSelectionListener {

    //- PUBLIC

    /**
     *
     */
    public RunOnChangeListener(Runnable runme) {
        if(runme==null) throw new NullPointerException("runme parameter must not be null");
        this.runnable = runme;
    }

    /**
     * @see ActionListener
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        runnable.run();
    }

    /**
     * @see ChangeListener
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        runnable.run();
    }

    /**
     * @see DocumentListener
     */
    @Override
    public void changedUpdate(DocumentEvent e) {
        runnable.run();
    }

    /**
     * @see DocumentListener
     */
    @Override
    public void insertUpdate(DocumentEvent e) {
        runnable.run();
    }

    /**
     * @see DocumentListener
     */
    @Override
    public void removeUpdate(DocumentEvent e) {
        runnable.run();
    }

    /**
     * @see ItemListener
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        runnable.run();
    }

    /**
     * @see ListSelectionListener
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        runnable.run();
    }

    //- PRIVATE

    private final Runnable runnable;
}

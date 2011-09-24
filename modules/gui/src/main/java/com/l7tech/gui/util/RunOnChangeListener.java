package com.l7tech.gui.util;

import javax.swing.event.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Utility listener that runs the given Runnable whenever it receives a change event.
 */
public class RunOnChangeListener implements ActionListener, ChangeListener, DocumentListener, ItemListener, ListSelectionListener, TableModelListener, PropertyChangeListener {

    //- PUBLIC

    public RunOnChangeListener() {
        this.runnable = null;
    }

    /**
     *
     */
    public RunOnChangeListener( final Runnable runme ) {
        if(runme==null) throw new NullPointerException("runme parameter must not be null");
        this.runnable = runme;
    }

    /**
     * @see ActionListener
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        run();
    }

    /**
     * @see ChangeListener
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        run();
    }

    /**
     * @see DocumentListener
     */
    @Override
    public void changedUpdate(DocumentEvent e) {
        run();
    }

    /**
     * @see DocumentListener
     */
    @Override
    public void insertUpdate(DocumentEvent e) {
        run();
    }

    /**
     * @see DocumentListener
     */
    @Override
    public void removeUpdate(DocumentEvent e) {
        run();
    }

    /**
     * @see ItemListener
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        run();
    }

    /**
     * @see ListSelectionListener
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        run();
    }

    /**
     * @see TableModelListener
     */
    @Override
    public void tableChanged( final TableModelEvent e ) {
        run();
    }

    /**
     * @see PropertyChangeListener
     */
    @Override
    public void propertyChange( PropertyChangeEvent e ) {
        run();
    }

    //- PROTECTED

    protected void run() {
        if ( runnable != null ) runnable.run();
    }

    //- PRIVATE

    private final Runnable runnable;
}

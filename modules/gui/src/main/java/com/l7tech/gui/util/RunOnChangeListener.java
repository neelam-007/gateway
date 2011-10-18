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

    /**
     * Create a run on change listener wihout a runnable.
     *
     * <p>When using the constructor the <code>run</code> method is typically
     * overridden.</p>
     *
     * @see #run
     */
    public RunOnChangeListener() {
        this.runnable = null;
    }

    /**
     * Create a run on change listener that invokes the given runnable.
     *
     * <p>NOTE: Rather than creating a Runnable instance and a RunOnChangeListener
     * you may want to use the no-arg constructor and override the <code>run</code>
     * method.</p>
     *
     * @param runme The runnable to run.
     * @see #run
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

    /**
     * Perform the action associated with this run on change listener.
     *
     * <p>This implementation invokes the wrapped Runnable (if any)</p>
     */
    protected void run() {
        if ( runnable != null ) runnable.run();
    }

    //- PRIVATE

    private final Runnable runnable;
}

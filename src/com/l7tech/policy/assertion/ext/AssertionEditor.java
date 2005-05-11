package com.l7tech.policy.assertion.ext;

/**
 * The <code>AssertionEditor</code>  support specific assertion
 * editing.
 * The editor for a given assertion is obtained by invoking 
 * {@link CustomAssertionUI#getEditor(CustomAssertion)}.
 *
 * <code>AssertionEditor</code> implementations are typically
 * UI Dialogs or Frames, or they may invoke/delegate editing to the
 * UI elements they contain.
 * The <code>AssertionEditor</code> caller (management console
 * for example) communicates with the editor using {@link EditListener}.
 *
 * @see CustomAssertionUI
 * @see EditListener
 *
 * @author emil
 * @version 2-May-2005
 */
public interface AssertionEditor {

    /**
     * Invoke assertion editor
     */
    public void edit();

    /**
     * Adds the editor listener to the list
     *
     * @param listener the edit listener
     */
    public void addEditListener(EditListener listener);

    /**
     * Removes the edit listener from the list
     *
     * @param listener the edit listener
     */
    public void removeEditListener(EditListener listener);
}

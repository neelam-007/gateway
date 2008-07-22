package com.l7tech.policy.assertion.ext;

/**
 * The <code>AssertionEditor</code>  supports a custom GUI for configuring a custom assertion.
 * <p/>
 * The editor for a given assertion is obtained by invoking 
 * {@link CustomAssertionUI#getEditor(CustomAssertion)}.
 * <p/>
 * <code>AssertionEditor</code> implementations are typically
 * UI Dialogs or Frames, or they may invoke/delegate editing to the
 * UI elements they contain.
 * The <code>AssertionEditor</code> caller (SecureSpan Manager
 * for example) communicates with the editor using {@link EditListener}.
 * <p/>
 * If no custom editor is provided, the SecureSPan Manager will generate a simple
 * property editor GUI showing the get/set properties in the concrete {@link CustomAssertion}.
 *
 * @see CustomAssertionUI
 * @see EditListener
 *
 * @author emil
 * @version 2-May-2005
 */
public interface AssertionEditor {

    /**
     * Invoke assertion editor.
     */
    public void edit();

    /**
     * Adds the editor listener to the list.
     *
     * @param listener the edit listener
     */
    public void addEditListener(EditListener listener);

    /**
     * Removes the edit listener from the list.
     *
     * @param listener the edit listener
     */
    public void removeEditListener(EditListener listener);
}

package com.l7tech.policy.assertion.ext;

import javax.swing.*;

/**
 * <code>CustomAssertionUI</code> implementations are specified in the
 * custom assertion configuration and they support specific custom
 * assertion editors and icons.
 * The management console (ssm) may query for <code>CustomAssertionUI</code>
 * for an assertion during runtime and will use it to edit the give assertion.
 * This allows for pluggable UI components to be deployed and configured on the
 * server, and dynamically downloaded on demand by the management console. 
 *
 * @see AssertionEditor
 *
 * @author emil
 * @version 3-May-2005
 */
public interface CustomAssertionUI {
    /**
     * Returns the <code>AssertionEditor</code> for the given
     * assertion or <b>null</b> if the editor is not available.
     *
     * @param assertion the assertion to retreive the editor for
     * @return  the assertion editor or <b>null</b> if no editor is configured
     */
    AssertionEditor getEditor(CustomAssertion assertion);

    /**
     * Returns the small icon for the assertion or <b>null</b> if no icon is
     * available
     *
     * @return the assertion small icon, or <b>null</b>
     */
    ImageIcon getSmallIcon();

    /**
     * Returns the large icon for the assertion or <b>null</b> if no icon is
     * available
     *
     * @return the assertion large icon, or <b>null</b>
     */
    ImageIcon getLargeIcon();
}

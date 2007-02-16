package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;

/**
 * Interface implemented by modular assertion properties dialogs that will be invoked by
 * DefaultAssertionPropertiesAction.  Implementors must have a nullary constructor.
 */
public interface AssertionPropertiesEditor<AT extends Assertion> {
    /**
     * Get a dialog instance, ready for display with DialogDisplayer.
     * This dialog should be modal and must dispose() itself immediately when dismissed (Ok, Cancel, or closebox).
     * <p/>
     * In the common case where the editor dialog is in fact the APE itself, this method would just
     * return "this".
     *
     * @return a JDialog instance ready to be passed to DialogDisplayer.  Never null.
     */
    JDialog getDialog();

    /**
     * Check if this AssertionPropertiesEditor instance has been dismissed with the Ok button (or equivalent).
     *
     * @return false if this instance has not yet been displayed, has been displayed but has not yet been dismissed,
     *               or has been closed or canceled without confirmation.
     *         true only if this instance has been confirmed with the Ok button (or equivalent), and it is now safe
     *               to call {@link #getData} to retrieve the edited bean properties.
     */
    boolean isConfirmed();

    /**
     * Configure the view with the data from the specified assertion bean.
     * This call should immediately configure all the editor widgets, before returning.
     *
     * @param assertion the assertion bean that provides that data that should be copied into the view.  Must not be null.
     */
    void setData(AT assertion);

    /**
     * Copy the data out of the view into an assertion bean instance.
     * The provided bean should be filled and returned, if possible, but impelementors may create and return
     * a new bean instead, if they must.
     *
     * @param assertion a bean to which the data from the view can be copied, if possible.  Must not be null.
     * @return a possibly-new assertion bean populated with data from the view.  Not necessarily the same bean that was passed in.
     *         Never null.
     */
    AT getData(AT assertion);
}

package com.l7tech.console.panels;

import com.l7tech.console.policy.PolicyPositionAware;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;

/**
 * This is a temporary solution for all property dialogs which arn't a subclass of AssertionPropertiesOkCancelSupport
 * , so that they can use meta data to populate the dialogs title
 * Introducing this simple class so that I don't need to rework each assertion right now to get them using meta
 * data for their dialog titles
 * New Assertions should <b>NOT</b> use this class but should use AssertionPropertiesOkCancelSupport or a subclass.
 * This class however needs to be updated to use meta data, see it's DefaultAssertionPropertiesEditor implementation
 * where it passes meta data to it's super, class AssertionPropertiesOkCancelSupport could just extract this info itself
 *
 * User: darmstrong
 * Date: Sep 3, 2009
 * Time: 11:43:18 AM
 */
public class LegacyAssertionPropertyDialog extends JDialog implements PolicyPositionAware {

    //- PUBLIC

    public LegacyAssertionPropertyDialog(final Frame owner, final Assertion assertion, final boolean modal) {
        super(owner, modal);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        setTitle(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        Utilities.setEscKeyStrokeDisposes( this );
    }

    @Override
    public PolicyPosition getPolicyPosition() {
        return policyPosition;
    }

    @Override
    public void setPolicyPosition( final PolicyPosition policyPosition ) {
        this.policyPosition = policyPosition;
    }

    //- PROTECTED

    /**
     * Get the assertion prior to this assertion in the policy.
     *
     * <p>This will only be available when adding an assertion to the policy.</p>
     *
     * @return The previous assertion (null if there is none)
     */
    @SuppressWarnings({ "unchecked" })
    protected Assertion getPreviousAssertion() {
        return policyPosition==null ? null : policyPosition.getPreviousAssertion();
    }

    //- PRIVATE

    private PolicyPosition policyPosition;
}

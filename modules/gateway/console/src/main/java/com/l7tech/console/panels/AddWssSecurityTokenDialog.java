package com.l7tech.console.panels;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;

import java.awt.*;

/**
 * Properties dialog for "Add Security Token" assertion.
 */
public class AddWssSecurityTokenDialog extends AssertionOkCancelDialog<AddWssSecurityToken> {
    public AddWssSecurityTokenDialog(Frame owner, AddWssSecurityToken assertion, boolean readOnly) {
        super(owner, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString(), true, new AddWssSecurityTokenPanel(assertion), readOnly, assertion);
    }
}

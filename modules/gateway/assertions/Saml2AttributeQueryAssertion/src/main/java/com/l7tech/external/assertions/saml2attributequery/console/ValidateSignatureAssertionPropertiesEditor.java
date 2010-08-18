package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.external.assertions.saml2attributequery.ValidateSignatureAssertion;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.AssertionPropertiesEditor;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 8:21:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class ValidateSignatureAssertionPropertiesEditor implements AssertionPropertiesEditor<ValidateSignatureAssertion> {
    protected boolean readOnly;
    private ValidateSignatureAssertion assertion;
    private ValidateSignatureAssertionPropertiesDialog dialog;

    public ValidateSignatureAssertionPropertiesEditor(ValidateSignatureAssertion assertion) {
        this.assertion = assertion;
        createDialog();
    }

    public boolean isConfirmed() {
        if(dialog == null) {
            return false;
        } else {
            return dialog.isConfirmed();
        }
    }

    public ValidateSignatureAssertion getData(ValidateSignatureAssertion ass) {
        ass.setVariableName(assertion.getVariableName());

        return ass;
    }

    public void setData(ValidateSignatureAssertion ass) {
        assertion = ass;

        createDialog();
    }

    public JDialog getDialog() {
        return dialog;
    }

    public Object getParameter( final String name ) {
        Object value = null;

        if ( PARAM_READONLY.equals( name )) {
            value = readOnly;
        }

        return value;
    }

    public void setParameter( final String name, Object value ) {
        if ( PARAM_READONLY.equals( name ) && value instanceof Boolean ) {
            readOnly = (Boolean) value;
        }
    }

    private void createDialog() {
        Frame mw = TopComponents.getInstance().getTopParent();
        dialog = new ValidateSignatureAssertionPropertiesDialog(mw, true, assertion, readOnly);
    }
}
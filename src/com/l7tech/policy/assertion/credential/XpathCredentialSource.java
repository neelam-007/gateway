package com.l7tech.policy.assertion.credential;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.XpathBasedAssertion;

/**
 * Gathers credentials based on XPath expressions pointing to the login and password.
 *
 * The superclass's {@link #xpathExpression} is used to hold the XPath pointing to
 * the login.
 *
 * @author alex
 */
public class XpathCredentialSource extends XpathBasedAssertion {
    private boolean removeLoginElement;
    private boolean removePasswordElement;
    private XpathExpression passwordExpression;

    public boolean isCredentialSource() {
        return true;
    }

    public XpathExpression getPasswordExpression() {
        return passwordExpression;
    }

    public void setPasswordExpression(XpathExpression passwordExpression) {
        this.passwordExpression = passwordExpression;
    }

    public boolean isRemoveLoginElement() {
        return removeLoginElement;
    }

    public void setRemoveLoginElement(boolean removeLoginElement) {
        this.removeLoginElement = removeLoginElement;
    }

    public boolean isRemovePasswordElement() {
        return removePasswordElement;
    }

    public void setRemovePasswordElement(boolean removePasswordElement) {
        this.removePasswordElement = removePasswordElement;
    }

}

/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.message.MessageElement;

import javax.xml.soap.SOAPException;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientWssBasic extends ClientWssCredentialSource implements ClientAssertion {
    public ClientWssBasic( WssBasic data ) {
        this.data = data;
    }

    /**
     * Decorate the xml soap message with a WSS header containing the username and password.
     *
     * @param request
     * @return
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        SOAPHeaderElement secHeader = null;
        try {
            secHeader = getSecurityElement(request);
        } catch (SOAPException e) {
            throw new PolicyAssertionException(e.getMessage(), e);
        }
        // check whether the UsernameToken is not already there
        MessageElement usernametokenelement = null;
        for (Iterator i = secHeader.getChildElements();i.hasNext();) {
            MessageElement el = (MessageElement)i.next();
            if (el.getName().equals(USERNAME_TOKEN_ELEMENT_NAME)) {
                usernametokenelement = el;
            }
        }
        // if not present, add it to security header
        if (usernametokenelement == null) {
            usernametokenelement = new MessageElement(SECURITY_NAMESPACE, USERNAME_TOKEN_ELEMENT_NAME);
            try {
                secHeader.addChild(usernametokenelement);
            } catch (SOAPException e) {
                throw new PolicyAssertionException(e.getMessage(), e);
            }
        }
        else {
            // the usernametoken element is already there. what should i do?
            // todo, what should the correct course of action be here?
            return AssertionStatus.NOT_APPLICABLE;
        }

        // get the username and passwords
        String username = request.getSsg().getUsername();
        char[] password = request.getSsg().password();
        if (username == null || password == null || username.length() < 1) {
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.AUTH_REQUIRED;
        }

        // add username and password to the usernametoken element
        try {
            MessageElement usernameElement = new MessageElement(SECURITY_NAMESPACE, "Username");
            usernameElement.addTextNode(username);
            MessageElement passwordElement = new MessageElement(SECURITY_NAMESPACE, "Password");
            passwordElement.addTextNode(new String(password));
            usernametokenelement.addChild(usernameElement);
            usernametokenelement.addChild(passwordElement);
        } catch (SOAPException e) {
            throw new PolicyAssertionException(e.getMessage(), e);
        }
        return AssertionStatus.NONE;
    }

    protected static final String USERNAME_TOKEN_ELEMENT_NAME = "UsernameToken";

    protected WssBasic data;
}

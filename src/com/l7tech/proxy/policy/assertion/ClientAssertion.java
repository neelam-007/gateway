/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ResponseValidationException;
import com.l7tech.proxy.message.PolicyApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientAssertion implements ClientDecorator {

    /**
     * ClientProxy clinet-side processing of the given response.
     * @param context
     * @return AssertionStatus.NONE if this Assertion was applied to the response successfully; otherwise, some error conde
     * @throws PolicyAssertionException if the policy was invalid
     */
    public abstract AssertionStatus unDecorateReply(PolicyApplicationContext context)
            throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException,
            SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException;

    /**
     * @return the human-readable node name that is displayed.
     */
    public abstract String getName();

    /**
     * subclasses override this method specifying the resource name of the
     * icon to use when this assertion is displayed in the tree view.
     *
     * @param open for nodes that can be opened, can have children
     * @return a string such as "com/l7tech/proxy/resources/tree/assertion.png"
     */
    public abstract String iconResource(boolean open);
}

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is the tree node corresponding to the RequestWssX509Cert assertion type.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class RequestWssX509CertTreeNode extends LeafAssertionTreeNode {
    private RequestWssX509Cert data;
    public RequestWssX509CertTreeNode(RequestWssX509Cert assertion) {
        super(assertion);
        data = assertion;
    }

    public String getName() {
        return "WSS Sign SOAP Request" + SecurityHeaderAddressableSupport.getActorSuffix(data);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }

    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(new EditXmlSecurityRecipientContextAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }
}

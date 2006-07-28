package com.l7tech.console.tree.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

import com.l7tech.console.action.EditWsFederationPassiveTokenRequestAction;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.Assertion;

/**
 * Tree node for WsFederationPassiveTokenRequest ....
 *
 * @author $Author$
 * @version: $Revision$
 */
public class WsFederationPassiveTokenRequestTreeNode extends LeafAssertionTreeNode {

    //- PUBLIC

    public WsFederationPassiveTokenRequestTreeNode( WsFederationPassiveTokenRequest assertion ) {
        super( assertion );
    }

    public WsFederationPassiveTokenRequestTreeNode( WsFederationPassiveTokenExchange assertion ) {
        super( assertion );
    }

    public Action getPreferredAction() {
        return new EditWsFederationPassiveTokenRequestAction(this);
    }

    public Action[] getActions() {
        List actions = new ArrayList();
        actions.add(getPreferredAction());
        actions.addAll(Arrays.asList(super.getActions()));
        return (Action[])actions.toArray(new Action[0]);
    }

    public boolean canDelete() {
        return true;
    }

    public String getName() {
        String name = null;
        Assertion assertion = asAssertion();
        if (assertion instanceof WsFederationPassiveTokenRequest) {
            name = "Obtain credentials using WS-Federation request to " + ((WsFederationPassiveTokenRequest)assertion).getIpStsUrl();
        }
        else {
            WsFederationPassiveTokenExchange _assertion = (WsFederationPassiveTokenExchange) assertion;
            if((_assertion.getIpStsUrl()!=null && _assertion.getIpStsUrl().length()>0) || !_assertion.isAuthenticate()) {
                name = "Exchange credentials using WS-Federation request to " + _assertion.getIpStsUrl();
            }
            else {
                name = "Authenticate with WS-Federation protected service at " + _assertion.getReplyUrl();
            }
        }
        return name;
    }

    //- PROTECTED

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }
}

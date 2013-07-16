package com.l7tech.external.assertions.authandmgmtserviceinstaller.server;

import com.l7tech.external.assertions.authandmgmtserviceinstaller.ApiPortalAuthAndMgmtServiceInstallerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;

import java.io.IOException;

/**
 * Server side implementation of the ApiPortalAuthAndMgmtServiceInstallerAssertion.
 * @see com.l7tech.external.assertions.authandmgmtserviceinstaller.ApiPortalAuthAndMgmtServiceInstallerAssertion
 *
 * @author ghuang
 */
public class ServerApiPortalAuthAndMgmtServiceInstallerAssertion extends AbstractServerAssertion<ApiPortalAuthAndMgmtServiceInstallerAssertion> {
    private final String[] variablesUsed;

    public ServerApiPortalAuthAndMgmtServiceInstallerAssertion( final ApiPortalAuthAndMgmtServiceInstallerAssertion assertion ) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        return AssertionStatus.FAILED;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     *
     * DELETEME if not required.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }
}
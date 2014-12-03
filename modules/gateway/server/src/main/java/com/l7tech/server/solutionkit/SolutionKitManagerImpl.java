package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SolutionKitManagerImpl extends HibernateEntityManager<SolutionKit, SolutionKitHeader> implements SolutionKitManager {
    private static final Logger logger = Logger.getLogger(SolutionKitManagerImpl.class.getName());

    private static final String REST_GATEWAY_MANAGEMENT_POLICY_XML =
        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
            "<wsp:All wsp:Usage=\"Required\">" +
            "<L7p:RESTGatewayManagement>" +
            "<L7p:OtherTargetMessageVariable stringValue=\"request\"/>" +
            "<L7p:Target target=\"OTHER\"/>" +
            "</L7p:RESTGatewayManagement>" +
            "</wsp:All>" +
            "</wsp:Policy>";

    private ServerAssertion serverRestGatewayManagementAssertion = null;

    public SolutionKitManagerImpl() {
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SolutionKit.class;
    }

    @Override
    public void install(@NotNull SolutionKit solutionKit, @NotNull String bundle) throws SaveException, SolutionKitException {
        // Install bundle.
        //
        String result = this.installBundle(solutionKit, bundle);

        // Save solution kit entity.
        //
        solutionKit.setLastUpdateTime(System.currentTimeMillis());
        solutionKit.setMappings(result);
        save(solutionKit);
    }

    @Override
    public void uninstall(@NotNull Goid goid) throws DeleteException, FindException, SolutionKitException {
        // todo (kpak) - uninstall bundle.
        //

        // Delete solution kit entity.
        //
        delete(goid);
    }

    @Override
    protected SolutionKitHeader newHeader(SolutionKit entity) {
        return new SolutionKitHeader(entity);
    }

    @Override
    protected UniqueType getUniqueType() {
        // todo (kpak) - Change to UniqueType.OTHER, and override getUniqueConstraints() method to return name and prefix.
        return UniqueType.NONE;
    }

    private RestmanInvoker createRestmanInvoker() throws SolutionKitException {
        if (serverRestGatewayManagementAssertion == null) {
            WspReader wspReader = this.applicationContext.getBean("wspReader", WspReader.class);
            ServerPolicyFactory serverPolicyFactory = this.applicationContext.getBean("policyFactory", ServerPolicyFactory.class);
            try {
                Assertion assertion = wspReader.parseStrictly(REST_GATEWAY_MANAGEMENT_POLICY_XML, WspReader.Visibility.omitDisabled);
                serverRestGatewayManagementAssertion = serverPolicyFactory.compilePolicy(assertion, false);
            } catch (IOException | ServerPolicyException | LicenseException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                throw new SolutionKitException("Unable to initialize ServerRESTGatewayManagementAssertion.", e);
            }
        }

        GatewayManagementInvoker invoker = new GatewayManagementInvoker() {
            @Override
            public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                return serverRestGatewayManagementAssertion.checkRequest(context);
            }
        };

        return new RestmanInvoker(new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                // nothing to do in cancelled callback.
                return true;
            }
        }, invoker);
    }

    private String installBundle(SolutionKit solutionKit, String bundle) throws SolutionKitException {
        final RestmanInvoker restmanInvoker = createRestmanInvoker();
        final PolicyEnforcementContext pec = restmanInvoker.getContext(bundle);

        try {
            Pair<AssertionStatus, RestmanMessage> result = restmanInvoker.callManagementCheckInterrupted(pec, bundle);
            if (AssertionStatus.NONE != result.left) {
                String msg = "Unable to install bundle: " + result.left.getMessage();
                logger.log(Level.WARNING, msg);
                throw new SolutionKitException(msg);
            }
            if (result.right.hasMappingError()) {
                String msg = "Unable to install bundle due to mapping error.";
                logger.log(Level.WARNING, msg);
                throw new SolutionKitException(msg);
            }
            return result.right.getAsString();
        } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse | GatewayManagementDocumentUtilities.UnexpectedManagementResponse | IOException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new SolutionKitException(ExceptionUtils.getMessage(e), e);
        } catch (InterruptedException e) {
            // do nothing.
        }
        return "";
    }
}
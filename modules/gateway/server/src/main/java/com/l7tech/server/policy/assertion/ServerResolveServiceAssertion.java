package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HasServiceOid;
import com.l7tech.message.HasServiceOidImpl;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.ResolveServiceAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Server implementation of ResolveServiceAssertion.
 */
public class ServerResolveServiceAssertion extends AbstractServerAssertion<ResolveServiceAssertion> {
    @Inject
    ServiceCache serviceCache;

    private final String[] varsUsed;


    public ServerResolveServiceAssertion(@NotNull final ResolveServiceAssertion assertion) {
        super(assertion);
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context.getService() != null) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Service has already been resolved");
            return AssertionStatus.SERVER_ERROR;
        }

        Map<String, ?> vars = context.getVariableMap(varsUsed, getAudit());

        final PublishedService service;
        try {
            Collection<PublishedService> resolvedSet = serviceCache.resolve(ExpandVariables.process(assertion.getUri(), vars, getAudit()), null, null);
            if (resolvedSet == null || resolvedSet.isEmpty()) {
                getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No service was matched.");
                return AssertionStatus.FAILED;
            }

            if (resolvedSet.size() > 1) {
                getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "More than one service matched the specified parameters.");
                return AssertionStatus.FAILED;
            }

            service = resolvedSet.iterator().next();

        } catch (ServiceResolutionException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Service resolution failed: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }

        final long serviceOid = service.getOid();
        context.getRequest().attachKnob(HasServiceOid.class, new HasServiceOidImpl(serviceOid));
        getAudit().logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, "Resolved to service OID " + serviceOid);
        return AssertionStatus.NONE;
    }
}

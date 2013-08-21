package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HasServiceId;
import com.l7tech.message.HasServiceIdImpl;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.ResolveServiceAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerResolveServiceAssertion(@NotNull final ResolveServiceAssertion assertion) {
        this(assertion, null);
    }

    public ServerResolveServiceAssertion(@NotNull ResolveServiceAssertion assertion, @Nullable AuditFactory auditFactory) {
        super(assertion, auditFactory);
        this.varsUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context.getService() != null) {
            getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_ALREADY_RESOLVED);
            return AssertionStatus.SERVER_ERROR;
        }

        if (context.getRequest().getKnob(HasServiceId.class) != null) {
            getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_ALREADY_HARDWIRED);
            return AssertionStatus.SERVER_ERROR;
        }

        Map<String, ?> vars = context.getVariableMap(varsUsed, getAudit());

        final PublishedService service;
        try {
            Collection<PublishedService> resolvedSet = serviceCache.resolve(ExpandVariables.process(assertion.getUri(), vars, getAudit()), null, null);
            if (resolvedSet == null || resolvedSet.isEmpty()) {
                getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_NOT_FOUND);
                return AssertionStatus.FAILED;
            }

            if (resolvedSet.size() > 1) {
                getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_FOUND_MULTI);
                return AssertionStatus.FAILED;
            }

            service = resolvedSet.iterator().next();

        } catch (ServiceResolutionException e) {
            getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_FAILED, new String[] { "Service resolution failed: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }

        final Goid serviceGoid = service.getGoid();
        context.getRequest().attachKnob(HasServiceId.class, new HasServiceIdImpl(serviceGoid));
        getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_SUCCEEDED, "Resolved to service GOID " + serviceGoid);
        return AssertionStatus.NONE;
    }
}

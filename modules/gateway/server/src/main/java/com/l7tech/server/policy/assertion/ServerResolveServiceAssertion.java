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
        Map<String, ?> vars = context.getVariableMap(varsUsed, getAudit());

        final String variablePrefix = assertion.getPrefix();

        if (null == variablePrefix || variablePrefix.isEmpty()) {
            logAndAudit(AssertionMessages.RESOLVE_SERVICE_NO_PREFIX);
            return AssertionStatus.FAILED;
        }

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
            getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_FAILED,
                    new String[] { ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }

        final Goid serviceGoid = service.getGoid();
        context.getRequest().attachKnob(HasServiceId.class, new HasServiceIdImpl(serviceGoid));
        getAudit().logAndAudit(AssertionMessages.RESOLVE_SERVICE_SUCCEEDED, serviceGoid.toString());

        context.setVariable(variablePrefix + "." + ResolveServiceAssertion.OID_SUFFIX, serviceGoid.toString());
        context.setVariable(variablePrefix + "." + ResolveServiceAssertion.NAME_SUFFIX, service.getName());
        context.setVariable(variablePrefix + "." + ResolveServiceAssertion.SOAP_SUFFIX, service.isSoap());

        return AssertionStatus.NONE;
    }
}

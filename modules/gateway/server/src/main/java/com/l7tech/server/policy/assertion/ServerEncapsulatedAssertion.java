package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Server implementation of encapsulated assertion invoker.
 */
public class ServerEncapsulatedAssertion extends AbstractServerAssertion<EncapsulatedAssertion> implements InitializingBean {
    @Inject
    @Named("encapsulatedAssertionConfigManager")
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;

    @Inject
    @Named("applicationEventProxy")
    private ApplicationEventProxy applicationEventProxy;

    @Inject
    private PolicyCache policyCache;

    private final AtomicReference<Either<String,EncapsulatedAssertionConfig>> configOrErrorRef = new AtomicReference<Either<String,EncapsulatedAssertionConfig>>();
    private ApplicationListener updateListener;

    public ServerEncapsulatedAssertion(final @NotNull EncapsulatedAssertion assertion) {
        super(assertion);
    }

    public ServerEncapsulatedAssertion(final @NotNull EncapsulatedAssertion assertion, final @Nullable AuditFactory auditFactory) {
        super(assertion, auditFactory);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        configOrErrorRef.set(loadConfig());
        initListener();
    }

    private void initListener() {
        final Either<String, EncapsulatedAssertionConfig> configOrError = configOrErrorRef.get();
        if (configOrError.isRight()) {
            final long ourConfigId = configOrError.right().getOid();
            updateListener = new ApplicationListener() {
                @Override
                public void onApplicationEvent(ApplicationEvent event) {
                    if (event instanceof EntityInvalidationEvent) {
                        EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
                        for (long id : eie.getEntityIds()) {
                            if (id == ourConfigId) {
                                logger.info("Reloading encapsulated assertion config for OID " + ourConfigId);
                                configOrErrorRef.set(loadConfig());
                            }
                        }
                    }
                }
            };
            applicationEventProxy.addApplicationListener(updateListener);
        }
    }

    @Override
    public void close() {
        if (updateListener != null) {
            applicationEventProxy.removeApplicationListener(updateListener);
            updateListener = null;
        }
        super.close();
    }

    private Either<String,EncapsulatedAssertionConfig> loadConfig() {
        final String strId = assertion.getEncapsulatedAssertionConfigId();
        if (strId == null) {
            final String msg = "Encapsulated assertion lacks a config ID";
            logger.log(Level.WARNING, msg);
            return Either.left(msg);
        }
        try {
            final EncapsulatedAssertionConfig config = encapsulatedAssertionConfigManager.findByPrimaryKey(Long.parseLong(strId));
            if (config == null) {
                final String msg = "No encapsulated assertion config found with ID " + strId;
                logger.log(Level.WARNING, msg);
                return Either.left(msg);
            }
            return Either.right(config);
        } catch (FindException e) {
            final String msg = "Error looking up encapsulated assertion config with ID " + strId + ": " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            return Either.left(msg);
        } catch (NumberFormatException e) {
            final String msg = "Invalid encapsulated assertion config ID " + strId;
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            return Either.left(msg);
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Either<String, EncapsulatedAssertionConfig> configOrError = configOrErrorRef.get();
        if (configOrError == null) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Server Encapsulated Assertion was not initialized"); // must call afterPropertiesSet()
            return AssertionStatus.SERVER_ERROR;
        }
        if (configOrError.isLeft()) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Invalid Encapsulated Assertion Config: " + configOrError.left());
            return AssertionStatus.SERVER_ERROR;
        }

        EncapsulatedAssertionConfig config = configOrError.right();
        final Policy policy = config.getPolicy();

        // TODO cache policy handle in instance field until policy change is detected, instead of looking up a new one for every request
        final ServerPolicyHandle sph = policyCache.getServerPolicy(policy.getOid());
        try {
            PolicyEnforcementContext childContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(context);

            // TODO configure input values from assertion bean
            // TODO configure input values from UsesVariables

            AssertionStatus result;
            try {
                result = sph.checkRequest(childContext);
            } catch (AssertionStatusException e) {
                result = e.getAssertionStatus();
            } // TODO handle exception thrown by policy, if we don't want to allow it to terminate the calling policy as well

            // TODO configure output values

            return result;

        } finally {
            ResourceUtils.closeQuietly(sph);
        }
    }
}

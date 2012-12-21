package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionStringEncoding;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.ShadowsParentVariables;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.variable.ExpandVariables;
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
import java.util.Map;
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
    private final AtomicReference<String[]> varsUsed = new AtomicReference<String[]>(new String[0]);
    private ApplicationListener updateListener;

    public ServerEncapsulatedAssertion(final @NotNull EncapsulatedAssertion assertion) {
        super(assertion);
        updateConfig(loadInitialConfig(assertion));
    }

    public ServerEncapsulatedAssertion(final @NotNull EncapsulatedAssertion assertion, final @Nullable AuditFactory auditFactory) {
        super(assertion, auditFactory);
        updateConfig(loadInitialConfig(assertion));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        updateConfig(loadConfig());
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
                                updateConfig(loadConfig());
                                break;
                            }
                        }
                    }
                }
            };
            applicationEventProxy.addApplicationListener(updateListener);
        }
    }

    private void updateConfig(Either<String, EncapsulatedAssertionConfig> newVal) {
        configOrErrorRef.set(newVal);
        if (newVal != null && newVal.isRight()) {
            // Get most up-to-date variablesUsed based on most up-to-date config
            varsUsed.set(EncapsulatedAssertion.getVariablesUsed(assertion, newVal.right()));
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

    private Either<String,EncapsulatedAssertionConfig> loadInitialConfig(EncapsulatedAssertion assertion) {
        EncapsulatedAssertionConfig config = assertion.config();
        return config == null ? null : Either.<String,EncapsulatedAssertionConfig>right(config);
    }

    private Either<String,EncapsulatedAssertionConfig> loadConfig() {
        final String guid = assertion.getEncapsulatedAssertionConfigGuid();
        if (guid == null) {
            final String msg = "Encapsulated assertion lacks a config GUID";
            logger.log(Level.WARNING, msg);
            return Either.left(msg);
        }
        try {
            final EncapsulatedAssertionConfig config = encapsulatedAssertionConfigManager.findByGuid(guid);
            if (config == null) {
                final String msg = "No encapsulated assertion config found with GUID " + guid;
                logger.log(Level.WARNING, msg);
                return Either.left(msg);
            }
            return Either.right(config);
        } catch (FindException e) {
            final String msg = "Error looking up encapsulated assertion config with GUID " + guid + ": " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            return Either.left(msg);
        } catch (NumberFormatException e) {
            final String msg = "Invalid encapsulated assertion config GUID " + guid;
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            return Either.left(msg);
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        final Either<String, EncapsulatedAssertionConfig> configOrError = configOrErrorRef.get();
        if (configOrError.isLeft()) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Invalid Encapsulated Assertion Config: " + configOrError.left());
            return AssertionStatus.SERVER_ERROR;
        }

        EncapsulatedAssertionConfig config = configOrError.right();
        String[] varsUsed = this.varsUsed.get();
        Map<String, Object> variableMap = context.getVariableMap(varsUsed, getAudit());
        final Policy policy = config.getPolicy();

        PolicyEnforcementContext childContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(context);
        ShadowsParentVariables spv = (ShadowsParentVariables) childContext;

        populateInputVariables(config, variableMap, context, childContext, spv);

        // TODO cache policy handle in instance field until policy change is detected, instead of looking up a new one for every request
        AssertionStatus result = lookupAndExecutePolicy(policy.getOid(), childContext);

        populateOutputVariables(context, config, childContext);

        return result;

    }

    private AssertionStatus lookupAndExecutePolicy(final long policyOid, PolicyEnforcementContext childContext) throws PolicyAssertionException, IOException {
        AssertionStatus result;
        final ServerPolicyHandle sph = policyCache.getServerPolicy(policyOid);
        try {
            result = executePolicy(sph, childContext);
        } finally {
            ResourceUtils.closeQuietly(sph);
        }
        return result;
    }

    private AssertionStatus executePolicy(ServerPolicyHandle sph, PolicyEnforcementContext childContext) throws PolicyAssertionException, IOException {
        AssertionStatus result;
        try {
            result = sph.checkRequest(childContext);
        } catch (AssertionStatusException e) {
            result = e.getAssertionStatus();
        } // TODO handle exception thrown by policy, if we don't want to allow it to terminate the calling policy as well
        return result;
    }

    private void populateOutputVariables(PolicyEnforcementContext context, EncapsulatedAssertionConfig config, PolicyEnforcementContext childContext) {
        // Configure output values
        for (EncapsulatedAssertionResultDescriptor res : config.getResultDescriptors()) {
            try {
                final String varName = res.getResultName();
                Object value = childContext.getVariable(varName);
                context.setVariable(varName, value);
            } catch (NoSuchVariableException e) {
                /* FALLTHROUGH and leave value undefined in parent context */
            }
        }
    }

    private void populateInputVariables(EncapsulatedAssertionConfig config, Map<String, Object> variableMap, PolicyEnforcementContext parentContext, PolicyEnforcementContext childContext, ShadowsParentVariables spv) {
        // Configure input values
        for (EncapsulatedAssertionArgumentDescriptor arg : config.getArgumentDescriptors()) {
            if (arg.isGuiPrompt()) {
                final String parameterValueString = assertion.getParameter(arg.getArgumentName());
                if (EncapsulatedAssertionArgumentDescriptor.valueIsParentContextVariableNameForDataType(arg.getArgumentType())) {
                    // Add a reference under the requested name to the underlying value object from the parent pec (Message or Element)
                    if (parameterValueString != null) {
                        try {
                            Object parentValue = parentContext.getVariable(parameterValueString);
                            childContext.setVariable(arg.getArgumentName(), parentValue);
                        } catch (NoSuchVariableException e) {
                            getAudit().logAndAudit(AssertionMessages.NO_SUCH_VARIABLE_WARNING, parameterValueString);
                        }
                    }
                } else {
                    // Populate value from assertion bean
                    final Object value = valueFromString(variableMap, arg, parameterValueString);
                    if (value != null)
                        childContext.setVariable(arg.getArgumentName(), value);
                }
            } else {
                // Get live value from parent PEC, by passing through
                // TODO make prefix matching configurable?
                spv.putParentVariable(arg.getArgumentName(), true);
            }
        }
    }

    @Nullable
    public Object valueFromString(Map<String,?> variableMap, EncapsulatedAssertionArgumentDescriptor arg, @Nullable String stringVal) {
        if (stringVal == null)
            stringVal = arg.getDefaultValue();

        if (stringVal == null)
            return null;

        if (EncapsulatedAssertionArgumentDescriptor.allowVariableInterpolationForDataType(arg.getArgumentType())) {
            return ExpandVariables.process(stringVal, variableMap, getAudit());
        }

        return EncapsulatedAssertionStringEncoding.decodeFromString(arg.dataType(), stringVal);
    }

    // protected getters/setters for unit tests

    void setEncapsulatedAssertionConfigManager(@NotNull final EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager) {
        this.encapsulatedAssertionConfigManager = encapsulatedAssertionConfigManager;
    }

    void setApplicationEventProxy(@NotNull final ApplicationEventProxy applicationEventProxy) {
        this.applicationEventProxy = applicationEventProxy;
    }

    void setPolicyCache(@NotNull final PolicyCache policyCache) {
        this.policyCache = policyCache;
    }

    AtomicReference<Either<String, EncapsulatedAssertionConfig>> getConfigOrErrorRef() {
        return configOrErrorRef;
    }

    ApplicationListener getUpdateListener() {
        return updateListener;
    }
}

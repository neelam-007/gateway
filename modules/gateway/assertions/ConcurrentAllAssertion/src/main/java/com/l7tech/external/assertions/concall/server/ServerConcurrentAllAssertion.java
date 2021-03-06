package com.l7tech.external.assertions.concall.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.concall.ConcurrentAllAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.RequestId;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.metrics.GatewayMetricsUtils;
import com.l7tech.server.message.metrics.LatencyMetrics;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ConcurrentAllAssertion.
 *
 * @see com.l7tech.external.assertions.concall.ConcurrentAllAssertion
 */
public class ServerConcurrentAllAssertion extends ServerCompositeAssertion<ConcurrentAllAssertion> {
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(ServerConcurrentAllAssertion.class.getName());

    private static final Object assertionExecutorInitLock = new Object();
    private static volatile ExecutorService assertionExecutor;

    private final List<String[]> varsUsed;
    private final List<String[]> varsSet;
    private final AuditFactory auditFactory;

    public ServerConcurrentAllAssertion(ConcurrentAllAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException, LicenseException {
        super(assertion, beanFactory);
        this.auditFactory = beanFactory.getBean( "auditFactory", AuditFactory.class );

        final List<Assertion> kids = assertion.getChildren();
        if (kids == null || kids.isEmpty()) {
            this.varsUsed = Collections.emptyList();
            this.varsSet = Collections.emptyList();
        } else {
            PolicyCache policyCache = beanFactory.getBean("policyCache", PolicyCache.class);
            this.varsUsed = Collections.unmodifiableList(getVariablesUsedByChildren(kids, policyCache));
            this.varsSet = Collections.unmodifiableList(getVariablesSetByChildren(kids, policyCache));
        }

        // Initialize the executor if necessary
        if (assertionExecutor == null)
            initializeAssertionExecutor(beanFactory);
    }

    private static void initializeAssertionExecutor(BeanFactory beanFactory) {
        Config config = beanFactory == null ? ConfigFactory.getCachedConfig() : beanFactory.getBean("serverConfig", Config.class);
        int globalMaxConcurrency = config.getIntProperty(ConcurrentAllAssertion.SC_MAX_CONC, 64);
        int globalCoreConcurrency = config.getIntProperty(ConcurrentAllAssertion.SC_CORE_CONC, 32);
        int globalMaxWorkQueue = config.getIntProperty(ConcurrentAllAssertion.SC_MAX_QUEUE, 64);
        synchronized (assertionExecutorInitLock) {
            if (assertionExecutor == null) {
                assertionExecutor = createAssertionExecutor(globalMaxConcurrency, globalCoreConcurrency, globalMaxWorkQueue);
            }
        }
    }

    private static ThreadPoolExecutor createAssertionExecutor(int globalMaxConcurrency, int globalCoreConcurrency, int globalMaxWorkQueue) {
        BlockingQueue<Runnable> assertionQueue = new ArrayBlockingQueue<Runnable>(globalMaxWorkQueue, true);
        return new ThreadPoolExecutor(globalCoreConcurrency, globalMaxConcurrency, 5L * 60L, TimeUnit.SECONDS, assertionQueue, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Reset the executor limits.  This method is intended to be used only by unit tests.
     * <p/>
     * This will shut down the existing executor and create a new one in its place.
     * <p/>
     * Caller must ensure that no other threads call {@link #checkRequest} during this process.
     *
     * @param maxConc  new max concurrency.
     * @param coreConc new core concurrency.
     * @param maxQueue new max queue length.
     */
    static void resetAssertionExecutor(int maxConc, int coreConc, int maxQueue) {
        synchronized (assertionExecutorInitLock) {
            if (assertionExecutor != null) {
                assertionExecutor.shutdown();
            }
            assertionExecutor = createAssertionExecutor(maxConc, coreConc, maxQueue);
        }
    }

    private static class KidContext {
        final ServerAssertion serverAssertion;
        final PolicyEnforcementContext context;
        final Future<KidResult> futureResult;
        KidResult actualResult;
        final AssertionMetricsWrapper assertionMetricsWrapper;

        private KidContext(
                final ServerAssertion serverAssertion,
                final PolicyEnforcementContext context,
                final Future<KidResult> futureResult,
                final AssertionMetricsWrapper assertionMetricsWrapper
        ) {
            this.serverAssertion = serverAssertion;
            this.context = context;
            this.futureResult = futureResult;
            this.assertionMetricsWrapper = assertionMetricsWrapper;
        }

        /**
         * @return the associated {@link LatencyMetrics} for the kid PEC or {@code null} if it does not exist.
         */
        @Nullable
        public LatencyMetrics getAssertionMetrics() {
            return assertionMetricsWrapper != null ? assertionMetricsWrapper.getAssertionMetrics() : null;
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Build copies of the context
        final List<ServerAssertion> kids = getChildren();
        final List<KidContext> contexts = new ArrayList<KidContext>(kids.size());
        try {
            return doCheckRequest(kids, context, contexts);
        } finally {
            // Ensure all futures are either completed or canceled
            cleanupContexts(contexts);
        }
    }

    private void cleanupContexts(List<KidContext> contexts) {
        if (contexts == null || contexts.isEmpty())
            return;

        for (KidContext context : contexts) {
            Future<KidResult> future = context.futureResult;
            if (future != null) {
                if (!future.isDone())
                    future.cancel(true);
            }
            ResourceUtils.closeQuietly(context.context);
        }
    }

    private static class KidResult {
        final AssertionStatus assertionStatus;
        final Map<Object, List<AuditDetail>> details;

        private KidResult(AssertionStatus assertionStatus, Map<Object, List<AuditDetail>> details) {
            this.assertionStatus = assertionStatus;
            this.details = details;
        }
    }

    /**
     * {@link LatencyMetrics} is created in the kid PEC and is immutable. {@link com.l7tech.server.event.metrics.AssertionFinished AssertionFinished} is published after the completion of all the kid PECs.
     * Therefore, a wrapper class is created to contain the current kidPECs AssertionMetrics.
     */
    private static final class AssertionMetricsWrapper {
        private LatencyMetrics assertionMetrics;

        void setAssertionMetrics(final LatencyMetrics assertionMetrics) {
            this.assertionMetrics = assertionMetrics;
        }
        LatencyMetrics getAssertionMetrics() {
            return this.assertionMetrics;
        }
    }

    private AssertionStatus doCheckRequest(List<ServerAssertion> kids, PolicyEnforcementContext context, List<KidContext> contexts) throws IOException {
        assert kids.size() == varsUsed.size();
        Iterator<String[]> varsUsedIter = varsUsed.iterator();
        for (final ServerAssertion kid : kids) {
            final String[] varsUsedByKid = varsUsedIter.next();

            final Map<String, Object> kidVarMap = context.getVariableMap(varsUsedByKid, getAudit());
            final PolicyEnforcementContext kidPec = copyContext(context, kidVarMap);
            final AssertionMetricsWrapper assertionMetricsWrapper = new AssertionMetricsWrapper();
            Future<KidResult> kidResult = assertionExecutor.submit(new Callable<KidResult>() {
                @Override
                public KidResult call() throws Exception {
                    try {
                        return AuditContextFactory.doWithCustomAuditContext(new DetailCollectingAuditContext(), new Callable<KidResult>() {
                            @Override
                            public KidResult call() throws Exception {
                                // Configure the thread-local audit context to buffer detail messages
                                AuditContext auditContext = AuditContextFactory.getCurrent();

                                final AssertionStatus status = PolicyEnforcementContextFactory.doWithCurrentContext(
                                        kidPec,
                                        new Callable<AssertionStatus>() {
                                            @Override
                                            public AssertionStatus call() throws Exception {
                                                kidPec.assertionStarting( kid );
                                                final long startTime = timeSource.currentTimeMillis();
                                                try {
                                                    return kid.checkRequest(kidPec);
                                                } catch (AssertionStatusException e) {
                                                    return e.getAssertionStatus();
                                                } catch (Throwable t) {
                                                    auditFactory.newInstance(this, logger).logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                                            new String[] { "Unable to run concurrent assertion: " + ExceptionUtils.getMessage(t) }, t);
                                                    return AssertionStatus.SERVER_ERROR;
                                                } finally {
                                                    assertionMetricsWrapper.setAssertionMetrics(new LatencyMetrics(startTime, timeSource.currentTimeMillis()));
                                                }
                                            }
                                        }
                                );

                                Map<Object, List<AuditDetail>> details = auditContext.getDetails();
                                return new KidResult(status, details);
                            }
                        });

                    } catch (Throwable t) {
                        Map<Object, List<AuditDetail>> fakeDetails = new HashMap<Object, List<AuditDetail>>();
                        fakeDetails.put(ServerConcurrentAllAssertion.this, Arrays.asList(new AuditDetail(
                                AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {
                                    "Unexpected error preparing to run concurrent assertion: " + ExceptionUtils.getMessage(t)
                                }, t)));
                        return new KidResult(AssertionStatus.SERVER_ERROR, fakeDetails);
                    }
                }
            });
            contexts.add(new KidContext(kid, kidPec, kidResult, assertionMetricsWrapper));
        }

        // Collect all results
        assert contexts.size() == varsSet.size();
        Iterator<String[]> varsSetIter = varsSet.iterator();
        for (KidContext kidContext : contexts) {
            final String[] varsSetByKid = varsSetIter.next();

            try {
                kidContext.actualResult = kidContext.futureResult.get();
                mergeContextVariables(varsSetByKid, kidContext.context, context);

            } catch (InterruptedException e) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Concurrent execution interrupted", e);
            } catch (ExecutionException e) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Concurrent execution failed", e);
            }
        }

        AssertionStatus result = AssertionStatus.NONE;
        for (KidContext kidContext : contexts) {
            ServerAssertion kid = kidContext.serverAssertion;

            KidResult kidResult = kidContext.actualResult;
            result = kidResult.assertionStatus;
            importAuditDetails(kidResult.details);

            context.assertionFinished(kid, result, kidContext.getAssertionMetrics());

            if (result != AssertionStatus.NONE) {
                seenAssertionStatus(context, result);
                return result;
            }
        }
        return result;
    }

    private void importAuditDetails(Map<Object, List<AuditDetail>> details) {
        AuditContext auditContext = AuditContextFactory.getCurrent();
        if (details != null) for (Map.Entry<Object, List<AuditDetail>> objectListEntry : details.entrySet()) {
            Object source = objectListEntry.getKey();
            List<AuditDetail> detailList = objectListEntry.getValue();
            if (detailList != null) for (AuditDetail auditDetail : detailList) {
                auditContext.addDetail(auditDetail, source);
            }
        }
    }

    private void mergeContextVariables(String[] varsSetByKid, PolicyEnforcementContext source, PolicyEnforcementContext dest) throws IOException {
        Map<String, Object> map = source.getVariableMap(varsSetByKid, getAudit());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            copyValue(dest, name, value);
        }
    }

    /**
     * Create a new unregistered PolicyEnforcementContext with a blank request and response, the requestId of the parent, and all String and Message context
     * variables deep-copied from the specified source context. The PEC does not replace any current thread-local PEC that is already registered
     *
     * @param source the context to copy.  Required.
     * @param varsMap the variables to copy over.  Required.
     * @return a new context with some material copied from the specified one.
     * @throws java.io.IOException if a Message variable is to be copied and it cannot be read
     *
     * @see PolicyEnforcementContextFactory#createUnregisteredPolicyEnforcementContext(Message, Message, RequestId, boolean)
     */
    private PolicyEnforcementContext copyContext(PolicyEnforcementContext source, Map<String, Object> varsMap) throws IOException {
        final PolicyEnforcementContext ret = PolicyEnforcementContextFactory.createUnregisteredPolicyEnforcementContext(new Message(), new Message(), source.getRequestId(), true);
        GatewayMetricsUtils.setPublisher(source, ret);

        ret.setRequestWasCompressed(source.isRequestWasCompressed());
        ret.setService(source.getService());
        ret.setServicePolicyMetadata(source.getServicePolicyMetadata());
        ret.setCurrentPolicyMetadata(source.getCurrentPolicyMetadata());
        ret.setAuditLevel(source.getAuditLevel());
        ret.setPolicyExecutionAttempted(true);
        ret.setTraceListener(source.getTraceListener());

        for (Map.Entry<String, Object> entry : varsMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            copyValue(ret, name, value);
        }

        source.passDownAssertionOrdinal(ret);

        return ret;
    }

    private static void copyValue(PolicyEnforcementContext ret, String name, Object value) throws IOException {
        if (value == null) {
            safeSetVariable(ret, name, null);
        } else if (value instanceof String) {
            safeSetVariable(ret, name, value);
        } else if (value instanceof Message) {
            safeSetVariable(ret, name, cloneMessageBody((Message)value));
        } else if (value instanceof Object[]) {
            Class<?> elementType = value.getClass().getComponentType();
            if (isSafeToCopyArrayOf(elementType)) {
                Object[] arr = ((Object[])value);
                safeSetVariable(ret, name, Arrays.copyOf(arr, arr.length));
            } else {
                if (logger.isLoggable(Level.FINE))
                    logger.log(Level.FINE, "Variable named " + name + " has unsupported type: " + value.getClass());
            }
        } else {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Variable named " + name + " has unsupported type: " + value.getClass());
        }
    }

    private static boolean isSafeToCopyArrayOf(Class<?> elementType) {
        // We know we can safely copy with Arrays.copyOf(Object[]) arrays of primitive types (int, char, byte, short, double, etc),
        // arrays of String, and arrays of boxed primitive types (Integer, Character, Byte, Short, Double, etc).
        // We recognized boxed primitive types by checking for subclasses of java.lang.Number that are in the java.lang package.
        return elementType.isPrimitive() ||
               String.class.equals(elementType) ||
               (Number.class.isAssignableFrom(elementType) && elementType.getPackage().equals(Integer.class.getPackage()));
    }

    private static void safeSetVariable(PolicyEnforcementContext ctx, String name, Object value) {
        try {
            ctx.setVariable(name, value);
        } catch (VariableNotSettableException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Unable to set non-settable variable: " + name);
            // Ignore it and fallthrough
        }
    }

    static Message cloneMessageBody(Message source) throws IOException {
        MimeKnob mk = source.getKnob(MimeKnob.class);
        if (mk == null || !source.isInitialized())
            return new Message(); // not yet initialized

        try {
            byte[] sourceBytes = IOUtils.slurpStream(mk.getEntireMessageBodyAsInputStream());
            return new Message(new ByteArrayStashManager(), mk.getOuterContentType(), new ByteArrayInputStream(sourceBytes));
        } catch (NoSuchPartException e) {
            throw new IOException(e);
        }
    }

    private static List<String[]> getVariablesSetByChildren(List<Assertion> kids, PolicyCache policyCache) {
        List<String[]> setByKids = new ArrayList<String[]>(kids.size());
        for (Assertion kid : kids) {
            Map<String, VariableMetadata> setvars = PolicyVariableUtils.getVariablesSetByDescendantsAndSelf(kid, makeVariableCollatingTranslator(policyCache));
            setByKids.add(setvars.keySet().toArray(new String[setvars.keySet().size()]));
        }
        return setByKids;
    }

    private static List<String[]> getVariablesUsedByChildren(List<Assertion> kids, PolicyCache policyCache) {
        List<String[]> usedByKids = new ArrayList<String[]>(kids.size());
        for (Assertion kid : kids) {
            String[] usedvars = PolicyVariableUtils.getVariablesUsedByDescendantsAndSelf(kid, makeVariableCollatingTranslator(policyCache));
            usedByKids.add(usedvars);
        }
        return usedByKids;
    }

    /**
     * A translator that replaces Include assertions by a fake assertion that advertises that it uses and sets any variables
     * used or set within the Include.  It gets this info by looking up policy metadata from the policy cache.
     *
     * @param policyCache the PolicyCache instance, for looking up policy metadata, or null.
     * @return an AssertionTranslator that will replace Include assertions by a fake assertion that collates the fragments used and set variables.
     */
    private static AssertionTranslator makeVariableCollatingTranslator(final PolicyCache policyCache) {
        return new AssertionTranslator() {
            @Override
            public Assertion translate(@Nullable Assertion sourceAssertion) throws PolicyAssertionException {
                if (sourceAssertion == null || !(sourceAssertion instanceof Include) || policyCache == null) {
                    return sourceAssertion;
                }

                final String guid = ((Include) sourceAssertion).getPolicyGuid();
                if (guid == null)
                    return sourceAssertion;

                PolicyMetadata meta = policyCache.getPolicyMetadataByGuid(guid);
                if (meta == null) {
                    return sourceAssertion;
                }

                return new VariableCollatingAssertion(meta.getVariablesUsed(), meta.getVariablesSet());
            }

            @Override
            public void translationFinished(@Nullable Assertion sourceAssertion) {
            }
        };
    }

    // A fake assertion whose only purpose is to advertise UsesVariables and SetsVariables on behalf of an include assertion
    private static class VariableCollatingAssertion extends Assertion implements UsesVariables, SetsVariables {
        final String[] variablesUsed;
        final VariableMetadata[] variablesSet;

        private VariableCollatingAssertion(String[] variablesUsed, VariableMetadata[] variablesSet) {
            this.variablesUsed = variablesUsed;
            this.variablesSet = variablesSet;
        }

        @Override
        public String[] getVariablesUsed() {
            return variablesUsed;
        }

        @Override
        public VariableMetadata[] getVariablesSet() {
            return variablesSet;
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        logger.log(Level.INFO, "ServerConcurrentAllAssertion is preparing itself to be unloaded; shutting down assertion executor");
        assertionExecutor.shutdownNow();
    }
}

package com.l7tech.external.assertions.concall.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.concall.ConcurrentAllAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyMetadata;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ConcurrentAllAssertion.
 *
 * @see com.l7tech.external.assertions.concall.ConcurrentAllAssertion
 */
public class ServerConcurrentAllAssertion extends ServerCompositeAssertion<ConcurrentAllAssertion> {
    private static final Logger logger = Logger.getLogger(ServerConcurrentAllAssertion.class.getName());

    private static final Object assertionExecutorInitLock = new Object();
    private static volatile ExecutorService assertionExecutor;

    private final Auditor auditor;
    private final List<String[]> varsUsed;
    private final List<String[]> varsSet;
    private final BeanFactory beanFactory;
    private final ApplicationEventPublisher eventPub;

    public ServerConcurrentAllAssertion(ConcurrentAllAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws PolicyAssertionException, LicenseException {
        super(assertion, beanFactory);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);
        this.beanFactory = beanFactory;
        this.eventPub = eventPub;

        final List<Assertion> kids = getEnabledImmediateChildAssertions(assertion);
        if (kids == null || kids.isEmpty()) {
            this.varsUsed = Collections.emptyList();
            this.varsSet = Collections.emptyList();
        } else {
            PolicyCache policyCache = beanFactory == null ? null : (PolicyCache)beanFactory.getBean("policyCache");
            this.varsUsed = Collections.unmodifiableList(getVariablesUsedByChildren(kids, policyCache));
            this.varsSet = Collections.unmodifiableList(getVariablesSetByChildren(kids, policyCache));
        }

        // Initialize the executor if necessary
        if (assertionExecutor == null)
            initializeAssertionExecutor(beanFactory);
    }

    private static void initializeAssertionExecutor(BeanFactory beanFactory) {
        ServerConfig serverConfig = beanFactory == null ? ServerConfig.getInstance() : (ServerConfig)beanFactory.getBean("serverConfig", ServerConfig.class);
        int globalMaxConcurrency = serverConfig.getIntProperty(ConcurrentAllAssertion.SC_MAX_CONC, 64);
        int globalCoreConcurrency = serverConfig.getIntProperty(ConcurrentAllAssertion.SC_CORE_CONC, 32);
        int globalMaxWorkQueue = serverConfig.getIntProperty(ConcurrentAllAssertion.SC_MAX_QUEUE, 64);
        synchronized (assertionExecutorInitLock) {
            if (assertionExecutor == null) {
                assertionExecutor = createAssertionExecutor(globalMaxConcurrency, globalCoreConcurrency, globalMaxWorkQueue);
            }
        }
    }

    private static ThreadPoolExecutor createAssertionExecutor(int globalMaxConcurrency, int globalCoreConcurrency, int globalMaxWorkQueue) {
        BlockingQueue<Runnable> assertionQueue = new ArrayBlockingQueue<Runnable>(globalMaxWorkQueue, true);
        return new ThreadPoolExecutor(globalCoreConcurrency, globalMaxConcurrency, 5 * 60, TimeUnit.SECONDS, assertionQueue, new ThreadPoolExecutor.CallerRunsPolicy());
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
            assertionExecutor.shutdown();
            assertionExecutor = createAssertionExecutor(maxConc, coreConc, maxQueue);
        }
    }

    public List<ServerAssertion> getEnabledImmediateChildServerAssertions() {
        List<ServerAssertion> ret = new ArrayList<ServerAssertion>();
        for (ServerAssertion serverAssertion : getChildren()) {
            if (serverAssertion.getAssertion().isEnabled())
                ret.add(serverAssertion);
        }
        return ret;
    }

    public static List<Assertion> getEnabledImmediateChildAssertions(CompositeAssertion comp) {
        List<Assertion> ret = new ArrayList<Assertion>();
        for (Assertion assertion : comp.getChildren()) {
            if (assertion.isEnabled() && !(assertion instanceof CommentAssertion))
                ret.add(assertion);
        }
        return ret;
    }

    private static class KidContext {
        final ServerAssertion serverAssertion;
        final PolicyEnforcementContext context;
        final Future<KidResult> futureResult;
        KidResult actualResult;

        private KidContext(ServerAssertion serverAssertion, PolicyEnforcementContext context, Future<KidResult> futureResult) {
            this.serverAssertion = serverAssertion;
            this.context = context;
            this.futureResult = futureResult;
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Build copies of the context
        final List<ServerAssertion> kids = getEnabledImmediateChildServerAssertions();
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

    private AssertionStatus doCheckRequest(List<ServerAssertion> kids, PolicyEnforcementContext context, List<KidContext> contexts) throws IOException {
        assert kids.size() == varsUsed.size();
        Iterator<String[]> varsUsedIter = varsUsed.iterator();
        for (final ServerAssertion kid : kids) {
            final String[] varsUsedByKid = varsUsedIter.next();

            final Map<String, Object> kidVarMap = context.getVariableMap(varsUsedByKid, auditor);
            final PolicyEnforcementContext kidPec = copyContext(context, kidVarMap);
            Future<KidResult> kidResult = assertionExecutor.submit(new Callable<KidResult>() {
                @Override
                public KidResult call() throws Exception {
                    try {
                        // Configure the thread-local audit context to buffer detail messages
                        AuditContext auditContext = (AuditContext)beanFactory.getBean("auditContext", AuditContext.class);
                        auditContext.clear();

                        AssertionStatus status = doCall();

                        Map<Object, List<AuditDetail>> details = auditContext.getDetails();
                        auditContext.clear();
                        return new KidResult(status, details);

                    } catch (Throwable t) {
                        Map<Object, List<AuditDetail>> fakeDetails = new HashMap<Object, List<AuditDetail>>();
                        fakeDetails.put(ServerConcurrentAllAssertion.this, Arrays.asList(new AuditDetail(
                                AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {
                                    "Unexpected error preparing to run concurrent assertion: " + ExceptionUtils.getMessage(t)
                                }, t)));
                        return new KidResult(AssertionStatus.SERVER_ERROR, fakeDetails);
                    }
                }

                AssertionStatus doCall() throws Exception {
                    try {
                        return kid.checkRequest(kidPec);
                    } catch (AssertionStatusException e) {
                        return e.getAssertionStatus();
                    } catch (Throwable t) {
                        new Auditor(this, beanFactory, eventPub, logger).logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                new String[] { "Unable to run concurrent assertion: " + ExceptionUtils.getMessage(t) }, t);
                        return AssertionStatus.SERVER_ERROR;
                    }
                }
            });
            contexts.add(new KidContext(kid, kidPec, kidResult));
        }

        // Collect all results
        assert contexts.size() == varsSet.size();
        Iterator<String[]> varsSetIter = varsSet.iterator();
        for (KidContext kidContext : contexts) {
            final String[] varsSetByKid = varsSetIter.next();
            assert kidContext.serverAssertion.getAssertion().isEnabled() && !(kidContext.serverAssertion.getAssertion() instanceof CommentAssertion);

            try {
                kidContext.actualResult = kidContext.futureResult.get();
                mergeContextVariables(varsSetByKid, kidContext.context, context);

            } catch (InterruptedException e) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Concurrent execution interrupted", e);
            } catch (ExecutionException e) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, "Concurrent execution failed", e);
            }
        }

        AssertionStatus result = AssertionStatus.FAILED;
        for (KidContext kidContext : contexts) {
            ServerAssertion kid = kidContext.serverAssertion;
            assert kid.getAssertion().isEnabled() && !(kid.getAssertion() instanceof CommentAssertion);

            KidResult kidResult = kidContext.actualResult;
            result = kidResult.assertionStatus;
            importAuditDetails(kidResult.details);

            context.assertionFinished(kid, result);

            if (result != AssertionStatus.NONE) {
                seenAssertionStatus(context, result);
                return result;
            }
        }
        return result;
    }

    private void importAuditDetails(Map<Object, List<AuditDetail>> details) {
        AuditContext auditContext = (AuditContext)beanFactory.getBean("auditContext", AuditContext.class);
        if (details != null) for (Map.Entry<Object, List<AuditDetail>> objectListEntry : details.entrySet()) {
            Object source = objectListEntry.getKey();
            List<AuditDetail> detailList = objectListEntry.getValue();
            if (detailList != null) for (AuditDetail auditDetail : detailList) {
                auditContext.addDetail(auditDetail, source);
            }
        }
    }

    private void mergeContextVariables(String[] varsSetByKid, PolicyEnforcementContext source, PolicyEnforcementContext dest) throws IOException {
        Map<String, Object> map = source.getVariableMap(varsSetByKid, auditor);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                safeSetVariable(dest, name, value);
            } else if (value instanceof Message) {
                safeSetVariable(dest, name, cloneMessageBody((Message)value));
            }
        }
    }

    /**
     * Create a new PolicyEnforcmentContext with a blank request and response, all String and Message context
     * variables deep-copied from the specified source context.
     *
     * @param source the context to copy.  Required.
     * @param varsMap the variables to copy over.  Required.
     * @return a new context with some material copied from the specified one.
     * @throws java.io.IOException if a Message variable is to be copied and it cannot be read
     */
    private PolicyEnforcementContext copyContext(PolicyEnforcementContext source, Map<String, Object> varsMap) throws IOException {
        PolicyEnforcementContext ret = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        ret.setRequestWasCompressed(source.isRequestWasCompressed());
        ret.setService(source.getService());
        ret.setAuditLevel(source.getAuditLevel());
        ret.setPolicyExecutionAttempted(true);

        for (Map.Entry<String, Object> entry : varsMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                safeSetVariable(ret, name, value);
            } else if (value instanceof Message) {
                safeSetVariable(ret, name, cloneMessageBody((Message)value));
            }
        }

        return ret;
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
        if (mk == null)
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
            assert kid.isEnabled() && !(kid instanceof CommentAssertion);
            Map<String, VariableMetadata> setvars = PolicyVariableUtils.getVariablesSetByDescendantsAndSelf(kid, makeVariableCollatingTranslator(policyCache));
            setByKids.add(setvars.keySet().toArray(new String[setvars.keySet().size()]));
        }
        return setByKids;
    }

    private static List<String[]> getVariablesUsedByChildren(List<Assertion> kids, PolicyCache policyCache) {
        List<String[]> usedByKids = new ArrayList<String[]>(kids.size());
        for (Assertion kid : kids) {
            assert kid.isEnabled() && !(kid instanceof CommentAssertion);
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
            public Assertion translate(Assertion sourceAssertion) throws PolicyAssertionException {
                if (sourceAssertion == null || !sourceAssertion.isEnabled() || !(sourceAssertion instanceof Include) || policyCache == null) {
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
            public void translationFinished(Assertion sourceAssertion) {
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

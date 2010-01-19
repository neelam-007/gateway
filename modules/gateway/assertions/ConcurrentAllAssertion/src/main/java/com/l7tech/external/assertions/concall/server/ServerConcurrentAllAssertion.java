package com.l7tech.external.assertions.concall.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.concall.ConcurrentAllAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
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

    private static ExecutorService assertionExecutor;

    private final Auditor auditor;
    private final List<String[]> varsUsed;
    private final List<String[]> varsSet;

    public ServerConcurrentAllAssertion(ConcurrentAllAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub) throws PolicyAssertionException, LicenseException {
        super(assertion, beanFactory);
        this.auditor = new Auditor(this, beanFactory, eventPub, logger);

        final List<Assertion> kids = assertion.getChildren();
        if (kids == null || kids.isEmpty()) {
            this.varsUsed = Collections.emptyList();
            this.varsSet = Collections.emptyList();
        } else {
            this.varsUsed = Collections.unmodifiableList(getVariablesUsedByChildren(kids));
            this.varsSet = Collections.unmodifiableList(getVariablesSetByChildren(kids));
        }

        // Initialize the executor if necessary
        if (assertionExecutor == null) {
            ServerConfig serverConfig = beanFactory == null ? ServerConfig.getInstance() : (ServerConfig)beanFactory.getBean("serverConfig", ServerConfig.class);
            int globalMaxConcurrency = serverConfig.getIntProperty(ConcurrentAllAssertion.SC_MAX_CONC, 64);
            int globalCoreConcurrency = serverConfig.getIntProperty(ConcurrentAllAssertion.SC_CORE_CONC, 32);
            int globalMaxWorkQueue = serverConfig.getIntProperty(ConcurrentAllAssertion.SC_MAX_QUEUE, 512);

            BlockingQueue<Runnable> assertionQueue = new ArrayBlockingQueue<Runnable>(globalMaxWorkQueue, true);
            assertionExecutor = new ThreadPoolExecutor(globalCoreConcurrency, globalMaxConcurrency, 5 * 60, TimeUnit.SECONDS, assertionQueue);
        }
    }

    private static class KidContext {
        final ServerAssertion serverAssertion;
        final PolicyEnforcementContext context;
        final Future<AssertionStatus> futureResult;
        AssertionStatus actualResult;

        private KidContext(ServerAssertion serverAssertion, PolicyEnforcementContext context, Future<AssertionStatus> futureResult) {
            this.serverAssertion = serverAssertion;
            this.context = context;
            this.futureResult = futureResult;
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // Build copies of the context
        final List<ServerAssertion> kids = getChildren();
        final List<KidContext> contexts = new ArrayList<KidContext>(kids.size());
        try {
            return doCheckRequest(context, contexts);
        } finally {
            // Ensure all futures are either completed or canceled
            cleanupContexts(contexts);
        }
    }

    private void cleanupContexts(List<KidContext> contexts) {
        if (contexts == null || contexts.isEmpty())
            return;

        for (KidContext context : contexts) {
            Future<AssertionStatus> future = context.futureResult;
            if (future != null) {
                if (!future.isDone())
                    future.cancel(true);
            }
            ResourceUtils.closeQuietly(context.context);
        }
    }

    private AssertionStatus doCheckRequest(PolicyEnforcementContext context, List<KidContext> contexts) throws IOException {
        List<ServerAssertion> kids = getChildren();
        assert kids.size() == varsUsed.size();
        Iterator<String[]> varsUsedIter = varsUsed.iterator();
        for (final ServerAssertion kid : kids) {
            final String[] varsUsedByKid = varsUsedIter.next();

            if (! kid.getAssertion().isEnabled())
                continue;

            final Map<String, Object> kidVarMap = context.getVariableMap(varsUsedByKid, auditor);
            final PolicyEnforcementContext kidPec = copyContext(context, kidVarMap);
            Future<AssertionStatus> kidResult = assertionExecutor.submit(new Callable<AssertionStatus>() {
                @Override
                public AssertionStatus call() throws Exception {
                    try {
                        return kid.checkRequest(kidPec);
                    } catch (AssertionStatusException e) {
                        return e.getAssertionStatus();
                    } catch (Throwable t) {
                        logger.log(Level.WARNING, "Unexpected exception while running concurrent assertion: " + ExceptionUtils.getMessage(t), t);
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
            ServerAssertion kid = kidContext.serverAssertion;

            // If the assertion is disabled, then ignore it and continue to check the next assertion.
            if (! kid.getAssertion().isEnabled())
                continue;

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

            // If the assertion is disabled, then ignore it and continue to check the next assertion.
            if (kidContext.actualResult == null || !kid.getAssertion().isEnabled())
                continue;

            result = kidContext.actualResult;

            context.assertionFinished(kid, result);

            if (result != AssertionStatus.NONE) {
                seenAssertionStatus(context, result);
                rollbackDeferredAssertions(context);
                return result;
            }
        }
        return result;
    }

    private void mergeContextVariables(String[] varsSetByKid, PolicyEnforcementContext source, PolicyEnforcementContext dest) throws IOException {
        Map<String, Object> map = source.getVariableMap(varsSetByKid, auditor);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                dest.setVariable(name, value);
            } else if (value instanceof Message) {
                dest.setVariable(name, cloneMessageBody((Message)value));
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
                ret.setVariable(name, value);
            } else if (value instanceof Message) {
                ret.setVariable(name, cloneMessageBody((Message)value));
            }
        }

        return ret;
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

    private static List<String[]> getVariablesSetByChildren(List<Assertion> kids) {
        List<String[]> setByKids = new ArrayList<String[]>(kids.size());
        for (Assertion kid : kids) {
            setByKids.add(getVariablesSetIncludingDescendants(kid));
        }
        return setByKids;
    }

    private static String[] getVariablesUsedIncludingDescendants(Assertion kid) {
        Set<String> vars = new HashSet<String>();
        gatherVariablesUsedRecursive(kid, vars);
        return vars.toArray(new String[vars.size()]);
    }

    private static void gatherVariablesUsedRecursive(Assertion kid, Set<String> vars) {
        if (kid instanceof CompositeAssertion) {
            for (Assertion newKid : ((CompositeAssertion)kid).getChildren())
                gatherVariablesUsedRecursive(newKid, vars);
        } else if (kid instanceof UsesVariables) {
            vars.addAll(Arrays.asList(((UsesVariables)kid).getVariablesUsed()));
        }
    }

    private static List<String[]> getVariablesUsedByChildren(List<Assertion> kids) {
        List<String[]> usedByKids = new ArrayList<String[]>(kids.size());
        for (Assertion kid : kids) {
            usedByKids.add(getVariablesUsedIncludingDescendants(kid));
        }
        return usedByKids;
    }

    private static String[] getVariablesSetIncludingDescendants(Assertion kid) {
        Set<String> vars = new HashSet<String>();
        gatherVariablesSetRecursive(kid, vars);
        return vars.toArray(new String[vars.size()]);
    }

    private static void gatherVariablesSetRecursive(Assertion kid, Set<String> vars) {
        if (kid instanceof CompositeAssertion) {
            for (Assertion newKid : ((CompositeAssertion)kid).getChildren())
                gatherVariablesSetRecursive(newKid, vars);
        } else if (kid instanceof SetsVariables) {
            final VariableMetadata[] metadataSet = ((SetsVariables) kid).getVariablesSet();
            vars.addAll(metadataVarnames(metadataSet));
        }
    }

    private static List<String> metadataVarnames(VariableMetadata[] metas) {
        List<String> ret = new ArrayList<String>();
        for (VariableMetadata meta : metas) {
            ret.add(meta.getName());
        }
        return ret;
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

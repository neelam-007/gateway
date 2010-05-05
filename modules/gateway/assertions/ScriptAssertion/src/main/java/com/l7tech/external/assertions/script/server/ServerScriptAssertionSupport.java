package com.l7tech.external.assertions.script.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 */
public class ServerScriptAssertionSupport {
    private static final Logger logger = Logger.getLogger(ServerScriptAssertionSupport.class.getName());

    public static ServerAssertion createServerAssertion(final Assertion assertion, final String scriptName, final String bsfLanguageName, final String script, final ApplicationContext applicationContext) {
        final Auditor auditor = new Auditor(assertion, applicationContext, logger);
        final String source = "policy/?????/compiledScript/" + scriptName;
        return new CompiledScriptServerAssertion(assertion, auditor, applicationContext, bsfLanguageName, source, script);
    }

    public static boolean isTruth(Object result) throws BSFException {
        if (result == null)
            throw new BSFException("Script return value was null");
        if (result instanceof Boolean)
            return (Boolean)result;
        if (result instanceof Double || result instanceof Float) {
            Number number = (Number)result;
            return number.doubleValue() != 0;
        }
        if (result instanceof Number) {
            Number number = (Number)result;
            return number.longValue() != 0;
        }
        throw new BSFException("Script did not return (true or nonzero) or (false or zero).  Return value: " + result.getClass() + "=" + result);
    }

    private static class CompiledScriptServerAssertion implements ServerAssertion {
        private final Assertion assertion;
        private final Auditor auditor;
        private final ApplicationContext applicationContext;
        private final String bsfLanguageName;
        private final String source;
        private final String script;

        public CompiledScriptServerAssertion(Assertion assertion, Auditor auditor, ApplicationContext applicationContext, String bsfLanguageName, String source, String script) {
            this.assertion = assertion;
            this.auditor = auditor;
            this.applicationContext = applicationContext;
            this.bsfLanguageName = bsfLanguageName;
            this.source = source;
            this.script = script;
        }

        @Override
        public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
            BSFManager manager = null;
            try {
                manager = new BSFManager();
                manager.setClassLoader(getClass().getClassLoader());
                manager.declareBean("assertion", assertion, assertion.getClass());
                manager.declareBean("auditor", auditor, auditor.getClass());
                manager.declareBean("appContext", applicationContext, applicationContext.getClass());
                manager.declareBean("policyContext", context, context.getClass());
                Object result = manager.eval(bsfLanguageName, source, 0, 0, script);

                if (isTruth(result))
                    return AssertionStatus.NONE;
                else
                    return AssertionStatus.FALSIFIED;

            } catch (BSFException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                    new String[] { "Script failure: " + ExceptionUtils.getMessage(e) },
                                    e);
                return AssertionStatus.FAILED;
            } finally {
                if (manager != null) manager.terminate();
            }
        }

        @Override
        public Assertion getAssertion() {
            return assertion;
        }

        @Override
        public void close() throws IOException {
        }
    }
}

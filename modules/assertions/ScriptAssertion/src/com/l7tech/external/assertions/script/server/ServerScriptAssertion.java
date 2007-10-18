package com.l7tech.external.assertions.script.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.external.assertions.script.ScriptAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the ScriptAssertion.
 *
 * @see com.l7tech.external.assertions.script.ScriptAssertion
 */
public class ServerScriptAssertion extends AbstractServerAssertion<ScriptAssertion> {
    private static final Logger logger = Logger.getLogger(ServerScriptAssertion.class.getName());

    private final ScriptAssertion assertion;
    private final Auditor auditor;
    private ApplicationContext applicationContext;
    private String source;
    private String script;

    public ServerScriptAssertion(ScriptAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = new Auditor(this, context, logger);
        this.applicationContext = context;
        this.source = "policy/?????/assertionOrdinal/" + assertion.getOrdinal() + "/script";
        this.script = assertion.decodeScript();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        BSFManager manager = null;
        try {
            manager = new BSFManager();
            manager.setClassLoader(getClass().getClassLoader());
            manager.declareBean("assertion", assertion, assertion.getClass());
            manager.declareBean("auditor", auditor, auditor.getClass());
            manager.declareBean("appContext", applicationContext, applicationContext.getClass());
            manager.declareBean("policyContext", context, context.getClass());
            Object result = manager.eval(assertion.getLanguage().getBsfLanguageName(), source, 0, 0, script);

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

    private boolean isTruth(Object result) throws BSFException {
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
}

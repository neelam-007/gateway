package com.l7tech.external.assertions.ipm.server;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.external.assertions.ipm.IpmAssertion;
import com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the IpmAssertion.
 *
 * @see com.l7tech.external.assertions.ipm.IpmAssertion
 */
public class ServerIpmAssertion extends AbstractServerAssertion<IpmAssertion> {
    private static final Logger logger = Logger.getLogger(ServerIpmAssertion.class.getName());
    private static final int DEFAULT_BUFF_SIZE = 131040;

    private final IpmAssertion assertion;
    private final Auditor auditor;
    private final String varname;
    private final ThreadLocal<CompiledTemplate> compiledTemplate;
    private final int buffSize;

    public ServerIpmAssertion(IpmAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        varname = assertion.getSourceVariableName();

        Class<? extends CompiledTemplate> ctClass = null;
        try {
            CompiledTemplate ct = new TemplateCompiler(assertion.template()).compile();
            ct.close();
            ctClass = ct.getClass();
        } catch (TemplateCompilerException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                new String[] { "Unable to compile template; assertion will always fail: " + ExceptionUtils.getMessage(e) }, e);
        }

        if (context != null) {
            ServerConfig serverConfig = (ServerConfig)context.getBean("serverConfig", ServerConfig.class);
            buffSize = serverConfig.getIntPropertyCached(IpmAssertion.PARAM_IPM_OUTPUTBUFFER, DEFAULT_BUFF_SIZE, 120000L);
        } else {
            buffSize = DEFAULT_BUFF_SIZE;
        }

        if (ctClass == null) {
            compiledTemplate = null;
        } else {
            final Class<? extends CompiledTemplate> ctClass1 = ctClass;
            compiledTemplate = new ThreadLocal<CompiledTemplate>() {
                protected CompiledTemplate initialValue() {
                    try {
                        return ctClass1.newInstance();
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (compiledTemplate == null) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Template compilation failed; assertion fails");
            return AssertionStatus.SERVER_ERROR;
        }

        String databuff;
        try {
            Object got = context.getVariable(varname);
            databuff = got.toString();
        } catch (NoSuchVariableException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Missing variable: " + varname }, e);
            return AssertionStatus.FAILED;
        }

        CompiledTemplate ct = compiledTemplate.get();
        ct.setOutputBufferSize(buffSize);
        ct.init(databuff.toCharArray());
        ct.expand();

        int size = ct.getResultSize();
        char[] resultChars = ct.getResult();
        String result = new String(resultChars, 0, size);
        context.setVariable(assertion.getTargetVariableName(), result);

        return AssertionStatus.NONE;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerIpmAssertion is preparing itself to be unloaded");
    }
}

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
import java.util.logging.Logger;

/**
 * Server side implementation of the IpmAssertion.
 *
 * @see com.l7tech.external.assertions.ipm.IpmAssertion
 */
public class ServerIpmAssertion extends AbstractServerAssertion<IpmAssertion> {
    private static final Logger logger = Logger.getLogger(ServerIpmAssertion.class.getName());
    private static final int DEFAULT_BUFF_SIZE = 131040;

    private static final ThreadLocal<char[]> outBuffs = new ThreadLocal<char[]>();

    private final Auditor auditor;
    private final String varname;
    private final ThreadLocal<CompiledTemplate> compiledTemplate;
    private final ServerConfig serverConfig;

    public ServerIpmAssertion(IpmAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        //noinspection ThisEscapedInObjectConstruction
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);
        varname = assertion.getSourceVariableName();

        Class<? extends CompiledTemplate> ctClass = null;
        try {
            CompiledTemplate ct = new TemplateCompiler(assertion.template()).compile();
            ctClass = ct.getClass();
        } catch (TemplateCompilerException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                new String[] { "Unable to compile template; assertion will always fail: " + ExceptionUtils.getMessage(e) }, e);
        }

        serverConfig = context == null ? null : (ServerConfig)context.getBean("serverConfig", ServerConfig.class);

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
        String result = ct.expand(databuff.toCharArray(), getOutputBuffer());
        context.setVariable(assertion.getTargetVariableName(), result);

        return AssertionStatus.NONE;
    }

    private char[] getOutputBuffer() {
        int buffSize = serverConfig == null
                       ? DEFAULT_BUFF_SIZE
                       : serverConfig.getIntPropertyCached(IpmAssertion.PARAM_IPM_OUTPUTBUFFER, DEFAULT_BUFF_SIZE, 120000L);

        char[] out = outBuffs.get();
        if (out == null || out.length != buffSize) {
            out = new char[buffSize];
            outBuffs.set(out);
        }

        return out;
    }
}

package com.l7tech.external.assertions.js.server;

import com.l7tech.external.assertions.js.JavaScriptAssertion;
import com.l7tech.external.assertions.js.RuntimeScriptException;
import com.l7tech.external.assertions.js.features.JavaScriptExecutor;
import com.l7tech.external.assertions.js.features.JavaScriptExecutorOptions;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.script.CompiledScript;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.DEFAULT_EXECUTION_TIMEOUT;
import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.EXECUTION_TIMEOUT_PROPERTY;

/**
 * Server side implementation of the JavaScriptAssertion.
 *
 * @see com.l7tech.external.assertions.js.JavaScriptAssertion
 */
public class ServerJavaScriptAssertion extends AbstractServerAssertion<JavaScriptAssertion> {

    private static final Logger LOGGER = Logger.getLogger(ServerJavaScriptAssertion.class.getName());
    private final Config serverConfig;
    private final JavaScriptExecutor scriptExecutor;
    private transient CompiledScript compiledScript;

    public ServerJavaScriptAssertion(final JavaScriptAssertion assertion, final ApplicationContext applicationContext) {
        this(assertion, applicationContext, JavaScriptExecutor.getInstance());
    }

    public ServerJavaScriptAssertion(final JavaScriptAssertion assertion, final ApplicationContext applicationContext, final JavaScriptExecutor scriptExecutor) {
        super(assertion);
        this.serverConfig = applicationContext.getBean("serverConfig", ServerConfig.class);
        this.scriptExecutor = scriptExecutor;
        this.compiledScript = scriptExecutor.getCompiledScript(new JavaScriptExecutorOptions(
                assertion.getName(),
                assertion.getScript(),
                assertion.isStrictModeEnabled(),
                0));
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) {
        final String[] varsUsed = assertion.getVariablesUsed();
        final Map<String, Object> variableMap = context.getVariableMap(varsUsed, getAudit());
        final String scriptName = getScriptName(context);
        try {
            final JavaScriptExecutorOptions executorOptions = new JavaScriptExecutorOptions(
                    scriptName,
                    assertion.getScript(),
                    assertion.isStrictModeEnabled(),
                    getScriptExecutionTimeout(variableMap, assertion.getExecutionTimeout()));
            final Pair<CompiledScript, Object> resultPair = scriptExecutor.execute(compiledScript, context, executorOptions);

            // Update the compiled script if underlying script engine is changed.
            if (!compiledScript.getEngine().equals(resultPair.left.getEngine())) {
                compiledScript = resultPair.left;
            }

            final Object result = resultPair.right;

            /**
             * The script is expected to return TRUE or FALSE if the script was executed. If TRUE is NOT returned we
             * must falsify the assertion.
             */
            if (result != null && BooleanUtils.toBoolean(result.toString())) {
                return AssertionStatus.NONE;
            } else {
                LOGGER.warning("The javascript did not return TRUE. Falsifying the assertion.");
                return AssertionStatus.FALSIFIED;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | ScriptException | RuntimeScriptException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{ "Script failure: " + scriptName + ":" + ExceptionUtils.getMessage( e ) },
                    ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{ "Unexpected script failure: " + scriptName + ":" + ExceptionUtils.getMessage( e ) },
                    ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        }
    }

    private int getScriptExecutionTimeout(final Map<String, Object> variableMap, final String executionTimeoutString) {
        int executionTimeout = serverConfig.getIntProperty(EXECUTION_TIMEOUT_PROPERTY, DEFAULT_EXECUTION_TIMEOUT);

        if (StringUtils.isNotBlank(executionTimeoutString)) {
            try {
                executionTimeout = Integer.parseInt(ExpandVariables.process(executionTimeoutString, variableMap, getAudit()));
            } catch (NumberFormatException ex) {
                LOGGER.warning("Unable to parse Execution timeout. Using the global timeout value.");
            }
        }

        if (executionTimeout < 0) {
            LOGGER.warning("Invalid execution timeout value configured. Using the default timeout value " + DEFAULT_EXECUTION_TIMEOUT);
            executionTimeout = DEFAULT_EXECUTION_TIMEOUT;
        }

        return executionTimeout;
    }

    private String getScriptName(final PolicyEnforcementContext context) {
        String name = assertion.getName();
        if (StringUtils.isBlank(name)) {
            StringBuilder builder = new StringBuilder();
            if (context.getService() != null) {
                builder.append(context.getService().getName());
            }
            builder.append("_").append(assertion.getOrdinal());
            name = builder.toString();
        }
        return name;
    }
}

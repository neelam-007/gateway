package com.l7tech.external.assertions.js.features;

import com.l7tech.external.assertions.js.features.bindings.JavaScriptLoggerImpl;
import com.l7tech.server.message.PolicyEnforcementContext;

import javax.script.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.USE_STRICT_MODE_DIRECTIVE_STATEMENT;

/**
 * This class executes the Javascript using the JavaScriptEngineManager. Handles the execution time of the script.
 */
public class JavaScriptExecutor {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptExecutor.class.getName());

    private JavaScriptExecutorOptions executorOptions;

    public JavaScriptExecutor(final JavaScriptExecutorOptions executorOptions) {
        this.executorOptions = executorOptions;
    }

    /**
     * Executes the Javascript.
     * @param policyContext
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     * @throws ScriptException
     */
    public Object execute(final PolicyEnforcementContext policyContext) throws InterruptedException, ExecutionException, TimeoutException, ScriptException, JavaScriptException {
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        final JavaScriptEngineManager manager = JavaScriptEngineManager.getInstance();

        final String script = createWrappedJavaScript();
        final ScriptContext scriptContext = createScriptContext(manager, policyContext);
        final CompiledScript compiledScript = manager.getCompiledScript(script);

        final Future<Object> future = executor.submit(() -> compiledScript.eval(scriptContext));
        final Object result;

        if (executorOptions.getExecutionTimeout() != 0) {
            result = future.get(executorOptions.getExecutionTimeout(), TimeUnit.MILLISECONDS);
        } else {
            result = future.get();
        }

        executor.shutdownNow();
        return result;
    }

    /**
     * The script needs to wrapped under
     * (function() {
     *     // script goes here...
     * })();
     * to bring all the explicit global definitions to the functions local scope and return the status of the
     * execution. The script is expected to return TRUE or FALSE.
     *
     * @return the wrapped script to be executed
     */
    private String createWrappedJavaScript() {
        final StringBuilder scriptBuilder = new StringBuilder();

        if (executorOptions.isStrictModeEnabled()) {
            scriptBuilder.append(USE_STRICT_MODE_DIRECTIVE_STATEMENT);
        }

        scriptBuilder.append("(function(){");
        scriptBuilder.append(executorOptions.getScript());
        scriptBuilder.append("})();");

        return scriptBuilder.toString();
    }

    /**
     * Creates the ScriptContext with restricted objects.
     * @param manager JavaScriptEngineManager
     * @param policyContext
     * @return new ScriptContext instance with the required bindings
     */
    private ScriptContext createScriptContext(final JavaScriptEngineManager manager, final PolicyEnforcementContext policyContext) {
        final ScriptContext scriptContext = manager.getScriptEngineContext();
        final Bindings engineScopeBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);

        engineScopeBindings.put("context",
                new JavaScriptPolicyEnforcementContextImpl(policyContext, manager.getJsonScriptObjectMirror()));
        engineScopeBindings.put("logger", new JavaScriptLoggerImpl(executorOptions.getScriptName()));

        return scriptContext;
    }
}
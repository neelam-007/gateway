package com.l7tech.external.assertions.js.features;

import com.l7tech.external.assertions.js.RuntimeScriptException;
import com.l7tech.external.assertions.js.features.bindings.JavaScriptLogger;
import com.l7tech.external.assertions.js.features.bindings.JavaScriptPolicyEnforcementContextImpl;
import com.l7tech.external.assertions.js.features.bindings.JavaScriptLoggerImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Pair;

import javax.script.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.USE_STRICT_MODE_DIRECTIVE_STATEMENT;
import static com.l7tech.external.assertions.js.features.JavaScriptExecutor.JavaScriptExecutorPart.*;

/**
 * This class executes the Javascript using the JavaScriptEngineManager. Handles the execution time of the script.
 */
public class JavaScriptExecutor {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptExecutor.class.getName());

    public enum JavaScriptExecutorPart {
        EXECUTOR_FULL,
        EXECUTOR_ENGINE,
        EXECUTOR_POOL
    }

    private static final ExecutorService EXECUTOR_POOL = Executors.newCachedThreadPool();
    private final ScriptEngineProvider scriptEngineProvider;
    private transient JavaScriptEngineEssentials scriptEngineEssentials;

    private JavaScriptExecutor() {
        this.scriptEngineProvider = new NashornScriptEngineProvider();
        this.scriptEngineEssentials = new JavaScriptEngineEssentials(scriptEngineProvider);
    }

    /**
     * Lazy initializer for JavaScriptExecutor
     */
    private static class JavaScriptExecutorLazyInitializer {
        private static final JavaScriptExecutor INSTANCE = new JavaScriptExecutor();
        private JavaScriptExecutorLazyInitializer() {}
    }

    /**
     * Returns the singleton instance of the JavaScriptExecutor class.
     * @return
     */
    public static JavaScriptExecutor getInstance() {
        return JavaScriptExecutorLazyInitializer.INSTANCE;
    }

    /**
     * Gets the CompiledScript object from the cache.
     * @param executorOptions Execution options like enabling strict mode, execution timeout, and script to execute
     * @return CompiledScript The compiled javascript
     * @throws RuntimeScriptException if script compilation fails
     */
    public CompiledScript getCompiledScript(final JavaScriptExecutorOptions executorOptions) {
        try {
            return getCompiledScript(scriptEngineEssentials.getScriptEngine(), getWrappedJavaScript(executorOptions));
        } catch (ScriptException e) {
            throw new RuntimeScriptException(e);
        }
    }

    /**
     * Executes the Javascript.
     * @param compiledScript Compiled javascript to execute
     * @param policyContext PolicyEnforcementContext for the current call
     * @param executorOptions Execution options like strict mode, execution timeout, and the script
     * @return Returns the pair of new compiled script if the script was compiled again or the same compile
     * script and Script result (the script result is the value returned from the JavaScript code. It should be TRUE
     * or FALSE but it depends on the script code.)
     * @throws ScriptException thrown when the script fails for example syntax errors
     * @throws InterruptedException thrown if the execution is interrupted
     * @throws ExecutionException thrown if the execution fails
     * @throws TimeoutException thrown if the execution times out
     */
    public Pair<CompiledScript, Object> execute(final CompiledScript compiledScript, final PolicyEnforcementContext policyContext, final JavaScriptExecutorOptions executorOptions) throws ScriptException, InterruptedException, ExecutionException, TimeoutException {
        final CompiledScript localCompiledScript;
        if (!scriptEngineEssentials.getScriptEngine().equals(compiledScript.getEngine())) {
            localCompiledScript = getCompiledScript(scriptEngineEssentials.getScriptEngine(), getWrappedJavaScript(executorOptions));
        } else {
            localCompiledScript = compiledScript;
        }

        final ScriptContext localScriptContext = getScriptContext(scriptEngineEssentials, policyContext, executorOptions);
        if (executorOptions.getExecutionTimeout() <= 0) {
            return new Pair<>(localCompiledScript, localCompiledScript.eval(localScriptContext));
        } else {
            final Future<Object> future = EXECUTOR_POOL.submit(() -> localCompiledScript.eval(localScriptContext));
            final Object result = future.get(executorOptions.getExecutionTimeout(), TimeUnit.MILLISECONDS);
            return new Pair<>(localCompiledScript, result);
        }
    }

    /**
     * Re-initialize the ScriptEngine and clear the compiled scripts. Required when the ECMAVersion changes.
     */
    public synchronized void update(final JavaScriptExecutorPart option) {
        if (option == EXECUTOR_ENGINE || option == EXECUTOR_FULL) {
            scriptEngineEssentials = new JavaScriptEngineEssentials(scriptEngineProvider);
        }

        if (option == JavaScriptExecutorPart.EXECUTOR_POOL || option == EXECUTOR_FULL) {
            // TODO : Update pool's configuration
        }
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
    private String getWrappedJavaScript(final JavaScriptExecutorOptions executorOptions) {
        final StringBuilder scriptBuilder = new StringBuilder();

        if (executorOptions.isStrictModeEnabled()) {
            scriptBuilder.append(USE_STRICT_MODE_DIRECTIVE_STATEMENT);
        }

        scriptBuilder.append("(function(){");
        scriptBuilder.append(executorOptions.getScript());
        scriptBuilder.append("})();");

        return scriptBuilder.toString();
    }

    private CompiledScript getCompiledScript(final ScriptEngine scriptEngine, final String script) throws ScriptException {
        return ((Compilable)scriptEngine).compile(script);
    }

    /**
     * Returns the ScriptContext with restricted objects.
     * @param scriptEngineEssentials JavaScriptEngineEssentials containing JSON ScriptObjectMirror
     * @param policyContext PolicyEnforcementContext
     * @return new ScriptContext instance with the required bindings
     */
    private ScriptContext getScriptContext(final JavaScriptEngineEssentials scriptEngineEssentials, final
    PolicyEnforcementContext policyContext, final JavaScriptExecutorOptions executorOptions) {
        final ScriptContext scriptContext = scriptEngineEssentials.getScriptContext();
        final Bindings engineScopeBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);

        engineScopeBindings.put("context",
                new JavaScriptPolicyEnforcementContextImpl(policyContext, scriptEngineEssentials.getScriptJsonObjectMirror()));
        engineScopeBindings.put("logger", new JavaScriptLoggerImpl(executorOptions.getScriptName()));

        return scriptContext;
    }
}
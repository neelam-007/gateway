package com.l7tech.external.assertions.js.features;

import com.l7tech.external.assertions.js.RuntimeScriptException;
import com.l7tech.util.ExceptionUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Singleton manager class for creating Script context and executing the Javascript.
 */
public class JavaScriptEngineManager {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptEngineManager.class.getName());

    private ScriptEngine scriptEngine;
    private ScriptEngineProvider scriptEngineProvider;

    /* The cache to hold Compile JavaScripts */
    private final Map<String, CompiledScript> compiledScriptCache = new ConcurrentHashMap<>();

    /* The evaluated JSON script object mirror required for JSON processing. */
    private ScriptObjectMirror jsonScriptObjectMirror;

    public JavaScriptEngineManager() {
        scriptEngineProvider = new NashornScriptEngineProvider();
        scriptEngine = scriptEngineProvider.createScriptEngine();
        try {
            jsonScriptObjectMirror = (ScriptObjectMirror) scriptEngine.eval("JSON");
        } catch (ScriptException e) {
            LOGGER.warning("Unable to evaluate JSON ScriptObjectMirror: " + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * Helper initializer class for making JavaScriptEngineManager singleton.
     */
    private static class JavaScriptEngineManagerLazyInitializer {
        private static final JavaScriptEngineManager INSTANCE = new JavaScriptEngineManager();

        private JavaScriptEngineManagerLazyInitializer() {}
    }

    /**
     * Returns the singleton instance of the JavaScriptEngineManager class.
     * @return
     */
    public static JavaScriptEngineManager getInstance() {
        return JavaScriptEngineManagerLazyInitializer.INSTANCE;
    }

    /**
     * Gets the CompiledScript object from the cache.
     * @param script
     * @return
     * @throws ScriptException
     */
    public CompiledScript getCompiledScript(final String script) throws ScriptException {
        try {
            return compiledScriptCache.computeIfAbsent(script, s -> {
                try {
                    return ((Compilable) scriptEngine).compile(script);
                } catch (ScriptException e) {
                    throw new RuntimeScriptException(e);
                }
            });
        } catch (RuntimeScriptException ex) {
            throw (ScriptException) ex.getCause();
        }
    }

    public ScriptContext getScriptEngineContext() {
        return scriptEngineProvider.createScriptEngineContext(scriptEngine);
    }

    public ScriptEngine getScriptEngine() {
        return this.scriptEngine;
    }

    public ScriptObjectMirror getJsonScriptObjectMirror() {
        return jsonScriptObjectMirror;
    }

    /**
     * Re-initialize the ScriptEngine and clear the compiled scripts. Required when the ECMAVersion changes.
     */
    public synchronized void reInitializeEngine() {
        scriptEngine = scriptEngineProvider.createScriptEngine();
        compiledScriptCache.clear();
        try {
            jsonScriptObjectMirror = (ScriptObjectMirror) scriptEngine.eval("JSON");
        } catch (ScriptException e) {
            LOGGER.warning("Unable to evaluate JSON ScriptObjectMirror: " + ExceptionUtils.getMessage(e));
        }
    }
}
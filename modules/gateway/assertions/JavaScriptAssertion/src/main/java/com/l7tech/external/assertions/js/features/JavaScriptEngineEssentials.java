package com.l7tech.external.assertions.js.features;

import com.l7tech.external.assertions.js.RuntimeScriptException;
import com.l7tech.util.ExceptionUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.*;
import java.util.logging.Logger;

/**
 * Represents main place holder for ScriptEngine essentials inorder to run the compiled script.
 */
public class JavaScriptEngineEssentials {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptEngineEssentials.class.getName());
    private final ScriptEngineProvider scriptEngineProvider;
    private final ScriptEngine scriptEngine;
    private final ScriptObjectMirror scriptJsonObjectMirror;
    private final Bindings scriptGlobalBindings;

    public JavaScriptEngineEssentials(final ScriptEngineProvider scriptEngineProvider) {
        this.scriptEngineProvider = scriptEngineProvider;
        this.scriptEngine = scriptEngineProvider.createScriptEngine();
        this.scriptGlobalBindings = scriptEngineProvider.createScriptEngineBindings(scriptEngine);

        // Initialize the global bindings by running the engine.global.js script
        // This binding will be used while running the user specified script.
        // So that, they can see the expected global definitions.
        final InputStream inputStream = JavaScriptEngineEssentials.class.getClassLoader().getResourceAsStream("com/l7tech/external/assertions/js/engine.global.js");
        if (inputStream != null) {
            try (Reader scriptReader = new InputStreamReader(inputStream)) {
                scriptEngine.eval(scriptReader, scriptGlobalBindings);
            } catch (ScriptException e) {
                throw new RuntimeScriptException(e);
            } catch (IOException e) {
                throw new RuntimeScriptException(new ScriptException(e));
            }
        }

        this.scriptJsonObjectMirror = createScriptJsonObjectMirror(scriptEngine);
    }

    /**
     * Returns <code>ScriptEngine</code> instance.
     * @return
     */
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    /**
     * Returns <code>ScriptObjectMirror</code> for JSON definition.
     * @return
     */
    public ScriptObjectMirror getScriptJsonObjectMirror() {
        return scriptJsonObjectMirror;
    }

    /**
     * Returns <code>ScriptContext</code> instance to run the compiled script.
     * @return <code>ScriptContext</code> instance containing new ENGINE_SCOPE bindings but common GLOBAL_SCOPE bindings.
     */
    public ScriptContext getScriptContext() {
        final ScriptContext scriptContext = scriptEngineProvider.createScriptEngineContext(scriptEngine);
        scriptContext.setBindings(scriptGlobalBindings, ScriptContext.GLOBAL_SCOPE);
        return scriptContext;
    }

    private ScriptObjectMirror createScriptJsonObjectMirror(final ScriptEngine scriptEngine) {
        try {
            return (ScriptObjectMirror) scriptEngine.eval("JSON");
        } catch (ScriptException e) {
            LOGGER.warning("Unable to evaluate JSON ScriptObjectMirror: " + ExceptionUtils.getMessage(e));
            return null;
        }
    }
}

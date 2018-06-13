package com.l7tech.external.assertions.js.features;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

public interface ScriptEngineProvider {

    /**
     * Provides the Nashorn Script Engine
     * @return
     */
    ScriptEngine createScriptEngine();

    /**
     * Returns new <code>ScriptContext</code> to run the script by <code>ScriptEngine</code>
     * @param scriptEngine ScriptEngine for which the context needs to be provided to run the script
     * @return new <code>ScriptContext</code>.
     */
    ScriptContext createScriptEngineContext(final ScriptEngine scriptEngine);

    /**
     * Returns new <code>Bindings</code> to run the script by <code>ScriptEngine</code>
     * @param scriptEngine ScriptEngine for which the context's bindings needs to be provided to run the script
     * @return new <code>Bindings</code>
     */
    Bindings createScriptEngineBindings(final ScriptEngine scriptEngine);

}

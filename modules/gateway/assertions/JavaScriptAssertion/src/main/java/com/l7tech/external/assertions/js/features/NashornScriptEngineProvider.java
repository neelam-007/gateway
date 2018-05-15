package com.l7tech.external.assertions.js.features;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.util.ConfigFactory;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.SimpleScriptContext;
import java.io.OutputStreamWriter;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.*;

/**
 * Nashorn Javascript Engine Provider
 */
public class NashornScriptEngineProvider implements ScriptEngineProvider {

    private static final String ARG_LANGUAGE = "--language=";
    private static final String ARG_NO_JAVA = "--no-java";
    private static final String ARG_NO_SYNTAX_EXTENSIONS = "--no-syntax-extensions";
    private static final String[] UNSAFE_DEFINITIONS = { "Function", "eval", "uneval", "load", "loadWithNewGlobal", "exit", "quit", "print" };
    private NashornScriptEngineFactory scriptEngineFactory = new NashornScriptEngineFactory();

    @Override
    public ScriptEngine createScriptEngine() {
        return scriptEngineFactory.getScriptEngine(
                getScriptEngineOptions(),
                new AlwaysFailingClassLoader(),
                new AlwaysFailingClassFilter());
    }

    @Override
    public ScriptContext createScriptEngineContext(final ScriptEngine scriptEngine) {
        final ScriptContext scriptContext = new SimpleScriptContext();

        scriptContext.setBindings(createScriptEngineBindings(scriptEngine), ScriptContext.GLOBAL_SCOPE);
        scriptContext.setBindings(createScriptEngineBindings(scriptEngine), ScriptContext.ENGINE_SCOPE);

        scriptContext.setErrorWriter(new OutputStreamWriter( new NullOutputStream()));
        scriptContext.setWriter(new OutputStreamWriter( new NullOutputStream()));

        return scriptContext;
    }

    private Bindings createScriptEngineBindings(final ScriptEngine scriptEngine) {
        final Bindings bindings = scriptEngine.createBindings();

        for (final String def : UNSAFE_DEFINITIONS) {
            bindings.remove(def);
        }

        return bindings;
    }

    private String[] getScriptEngineOptions() {
        final String ecmaVersion = ConfigFactory.getProperty(ECMA_VERSION_PROPERTY, DEFAULT_ECMA_VERSION);
        return new String[] {
                ARG_LANGUAGE + ecmaVersion,
                ARG_NO_JAVA,
                ARG_NO_SYNTAX_EXTENSIONS
        };
    }

    /**
     * ClassLoader implementation which fails always to avoid usage of any Java classes
     */
    private static class AlwaysFailingClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass( final String name ) throws ClassNotFoundException {
            throw new ClassNotFoundException( "Class loading not permitted within script: " + name );
        }
    }

    /**
     * ClassFilter implementation which returns false to avoid exposing any Java class to the script.
     */
    private static class AlwaysFailingClassFilter implements ClassFilter {
        @Override
        public boolean exposeToScripts( final String s ) {
            return false;
        }
    }
}

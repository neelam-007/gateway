package com.l7tech.external.assertions.js.server;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.js.JavaScriptAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.script.ScriptContext;
import javax.script.ScriptException;

/**
 * Server side implementation of the JavaScriptAssertion.
 *
 * @see com.l7tech.external.assertions.js.JavaScriptAssertion
 */
public class ServerJavaScriptAssertion extends AbstractServerAssertion<JavaScriptAssertion> {
    static boolean allowNoSecurityManager = false;  // for testing

    private ApplicationContext applicationContext;
    private String script;

    public ServerJavaScriptAssertion(JavaScriptAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.applicationContext = context;
        this.script = assertion.decodeScript();

        if ( null == System.getSecurityManager() && !allowNoSecurityManager ) {
            throw new PolicyAssertionException( assertion, "Unable to instantiate JavaScriptAssertion when Gateway is running without a SecurityManager" );
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

        // Restrict access to Java classes not passed in as part of the published interface for scripts
        NashornScriptEngine engine = (NashornScriptEngine)factory.getScriptEngine(
                new String[] { "-strict", "--no-java", "--no-syntax-extensions" },
                new AlwaysFailingClassLoader(),
                new AlwaysFailingClassFilter()
        );

        ScriptContext scriptContext = engine.getContext();

        // Prevent stdout/stderr written from script from going anywhere
        scriptContext.setErrorWriter( new OutputStreamWriter( new NullOutputStream() ) );
        scriptContext.setWriter( new OutputStreamWriter( new NullOutputStream() ) );

        // Prevent script from loading remote scripts
        engine.put("load", null);
        engine.put("loadWithNewGlobal", null);

        // Prevent script from doing dynamic evaluation of other script code
        engine.put("eval", null);

        // Prevent script from terminating the Gateway process
        engine.put("exit", null);
        engine.put("quit", null);

        // Hook up public API for JavaScript to consume
        engine.put( "context", context );
        engine.put( "appContext", applicationContext );
        engine.put( "request", context.getRequest() );
        engine.put( "response", context.getResponse() );

        try {
            Object result = engine.eval( script );

            if (isTruth(result))
                return AssertionStatus.NONE;
            else
                return AssertionStatus.FALSIFIED;

        } catch ( ScriptException e ) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{ "Script failure: " + ExceptionUtils.getMessage( e ) },
                    ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        }
    }

    private boolean isTruth(Object result) throws ScriptException {
        if (result == null)
            throw new ScriptException("Script return value was null");
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
        throw new ScriptException("Script did not return (true or nonzero) or (false or zero).  Return value: " + result.getClass() + "=" + result);
    }

    public static class AlwaysFailingClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass( String name ) throws ClassNotFoundException {
            throw new ClassNotFoundException( "Class loading not permitted within script: " + name );
        }
    }

    public static class AlwaysFailingClassFilter implements ClassFilter {
        @Override
        public boolean exposeToScripts( String s ) {
            return false;
        }
    }
}

package com.l7tech.external.assertions.script.server;

import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.message.Message;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;
import org.apache.bsf.BSFManager;

import java.util.logging.Logger;
import java.io.PrintStream;

/**
 * Test the ScriptAssertion.
 */
public class ServerScriptAssertionTest {

    private static final Logger log = Logger.getLogger(ServerScriptAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    public static class MyThing {
        public String blee;
        public int blah;
        boolean printedCrap = false;

        public String getBlee() {
            return blee;
        }

        public void setBlee(String blee) {
            this.blee = blee;
        }

        public int getBlah() {
            return blah;
        }

        public void setBlah(int blah) {
            this.blah = blah;
        }

        public void printCrap() {
            System.out.println("Upcall back to Java: printing crap: \"CRAP!!\"");
            printedCrap = true;
        }
    }

    private final String js =
            "\n" +
            "function checkRequest(appContext, policyContext) {\n" +
            "  out.println(\"In javascript: appContext=\" + appContext + \", policyContext=\" + policyContext);\n" +
            "  appContext.printCrap();\n" +
            "  appContext.setBlah(5);\n" +
            "  return true;\n" +
            "}\n" +
            "\n" +
            "checkRequest(appContext, policyContext);";

    @Test
    public void testJavascript() throws Exception {
        BSFManager manager = new BSFManager();
        MyThing myThing = new MyThing();
        manager.declareBean("appContext", myThing, MyThing.class);
        manager.declareBean("policyContext", PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message()), PolicyEnforcementContext.class);
        manager.declareBean("out", System.out, PrintStream.class);
        show(manager.eval("javascript", "policy/293823/assertionOrdinal/4242/script", 0, 0, js));
        manager.terminate();
        assertTrue(myThing.printedCrap);
        assertEquals(myThing.getBlah(), 5);
    }

    private void show(Object result) {
        if (result == null)
            System.out.println("Script returned null");
        else
            System.out.println("Script returned result " + result.getClass() + "=" + result.toString());
    }

}

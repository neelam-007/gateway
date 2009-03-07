package com.l7tech.util;

import org.junit.*;
import static org.junit.Assert.*;

import javax.tools.Diagnostic;
import java.io.*;
import java.util.logging.Logger;

/**
 * Test for {@link Codegen}
 */
public class CodegenTest {
    private static final Logger log = Logger.getLogger(CodegenTest.class.getName());

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    private static final String INTERFACE_SOURCE =
            "package com.l7tech.util;\n" +
            "import java.io.PrintStream;\n" +
            "public interface CodegenTestThingPrinter {\n" +
            "    /**\n" +
            "     * Pring a greeting to the specified PrintStream, including the given suffix.\n" +
            "     *\n" +
            "     * @param out a PrintStream to send the greeting to.  Required.\n" +
            "     * @param suffix the suffix to attach.  Required.\n" +
            "     * @return the String that was printed to the PrintStream.\n" +
            "     */\n" +
            "    String printThing(PrintStream out, String suffix);\n" +
            '}';

    @Test
    public void testCodegen() throws Exception {
        StringWriter writer = new StringWriter();
        PrintWriter out = null;
        try {
            out = new PrintWriter(writer);
            out.println("package com.l7tech.util;");
            out.println("public class DynamicThingPrinter implements com.l7tech.util.CodegenTestThingPrinter {");
            out.println("  public String printThing(java.io.PrintStream out, String suffix) {");
            out.println("    String ret = \"Hello from generated code!  Suffix=\" + suffix;");
            out.println("    out.println(ret);");
            out.println("    return ret;");
            out.println("  }");
            out.println("}");
        } finally {
            if (out != null) out.close();
        }

        final Codegen codegen = new Codegen("com.l7tech.util.DynamicThingPrinter", writer.toString());
        codegen.addJavaFile(CodegenTestThingPrinter.class.getName(), INTERFACE_SOURCE);

        try {
            Class thingClass = codegen.compile(getClass().getClassLoader());
            CodegenTestThingPrinter thingPrinter = ((CodegenTestThingPrinter)thingClass.newInstance());
            //noinspection IOResourceOpenedButNotSafelyClosed
            final PrintStream nullps = new PrintStream(new OutputStream() {
                public void write(int b) throws IOException { }
            });
            String result = thingPrinter.printThing(nullps, "Walla Walla");
            assertEquals("Hello from generated code!  Suffix=Walla Walla", result);
        } catch (Exception e) {
            for (Diagnostic diagnostic : codegen.getDiagnostics()) {
                log.warning("code:" + diagnostic.getCode());
                log.warning("kind:" + diagnostic.getKind());
                log.warning("position: " + diagnostic.getPosition());
                log.warning("startPosition: " + diagnostic.getStartPosition());
                log.warning("endPosition: " + diagnostic.getEndPosition());
                log.warning("source: " + diagnostic.getSource());
                log.warning("message: " + diagnostic.getMessage(null));
            }
            fail("Codegen compile failed; see log for more info");
        }
    }
}

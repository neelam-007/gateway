package com.l7tech.external.assertions.ipm.server;

import com.l7tech.common.util.XmlUtil;
import static com.l7tech.external.assertions.ipm.server.ServerIpmAssertionTest.*;
import com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate;
import com.l7tech.skunkworks.BenchmarkRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 */
public class TemplateCompilerTest {
    private static final Logger log = Logger.getLogger(TemplateCompilerTest.class.getName());

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTemplateCompiler() throws Exception {
        final TemplateCompiler templateCompiler = new TemplateCompiler(loadFile(TEMPLATE_PAC_REPLY));
        CompiledTemplate ct;
        try {
            ct = templateCompiler.compile();
        } finally {
            //log.info("Generated Java source: " + templateCompiler.getJavaSource());
        }

        String requestSoapStr = loadFile(SOAP_PAC_REPLY);
        String requestStr = extractDataBuff(requestSoapStr);

        ct.init(requestStr.toCharArray());
        ct.expand();
        String expandedStr = new String(ct.getResult(), 0, ct.getResultSize());

        assertTrue(expandedStr.length() > 0);
        //log.info("Expansion result:\n" + expandedStr);
        XmlUtil.stringToDocument(expandedStr);

        String oneline = expandedStr.replaceAll("(\\012|\\015)", "");
        assertTrue(oneline.endsWith("<PACQUERY-SKU-QTY>0000000</PACQUERY-SKU-QTY>" +
                                    "<PACQUERY-SKU-QTY-SIGN> </PACQUERY-SKU-QTY-SIGN>" +
                                    "</PACQUERY-ORDER-SKU-INFO>" +
                                    "<PACQUERY-MORE-ORDER-SKU-INFO>N</PACQUERY-MORE-ORDER-SKU-INFO>" +
                                    "</PREMIER-ACCESS-QUERY-REPLY>"));
    }
    
    //@Test
    public void testPerformance() throws Exception {
        final TemplateCompiler templateCompiler = new TemplateCompiler(loadFile(TEMPLATE_PAC_REPLY));
        final CompiledTemplate compiledTemplate;
        compiledTemplate = templateCompiler.compile();

        String requestSoapStr = loadFile(SOAP_PAC_REPLY);
        final String requestStr = extractDataBuff(requestSoapStr);

        new BenchmarkRunner(new Runnable() {
            public void run() {
                for (int i = 0; i < 100; ++i) {
                    /* Do nothing */
                }
            }
        }, 1000, "donothing").run();

        compiledTemplate.close();
        final Class<? extends CompiledTemplate> ctClass = compiledTemplate.getClass();
        final ThreadLocal<CompiledTemplate> testCt = new ThreadLocal<CompiledTemplate>() {
            protected CompiledTemplate initialValue() {
                try {
                    return ctClass.newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final Runnable expansionRunnable = new Runnable() {
            public void run() {
                CompiledTemplate ct = testCt.get();
                for (int i = 0; i < 1000; ++i) {
                    expandOne(ct);
                }
            }

            private void expandOne(CompiledTemplate ct) {
                try {
                    ct.init(requestStr.toCharArray());
                    ct.expand();
                    //noinspection UnusedDeclaration
                    String expandedStr = new String(ct.getResult(), 0, ct.getResultSize());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        new BenchmarkRunner(expansionRunnable, 10, "expansion burn-in").run();
        new BenchmarkRunner(expansionRunnable, 10, "expansion").run();
    }

    @Test
    public void testPrecompiledTemplate() throws Exception {
        String requestSoapStr = loadFile(SOAP_PAC_REPLY);
        String requestStr = extractDataBuff(requestSoapStr);

        CompiledTemplate ct = new PreCompiledTemplate();
        ct.init(requestStr.toCharArray());
        ct.expand();
        String expandedStr = new String(ct.getResult(), 0, ct.getResultSize());

        assertTrue(expandedStr.length() > 0);
        //log.info("Expansion result:\n" + expandedStr);
        XmlUtil.stringToDocument(expandedStr);

        String oneline = expandedStr.replaceAll("(\\012|\\015)", "");
        assertTrue(oneline.endsWith("<PACQUERY-SKU-QTY>0000000</PACQUERY-SKU-QTY>" +
                                    "<PACQUERY-SKU-QTY-SIGN> </PACQUERY-SKU-QTY-SIGN>" +
                                    "</PACQUERY-ORDER-SKU-INFO>" +
                                    "<PACQUERY-MORE-ORDER-SKU-INFO>N</PACQUERY-MORE-ORDER-SKU-INFO>" +
                                    "</PREMIER-ACCESS-QUERY-REPLY>"));
    }

    /** GENERATED by template compiler -- do not edit, except to replace with updated version from output of testTemplateCompiler(). */
    private static class PreCompiledTemplate extends CompiledTemplate {
        protected void doExpand() throws java.io.IOException {
          write("<PREMIER-ACCESS-QUERY-REPLY>".toCharArray());
          write("<PACQUERY-ORDER-NBR>".toCharArray());
          copy(9);
          write("</PACQUERY-ORDER-NBR>\n".toCharArray());
          write("<PACQUERY-TIE-NUM>".toCharArray());
          copy(3);
          write("</PACQUERY-TIE-NUM>\n".toCharArray());
          write("<PACQUERY-CONTRACT-STATUS>".toCharArray());
          copy(5);
          write("</PACQUERY-CONTRACT-STATUS>\n".toCharArray());
          write("<PACQUERY-SYSTEM-TYPE>".toCharArray());
          copy(5);
          write("</PACQUERY-SYSTEM-TYPE>\n".toCharArray());
          write("<PACQUERY-ORDER-SKU-INFO-NBR>".toCharArray());
          copy(3);
          write("</PACQUERY-ORDER-SKU-INFO-NBR>\n".toCharArray());
          for (int i1 = 0; i1 < 200; i1++) {
              write("<PACQUERY-ORDER-SKU-INFO>".toCharArray());
              write("<PACQUERY-DETAIL-SEQ-NBR>".toCharArray());
              copy(3);
              write("</PACQUERY-DETAIL-SEQ-NBR>\n".toCharArray());
              write("<PACQUERY-SKU-NBR>".toCharArray());
              copy(13);
              write("</PACQUERY-SKU-NBR>\n".toCharArray());
              write("<PACQUERY-SKU-DESC>".toCharArray());
              copy(40);
              write("</PACQUERY-SKU-DESC>\n".toCharArray());
              write("<PACQUERY-SKU-QTY>".toCharArray());
              copy(7);
              write("</PACQUERY-SKU-QTY>\n".toCharArray());
              write("<PACQUERY-SKU-QTY-SIGN>".toCharArray());
              copy(1);
              write("</PACQUERY-SKU-QTY-SIGN>\n".toCharArray());
              write("</PACQUERY-ORDER-SKU-INFO>\n".toCharArray());
          }
          write("<PACQUERY-MORE-ORDER-SKU-INFO>".toCharArray());
          copy(1);
          write("</PACQUERY-MORE-ORDER-SKU-INFO>\n".toCharArray());
          write("</PREMIER-ACCESS-QUERY-REPLY>\n".toCharArray());
        }
    }
}

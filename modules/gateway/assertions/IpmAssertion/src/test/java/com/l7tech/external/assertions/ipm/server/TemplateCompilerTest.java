package com.l7tech.external.assertions.ipm.server;

import com.l7tech.common.io.XmlUtil;
import static com.l7tech.external.assertions.ipm.server.ServerIpmAssertionTest.*;
import com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unit tests for {@link TemplateCompiler}.
 */
public class TemplateCompilerTest {
    protected static final Logger logger = Logger.getLogger(TemplateCompilerTest.class.getName());

    public static final int BUFFSIZE = 131040;

    /** Set to true to do slower but more accurate performance tests. */
    private static final boolean LONG_PERFORMANCE_TEST = Boolean.getBoolean("ipm.test.perf.full");

    @Test
    public void testTemplateCompiler() throws Exception {
        final TemplateCompiler templateCompiler = new TemplateCompiler(loadFile(TEMPLATE_PAC_REPLY));
        CompiledTemplate ct;
        try {
            ct = templateCompiler.compile();
        } finally {
            logger.info("Generated Java source: \n" + templateCompiler.getJavaSource());
        }

        String requestStr = extractDataBuff(loadFile(SOAP_PAC_REPLY));

        String expandedStr = ct.expand(requestStr.toCharArray(), new char[BUFFSIZE]);
        checkExpandedString(expandedStr, null);

        byte[] out = new byte[BUFFSIZE * 2];
        int got = ct.expandBytes(requestStr.toCharArray(), out);
        expandedStr = new String(out, 0, got, "UTF-8");
        checkExpandedString(expandedStr, null);
    }

    private static void checkExpandedString(String expandedStr, String extraSuffix) throws SAXException {
        assertTrue(expandedStr.length() > 0);
        logger.info("Expansion result:\n" + expandedStr);
        XmlUtil.stringToDocument(expandedStr);

        checkExpandedStringEnding(expandedStr, extraSuffix);
    }

    private static void checkExpandedStringEnding(String expandedStr, String extraSuffix) {
        String oneline = expandedStr.replaceAll("(\\012|\\015)", "");
        assertTrue(oneline.endsWith("<PACQUERY-SKU-QTY>0000000</PACQUERY-SKU-QTY>" +
                                    "<PACQUERY-SKU-QTY-SIGN> </PACQUERY-SKU-QTY-SIGN>" +
                                    "</PACQUERY-ORDER-SKU-INFO>" +
                                    "<PACQUERY-MORE-ORDER-SKU-INFO>N</PACQUERY-MORE-ORDER-SKU-INFO>" +
                                    "</PREMIER-ACCESS-QUERY-REPLY>" + (extraSuffix == null ? "" : extraSuffix)));
    }

    @Test
    public void testTemplateWithExtraSurroundingXml() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        template = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                   "<soap:Body>" +
                   template +
                   "</soap:Body>\n" +
                   "</soap:Envelope>";

        String requestStr = extractDataBuff(loadFile(SOAP_PAC_REPLY));

        CompiledTemplate ct = new TemplateCompiler(template).compile();
        String expandedStr = ct.expand(requestStr.toCharArray(), new char[BUFFSIZE]);

        final String expectedSuffix = "</soap:Body></soap:Envelope>";
        checkExpandedString(expandedStr, expectedSuffix);

        byte[] out = new byte[BUFFSIZE * 2];
        int got = ct.expandBytes(requestStr.toCharArray(), out);
        expandedStr = new String(out, 0, got, "UTF-8");
        checkExpandedString(expandedStr, expectedSuffix);
    }

    @Test
    public void testInputTooShort() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        CompiledTemplate ct = new TemplateCompiler(template).compile();

        String shortInput = "630040259002NONE WSTAT021022220-3457     C BASE WS410 MT 400MHZ/512K             0000005 023220-0657     C TERMINAT";

        try {
            ct.expand(shortInput.toCharArray(), new char[BUFFSIZE]);
            fail("Expected exception was not thrown");
        } catch (CompiledTemplate.InputBufferEmptyException e) {
            // Ok
        }

        try {
            ct.expandBytes(shortInput.toCharArray(), new byte[BUFFSIZE*2]);
            fail("Expected exception was not thrown");
        } catch (CompiledTemplate.InputBufferEmptyException e) {
            // Ok
        }
    }

    @Test
    public void testOutputBufferTooSmall() throws Exception {
        int shortBuffsize = 8192;

        String template = loadFile(TEMPLATE_PAC_REPLY);
        CompiledTemplate ct = new TemplateCompiler(template).compile();

        String requestStr = extractDataBuff(loadFile(SOAP_PAC_REPLY));

        try {
            ct.expand(requestStr.toCharArray(), new char[shortBuffsize]);
            fail("Expected exception was not thrown");
        } catch (CompiledTemplate.OutputBufferFullException e) {
            // Ok
        }

        try {
            ct.expandBytes(requestStr.toCharArray(), new byte[shortBuffsize*2]);
            fail("Expected exception was not thrown");
        } catch (CompiledTemplate.OutputBufferFullException e) {
            // Ok
        }
    }

    @Test
    public void testExpansionOfNonAscii() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        String requestStr = extractDataBuff(loadFile(SOAP_PAC_REPLY));
        final String unicodeSubstring = "M\u03A3N";
        requestStr = requestStr.replaceAll("MON", unicodeSubstring);

        CompiledTemplate ct = new TemplateCompiler(template).compile();
        String expandedStr = ct.expand(requestStr.toCharArray(), new char[BUFFSIZE]);

        checkExpandedString(expandedStr, null);
        assertTrue(expandedStr.contains(unicodeSubstring));

        byte[] out = new byte[BUFFSIZE * 2];
        int got = ct.expandBytes(requestStr.toCharArray(), out);
        expandedStr = new String(out, 0, got, "UTF-8");
        checkExpandedString(expandedStr, null);
        assertTrue(expandedStr.contains(unicodeSubstring));
    }

    @Test
    public void testExpansionOfNonBmp() throws Exception {
        String template = loadFile(TEMPLATE_PAC_REPLY);
        String requestStr = extractDataBuff(loadFile(SOAP_PAC_REPLY));
        final String unicodeSubstring = "M\uD811\uDE42"; // some character outside the Basic Multilingual Plane
        requestStr = requestStr.replaceAll("MON", unicodeSubstring);

        CompiledTemplate ct = new TemplateCompiler(template).compile();
        String expandedStr = ct.expand(requestStr.toCharArray(), new char[BUFFSIZE]);

        checkExpandedString(expandedStr, null);
        assertTrue(expandedStr.contains(unicodeSubstring));

        byte[] out = new byte[BUFFSIZE * 2];
        try {
            int got = ct.expandBytes(requestStr.toCharArray(), out);
            expandedStr = new String(out, 0, got, "UTF-8");
            checkExpandedString(expandedStr, null);
            assertTrue(expandedStr.contains(unicodeSubstring));

            // This worked, but shouldn't have.  The test should be altered if this limitation is removed.
            fail("Expected exception not thrown -- bytes expansion is current unable to cope with a non-BMP character");
        } catch (IOException e) {
            if (e.getMessage().contains("Unable to process input character outside the Basic Multilingual Plane:")) {
                // Ok -- this is a current, documented limitation of the bytes expansion
                logger.log(Level.FINE, "Caught expected exception: Bytes expansion unable to cope with non-BMP character");
            } else {
                throw e;
            }
        }
    }

    @Test
    public void testExpandCharPerformance() throws Exception {
        final TemplateCompiler templateCompiler = new TemplateCompiler(loadFile(TEMPLATE_PAC_REPLY));
        final CompiledTemplate compiledTemplate;
        compiledTemplate = templateCompiler.compile();

        String requestSoapStr = loadFile(SOAP_PAC_REPLY);
        final String requestStr = extractDataBuff(requestSoapStr);

        runInLoop(new Runnable() {
            public void run() {
                for (int i = 0; i < 100; ++i) {
                    /* Do nothing */
                }
            }
        }, 1000, "donothing");

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
                char[] out = new char[BUFFSIZE];
                for (int i = 0; i < 1000; ++i) {
                    expandOne(ct, out);
                }
            }

            private void expandOne(CompiledTemplate ct, char[] out) {
                try {
                    //noinspection UnusedDeclaration
                    String expandedStr = ct.expand(requestStr.toCharArray(), out);
                } catch (CompiledTemplate.InputBufferEmptyException e) {
                    throw new RuntimeException(e);
                } catch (CompiledTemplate.OutputBufferFullException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        int iterations = getPerfIterations();
        runInLoop(expansionRunnable, iterations, "char expansion burn-in");
        runInLoop(expansionRunnable, iterations, "char expansion");
    }

    @Test
    public void testExpandBytesPerformance() throws Exception {
        final TemplateCompiler templateCompiler = new TemplateCompiler(loadFile(TEMPLATE_PAC_REPLY));
        final CompiledTemplate compiledTemplate;
        compiledTemplate = templateCompiler.compile();

        String requestSoapStr = loadFile(SOAP_PAC_REPLY);
        final String requestStr = extractDataBuff(requestSoapStr);

        runInLoop(new Runnable() {
            public void run() {
                for (int i = 0; i < 100; ++i) {
                    /* Do nothing */
                }
            }
        }, 1000, "donothing");

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
                byte[] out = new byte[BUFFSIZE * 2];
                for (int i = 0; i < 1000; ++i) {
                    expandOne(ct, out);
                }
            }

            private void expandOne(CompiledTemplate ct, byte[] out) {
                try {
                    //noinspection UnusedDeclaration
                    int got = ct.expandBytes(requestStr.toCharArray(), out);
                    byte[] gotBytes = new byte[got];
                    System.arraycopy(out, 0, gotBytes, 0, got);
                } catch (CompiledTemplate.InputBufferEmptyException e) {
                    throw new RuntimeException(e);
                } catch (CompiledTemplate.OutputBufferFullException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        int iterations = getPerfIterations();
        runInLoop(expansionRunnable, iterations, "char expansion burn-in");
        runInLoop(expansionRunnable, iterations, "char expansion");
    }

    private static int getPerfIterations() {
        return LONG_PERFORMANCE_TEST ? 100 : 10;
    }

    private void runInLoop( final Runnable runnable, final int count, final String name ) {
        long start = System.currentTimeMillis();
        for ( int i=0; i<count; i++ ) {
            runnable.run();            
        }
        long end = System.currentTimeMillis();

        long t = (end - start);
        if (t == 0) t = 1;
        System.out.println( name + ": total time = " + t + "ms (" + count / (t / 1000f) + "/s)" );
    }

    @Test
    public void testPrecompiledTemplate() throws Exception {
        String requestSoapStr = loadFile(SOAP_PAC_REPLY);
        String requestStr = extractDataBuff(requestSoapStr);

        CompiledTemplate ct = new PreCompiledTemplate();
        String expandedStr = ct.expand(requestStr.toCharArray(), new char[BUFFSIZE]);

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

    /** GENERATED by template compiler -- do not edit, except to replace with updated version from output of {@link TemplateCompilerTest#testTemplateCompiler()}. */
    private static class PreCompiledTemplate extends CompiledTemplate {
        protected void doExpand() throws InputBufferEmptyException, OutputBufferFullException {
            write(C1);
            copy(9);
            write(C2);
            copy(3);
            write(C3);
            copy(5);
            write(C4);
            copy(5);
            write(C5);
            copy(3);
            write(C6);
            for (int i1 = 0; i1 < 200; i1++) {
                write(C7);
                copy(3);
                write(C8);
                copy(13);
                write(C9);
                copy(40);
                write(C10);
                copy(7);
                write(C11);
                copy(1);
                write(C12);
            }
            write(C13);
            copy(1);
            write(C14);
        }

        protected void doExpandBytes() throws com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate.InputBufferEmptyException, com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate.OutputBufferFullException, java.io.IOException {
            writb(B15);
            cpyb(9);
            writb(B16);
            cpyb(3);
            writb(B17);
            cpyb(5);
            writb(B18);
            cpyb(5);
            writb(B19);
            cpyb(3);
            writb(B20);
            for (int i1 = 0; i1 < 200; i1++) {
                writb(B21);
                cpyb(3);
                writb(B22);
                cpyb(13);
                writb(B23);
                cpyb(40);
                writb(B24);
                cpyb(7);
                writb(B25);
                cpyb(1);
                writb(B26);
            }
            writb(B27);
            cpyb(1);
            writb(B28);
        }

        private static final char[] C1;
        private static final char[] C2;
        private static final char[] C3;
        private static final char[] C4;
        private static final char[] C5;
        private static final char[] C6;
        private static final char[] C7;
        private static final char[] C8;
        private static final char[] C9;
        private static final char[] C10;
        private static final char[] C11;
        private static final char[] C12;
        private static final char[] C13;
        private static final char[] C14;
        private static final byte[] B15;
        private static final byte[] B16;
        private static final byte[] B17;
        private static final byte[] B18;
        private static final byte[] B19;
        private static final byte[] B20;
        private static final byte[] B21;
        private static final byte[] B22;
        private static final byte[] B23;
        private static final byte[] B24;
        private static final byte[] B25;
        private static final byte[] B26;
        private static final byte[] B27;
        private static final byte[] B28;

        static {
            try {
                C1 = "<PREMIER-ACCESS-QUERY-REPLY><PACQUERY-ORDER-NBR>".toCharArray();
                C2 = "</PACQUERY-ORDER-NBR>\n<PACQUERY-TIE-NUM>".toCharArray();
                C3 = "</PACQUERY-TIE-NUM>\n<PACQUERY-CONTRACT-STATUS>".toCharArray();
                C4 = "</PACQUERY-CONTRACT-STATUS>\n<PACQUERY-SYSTEM-TYPE>".toCharArray();
                C5 = "</PACQUERY-SYSTEM-TYPE>\n<PACQUERY-ORDER-SKU-INFO-NBR>".toCharArray();
                C6 = "</PACQUERY-ORDER-SKU-INFO-NBR>\n".toCharArray();
                C7 = "<PACQUERY-ORDER-SKU-INFO><PACQUERY-DETAIL-SEQ-NBR>".toCharArray();
                C8 = "</PACQUERY-DETAIL-SEQ-NBR>\n<PACQUERY-SKU-NBR>".toCharArray();
                C9 = "</PACQUERY-SKU-NBR>\n<PACQUERY-SKU-DESC>".toCharArray();
                C10 = "</PACQUERY-SKU-DESC>\n<PACQUERY-SKU-QTY>".toCharArray();
                C11 = "</PACQUERY-SKU-QTY>\n<PACQUERY-SKU-QTY-SIGN>".toCharArray();
                C12 = "</PACQUERY-SKU-QTY-SIGN>\n</PACQUERY-ORDER-SKU-INFO>\n".toCharArray();
                C13 = "<PACQUERY-MORE-ORDER-SKU-INFO>".toCharArray();
                C14 = "</PACQUERY-MORE-ORDER-SKU-INFO>\n</PREMIER-ACCESS-QUERY-REPLY>\n".toCharArray();
                B15 = "<PREMIER-ACCESS-QUERY-REPLY><PACQUERY-ORDER-NBR>".getBytes("UTF-8");
                B16 = "</PACQUERY-ORDER-NBR>\n<PACQUERY-TIE-NUM>".getBytes("UTF-8");
                B17 = "</PACQUERY-TIE-NUM>\n<PACQUERY-CONTRACT-STATUS>".getBytes("UTF-8");
                B18 = "</PACQUERY-CONTRACT-STATUS>\n<PACQUERY-SYSTEM-TYPE>".getBytes("UTF-8");
                B19 = "</PACQUERY-SYSTEM-TYPE>\n<PACQUERY-ORDER-SKU-INFO-NBR>".getBytes("UTF-8");
                B20 = "</PACQUERY-ORDER-SKU-INFO-NBR>\n".getBytes("UTF-8");
                B21 = "<PACQUERY-ORDER-SKU-INFO><PACQUERY-DETAIL-SEQ-NBR>".getBytes("UTF-8");
                B22 = "</PACQUERY-DETAIL-SEQ-NBR>\n<PACQUERY-SKU-NBR>".getBytes("UTF-8");
                B23 = "</PACQUERY-SKU-NBR>\n<PACQUERY-SKU-DESC>".getBytes("UTF-8");
                B24 = "</PACQUERY-SKU-DESC>\n<PACQUERY-SKU-QTY>".getBytes("UTF-8");
                B25 = "</PACQUERY-SKU-QTY>\n<PACQUERY-SKU-QTY-SIGN>".getBytes("UTF-8");
                B26 = "</PACQUERY-SKU-QTY-SIGN>\n</PACQUERY-ORDER-SKU-INFO>\n".getBytes("UTF-8");
                B27 = "<PACQUERY-MORE-ORDER-SKU-INFO>".getBytes("UTF-8");
                B28 = "</PACQUERY-MORE-ORDER-SKU-INFO>\n</PREMIER-ACCESS-QUERY-REPLY>\n".getBytes("UTF-8");
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }
}

package com.l7tech.server.config.wizard;

import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import org.junit.Test;

import java.io.*;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;

public class ConsoleWizardUtilsTest {

    private static final String ERROR_MESSAGE = "error message";

    @Test
    public void testGetDataAllowEntries() throws Exception {
        StringBuilder sb = new StringBuilder();
        //Failed value
        sb.append("c\n");
        //Success value
        sb.append("Y\n");
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(sb.toString().getBytes()))));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ConsoleWizardUtils.setOut(new PrintStream(os));
        ConsoleWizardUtils.getData(new String[]{}, "", true, new String[]{"Y", "N"}, ERROR_MESSAGE);
        assertEquals(ERROR_MESSAGE, os.toString().trim());
    }

    @Test
    public void testGetDataPattern() throws Exception {
        StringBuilder sb = new StringBuilder();
        //Failed value
        sb.append("a b\n");
        //Success value
        sb.append("a\n");
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(sb.toString().getBytes()))));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ConsoleWizardUtils.setOut(new PrintStream(os));
        //Match anything no whitespace
        ConsoleWizardUtils.getData(new String[]{}, "", true, Pattern.compile("\\S+"), ERROR_MESSAGE);
        assertEquals(ERROR_MESSAGE, os.toString().trim());
    }

    @Test
    public void testGetDataGeneric() throws Exception {
        StringBuilder sb = new StringBuilder();
        //Failed value
        sb.append("abc\n");
        //Success value
        sb.append("http://layer7tech.com\n");
        final Functions.UnaryVoidThrows<String,Exception> verifier = new Functions.UnaryVoidThrows<String,Exception>(){
            @Override
            public void call( final String input ) throws Exception {
                if (!ValidationUtils.isValidUrl(input)) {
                    throw new IllegalArgumentException("Invalid URL");
                }
            }
        };
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(sb.toString().getBytes()))));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ConsoleWizardUtils.setOut(new PrintStream(os));
        ConsoleWizardUtils.getData(new String[]{}, "", true, verifier, ERROR_MESSAGE, false);
        assertEquals(ERROR_MESSAGE, os.toString().trim());
    }

    @Test
    public void testGetDataAllowEntriesDefault() throws Exception {
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream("\n".getBytes()))));
        String data = ConsoleWizardUtils.getData(new String[]{}, "Y", true, new String[]{"Y", "N"}, ERROR_MESSAGE);
        assertEquals("Y", data);
    }

    @Test
    public void testGetDataPatternDefault() throws Exception {
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream("\n".getBytes()))));
        String data = ConsoleWizardUtils.getData(new String[]{}, "Default", true, Pattern.compile("\\S+"), ERROR_MESSAGE);
        assertEquals("Default", data);
    }

    @Test
    public void testGetDataAllowEntriesNull() throws Exception {
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream("\n".getBytes()))));
        String data = ConsoleWizardUtils.getData(new String[]{}, "Y", true, (String[]) null, ERROR_MESSAGE);
        assertEquals("Y", data);
        String data2 = ConsoleWizardUtils.getData(new String[]{}, "Y", true, new String[]{}, ERROR_MESSAGE);
        assertEquals("Y", data2);

    }

    @Test
    public void testGetDataPatternNull() throws Exception {
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream("\n".getBytes()))));
        String data = ConsoleWizardUtils.getData(new String[]{}, "Default", true, (Pattern) null, ERROR_MESSAGE);
        assertEquals("Default", data);
    }

}

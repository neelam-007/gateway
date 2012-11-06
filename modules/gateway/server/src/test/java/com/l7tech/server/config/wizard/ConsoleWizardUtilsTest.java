package com.l7tech.server.config.wizard;

import org.junit.Test;

import java.io.*;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;

public class ConsoleWizardUtilsTest {

    private static final String ERROR_MESSAGE = "error message";

    @Test
    public void testGetDataAllowEntries() throws Exception {
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream("c\nY\n".getBytes()))));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ConsoleWizardUtils.setOut(new PrintStream(os));
        ConsoleWizardUtils.getData(new String[]{}, "", true, new String[]{"Y", "N"}, ERROR_MESSAGE);
        assertEquals(ERROR_MESSAGE, os.toString().trim());
    }

    @Test
    public void testGetDataPattern() throws Exception {
        ConsoleWizardUtils.setReader(new BufferedReader(new InputStreamReader(new ByteArrayInputStream("a b\na\n".getBytes()))));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ConsoleWizardUtils.setOut(new PrintStream(os));
        //Match anything no whitespace
        ConsoleWizardUtils.getData(new String[]{}, "", true, Pattern.compile("\\S+"), ERROR_MESSAGE);
        assertEquals(ERROR_MESSAGE, os.toString().trim());
    }
}

package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.PasswordValidator;

import java.io.*;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Apr 12, 2006
 * Time: 3:29:19 PM
 */
public class ConsoleWizardUtils {
    private static final String PREV_COMMAND = "B";
    public static final String GENERAL_NAV_HEADER = "At any time press " + PREV_COMMAND + " to go to the previous step";

    static ConsoleWizardUtils instance_ = null;

    public ConsoleWizardUtils(InputStream in, PrintWriter out) {
        reader = new BufferedReader(new InputStreamReader(in));
        this.out = out;
    }

    public static ConsoleWizardUtils getInstance(InputStream in, PrintWriter out) {
        if (instance_ == null) {
            instance_ = new ConsoleWizardUtils(in, out);
        }
        return instance_;
    }

    public String getData(String[] promptLines, String defaultValue, boolean isNavAware) throws IOException, WizardNavigationException {
        doPromptLines(promptLines);
        String input = reader.readLine();
        if (isNavAware) {
            handleInput(input);
        }

        if (defaultValue != null) {
            if (StringUtils.isEmpty(input)) {
                input = defaultValue;
            }
        }
        return input;
    }

    /**
     * prompts the user for input according to the prompts in firstPrompt and secondPrompt.
     * The input collected from these prompts is compared for equality (using string comparison).
     * If the inputs do not match, then the method retries "numAttempts" times.
     *
     * @param firstPrompt
     * @param secondPrompt
     * @param validator
     * @param numAttempts the number of times to retry if the data could not be confirmed
     * @return the confirmed data, or null if the data could not be confirmed in "numAttempts" attempts
     */
    public String getMatchingDataWithConfirm(String firstPrompt, String secondPrompt, int numAttempts, PasswordValidator validator) throws IOException, WizardNavigationException {

        String[] inputs = internalGetMatchingData(firstPrompt, secondPrompt);

        String[] validationErrors = validator.validate(inputs[0], inputs[1]);

        if (validationErrors != null && validationErrors.length > 0) {
            if (numAttempts < 0) {
                printText(validationErrors);
                return getMatchingDataWithConfirm(firstPrompt, secondPrompt, numAttempts,  validator);
            }

            --numAttempts;
            if (numAttempts > 0) {
                printText(validationErrors);
                return getMatchingDataWithConfirm(firstPrompt, secondPrompt, numAttempts, validator);
            }
        }

        return inputs[0];
    }

    private boolean doTheyMatch(String first, String second, boolean isCaseSensitive) {
        if (isCaseSensitive) {
            return StringUtils.equals(first, second);
        } else {
            return StringUtils.equalsIgnoreCase(first, second);
        }
    }

    private String[] internalGetMatchingData(String firstPrompt, String secondPrompt) throws IOException, WizardNavigationException {
        String[] inputs = new String[2];
        inputs[0] = getData(new String[]{firstPrompt}, null, false);
        inputs[1] = getData(new String[]{secondPrompt}, null, false);
        return inputs;
    }

    public void handleInput(String input) throws WizardNavigationException {
        if (input != null) {
            if (PREV_COMMAND.equalsIgnoreCase(input)) {
                throw new WizardNavigationException(WizardNavigationException.NAVIGATE_PREV);
            }
        };
    }

    public void printText(String[] textToPrint) {
        for (int i = 0; i < textToPrint.length; i++) {
            String s = textToPrint[i];
            out.print(s);
        }
        out.flush();
    }

    public void printText(String textToPrint) {
        out.print(textToPrint);
        out.flush();
    }

    private void doPromptLines(String[] promptLines) {
        if (promptLines != null) {
            for (int i = 0; i < promptLines.length; i++) {
                String promptLine = promptLines[i];
                out.print(promptLine);
            }
            out.flush();
        }
    }

    private final BufferedReader reader;
    private final PrintWriter out;
}

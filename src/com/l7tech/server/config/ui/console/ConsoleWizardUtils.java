package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.WizardInputValidator;

import java.io.*;
import java.util.Map;
import java.util.HashMap;

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
        handleInput(input, isNavAware);

        if (defaultValue != null) {
            if (StringUtils.isEmpty(input)) {
                input = defaultValue;
            }
        }
        return input;
    }

    /**
     * prompts the user for input according to the prompts in firstPrompt and secondPrompt.
     * The input collected from these prompts is validated using the supplied WizardValidator.
     * If the inputs do not validate, then the method retries "numAttempts" times.
     *
     * @param prompts
     * @param validator
     * @param numAttempts the number of times to retry if the data could not be confirmed
     * @return the confirmed data, or null if the data could not be confirmed in "numAttempts" attempts
     */
    public Map getValidatedDataWithConfirm(String[] prompts, String[] defaultValues, int numAttempts, WizardInputValidator validator) throws IOException, WizardNavigationException {

        Map inputs = internalGetInputs(prompts, defaultValues);
        String[] validationErrors = validator.validate(inputs);

        if (validationErrors != null) {
            //then there were some errors
            if (numAttempts >= 0) {
                --numAttempts;
            }
            printText(validationErrors);
            return getValidatedDataWithConfirm(prompts, defaultValues, numAttempts,  validator);

        }

        return inputs;
    }

    private Map internalGetInputs(String[] prompts, String[] defaultValues) throws IOException, WizardNavigationException {
        boolean isNoDefaults = false;
        if (prompts == null) {
            throw new IllegalArgumentException("Prompts cannot be null");
        }

        if (defaultValues == null || defaultValues.length != prompts.length) {
            isNoDefaults = true;
        }

        Map gotData = new HashMap();

        for (int i = 0; i < prompts.length; i++) {
            String prompt = prompts[i];
            if (isNoDefaults) {
                gotData.put(prompt, getData(new String[]{prompt}, null, false));
            } else {
                String defaultValue = defaultValues[i];
                gotData.put(prompt, getData(new String[]{prompt}, defaultValue, false));
            }
        }

        return gotData;
    }

    public void handleInput(String input, boolean canNavigate) throws WizardNavigationException {
        if (input != null) {
            if (canNavigate) {
                if (PREV_COMMAND.equalsIgnoreCase(input)) {
                    throw new WizardNavigationException(WizardNavigationException.NAVIGATE_PREV);
                }
            }
        };
    }

    public void printText(String[] textToPrint) {
        if (textToPrint != null) {
            for (int i = 0; i < textToPrint.length; i++) {
                String s = textToPrint[i];
                if (s != null) {
                    out.print(s);
                }
            }
            out.flush();
        }
    }

    public void printText(String textToPrint) {
        if (textToPrint != null) {
            out.print(textToPrint);
            out.flush();
        }
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

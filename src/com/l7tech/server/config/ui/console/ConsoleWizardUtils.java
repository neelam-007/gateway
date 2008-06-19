package com.l7tech.server.config.ui.console;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.server.config.WizardInputValidator;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.MessageFormat;

/**
 * User: megery
 * Date: Apr 12, 2006
 * Time: 3:29:19 PM
 */
public class ConsoleWizardUtils {
    public static final String PREV_COMMAND = "<";
    public static final String QUIT_COMMAND = "quit";
    public static final String QUIT_HEADER = "At any time type \"" + QUIT_COMMAND + "\" to quit";
    public static final String NAV_HEADER = "Press \"" + PREV_COMMAND + "\" to go to the previous step";

    private static ConsoleWizardUtils instance_ = null;
    public static final String EOL_CHAR = System.getProperty("line.separator");

    public static String[] YES_VALUES;
    public static String[] NO_VALUES;
    public static String[] YES_NO_VALUES;
    private Console console;

    static {
        YES_VALUES = new String[]{"Y","y","YES","yes","Yes"};
        NO_VALUES = new String[]{"N","n","NO","no","No"};
        YES_NO_VALUES = ArrayUtils.concat(YES_VALUES, NO_VALUES);
    }

    public static ConsoleWizardUtils getInstance(InputStream in, PrintStream out) {
        if (instance_ == null) {
            instance_ = new ConsoleWizardUtils(in, out);
        }
        return instance_;
    }

    //private constructor - only allow singleton creation via getInstance()
    private ConsoleWizardUtils(InputStream in, PrintStream out) {
        reader = new BufferedReader(new InputStreamReader(in));
        this.out = out;
        console = System.console();
    }

    public String getData(String[] promptLines, String defaultValue, boolean isNavAware, Pattern allowedEntriesPattern, String errorMessage) throws IOException, WizardNavigationException {
        boolean isValidInput = true;

        String input = null;
        do {
            isValidInput = true;
            printText(promptLines);
            input = readLine();
            if (input != null) input = input.trim();
            handleInput(input, isNavAware);

            if (StringUtils.isEmpty(input)) {
               if (defaultValue != null) input = defaultValue;
            }

            //if the wizard didn't recognize the input (i.e. non navigation input) then check it's validity here
            if (allowedEntriesPattern != null) {
                Matcher matcher = allowedEntriesPattern.matcher(input);
                isValidInput = matcher.matches();
            }
            if (!isValidInput) {
                if (StringUtils.isEmpty(errorMessage))
                    printText("*** Invalid Selection. Please select one of the options shown. ***\n");
                else
                    printText(errorMessage + "\n");
            }
        } while (!isValidInput);

        return input;
    }


    public String getData(String[] promptLines, String defaultValue, boolean isNavAware, String[] allowedEntries, String errorMessage) throws IOException, WizardNavigationException {
        boolean isValidInput = true;

        String input = null;
        do {
            isValidInput = true;
            printText(promptLines);
            input = readLine();
            if (input != null) input = input.trim();
            handleInput(input, isNavAware);

            if (StringUtils.isEmpty(input)) {
               if (defaultValue != null) input = defaultValue;
            }

            //if the wizard didn't recognize the input (i.e. non navigation input) then check it's validity here
            if (allowedEntries != null && allowedEntries.length != 0) {
                boolean foundAMatch = false;
                for (String allowedEntry : allowedEntries) {
                    foundAMatch = StringUtils.equals(input, allowedEntry);
                    if (foundAMatch) break;
                }
                isValidInput = foundAMatch;
            }
            if (!isValidInput) {
                if (StringUtils.isEmpty(errorMessage))
                    printText("*** Invalid Selection. Please select one of the options shown. ***\n");
                else
                    printText(errorMessage + "\n");

            }
        } while (!isValidInput);

        return input;
    }

    public String getSecretData(String[] promptLines, String defaultValue, boolean isNavAware, Pattern allowedEntries, String errorMessage) throws WizardNavigationException, IOException {

            boolean isValidInput;

            String input;
            do {
                isValidInput = true;
                printText(promptLines);
                input = readSecretLine();
                if (input != null) input = input.trim();
                handleInput(input, isNavAware);

                if (StringUtils.isEmpty(input)) {
                   if (defaultValue != null) input = defaultValue;
                }

                //if the wizard didn't recognize the input (i.e. non navigation input) then check it's validity here
                if (allowedEntries != null) {
                    Matcher matcher = allowedEntries.matcher(input);
                    isValidInput = matcher.matches();
                }
                if (!isValidInput) {
                    if (StringUtils.isEmpty(errorMessage))
                        printText("*** Invalid Selection. Please select one of the options shown. ***\n");
                    else
                        printText(errorMessage + "\n");

                }
            } while (!isValidInput);

            return input;
    }

    public String readSecretLine() throws IOException {
        if (console != null)
            return new String(console.readPassword());
        else
            return readLine();
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    /**
     * prompts the user for input according to the prompts in firstPrompt and secondPrompt.
     * The input collected from these prompts is validated using the supplied WizardValidator.
     * If the inputs do not validate, then the method retries "numAttempts" times.
     *
     * @param prompts the prompts to display to the user
     * @param validator the optional validator to validate the inputs
     * @param defaultValues the optional values corresponding to each of the inputs when there is no data entered by the user.
     * @param numAttempts the number of times to retry if the data could not be confirmed
     * @param isNavAware whether or not the invocation of this method should be aware of special navigation commands in the UI
     * @param secret whether the data to be collected is to be treated as secret or not (i.e. echoed to the screen)
     * @return the confirmed data, or null if the data could not be confirmed in "numAttempts" attempts
     */
    public Map getValidatedDataWithConfirm(String[] prompts, String[] defaultValues, int numAttempts, boolean isNavAware, WizardInputValidator validator, boolean secret) throws IOException, WizardNavigationException {

        Map inputs = null;
        if (!secret) inputs = internalGetInputs(prompts, defaultValues, isNavAware);
        else inputs = internalGetSecretInputs(prompts, defaultValues, isNavAware);
        
        String[] validationErrors = validator.validate(inputs);

        if (validationErrors != null) {
            //then there were some errors
            if (numAttempts >= 0) {
                --numAttempts;
            }
            printText(validationErrors);
            return getValidatedDataWithConfirm(prompts, defaultValues, numAttempts, isNavAware, validator,secret);
        }

        return inputs;
    }

    public void printText(String[] textToPrint) {
        if (textToPrint != null) {
            for (String s : textToPrint) {
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

    public void handleInput(String input, boolean canNavigate) throws WizardNavigationException {
        if (input != null) {
            if (QUIT_COMMAND.equalsIgnoreCase(input)) {
                out.print("Wizard cancelled at users request. Changes will not be applied. \n" +
                        "Exiting\n");
                out.flush();
                System.exit(1);
            } else if (PREV_COMMAND.equalsIgnoreCase(input)) {
                if (canNavigate) {
                    throw new WizardNavigationException(WizardNavigationException.NAVIGATE_PREV);
                }
            }
        }
    }

    private Map internalGetSecretInputs(String[] prompts, String[] defaultValues, boolean isNavAware) throws IOException, WizardNavigationException {
        boolean isNoDefaults = false;
        if (prompts == null) {
            throw new IllegalArgumentException("Prompts cannot be null");
        }

        if (defaultValues == null || defaultValues.length != prompts.length) {
            isNoDefaults = true;
        }

        Map<String, String> gotData = new HashMap<String, String>();

        for (int i = 0; i < prompts.length; i++) {
            String prompt = prompts[i];
            if (prompt == null) continue;
            if (isNoDefaults) {
                gotData.put(prompt, getSecretData(new String[]{prompt}, null, isNavAware, null, null));
            } else {
                String defaultValue = defaultValues[i];
                gotData.put(prompt, getSecretData(new String[]{prompt}, defaultValue, isNavAware, null, null));
            }
        }

        return gotData;
    }

    private Map internalGetInputs(String[] prompts, String[] defaultValues, boolean isNavAware) throws IOException, WizardNavigationException {
        boolean isNoDefaults = false;
        if (prompts == null) {
            throw new IllegalArgumentException("Prompts cannot be null");
        }

        if (defaultValues == null || defaultValues.length != prompts.length) {
            isNoDefaults = true;
        }

        Map<String, String> gotData = new HashMap<String, String>();

        for (int i = 0; i < prompts.length; i++) {
            String prompt = prompts[i];
            if (prompt == null) continue;
            if (isNoDefaults) {
                gotData.put(prompt, getData(new String[]{prompt}, null, isNavAware, (String[])null, null));
            } else {
                String defaultValue = defaultValues[i];
                gotData.put(prompt, getData(new String[]{prompt}, defaultValue, isNavAware, (String[])null, null));
            }
        }

        return gotData;
    }

    public boolean isYes(String answer) {
        return  ArrayUtils.contains(YES_VALUES, answer);
    }

    private final BufferedReader reader;
    private final PrintStream out;

    public String resolveHostName(String timeserverLine) throws UnknownHostException {
        if (StringUtils.isEmpty(timeserverLine))
            return null;

        if (InetAddressUtil.isValidIpAddress(timeserverLine))
            return timeserverLine;

        String resolvedIp;
        resolvedIp = InetAddress.getByName(timeserverLine).getHostAddress();

        return resolvedIp;

    }
}

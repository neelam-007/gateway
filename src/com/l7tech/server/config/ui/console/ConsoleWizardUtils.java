package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.WizardInputValidator;

import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Apr 12, 2006
 * Time: 3:29:19 PM
 */
public class ConsoleWizardUtils {
    public static String PREV_COMMAND = "<";
    public static String QUIT_COMMAND = "quit";
    public static final String QUIT_HEADER = "At any time type \"" + QUIT_COMMAND + "\" to quit";
    public static final String NAV_HEADER = "Press \"" + PREV_COMMAND + "\" to go to the previous step";

    private static ConsoleWizardUtils instance_ = null;
    public static final String EOL_CHAR = System.getProperty("line.separator");

    private Pattern validIpAddressPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$");

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
    }

    public String getData(String[] promptLines, String defaultValue, boolean isNavAware, Pattern allowedEntriesPattern) throws IOException, WizardNavigationException {
        boolean isValidInput = true;

        String input = null;
        do {
            isValidInput = true;
            printText(promptLines);
            input = readLine();
            handleInput(input, isNavAware);

            if (StringUtils.isEmpty(input)) {
               if (defaultValue != null) input = defaultValue;
            }

            //if the wizard didn't recognize the input (i.e. non navigation input) then check it's validity here
            if (allowedEntriesPattern != null) {
                Matcher matcher = allowedEntriesPattern.matcher(input);
                isValidInput = matcher.matches();
            }
            if (!isValidInput) printText("*** Invalid Selection. Please select one of the options shown. ***\n");
        } while (!isValidInput);

        return input;
    }

    public String getData(String[] promptLines, String defaultValue, boolean isNavAware, String[] allowedEntries) throws IOException, WizardNavigationException {
        boolean isValidInput = true;

        String input = null;
        do {
            isValidInput = true;
            printText(promptLines);
            input = readLine();
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
            if (!isValidInput) printText("*** Invalid Selection. Please select one of the options shown. ***\n");
        } while (!isValidInput);

        return input;
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    private String getValidatedData(String prompt, WizardInputValidator validator) {
        return null;
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
    public Map getValidatedDataWithConfirm(String[] prompts, String[] defaultValues, int numAttempts, boolean isNavAware, WizardInputValidator validator) throws IOException, WizardNavigationException {

        Map inputs = internalGetInputs(prompts, defaultValues, isNavAware);
        String[] validationErrors = validator.validate(inputs);

        if (validationErrors != null) {
            //then there were some errors
            if (numAttempts >= 0) {
                --numAttempts;
            }
            printText(validationErrors);
            return getValidatedDataWithConfirm(prompts, defaultValues, numAttempts, isNavAware, validator);
        }

        return inputs;
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
        };
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
            if (isNoDefaults) {
                gotData.put(prompt, getData(new String[]{prompt}, null, isNavAware, (String[])null));
            } else {
                String defaultValue = defaultValues[i];
                gotData.put(prompt, getData(new String[]{prompt}, defaultValue, isNavAware, (String[])null));
            }
        }

        return gotData;
    }

    public boolean isYes(String answer) {
        return StringUtils.equalsIgnoreCase("yes", answer) || StringUtils.equalsIgnoreCase("y", answer);
    }

    public boolean isValidIpAddress(String address, boolean isNetworkAddressAllowed) {

        if (address == null) return false;

        Matcher matcher = validIpAddressPattern.matcher(address);

        if (matcher.matches())
        {
            //at least it's got a sane format.
            int start = 0;
            int end = 255;

            for (int i = 1; i <= matcher.groupCount(); ++i) {
                String octetString = matcher.group(i);
                try {
                    int octet = Integer.parseInt(octetString);
                    if (i == 1)
                        start = 1;
                    else
                        start = isNetworkAddressAllowed ? 0 : 1;

                    if (octet < start || octet > end)
                        return false;

                } catch (NumberFormatException e) {
                    return false;
                }

            }
            return true;
        }

        return false;
    }

    private final BufferedReader reader;
    private final PrintStream out;

    public String resolveHostName(String timeserverLine) throws UnknownHostException {
        if (StringUtils.isEmpty(timeserverLine))
            return null;

        if (isValidIpAddress(timeserverLine, false))
            return timeserverLine;

        String resolvedIp = null;
        resolvedIp = InetAddress.getByName(timeserverLine).getHostAddress();

        return resolvedIp;

    }
}

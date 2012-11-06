package com.l7tech.server.config.wizard;

import com.l7tech.util.ArrayUtils;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.util.TextUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: megery
 * Date: Apr 12, 2006
 * Time: 3:29:19 PM
 */
public class ConsoleWizardUtils {

    // - PUBLIC

    public static final String PREV_COMMAND = "<";
    public static final String QUIT_COMMAND = "quit";
    public static final String QUIT_HEADER = "At any time type \"" + QUIT_COMMAND + "\" to quit";
    public static final String NAV_HEADER = "Press \"" + PREV_COMMAND + "\" to go to the previous step";

    public static String[] YES_VALUES;
    public static String[] NO_VALUES;
    public static String[] YES_NO_VALUES;

    static {
        YES_VALUES = new String[]{"Y","y","YES","yes","Yes"};
        NO_VALUES = new String[]{"N","n","NO","no","No"};
        YES_NO_VALUES = ArrayUtils.concat(YES_VALUES, NO_VALUES);
    }

    public static String getData(String[] promptLines, String defaultValue, boolean isNavAware, Pattern allowedEntriesPattern, String errorMessage) throws IOException, WizardNavigationException {
        return getData(promptLines, defaultValue, isNavAware, allowedEntriesPattern, errorMessage, false);
    }

    public static String getData(String[] promptLines, String defaultValue, boolean isNavAware, Pattern allowedEntriesPattern, String errorMessage, boolean isPassword) throws IOException, WizardNavigationException {
        boolean isValidInput;
        String input;
        do {
            isValidInput = true;
            printText(promptLines);
            if (isPassword) {
                input = readPassword();
            } else {
                input = readLine();
            }
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


    public static String getData(String[] promptLines, String defaultValue, boolean isNavAware, String[] allowedEntries, String errorMessage, boolean isPassword) throws IOException, WizardNavigationException {
        boolean isValidInput;
        String input;
        do {
            isValidInput = true;
            printText(promptLines);
            if (isPassword) {
                input = readPassword();
            } else {
                input = readLine();
            }
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
    public static String getData(String[] promptLines, String defaultValue, boolean isNavAware, String[] allowedEntries, String errorMessage) throws IOException, WizardNavigationException {
        return getData(promptLines, defaultValue, isNavAware, allowedEntries, errorMessage, false);
    }

    public static String readLine() throws IOException {
        return reader.readLine();
    }

    public static String readPassword() throws IOException {
        Console console = System.console();
        if (console != null)
            return TextUtils.string(console.readPassword());
        else
            return readLine();
    }

    public static void printText(String[] textToPrint) {
        if (textToPrint != null) {
            for (String s : textToPrint) {
                if (s != null) {
                    out.print(s);
                }
            }
            out.flush();
        }
    }

    public static void printText(String textToPrint) {
        if (textToPrint != null) {
            out.print(textToPrint);
            out.flush();
        }
    }

    public static void handleInput(String input, boolean canNavigate) throws WizardNavigationException {
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

    public static boolean isYes(String answer) {
        return  ArrayUtils.contains(YES_VALUES, answer);
    }

    // - PRIVATE

    private ConsoleWizardUtils() {}

    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private static PrintStream out = System.out;

    protected static void setReader(BufferedReader br) {
        reader = br;
    }

    protected static void setOut(PrintStream ps) {
        out = ps;
    }
}

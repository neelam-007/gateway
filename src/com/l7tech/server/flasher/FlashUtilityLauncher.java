package com.l7tech.server.flasher;

import com.l7tech.server.config.OSDetector;

import java.util.HashMap;
import java.io.IOException;

/**
 * Entrypoint for the ssg flashing utility.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class FlashUtilityLauncher {
    public static final String EOL_CHAR = System.getProperty("line.separator");

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            printusage();
            return;
        }

        try {
            HashMap<String, String> passedArgs = parseArguments(args);
            if (args[0].equals("import")) {
                Importer importer = new Importer();
                importer.doIt(passedArgs);
            } else if (args[0].equals("export")) {
                Exporter exporter = new Exporter();
                exporter.doIt(passedArgs);
            }
        } catch (InvalidArgumentException e) {
            System.out.println(e.getMessage());
            printusage();
        } catch (IOException e) {
            System.out.println("Error performing the operation.");
            System.out.println(e.getMessage());
        }
    }

    private static HashMap<String, String> parseArguments(String[] args) throws InvalidArgumentException {
        HashMap<String, String> output = new HashMap<String, String>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                throw new InvalidArgumentException("Invalid argument name: " + arg);
            }
            i++;
            if (i >= args.length) {
                throw new InvalidArgumentException("argument name: " + arg + " not provided a value");
            }
            String val = args[i];
            output.put(arg, val);
        }
        return output;
    }

    private static void printusage() {
        StringBuffer output = new StringBuffer();
        if (OSDetector.isWindows()) {
            output.append("usage: ssgflash.cmd [import | export] [OPTIONS]" + EOL_CHAR);
        } else {
            output.append("usage: ssgflash.sh [import | export] [OPTIONS]" + EOL_CHAR);
        }
        output.append("\tIMPORT OPTIONS:" + EOL_CHAR);
        for (CommandLineOption option : Importer.ALLOPTIONS) {
            output.append("\t" + option.name + "\t\t" + option.description + EOL_CHAR);
        }
        output.append(EOL_CHAR);
        output.append("\tEXPORT OPTIONS:" + EOL_CHAR);
        for (CommandLineOption option : Exporter.ALLOPTIONS) {
            output.append("\t" + option.name + "\t\t" + option.description + EOL_CHAR);
        }
        System.out.println(output);
    }

    public static class InvalidArgumentException extends Exception {
        public InvalidArgumentException(String reason) {super(reason);}
    }
}

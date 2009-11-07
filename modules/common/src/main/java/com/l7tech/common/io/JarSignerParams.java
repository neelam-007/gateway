package com.l7tech.common.io;

import static com.l7tech.common.io.JarSignerParams.Option.*;

import java.util.*;

/**
 * Parameter argument builder for the JarSigner utility.
 * jarsigner [ options ] jar-file alias
 *
 * @author jbufu
 */
public class JarSignerParams {

    // - PUBLIC

    /** Options passed to the jarsigner tool for signing JARs */
    public enum Option {
        KEYSTORE(true),
        STORETYPE(false),
        STOREPASS(true),
        KEYPASS(true),
        SIGFILE(false),
        SIGALG(false),
        DIGESTALG(false);

        public String getCommandLineArg() {
            return "-" + name().toLowerCase();
        }

        public boolean isRequired() {
            return required;
        }

        public static Option fromCommandLineArg(String arg) {
            try {
                return Option.valueOf(arg.substring(1).toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid jarsigner option: " + arg);
            }
        }

        // - PRIVATE

        private final boolean required;

        private Option(boolean required) {
            this.required = required;
        }
    }

    public JarSignerParams(String keystore, String keystorePassword, String alias, String password) {
        // required params
        options.put(KEYSTORE, keystore);
        options.put(STOREPASS, keystorePassword);
        options.put(KEYPASS, password);
        this.alias = alias;
    }

    public JarSignerParams(Map<Option,String> options, String alias) {
        this.options = new HashMap<Option, String>(options);
        this.alias = alias;

        if (alias == null || alias.isEmpty())
            throw new IllegalArgumentException("Jarsigner alias not provided.");
        for(Option o : Option.values()) {
            if (o.isRequired() && ! this.options.containsKey(o))
                throw new IllegalArgumentException("Required jarsigner option missing: " + o.getCommandLineArg());
        }
    }

    public String getAlias() {
        return alias;
    }

    public String[] getOptions() {
        List<String> result = new ArrayList<String>();
        for (Option o : options.keySet()) {
            result.add(o.getCommandLineArg());
            result.add(options.get(o));
        }
        return result.toArray(new String[result.size()]);
    }

    // - PRIVATE

    private Map<Option,String> options = new LinkedHashMap<Option, String>();
    private String alias;
}

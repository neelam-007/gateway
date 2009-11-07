package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.server.processcontroller.patching.PatchUtils;
import com.l7tech.server.processcontroller.patching.PatchPackageManager;
import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.server.processcontroller.patching.PatchPackage;
import com.l7tech.common.io.JarSignerParams;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * @author jbufu
 */
public class PatchBuilder {

    // - PUBLIC

    public static final int SYNTAX_ERROR = 1;
    public static final int PATCH_BUILDER_ERROR = 2;

    public static void main(String[] args) throws IOException, PatchException {
        BuilderConfig config = null;
        try {
            config = Option.parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + ExceptionUtils.getMessage(e) + "\n");
            printUsage();
            System.exit(SYNTAX_ERROR);
        }

        try {
            File patch = config.buildPatch();
            System.out.println("Created patch: " + patch.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error: " + ExceptionUtils.getMessage(e) + "\n");
            System.exit(PATCH_BUILDER_ERROR);
        }
    }

    // - PRIVATE

    private static void printUsage() {
        System.out.println("Usage: " + PatchBuilder.class.getSimpleName() + " <options>");
        for (Option o : Option.values()) {
            System.out.println("\t" + o.getCommandLineArg() + " " + o.getSyntax() + "\n\t\t" + (o.isRequired() ? "Required." : "Optional.") + o.getDescription());
        }
    }

    private static class BuilderConfig {

        // - PUBLIC

        public File buildPatch() throws IOException, PatchException {
            for (Option o : Option.values()) {
                if (o.isRequired() && ! options.containsKey(o))
                    throw new IllegalArgumentException("Required option missing: " + o.getCommandLineArg()); 
            }

            if (! options.containsKey(Option.ID))
                options.put(Option.ID, new ArrayList<String>() {{ add(PatchUtils.generatePatchId()); }});

            if (! options.containsKey(Option.PATCH_FILENAME))
                options.put(Option.PATCH_FILENAME, new ArrayList<String>() {{ add(getSingleValue(Option.ID) + PatchPackageManager.PATCH_EXTENSION); }});

            JarSignerParams signerParams = getSignerParams();
            PatchSpec patchSpec = getPatchSpec(this);

            return signerParams == null ?
                PatchUtils.buildUnsignedPatch(patchSpec) : PatchUtils.buildPatch(patchSpec, signerParams);
        }

        // - PRIVATE

        private Map<Option,List<String>> options = new HashMap<Option, List<String>>();
        public Map<JarSignerParams.Option, String> jarOptions = new LinkedHashMap<JarSignerParams.Option, String>();

        private boolean hasOption(Option o) {
            return options.containsKey(o);
        }

        private String getSingleValue(Option o) {
            List<String> values = options.get(o);
            return values == null || values.isEmpty() ? null : values.get(0);
        }

        private JarSignerParams getSignerParams() {
            if (jarOptions.isEmpty() && ! options.containsKey(Option.JARSIGNER_ALIAS))
                return null;

            if (jarOptions.isEmpty() ^ ! options.containsKey(Option.JARSIGNER_ALIAS))
                throw new IllegalArgumentException("Jarsigner options and key alias need to be specified for signing a patch file.");

            return new JarSignerParams(jarOptions, getSingleValue(Option.JARSIGNER_ALIAS));
        }

        private PatchSpec getPatchSpec(BuilderConfig config) {
            PatchSpec spec = new PatchSpec();
            for (Option o : Option.values()) {
                o.update(spec, config);
            }
            spec.validate();
            return spec;
        }
    }

    private static enum Option {

        ID("-id", "<patch_identifier>", false, "The patch identifier. A random one is generated if not specified.") {
            @Override
            public BuilderConfig parseArgs(final List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                spec.property(PatchPackage.Property.ID, config.getSingleValue(this));
            }},

        DESCRIPTION("-desc", "<description>", true, "The patch description.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                spec.property(PatchPackage.Property.DESCRIPTION, config.getSingleValue(this));
            }},

        NO_ROLLBACK("-no-rollback", "", false, "The generated patch will not be allowed to be rolled back once installed.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                extractArg(args, getCommandLineArg(), getCommandLineArg());
                config.options.put(this, new ArrayList<String>());
                return Option.parseNextArg(args, config);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                spec.property(PatchPackage.Property.ROLLBACK_ALLOWED, Boolean.toString(config.hasOption(this)));
            }},

        ROLLBACK_FOR_ID("-rollback-id", "<rollback_id>", false, "The patch identifier which the generated patch will roll back.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if (config.hasOption(this))
                    spec.property(PatchPackage.Property.ROLLBACK_FOR_ID, config.getSingleValue(this));
            }},

        JAVA_BINARY("-java-path", "<java_binary_path>", false, "The path to the java binary to be used for installing the patch. Defaults to process controller's configuration if not specified.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if (config.hasOption(this))
                    spec.property(PatchPackage.Property.JAVA_BINARY, config.getSingleValue(this));
            }},

        PATCH_FILENAME("-patchfile", "<patch_file_name>", false, "The filename for the generated patch. Defaults to <patch_identifier>.L7P") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if (config.hasOption(this))
                    spec.outputFilename(config.getSingleValue(this));
            }},

        JARSIGNER_OPTION("<jarsigner_option>", "<jarsigner_option_value>", false, "Options to be passed to the jarsigner tool. An unsigned patch is built if no jarsigner options are specified. If present, -keystore, -storepass, -keypass and -alias need to be specified.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractJarsignerOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                /* no spec update */
            }},

        JARSIGNER_ALIAS("-alias", "<key_alias>", false, "The private key alias to be used for signing the patch file. If present, jarsigner options need to be specified.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                /* no spec update */
            }},
        ;

        public static Option fromArg(String arg) {
            if (argToOption.containsKey(arg))
                return argToOption.get(arg);

            try {
                if (JarSignerParams.Option.fromCommandLineArg(arg) != null)
                    return JARSIGNER_OPTION;
            } catch (IllegalArgumentException e) {
                //
            }
            throw new IllegalArgumentException("Invalid option: " + arg);
        }

        public abstract BuilderConfig parseArgs(List<String> args, BuilderConfig config);

        public abstract void update(PatchSpec spec, BuilderConfig config);

        public String getCommandLineArg() {
            return commandLineArg;
        }

        public String getSyntax() {
            return syntax;
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        // - PRIVATE

        private String commandLineArg;
        private String syntax;
        private String description;
        private boolean required;

        private static Map<String,Option> argToOption = new HashMap<String, Option>();
        static {
            for (Option o : values())
                argToOption.put(o.getCommandLineArg(), o);
        }

        private Option(String commandLineArg, String syntax, boolean required, String description) {
            this.commandLineArg = commandLineArg;
            this.syntax = syntax;
            this.description = description;
            this.required = required;
        }

        private static BuilderConfig parseArgs(String[] args) {
            if (args == null || args.length == 0)
                throw new IllegalArgumentException("No arguments specified.");

            return Option.parseNextArg(new ArrayList<String>(Arrays.asList(args)), new BuilderConfig());
        }

        private static BuilderConfig parseNextArg(List<String> argList, BuilderConfig builderConfig) {
            if (argList.isEmpty()) {
                return builderConfig;
            } else {
                return Option.fromArg(argList.get(0)).parseArgs(argList, builderConfig);
            }
        }

        private static String extractArg(List<String> args, String optionName, String expectedArgName) {
            if (args == null || args.isEmpty())
                throw new IllegalArgumentException("Expected one more argument for option " + optionName);
            String arg = args.get(0);
            if (expectedArgName != null && ! expectedArgName.equals(arg))
                throw new IllegalArgumentException("Expected " + expectedArgName + ", got: " + arg);
            args.remove(0);
            return arg;
        }

        private static BuilderConfig extractOneArgumentOption(final List<String> args, BuilderConfig config, final Option option) {
            extractArg(args, option.getCommandLineArg(), option.getCommandLineArg());
            config.options.put(option, new ArrayList<String>() {{ add(extractArg(args, option.getCommandLineArg(), null)); }});
            return Option.parseNextArg(args, config);
        }

        private static BuilderConfig extractJarsignerOption(final List<String> args, BuilderConfig config, final Option option) {
            String jarOption = extractArg(args, option.getCommandLineArg(), null);
            String jarOptionValue = extractArg(args, jarOption, null);
            config.jarOptions.put(JarSignerParams.Option.fromCommandLineArg(jarOption), jarOptionValue);
            return config;
        }
    }
}

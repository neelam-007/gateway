package com.l7tech.server.processcontroller.patching.builder;

import com.l7tech.server.processcontroller.patching.PatchUtils;
import com.l7tech.server.processcontroller.patching.PatchPackageManager;
import com.l7tech.server.processcontroller.patching.PatchException;
import com.l7tech.server.processcontroller.patching.PatchPackage;
import com.l7tech.common.io.JarSignerParams;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import java.util.*;
import java.io.*;

/**
 * @author jbufu
 */
public class PatchBuilder {

    // - PUBLIC

    public static void main(String[] args) throws IOException, PatchException {
        BuilderConfig config = null;
        try {
            config = Option.parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + ExceptionUtils.getMessage(e) + "\n");
            printUsage();
            System.exit(SYNTAX_ERROR);
        }

        File patch = config.buildPatch();
        System.out.println("Created patch: " + patch.getAbsolutePath());
        System.exit(0);
    }

    // - PRIVATE

    private static final int SYNTAX_ERROR = 1;

    private static void printUsage() {
        System.out.println("Usage: " + PatchBuilder.class.getSimpleName() + " <options>");
        for (Option o : Option.values()) {
            System.out.println("\t" + o.getCommandLineArg() + " " + o.getSyntax() + "\n\t\t" + (o.isRequired() ? "Required." : "Optional.") + o.getDescription() + "\n");
        }
    }

    private static class BuilderConfig {

        // - PUBLIC

        /** Generates a patch from the collected options */
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
                spec.property(PatchPackage.Property.ROLLBACK_ALLOWED, Boolean.toString(!config.hasOption(this)));
            }},

        ROLLBACK_FOR_ID("-rollback-id", "<rollback_id>", false, "The patch identifier which the generated patch will roll back.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                spec.property(PatchPackage.Property.ROLLBACK_FOR_ID, config.getSingleValue(this));
            }},

        JAVA_BINARY("-java-path", "<java_binary_path>", false, "The path to the java binary to be used for installing the patch. Defaults to process controller's configuration if not specified.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                spec.property(PatchPackage.Property.JAVA_BINARY, config.getSingleValue(this));
            }},

        MAIN_CLASS("-main-class", "<class_name> <file_name>", false,
            "Specifies the patch JAR's Main-Class executable entry point. Use this if you have written your own patch executor. " +
            "If not specified, a default implementation is used that can handle the other options provided by this tool.") {
            @Override
            public BuilderConfig parseArgs(final List<String> args, BuilderConfig config) {
                extractArg(args, getCommandLineArg(), getCommandLineArg()); // -main-class
                config.options.put(this, new ArrayList<String>() {{
                    add(extractArg(args, getCommandLineArg(), null)); // class name
                    add(extractArg(args, getCommandLineArg(), null)); // file name
                }});
                return Option.parseNextArg(args,config);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if (config.hasOption(this)) {
                    if ( ! config.hasOption(this)) return;
                    spec.mainClass(config.options.get(this).get(0));
                    spec.entry(new PatchSpecFileEntry(spec.getMainClassEntryName(), config.options.get(this).get(1)));
                } else {
                    try {
                        spec.mainClass(PatchMain.class.getName());
                        PatchTaskBuilderUtils.addClassDependencies(spec, PatchMain.getClassDependencies());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException("Error adding class dependencies for " + PatchMain.class.getName(), e);
                    }
                }
            }},

        RESTART_AFTER("-restart-after", "<service_name>", false, "Restarts the specified at the end of patch application.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if (!config.hasOption(this)) return;
                Class<? extends PatchTask> taskClass = OSCommandPatchTask.class;
                for (String service : config.options.get(this)) {
                    PatchTaskBuilderUtils.addTask(spec, taskClass, PatchSpecTaskListEntry.Position.POST,
                        new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(("/sbin/service " + service + " restart").getBytes())));
                }
            }},

        STOP_SERVICE("-stop-service", "<service_name>", false, "Stops the specified service during the patch application and starts it at the end.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if (!config.hasOption(this)) return;
                Class<? extends PatchTask> taskClass = OSCommandPatchTask.class;
                for (String service : config.options.get(this)) {
                    PatchTaskBuilderUtils.addTask(spec, taskClass, PatchSpecTaskListEntry.Position.PRE,
                        new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(("/sbin/service " + service + " stop").getBytes())));
                    PatchTaskBuilderUtils.addTask(spec, taskClass, PatchSpecTaskListEntry.Position.POST,
                        new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(("/sbin/service " + service + " start").getBytes())));
                }
            }},

        REBOOT_NEEDED("-reboot-needed", "[reboot_message]", false, "Informs the user that a reboot is needed after the patch was applied; the reboot is not performed automatically.") {
            @Override
            public BuilderConfig parseArgs(final List<String> args, BuilderConfig config) {
                extractArg(args, getCommandLineArg(), getCommandLineArg()); // -reboot-needed
                config.options.put(this, new ArrayList<String>() {{
                    if (! args.isEmpty() && ! args.get(0).startsWith("-"))
                        add(extractArg(args, getCommandLineArg(), null));
                }});
                return Option.parseNextArg(args, config);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                String rebootMsg = config.options.get(this).isEmpty() ? "" : config.options.get(this).get(0);
                Class<? extends PatchTask> taskClass = RebootNeededPatchTask.class;
                PatchTaskBuilderUtils.addTask(spec, taskClass, PatchSpecTaskListEntry.Position.POST,
                    new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(rebootMsg.getBytes())));
            }},

        EXPECTED_RPMS("-expected-rpms", "<expected_rpm|file_with_expected_rpms>", false, "RPM name or file containing RPM names, one per line, in the default format used by 'rpm -q'") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                for (String rpmOrList : config.options.get(this)) {
                    String rpmList = concat(rpmList(rpmOrList), " ");
                    Class<? extends PatchTask> taskClass = OSCommandPatchTask.class;
                    PatchTaskBuilderUtils.addTask(spec, taskClass, PatchSpecTaskListEntry.Position.PRE,
                        new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(("/bin/rpm -q " + rpmList).getBytes())));
                }
            }},

        RPM_UPDATE("-rpm-update", "<rpm_file|file_with_rpm_file_names>", false, "Updates the specified RPM package, or the packages listed, one per line, in the specified file.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                for (final String rpmFileOrList : config.options.get(this)) {
                    List<String> rpmList = rpmFileOrList.toLowerCase().endsWith(".rpm") ? new ArrayList<String>() {{ add(rpmFileOrList); }} : rpmList(rpmFileOrList);
                    List<PatchSpecEntry> resources = new ArrayList<PatchSpecEntry>();
                    resources.add(new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(concat(rpmList, "\n").getBytes())));
                    resources.addAll(Functions.map(rpmList, new Functions.Unary<PatchSpecEntry, String>() {
                            @Override
                            public PatchSpecEntry call(String rpmFileName) {
                                return new PatchSpecFileEntry(rpmFileName, rpmFileName);
                            }
                        }));
                    Class<? extends PatchTask> taskClass = RpmUpdatePatchTask.class;
                    PatchTaskBuilderUtils.addTask(spec, taskClass, resources.toArray(new PatchSpecEntry[resources.size()]));
                }
            }},

        FILE_UPDATE("-file-update", "<target_file_path> <file>", false, "Updates the specified file with the provided replacement.") {
            @Override
            public BuilderConfig parseArgs(final List<String> args, BuilderConfig config) {
                extractArg(args, getCommandLineArg(), getCommandLineArg()); // -file-update
                config.options.put(this, new ArrayList<String>() {{
                    add(extractArg(args, getCommandLineArg(), null)); // target path
                    add(extractArg(args, getCommandLineArg(), null)); // local file name
                }});
                return Option.parseNextArg(args,config);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                Class<? extends PatchTask> taskClass = FileUpdatePatchTask.class;
                Iterator<String> argIterator = config.options.get(this).iterator();
                String targetPath;
                String file;
                while(argIterator.hasNext() && (targetPath = argIterator.next()) != null && (file = argIterator.next()) != null) {
                    List<PatchSpecEntry> resources = new ArrayList<PatchSpecEntry>();
                    resources.add(new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream((targetPath + "\n" + file).getBytes())));
                    resources.add(new PatchSpecFileEntry(file, file));
                    PatchTaskBuilderUtils.addTask(spec, taskClass, resources.toArray(new PatchSpecEntry[resources.size()]));
                }
            }},

        DB_UPDATE("-db-update", "<node_name>, <db_update_script>", false, "Runs the database update script on the specified node.") {
            @Override
            public BuilderConfig parseArgs(final List<String> args, BuilderConfig config) {
                extractArg(args, getCommandLineArg(), getCommandLineArg()); // -db-update
                config.options.put(this, new ArrayList<String>() {{
                    add(extractArg(args, getCommandLineArg(), null)); // node name
                    add(extractArg(args, getCommandLineArg(), null)); // db update script
                }});
                return Option.parseNextArg(args,config);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                Class<? extends PatchTask> taskClass = DbUpdatePatchTask.class;
                Iterator<String> argIterator = config.options.get(this).iterator();
                String nodeName;
                String dbUpdateScript;
                while(argIterator.hasNext() && (nodeName = argIterator.next()) != null && (dbUpdateScript = argIterator.next()) != null) {
                    List<PatchSpecEntry> resources = new ArrayList<PatchSpecEntry>();
                    resources.add(new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream((nodeName + "\n" + dbUpdateScript).getBytes())));
                    resources.add(new PatchSpecFileEntry(dbUpdateScript, dbUpdateScript));
                    PatchTaskBuilderUtils.addTask(spec, taskClass, resources.toArray(new PatchSpecEntry[resources.size()]));
                }
            }
        },

        COMMAND("-command", "<command_to_run>", false, "Executes the specified command on the gateway.") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                Class<? extends PatchTask> taskClass = OSCommandPatchTask.class;
                for (String command : config.options.get(this)) {
                    PatchTaskBuilderUtils.addTask(spec, taskClass, new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(command.getBytes())));
                }
            }
        },

        SCRIPT("-script", "<shell_script", false, "Executes the supplied shell script using the bash interpreter on the gateway") {
            @Override
            public BuilderConfig parseArgs(List<String> args, BuilderConfig config) {
                return extractOneArgumentOption(args, config, this);
            }
            @Override
            public void update(PatchSpec spec, BuilderConfig config) {
                if ( ! config.hasOption(this)) return;
                Class<? extends PatchTask> taskClass = ShellScriptPatchTask.class;
                for (String script : config.options.get(this)) {
                    List<PatchSpecEntry> resources = new ArrayList<PatchSpecEntry>();
                    resources.add(new PatchSpecStreamEntry(PatchTask.TASK_RESOURCE_FILE, new ByteArrayInputStream(script.getBytes())));
                    resources.add(new PatchSpecFileEntry(script, script));
                    PatchTaskBuilderUtils.addTask(spec, taskClass, resources.toArray(new PatchSpecEntry[resources.size()]));
                }
            }
        },

/*
        EXPECTED_FILE("-expected-file", "<existing_file_absolute_path> <expected_md5>", false, "Verifies that ...") {

        },
*/

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

        private static String concat(List<String> strings, String delimiter) {
            StringBuilder result = new StringBuilder();
            for (String s : strings) {
                result.append(s).append(delimiter);
            }
            return result.toString();
        }

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

        // - Option PRIVATE

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
            if (! config.options.containsKey(option))
                config.options.put(option, new ArrayList<String>());
            List<String> argList = config.options.get(option);
            argList.add(extractArg(args, option.getCommandLineArg(), null));
            return Option.parseNextArg(args, config);
        }

        private static BuilderConfig extractJarsignerOption(final List<String> args, BuilderConfig config, final Option option) {
            String jarOption = extractArg(args, option.getCommandLineArg(), null);
            String jarOptionValue = extractArg(args, jarOption, null);
            config.jarOptions.put(JarSignerParams.Option.fromCommandLineArg(jarOption), jarOptionValue);
            return Option.parseNextArg(args, config);
        }

        private static List<String> rpmList(final String rpmOrList) {
            if (rpmOrList == null || rpmOrList.isEmpty())
                throw new IllegalArgumentException("Invalid RPM list: " + rpmOrList);

            File rpmList = new File(rpmOrList);
            if (rpmList.exists()) {
                try {
                    List<String> result = new ArrayList<String>();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(rpmList)));
                    String line;
                    while ( (line = reader.readLine()) != null) {
                        result.add(line);
                    }
                    return result;
                } catch (IOException e) {
                    throw new RuntimeException("Error readin rpm file list: " + rpmOrList); 
                }
            } else {
                return new ArrayList<String>() {{ add(rpmOrList); }};
            }
        }
    }
}

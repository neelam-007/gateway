package com.l7tech.server.processcontroller.patching.client;

import com.l7tech.server.processcontroller.*;
import com.l7tech.server.processcontroller.patching.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.ws.WebServiceException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author jbufu
 */
public class PatchCli {

    // - PUBLIC

    public static final int SYNTAX_ERROR = 1;
    public static final int PATCH_API_ERROR = 2;

    public static void main(String[] args) {
        initLogging();

        logger.info("Running PatchCli patch client with arguments: " + Arrays.toString(args));

        String programName = null;
        PatchAction patchAction = null;
        try {
            if (args == null || args.length == 0)
                throw new IllegalArgumentException("No arguments specified.");
            List<String> argList = new ArrayList<>(Arrays.asList(args));
            programName = PatchCli.extactProgramName(argList);
            patchAction = PatchAction.fromArgs(argList);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            System.out.println("Error: " + ExceptionUtils.getMessage(e) + "\n");
            printUsage(programName);
            System.exit(SYNTAX_ERROR);
        }

        Throwable thrown = null;
        String errPrefix = null;
        try {
            PatchServiceApi api = getApi(patchAction);
            logger.log(Level.INFO, "Running patch action: " + patchAction.name());
            Collection<PatchStatus> result = patchAction.call(api);
            if (result != null && ! result.isEmpty()) {
                boolean isError = false;
                // Suppress error when deleting a patch with ERROR status.
                if (patchAction != PatchAction.DELETE) {
                    for(PatchStatus status : result) {
                        System.out.println(status.toString(patchAction.getOutputFormat()));
                        isError = isError || PatchStatus.State.ERROR.name().equals(status.getField(PatchStatus.Field.STATE));
                    }
                }
                if (patchAction.isReportStatusErrors()) {
                    System.out.println(isError ? "There were errors during the patch operation. See sspc logs in " + DEFAULT_PC_HOME + "/var/logs for details." : "Patch operation completed successfully.");
                }
            } else if (patchAction == PatchAction.VERSION) {
                System.out.println(patchAction.getPatchServiceApiVersion());
            } else if (patchAction == PatchAction.AUTODELETE && result == null) { // result == null represents autodelete function getting current auto delete configuration status.
                System.out.println(patchAction.getCurrentAutoDeleteStatus());     // result.isEmpty represents autodelete function setting auto delete configuration.
            } else if (patchAction == PatchAction.DELETE) {
                System.out.println("There are no patches that can be deleted.\n\nOnly patches that have .L7P files existing in patch repository can be deleted.");
            } else if (patchAction == PatchAction.UPLOAD) {
                System.out.println("No patches have been uploaded.");
            }
            logger.log(Level.INFO, patchAction.name() + " returned " + (result == null ? 0 : result.size()) + " results.");
        } catch (IOException e1) {
            thrown = e1;
            errPrefix = "I/O Error: ";
        } catch (PatchException e2) {
            thrown = e2;
            errPrefix = "Patch API Error: ";
        } catch (RuntimeException re) {
            // todo: perhaps remove check for instanceof WebServiceException
            // todo: probably it should be safe to treat RuntimeException(s) with cause IOException as an IO errors
            if (re instanceof WebServiceException && re.getCause() instanceof IOException) {
                thrown = re.getCause();
                errPrefix = "I/O Error: ";
            } else {
                thrown = re;
                errPrefix = "Runtime error (check configuration): ";
            }
        }

        if (thrown != null) {
            logger.log(Level.WARNING, errPrefix + ExceptionUtils.getMessage(thrown), ExceptionUtils.getDebugException(thrown));
            System.out.println(errPrefix + ExceptionUtils.getMessage(thrown) + "\n");
            System.exit(PATCH_API_ERROR);
        }

        System.exit(0);
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(PatchCli.class.getName());

    private static final String DEFAULT_PC_HOME = "/opt/SecureSpan/Controller";
    private static final String DEFAULT_PATCH_API_ENDPOINT = "https://localhost:8765/services/patchServiceApi";

    private static final String DEFAULT_PC_HOSTCONFIG_PATH = DEFAULT_PC_HOME + "/etc/host.properties";
    private static final String PC_HOSTCONFIG_PATH = SyspropUtil.getString( "com.l7tech.server.processcontroller.hostProperties" , DEFAULT_PC_HOSTCONFIG_PATH );

    private static final long PATCH_API_CONNECT_TIMEOUT = 30000;
    private static final long PATCH_API_RECEIVE_TIMEOUT = 600000;

    private static final String SORT_ARG_NAME = "-sort";
    private static final String SORT_FORMAT_GROUP_DELIMITER = "\\|";
    private static final String SORT_FORMAT_VALUE_DELIMITER = "::";
    private static final String SORT_DESCENDING_PREFIX = "-";
    private static final String IGNORE_DELETED_PATCHES_ARG_NAME = "-ignoreDeletedPatches";

    private static void initLogging() {
        // configure logging if the logs directory is found, else leave console output
        final File logsDir = new File("/opt/SecureSpan/Controller/var/logs");
        if ( logsDir.exists() && logsDir.canWrite() )
            JdkLoggerConfigurator.configure("com.l7tech.server.processcontroller.patching.client", "com/l7tech/server/processcontroller/patching/client/resources/logging.properties", DEFAULT_PC_HOME + "/etc/conf/patcher.properties");
        } else if ( !SyspropUtil.getBoolean( "com.l7tech.server.log.console", false ) ){
            final Logger rootLogger = Logger.getLogger( "" );
            for ( final Handler handler : rootLogger.getHandlers() ) {
                if ( handler instanceof ConsoleHandler ) {
                    rootLogger.removeHandler( handler );
                }
            }
        }
        if ( SyspropUtil.getBoolean( "com.l7tech.server.log.console", false ) ) {
            Logger.getLogger( "" ).addHandler( new ConsoleHandler() );
        }
    }

    private static PatchServiceApi getApi(PatchAction patchAction) {
        String target = patchAction.getTarget();
        if (target.toLowerCase().startsWith("http://") || target.toLowerCase().startsWith("https://")) {
            logger.log(Level.INFO, "Using Patch Service API endpoint: " + target);
            final HTTPClientPolicy clientPolicy = new HTTPClientPolicy();
            clientPolicy.setConnectionTimeout(PATCH_API_CONNECT_TIMEOUT);
            clientPolicy.setReceiveTimeout(PATCH_API_RECEIVE_TIMEOUT);
            clientPolicy.setCookie( "PC-AUTH=" + getHostSecret() );
            System.setProperty("org.apache.cxf.nofastinfoset", "true");

            return new CxfUtils.ApiBuilder(patchAction.getTarget()).clientPolicy(clientPolicy)
                //.inInterceptor(new LoggingInInterceptor())
                //.outInterceptor(new LoggingOutInterceptor())
                //.inFaultInterceptor(new LoggingInInterceptor())
                //.outFaultInterceptor(new LoggingOutInterceptor())
                .build(PatchServiceApi.class);
        } else {
            try {
                logger.log(Level.INFO, "Using patch configuration from Process Controller.");
                SyspropUtil.setProperty(ConfigService.PC_HOMEDIR_PROPERTY, target);
                final ConfigService config = new ConfigServiceImpl();
                PatchPackageManager packageManager = new PatchPackageManagerImpl(config.getPatchesDirectory(), new PatchTrustStore() {
                    @Override
                    public Set<X509Certificate> getTrustedCerts() {
                        return config.getTrustedPatchCerts();
                    }
                });
                PatchRecordManager recordManager = new PatchFileRecordManager(config.getPatchesLog());
                return new PatchServiceApiImpl(config, packageManager, recordManager);
            } catch (Exception e) {
                throw new IllegalStateException("Error getting patch API: " + ExceptionUtils.getMessage(e));
            }
        }
    }

    private static String getHostSecret() {
        String secret = SyspropUtil.getString( "com.l7tech.server.processcontroller.pcAuth", "" );
        if ( secret.isEmpty() ) {
            final File propertiesFile = new File(PC_HOSTCONFIG_PATH);
            if ( propertiesFile.exists() ) {
                try {
                    final Properties properties = loadProperties( propertiesFile );
                    secret = properties.getProperty( "host.secret", "" );
                } catch ( IOException e ) {
                    logger.warning( "Unable to load pc secret '"+ExceptionUtils.getMessage( e )+"'." );
                }
            }
        }
        return secret;
    }

    private static Properties loadProperties( final File propertiesFile ) throws IOException {
        final Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream( propertiesFile );
            properties.load(fileInputStream);
        } finally {
            ResourceUtils.closeQuietly(fileInputStream);
        }
        return properties;
    }

    static enum PatchAction {

        // - PUBLIC

        UPLOAD(" <filename>", "Uploads the patch represented by the <filename> to the gateway.") {
            @Override
            void extractActionArguments(List<String> args) {
                this.argument = extractOneStringActionArgument(args);
                File patchFile = new File(getArgument());
                if (! patchFile.exists() || ! patchFile.isFile())
                    throw new IllegalArgumentException("Invalid patch file: " + getArgument());
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException {
                final PatchStatus status = api.uploadPatch(new DataHandler(new FileDataSource(getArgument())));
                return new ArrayList<PatchStatus>() {{ add(status); }};
            }},
        INSTALL("<patch_id>", "Installs the (previously uploaded) patch represented by the provided ID on the gateway.") {
            @Override
            void extractActionArguments(List<String> args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                final PatchStatus status = api.installPatch(getArgument(), null);
                return new ArrayList<PatchStatus>() {{ add(status); }};
            }},
        DELETE("[<patch_id> | all]", "If <patch_id> specified, deletes the package archive of a patch represented by the provided ID from the gateway patch manager's repository.  If 'all' specified, bulk deletes all patch files.") {
            @Override
            void extractActionArguments(List<String> args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                return api.deletePackageArchive(getArgument());
            }},
        AUTODELETE("[true | false]", "Uses 'true'/'false' to enable/disable automatic patch deletion after installation.  If no option specified, displays the current status of this configuration.") {
            @Override
            void extractActionArguments(List<String> args) {
                if (args.size() == 1) {
                    argument = null;
                } else if (args.size() > 1) {
                    argument = extractOneStringActionArgument(args);
                }
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                final String option = getArgument();
                if (option == null) { // Display auto delete status
                    currentAutoDeleteStatus = String.valueOf(api.getAutoDelete());
                    return null;
                } else {
                    api.setAutoDelete(new Boolean(option));
                    return new ArrayList<>();
                }
            }},
        /**
         * New action to distinguish if Patch Service API is a new version or old version.
         * The new version has a few changes such as single deletion, bulk deletion, auto
         * deletion, etc.  This new action is hidden from user.
         */
        VERSION("", "Retrieve the version number of Patch Service API.  No arguments for this action.", true, false) {
            @Override
            void extractActionArguments(List<String> args) {
                // No any argument needed
                argument = null;
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                patchServiceApiVersion = api.getLatestPatchServiceApiVersion().toString();
                return null;
            }},
        STATUS("<patch_id>", "Returns the status of the patch represented by the provided ID on the gateway.", true, false) {
            @Override
            void extractActionArguments(List<String> args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                final PatchStatus status = api.getStatus(getArgument());
                return new ArrayList<PatchStatus>() {{ add(status); }};
            }},
        /**
         * Newly added optional argument {@code [-sort <sorting_format>]} is not exposed to the end user.
         * <p/>
         * The {@code sorting_format} format is "group_by_states|states_sort_field", where
         * {@code group_by_states} is optional and specifies whether to group the result by the specified
         * {@link com.l7tech.server.processcontroller.patching.PatchStatus.State state}'s (separated by {@code ::}), and
         * {@code states_sort_field} indicates the sorting {@link com.l7tech.server.processcontroller.patching.PatchStatus.Field field}(s)
         * (also separated by {@code ::}).<br>
         * Note that when {@code group_by_states} is specified, then the number of fields inside {@code states_sort_field} must match.
         * <p/>
         * Example: <br>
         *     {@code -sort "INSTALLED:UPLOADED|-LAST_MOD:ID"}<br>
         *         this indicates that the result should be grouped by state, having {@link com.l7tech.server.processcontroller.patching.PatchStatus.State#INSTALLED INSTALLED}
         *         patches first sorted by {@link com.l7tech.server.processcontroller.patching.PatchStatus.Field#LAST_MOD LAST_MOD} desc,
         *         followed by {@link com.l7tech.server.processcontroller.patching.PatchStatus.State#UPLOADED UPLOADED} sorted by {@link com.l7tech.server.processcontroller.patching.PatchStatus.Field#ID ID} asc,
         *         and the remaining patches should be unsorted and group by their natural state order (as defined by the enum itself).
         *     <p/>
         *     {@code -sort "ID"}<br>
         *         this indicates that the result should be ungrouped and sorted by {@link com.l7tech.server.processcontroller.patching.PatchStatus.Field#ID ID} in ascending order.
         *
         *  Newly added optional argument {@code <-ignoreDeletedPatches>} will indicated if a returned patch list contains those patches with .L7P files deleted.
         *  If the argument is provided, then the return list will not contains those deleted patches.
         *  If the arugment is not provied, then the return list will contain any status of patches.
         */
        LIST("[output_format]<-ignoreDeletedPatches>", "Returns a list of the patches and their statuses on the gateway. Status fields to be replaced by their value are preceded and followed by a colon, for example: \"Patch ID=:id:, status is :state:\"." +
            "\n\t\t<-ignoreDeletedPatches>: if provided, the return list will exclude those patches, whose L7P files have been deleted.", false, false) {
            @Override
            void extractActionArguments(List<String> args) {
                // first read the optional sort args
                if (!args.isEmpty() && args.size() > 1) {
                    final int sortArgIndex = args.indexOf(SORT_ARG_NAME);
                    if (sortArgIndex != -1) {
                        // make sure the sort format is next
                        if (sortArgIndex + 1 < args.size()) {
                            this.sortingFormat = args.get(sortArgIndex + 1);
                            args.remove(sortArgIndex + 1);
                        } else {
                            // silently fail as is there was no sorting specified
                            logger.log(Level.WARNING, "ignoring -sort without specifying sorting_format");
                        }
                        args.remove(sortArgIndex);
                    }
                }
                if (!args.isEmpty() && args.size() > 1) {
                    final int argIndex = args.indexOf(IGNORE_DELETED_PATCHES_ARG_NAME);
                    if (argIndex != -1) {
                        ignoreDeletedPatches = true;
                        args.remove(argIndex);
                    }
                }
                if (!args.isEmpty() && args.size() > 1) {
                    this.outputFormat = extractOneStringActionArgument(args);
                }
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                final Collection<PatchStatus> statuses = api.listPatches(ignoreDeletedPatches, true);
                if (statuses != null && statuses.size() > 1 && hasSortArg() ) {
                    final String[] grouping = sortingFormat.split(SORT_FORMAT_GROUP_DELIMITER);
                    if (grouping.length == 1) {
                        // do not group by status only sort by the specified field
                        return grouping[0].startsWith(SORT_DESCENDING_PREFIX) ?
                                sortedStatusesByField(PatchStatus.Field.valueOf(grouping[0].substring(1)), false, statuses): // descending
                                sortedStatusesByField(PatchStatus.Field.valueOf(grouping[0]), true, statuses); // ascending
                    } else if (grouping.length > 1) {
                        // group by the specified states and sort each state by the specified sort field
                        final String[] states = grouping[0].split(SORT_FORMAT_VALUE_DELIMITER);
                        final String[] fields = grouping[1].split(SORT_FORMAT_VALUE_DELIMITER);
                        if (states.length > 0 && states.length == fields.length) {
                            final Map<PatchStatus.State, Collection<PatchStatus>> result = new LinkedHashMap<>(PatchStatus.State.values().length);
                            // add requested states first
                            for (int i = 0; i < states.length; ++i) {
                                final PatchStatus.State state = PatchStatus.State.valueOf(states[i]);
                                if (!result.containsKey(state)) {
                                    result.put(
                                            state,
                                            fields[i].startsWith(SORT_DESCENDING_PREFIX) ?
                                                    sortedStatusesByField(PatchStatus.Field.valueOf(fields[i].substring(1)), false): // descending
                                                    sortedStatusesByField(PatchStatus.Field.valueOf(fields[i]), true) // ascending
                                    );
                                }
                            }
                            // add the remaining states in order of the enum, having unsorted array of patch statuses
                            for (final PatchStatus.State state : PatchStatus.State.values()) {
                                if (!result.containsKey(state))
                                    result.put(state, new ArrayList<PatchStatus>());
                            }
                            // loop through all statuses returned from the api and add them in the corresponding sorted collection
                            for (final PatchStatus status : statuses) {
                                final PatchStatus.State state = PatchStatus.State.valueOf(status.getField(PatchStatus.Field.STATE));
                                if (state == null) {
                                    throw new IllegalStateException("Mandatory field 'STATE' missing from patch status: " + status.toString());
                                }
                                final Collection<PatchStatus> patchStatuses = result.get(state);
                                if (patchStatuses == null) {
                                    throw new IllegalStateException("Missing state '" + state.name() + "' from sorted status map");
                                }
                                patchStatuses.add(status);
                            }
                            // finally create a resulting ordered collection
                            final Collection<PatchStatus> orderedStatuses = new ArrayList<>();
                            for (final Collection<PatchStatus> patchStatuses : result.values()) {
                                orderedStatuses.addAll(patchStatuses);
                            }
                            return orderedStatuses;
                        }
                    }
                    // silently ignore invalid format
                    logger.log(Level.WARNING, "ignoring invalid sorting_format: " + sortingFormat);
                }
                return statuses;
            }

            private SortedSet<PatchStatus> sortedStatusesByField(final PatchStatus.Field field, final boolean asc) {
                return sortedStatusesByField(field, asc, null);
            }

            private SortedSet<PatchStatus> sortedStatusesByField(final PatchStatus.Field field, final boolean asc, final Collection<PatchStatus> initialStatuses) {
                final SortedSet<PatchStatus> result = new TreeSet<>(new Comparator<PatchStatus>() {
                    @Override
                    public int compare(final PatchStatus o1, final PatchStatus o2) {
                        if (o1 == o2) {
                            // same instance
                            return 0;
                        }

                        final Object value1, value2;
                        if (PatchStatus.Field.LAST_MOD == field) {
                            // number comparison
                            value1 = o1 != null ? Long.parseLong(o1.getField(field)) : null;
                            value2 = o2 != null ? Long.parseLong(o2.getField(field)) : null;
                        } else {
                            // string comparison
                            value1 = o1 != null ? o1.getField(field) : null;
                            value2 = o2 != null ? o2.getField(field) : null;
                        }

                        final int diff;
                        if (value1 == null) {
                            diff = -1;
                        } else if (value2 == null) {
                            diff = 1;
                        } else {
                            // compare objects
                            diff = value1 instanceof Long ?
                                    ((Long)value1).compareTo((Long)value2) :
                                    ((String)value1).compareTo((String)value2);
                        }

                        return asc ? diff : -diff;
                    }
                });
                if (initialStatuses != null) {
                    result.addAll(initialStatuses);
                }
                return result;
            }

            private boolean hasSortArg() {
                return sortingFormat != null && !sortingFormat.trim().isEmpty();
            }
        };

        public String getSyntax() {
            return syntax;
        }

        public String getDescription() {
            return description;
        }

        public String getTarget() {
            return target;
        }

        public String getArgument() {
            return argument;
        }

        public String getOutputFormat() {
            return outputFormat;
        }

        public String getPatchServiceApiVersion() {
            return patchServiceApiVersion;
        }

        String getCurrentAutoDeleteStatus() {
            return currentAutoDeleteStatus;
        }

        public boolean isHidden() {
            return hidden;
        }

        public boolean isReportStatusErrors() {
            return reportStatusErrors;
        }

        public static PatchAction fromArgs(List<String> argList) {

            String target = getTarget(argList);
            if (target != null) {
                argList.remove(0);
            } else {
                target = PCUtils.isAppliance() ? DEFAULT_PATCH_API_ENDPOINT : DEFAULT_PC_HOME;
                logger.log(Level.INFO, "Using default target: " + target);
            }

            if (argList == null || argList.isEmpty())
                throw new IllegalArgumentException("No action specified.");

            PatchAction patchAction;
            try {
                patchAction = PatchAction.valueOf(argList.get(0).toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid action: " + argList.get(0), e);
            }

            patchAction.target = target;
            patchAction.extractActionArguments(argList);
            return patchAction;
        }

        public abstract Collection<PatchStatus> call(PatchServiceApi api) throws PatchException, IOException;

        // - PACKAGE

        abstract void extractActionArguments(List<String> args);

        String target;
        String argument;
        String outputFormat;
        String sortingFormat;
        String currentAutoDeleteStatus;
        String patchServiceApiVersion;
        boolean ignoreDeletedPatches;

        // - PRIVATE

        private final String syntax;
        private final String description;
        private final boolean hidden;
        private final boolean reportStatusErrors;

        private PatchAction(String syntax, String description) {
            this(syntax, description, false, true);
        }
        private PatchAction(String syntax, String description, boolean hidden, boolean reportStatusErrors) {
            this.syntax = syntax;
            this.description = description;
            this.hidden = hidden;
            this.reportStatusErrors = reportStatusErrors;
        }

        private static String extractOneStringActionArgument(List<String> args) {
            // action argument is #2
            if (args == null || args.size() < 2)
                throw new IllegalArgumentException("Argument for the patch action not specified.");

            return args.get(1);
        }

        private static String getTarget(List<String> argList) {
            if (argList == null || argList.size()== 0) return null;
            String firstArg = argList.get(0);

            try {
                PatchAction.valueOf(firstArg.toUpperCase());
                return null;
            } catch (Exception e) {
                // first arg is not an action
            }

            File maybePcHome = new File(firstArg);
            if (firstArg.toLowerCase().startsWith("http://") || firstArg.toLowerCase().startsWith("https://")) {
                try {
                    URL endpoint = new URL(firstArg);
                    return endpoint.toString();
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid patch API endpoint specified: " + ExceptionUtils.getMessage(e));
                }
            } else if (maybePcHome.exists() && maybePcHome.isDirectory()) {
                return firstArg;
            } else {
                throw new IllegalArgumentException("Invalid target specified: " + firstArg);
            }
        }
    }

    private static String extactProgramName(List<String> argList) {
        if (argList != null && argList.size() >= 2 && "-scriptname".equals(argList.get(0))) {
            String name = argList.get(1);
            argList.remove(0);
            argList.remove(0);
            return name;
        } else {
            return null;
        }
    }

    private static void printUsage(String programName) {
        System.out.println("Usage: " + (programName != null ? programName : PatchCli.class.getSimpleName()) + " [target] <action>");
        System.out.println("\n\t[target]: [patch API endpoint URL | Process Controller home directory]");
        System.out.println("\n\tIf not specified, the [target] defaults to:");
        System.out.println("\t\t\"" + DEFAULT_PATCH_API_ENDPOINT + "\" on appliances.");
        System.out.println("\t\t\"" + DEFAULT_PC_HOME + "\" on software installations.");
        System.out.println("\n\t<action>:");
        for (PatchAction o : PatchAction.values()) {
            if (o.isHidden()) continue;
            System.out.println("\t" + o.name().toLowerCase() + " " + o.getSyntax() + "\n\t\t" + o.getDescription());
        }
    }
}

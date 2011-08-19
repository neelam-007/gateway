package com.l7tech.server.processcontroller.patching.client;

import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.ConfigServiceImpl;
import com.l7tech.server.processcontroller.CxfUtils;
import com.l7tech.server.processcontroller.PCUtils;
import com.l7tech.server.processcontroller.patching.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
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
            List<String> argList = new ArrayList<String>(Arrays.asList(args));
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
                for(PatchStatus status : result) {
                    System.out.println(status.toString(patchAction.getOutputFormat()));
                    isError = isError || PatchStatus.State.ERROR.name().equals(status.getField(PatchStatus.Field.STATE));
                }
                if (patchAction.isReportStatusErrors()) {
                    System.out.println(isError ? "There were errors during the patch operation." : "Patch operation completed successfully.");
                }
            } else {
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
            thrown = re;
            errPrefix = "Runtime error (check configuration): ";
        }

        if (thrown != null) {
            logger.log(Level.WARNING, errPrefix + ExceptionUtils.getMessage(thrown), thrown instanceof PatchException ? null : thrown);
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


    private static void initLogging() {
        // configure logging if the logs directory is found, else leave console output
        final File logsDir = new File("/opt/SecureSpan/Controller/var/logs");
        if ( logsDir.exists() && logsDir.canWrite() ) {
            JdkLoggerConfigurator.configure("com.l7tech.server.processcontroller.patching.client", "com/l7tech/server/processcontroller/patching/client/resources/logging.properties", "etc/conf/patchinglogging.properties");
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

    private static enum PatchAction {

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
        DELETE("<patch_id>", "Deletes the package archive of patch represented by the provided ID from the gateway patch manager's repository.") {
            @Override
            void extractActionArguments(List<String> args) {
                this.argument = extractOneStringActionArgument(args);
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                final PatchStatus status = api.deletePackageArchive(getArgument());
                return new ArrayList<PatchStatus>() {{ add(status); }};
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
        LIST("[output_format]", "Returns a list of the patches and their statuses on the gateway. Status fields to be replaced by their value are preceded and followed by a colon, for example: \"Patch ID=:id:, status is :state:\"", false, false) {
            @Override
            void extractActionArguments(List<String> args) {
                if (! args.isEmpty() && args.size() > 1) {
                    this.outputFormat = extractOneStringActionArgument(args);
                }
            }
            @Override
            public Collection<PatchStatus> call(PatchServiceApi api) throws PatchException {
                return api.listPatches();
            }};

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

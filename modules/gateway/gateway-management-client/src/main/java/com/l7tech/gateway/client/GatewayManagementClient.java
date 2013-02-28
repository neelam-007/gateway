package com.l7tech.gateway.client;

import com.l7tech.gateway.api.Accessor.AccessorException;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.util.*;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.*;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Client utility for the SecureSpan Gateway management service.
 */
public class GatewayManagementClient {

    //- PUBLIC

    public static void main( final String[] args ) {
        JdkLoggerConfigurator.configure("com.l7tech.gateway.client", "com/l7tech/gateway/client/logging.properties", "logging.properties", true);
        GatewayManagementClient gmc = new GatewayManagementClient(args, System.in, System.out, System.err);
        final int exitCode = gmc.run();
        System.err.flush();
        System.exit( exitCode );
    }

    public GatewayManagementClient( final String args[],
                                    final InputStream in,
                                    final OutputStream out,
                                    final OutputStream err )  {
        final Arguments arguments = new Arguments();
        final Map<Integer,String> extraArgs = new TreeMap<Integer,String>();
        this.in = in;
        this.out = out;
        this.err = err;

        Command command = null;
        String error = null;
        try {
            try {
                Cli.process( arguments, args, extraArgs );

                final List<String> extraArguments = new ArrayList<String>();
                if ( extraArgs.size() > 2 ) {
                    for ( final Map.Entry<Integer,String> arg : extraArgs.entrySet() ) {
                        if ( arg.getKey() > 1 ) {
                            extraArguments.add( arg.getValue() );
                        }
                    }
                }

                command = buildCommand(extraArgs.get(1), extraArgs.get(0), arguments, extraArguments);
            } catch ( Cli.CliException ce ) {
                error = ce.getMessage();
                printError( ce.getMessage() );
                usage();
            }
        } catch ( CommandException ce ) {
            error = ce.getMessage();
            handleCommandException( ce );
        }

        this.commandLineArguments = args;
        this.arguments = arguments;
        this.command = command;
        this.error = error;
    }

    public int run() {
        int exitCode = 1;

        try {
            exitCode = doRun();
        } catch ( Exception e ) {
            handleErrorException( e );
        }

        return exitCode;
    }

    //- PRIVATE

    private static final XMLReporter SILENT_REPORTER = new XMLReporter() {
        @Override
        public void report( final String message, final String errorType, final Object relatedInformation, final Location location ) throws XMLStreamException {
            throw new XMLStreamException(message, location);
        }
    };

    private static final XMLResolver FAILING_RESOLVER = new XMLResolver() {
        @Override
        public Object resolveEntity( final String publicID, final String systemID, final String baseURI, final String namespace ) throws XMLStreamException {
            throw new XMLStreamException("External entity access forbidden '"+systemID+"' relative to '"+baseURI+"'.");
        }
    };

    private static final ResourceBundle resources = ResourceBundle.getBundle(GatewayManagementClient.class.getName());

    private static final String URL_TEMPLATE = "https://{0}:{1}/wsman";
    private static final String DEFAULT_HTTPS_PORT = "8443";
    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    private static final Charset charset = Charset.defaultCharset();

    private final InputStream in;
    private final OutputStream out;
    private final OutputStream err;
    private final String[] commandLineArguments;
    private final Arguments arguments;
    private final Command command;
    private final String error;

    private int doRun() {
        int exitCode = 1;
        if ( arguments.help ) {
            String commandName = null;
            for ( String arg : commandLineArguments ) {
                if ( isCommand(arg) ) {
                    commandName = arg;
                    break;
                }
            }

            if ( commandName == null ) {
                try {
                    usage();
                    exitCode = 0;
                } catch ( CommandException ce ) {
                    handleCommandException( ce );
                }
            } else {
                try {
                    String types = resources.getString( "help.types" );
                    String common = resources.getString( "help.common" );
                    String help = resources.getString( "help.command." + commandName );
                    printInfo(MessageFormat.format( help, types, common ));
                    exitCode = 0;
                } catch ( MissingResourceException mre ) {
                    printInfo("Help not available for command '"+commandName+"'.");
                }
            }
        } else if ( arguments.version ) {
            printInfoMessage( "message.version", getVersion() );
            exitCode = 0;
        } else {
            if ( command != null ) {
                try {
                    initProxy();
                    command.run();
                    exitCode = 0;
                } catch ( CommandException ce ) {
                    handleCommandException( ce );
                } catch ( ManagementRuntimeException mre ) {
                    handleManagementException( mre );
                }
            } else if ( error == null ) {
                try {
                    usage();
                    exitCode = 1;
                } catch ( CommandException ce ) {
                    handleCommandException( ce );
                }
            }
        }
        return exitCode;        
    }

    private void usage() throws CommandException {
        final StringBuilder usage = new StringBuilder(1024);
        try {
            Cli.usage( new Arguments(), usage );
        } catch ( IOException ioe ) {
            throw new CommandException("Error writing usage", ioe);
        }
        printInfoMessage( "usage" );
        printInfo(usage.toString());
    }

    private static String getVersion() {
        return BuildInfo.getProductVersion() + " (" + BuildInfo.getBuildNumber() + ")";
    }

    private void printInfo( final String message ) {
        printOutput( out, message );
    }

    private void printInfoMessage( final String messageKey, final Object... parameters ) {
        printOutputMessage( out, messageKey, parameters );
    }

    private void printError( final String message ) {
        printOutput( err, message );
    }

    private void printErrorMessage( final String messageKey, final Object... parameters ) {
        printOutputMessage( err, messageKey, parameters );
    }

    private void printOutputMessage( final OutputStream out, final String messageKey, final Object... parameters ) {
        printOutput( out, getMessage(messageKey, parameters) );
    }

    private void printOutput( final OutputStream out, final String message ) {
        try {
            out.write( (message + "\n").getBytes( charset ));
        } catch ( IOException ioe ) {
            // suppress error writing message
        }
    }

    private String getMessage( final String messageKey, final Object... parameters ) {
        return tabReplace(MessageFormat.format( resources.getString(messageKey), parameters ));
    }

    private String tabReplace( final String text ) {
        return text.replaceAll("\t", "    ");        
    }

    private Command buildCommand( final String commandName,
                                  final String hostPortOrUrl,
                                  final Arguments arguments,
                                  final List<String> extraArguments ) throws CommandException {
        final Command command = getCommand( commandName );


        if ( command != null ) {
            command.init( in, out, buildUrl( hostPortOrUrl ), arguments, Collections.unmodifiableList(extraArguments) );
        } else if ( commandName != null ) {
            printErrorMessage("message.unknowncommand", commandName);
        }

        return command;
    }

    private boolean isCommand( final String commandName ) {
        return getCommand( commandName ) != null;        
    }

    private Command getCommand( final String commandName ) {
        Command command = null;

        if ( "enumerate".equals(commandName) ) {
            command = new EnumerateCommand();
        } else if ( "get".equals(commandName) ) {
            command = new GetCommand();
        } else if ( "put".equals(commandName) ) {
            command = new PutCommand();
        } else if ( "create".equals(commandName) ) {
            command = new CreateCommand();
        } else if ( "delete".equals(commandName) ) {
            command = new DeleteCommand();
        } else if ( "export".equals(commandName) ) {
            command = new ExportCommand();
        } else if ( "import".equals(commandName) ) {
            command = new ImportCommand();
        } else if ( "validate".equals(commandName) ) {
            command = new ValidateCommand();
        } else if ( "keypurposes".equals(commandName) ) {
            command = new SetKeySpecialPurposesCommand();
        } else if ( "generatecsr".equals(commandName) ) {
            command = new GenerateCsrCommand();
        } else if ( "createkey".equals(commandName) ) {
            command = new CreateKeyCommand();
        } else if ( "importkey".equals(commandName) ) {
            command = new ImportKeyCommand();
        } else if ( "exportkey".equals(commandName) ) {
            command = new ExportKeyCommand();
        }

        return command;
    }

    private String buildUrl( final String hostPortOrUrl ) throws CommandException {
        String url = null;

        if ( hostPortOrUrl != null ) {
            final String hostPortOrUrlLowerCase = hostPortOrUrl.toLowerCase();

            if ( hostPortOrUrlLowerCase.startsWith(HTTP_PREFIX) ||
                 hostPortOrUrlLowerCase.startsWith(HTTPS_PREFIX) ) {
                if ( !ValidationUtils.isValidUrl(hostPortOrUrl) ) {
                    throw new CommandException("Invalid URL '"+hostPortOrUrl+"'.");
                }
                url = hostPortOrUrl;
            } else {
                Pair<String, String> hostAndPort = InetAddressUtil.getHostAndPort(hostPortOrUrl, DEFAULT_HTTPS_PORT);
                if ( ! ValidationUtils.isValidDomain(hostAndPort.left) ||
                     ! ValidationUtils.isValidInteger(hostAndPort.right, false, 1, 65535)) {
                    throw new CommandException("Invalid host or port '" + hostPortOrUrl + "'.");
                }
                url = MessageFormat.format( URL_TEMPLATE, hostAndPort.left, hostAndPort.right );
            }
        }

        return url;
    }

    private void initProxy() throws CommandException {
        if ( arguments.proxyHost != null ) {
            if ( !ValidationUtils.isValidDomain(arguments.proxyHost) ) {
                throw new CommandException("Invalid HTTP proxy hostname '" + arguments.proxyHost + "'.");
            }
            System.setProperty( "http.proxyHost", arguments.proxyHost );
            System.setProperty( "https.proxyHost", arguments.proxyHost );
        }
        if ( arguments.proxyPort != null ) {
            if ( !ValidationUtils.isValidInteger(arguments.proxyPort, false, 1, 65535) ) {
                throw new CommandException("Invalid HTTP proxy port '" + arguments.proxyPort + "'.");
            }
            System.setProperty( "http.proxyPort", arguments.proxyPort );
            System.setProperty( "https.proxyPort", arguments.proxyPort );
        }
    }

    private static String getEncoding( final byte[] xml ) {
        return getEncoding( new StreamSource( new ByteArrayInputStream(xml) ) );
    }

    private static String getEncoding( final String xml ) {
        return getEncoding( new StreamSource( new StringReader(xml) ) );
    }

    private static String getEncoding( final Source source ) {
        String encoding = null;

        final XMLInputFactory xif = XMLInputFactory.newInstance();
        xif.setXMLReporter( SILENT_REPORTER );
        xif.setXMLResolver( FAILING_RESOLVER );
        XMLStreamReader reader = null;
        try {
            reader = xif.createXMLStreamReader( source );
            while( reader.hasNext() ) {
                final int eventType = reader.next();
                if ( eventType == XMLStreamReader.START_DOCUMENT ) {
                    encoding = reader.getCharacterEncodingScheme();
                    break;
                }
            }
        } catch ( XMLStreamException e ) {
            // use default encoding.
        } finally {
            ResourceUtils.closeQuietly( reader );
        }

        if ( encoding == null ) {
            encoding = "UTF-8";
        }

        return encoding;
    }

    private void handleCommandException( final CommandException ce ) {
        if ( ExceptionUtils.causedBy( ce, Accessor.AccessorNetworkException.class )) {
            printErrorMessage("message.networkerror", ExceptionUtils.getMessage( ce ));
        } else if ( ExceptionUtils.causedBy( ce, Accessor.AccessorNotFoundException.class )) {
            printErrorMessage("message.notfound");
        } else if ( ce.getCause() == null ) {
            printError( ExceptionUtils.getMessage( ce ) );
        } else {
            handleErrorException( ce );
        }
    }

    private void handleManagementException( final ManagementRuntimeException mre ) {
        if ( mre instanceof Accessor.AccessorNetworkException ) {
            printErrorMessage("message.networkerror", ExceptionUtils.getMessage( mre ));
        } else if ( mre instanceof Accessor.AccessorSOAPFaultException ) {
            Accessor.AccessorSOAPFaultException soapFault = (Accessor.AccessorSOAPFaultException) mre;
            if ( "The action is not supported by the service.".equals( soapFault.getFault() ) ) {
                printErrorMessage("message.commandnotsupported", this.arguments.type);
            } else if ( "The sender was not authorized to access the resource.".equals( soapFault.getFault() ) ) {
                printErrorMessage("message.accessdenied");
            } else {
                printErrorMessage("message.soapfault", soapFault.getFault(), soapFault.getRole(), soapFault.getDetails());
            }
        } else if ( ExceptionUtils.causedBy( mre, IOException.class )) {
            final IOException ioe = ExceptionUtils.getCauseIfCausedBy( mre, IOException.class );
            if ( ExceptionUtils.getMessage(ioe).equals( "Unauthorized" ) ) {
                printErrorMessage("message.notauthorized");
            } else if ( ioe instanceof SSLHandshakeException ) {
                printErrorMessage("message.servernottrusted");
            } else {
                printErrorMessage("message.ioerror", ExceptionUtils.getMessage( ioe ));
            }
        } else {
            handleErrorException( mre );
        }
    }

    private void handleErrorException( final Exception ce ) {
        printErrorMessage("message.unexpectederror", ExceptionUtils.getMessage( ce ));

        final StringBuilder sb = new StringBuilder();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss z");

        sb.append(MessageFormat.format(resources.getString("error.datetime"), dateFormat.format(new Date())));
        sb.append(MessageFormat.format(resources.getString("error.version"), getVersion()));
        sb.append(MessageFormat.format( resources.getString( "error.systemproperties" ),
                SyspropUtil.getProperty( "java.version" ),
                SyspropUtil.getProperty( "java.specification.version" ),
                SyspropUtil.getProperty( "os.name" ),
                SyspropUtil.getProperty( "os.arch" ) ));
        sb.append(MessageFormat.format(resources.getString("error.memoryusage"),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().totalMemory()));
        sb.append(MessageFormat.format(resources.getString("error.stacktrace"), ExceptionUtils.getStackTraceAsString( ce )));
        sb.append(resources.getString("error.helpcentre"));
        final String errorReport = sb.toString().replaceAll("\n", SyspropUtil.getString("line.separator", "\n"));

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String filename = "GatewayManagementClient_Error_Report_" + sdf.format(new Date()) + ".txt";
        FileWriter out = null;
        try {
            out = new FileWriter(filename);
            IOUtils.copyStream( new StringReader(errorReport), out);
            printErrorMessage( "message.errorreport", filename );
        } catch ( IOException e ) {
            printErrorMessage( "message.errorreporterror", filename, ExceptionUtils.getMessage(e) );
        } finally {
            ResourceUtils.closeQuietly( out );
        }
    }

    private static class CommandException extends Exception {
        CommandException( final String message ) {
            super( message );
        }

        CommandException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }

    private abstract static class Command {

        //- PUBLIC

        public void init( final InputStream defaultIn,
                          final OutputStream defaultOut,
                          final String url,
                          final Arguments arguments,
                          final List<String> extraArguments ) {
            this.defaultIn = defaultIn;
            this.defaultOut = defaultOut;
            this.url = url;
            this.arguments = arguments;
            this.extraArguments = extraArguments;
        }

        public final void run() throws CommandException {
            checkArgs();
            Client client = null;
            try {
                client = buildClient();
                doRun( client );
            } finally {
                ResourceUtils.closeQuietly( client );
            }
        }

        //- PROTECTED

        protected Command() {
            this( false, false );
        }

        protected Command( final boolean requireFileInput,
                           final boolean requireFileOutput ) {
            this.requireFileInput = requireFileInput;
            this.requireFileOutput = requireFileOutput;
        }

        protected abstract void doRun( Client client ) throws CommandException;

        @SuppressWarnings( { "unchecked" } )
        protected final Class<? extends AccessibleObject> getAccessibleObjectType() throws CommandException {
            Class<? extends AccessibleObject> typeClass = null;
            final String type = arguments.type;
            if ( type == null ) {
                throw new CommandException( "Invalid options: type is required.");
            }

            final Collection<Class<? extends AccessibleObject>> typeClasses = getAccessibleObjectTypes();
            for ( Class<? extends AccessibleObject> moClass : typeClasses ) {
                XmlRootElement element = moClass.getAnnotation( XmlRootElement.class );
                if ( element != null && element.name().equalsIgnoreCase( type ) ) {
                    typeClass = moClass;
                    break;
                }
            }

            if ( typeClass == null ) {
                throw new CommandException( "Invalid options: invalid type '" + arguments.type + "'.");
            }

            return typeClass;
        }

        protected <AT extends AccessibleObject> PolicyAccessor<AT> getPolicyAccessor( final Client client,
                                                                                      final Class<AT> objectType ) throws CommandException {
            return getAccessor( client, objectType, PolicyAccessor.class );
        }

        protected <AT extends AccessibleObject> PrivateKeyMOAccessor getPrivateKeyAccessor( final Client client,
                                                                                            final Class<AT> objectType ) throws CommandException {
            if ( !PrivateKeyMO.class.isAssignableFrom( objectType ) ) {
                throw new CommandException("Command not applicable for type '"+arguments.type+"'");
            }
            return getAccessor( client, PrivateKeyMO.class, PrivateKeyMOAccessor.class );
        }

        protected final Map<String,Object> getSelectors() throws CommandException {
            final Map<String,Object> selectors = new HashMap<String,Object>();

            if ( arguments.id != null && !arguments.id.trim().isEmpty() ) {
                selectors.put( "id", arguments.id.trim() );
            }

            if ( arguments.name != null && !arguments.name.trim().isEmpty() ) {
                selectors.put( "name", arguments.name.trim() );
            }

            if ( arguments.guid != null && !arguments.guid.trim().isEmpty() ) {
                selectors.put( "guid", arguments.guid.trim() );
            }

            if (arguments.genericEntityType != null && !arguments.genericEntityType.trim().isEmpty()) {
                selectors.put( "genericEntityType", arguments.genericEntityType.trim() );
            }

            if ( selectors.isEmpty() ) {
                throw new CommandException("Invalid options: name, id or guid is required.");
            }

            return selectors;
        }

        protected final String getId() throws CommandException {
            if ( arguments.id == null || arguments.id.trim().isEmpty() ) {
                throw new CommandException("Invalid options: id is required.");
            }
            return arguments.id.trim();
        }

        protected final boolean useForce() {
            return arguments.force;
        }

        protected final boolean isSafe() {
            return arguments.safe;
        }

        protected final List<String> getExtraArguments() {
            return extraArguments;
        }

        protected final String getArg( final String option, final List<String> arguments, final int index ) throws CommandException {
            if ( index >= arguments.size() ) {
                throw new CommandException("Invalid options: '-"+option+"' option missing parameters.");
            }
            return arguments.get( index );
        }

        protected final boolean hasInput() {
            final String inText = arguments.in;
            final File inFile = arguments.inFile;

            return inText != null || inFile != null;
        }

        protected final ManagedObject readInput() throws CommandException {
            final String inText = arguments.in;
            final File inFile = arguments.inFile;

            checkInput();
            try {
                if ( inText != null ) {
                    return ManagedObjectFactory.read( inText, ManagedObject.class );
                } else if ( inFile.getName().equals( "-" ) ) {
                    return ManagedObjectFactory.read( defaultIn, ManagedObject.class );
                } else {
                    InputStream in = null;
                    try {
                        in = new FileInputStream( inFile );
                        return ManagedObjectFactory.read( in, ManagedObject.class );
                    } finally {
                        ResourceUtils.closeQuietly( in );
                    }
                }
            } catch ( IOException ioe ) {
                handleIOError( "input", ioe );
                return null; // does not occur
            }
        }

        protected final String readInputAsText() throws CommandException {
            final String inText = arguments.in;
            final File inFile = arguments.inFile;

            checkInput();

            return getArgValue( inText, inFile, null, "input" );
        }

        protected final byte[] readInputAsBytes() throws CommandException {
            final File inFile = arguments.inFile;

            checkInput(true);
            try {
                InputStream in = null;
                try {
                    if ( inFile.getName().equals( "-" ) ) {
                        in = new FilterInputStream( defaultIn ){
                            @Override
                            public void close() throws IOException {
                            }
                        };
                    } else {
                        in = new FileInputStream( inFile );
                    }
                    byte[] data = new byte[ (int) inFile.length() ];
                    if ( in.read( data ) != data.length ) {
                        throw new IOException( "File read failed." );
                    }
                    return data;
                } finally {
                    ResourceUtils.closeQuietly( in );
                }
            } catch ( IOException ioe ) {
                handleIOError( "input", ioe );
                return null; // does not occur
            }
        }

        protected final void writeOutput( final ManagedObject mo ) throws CommandException {
            writeOutput( new OutputWriter(){
                @Override
                public void write( final OutputStream out ) throws IOException, CommandException {
                    ManagedObjectFactory.write( mo, out );
                }
            } );
        }

        protected final void writeOutput( final byte[] data ) throws CommandException {
            writeOutput( new OutputWriter(){
                @Override
                public void write( final OutputStream out ) throws IOException, CommandException {
                    out.write( data );
                }
            } );
        }

        protected final void writeOutput( final OutputWriter outputWriter ) throws CommandException {
            final File outFile = arguments.outFile;

            checkOutput();

            if ( outputWriter != null ) {
                OutputStream out = null;
                try {
                    if ( outFile != null && !outFile.getName().equals( "-" ) ) {
                        if ( isSafe() && outFile.exists() ) {
                            throw new CommandException( "Output file already exists, not overwriting : " + outFile.getAbsolutePath() );
                        }
                        out = new FileOutputStream( outFile );
                    } else {
                        out = new FilterOutputStream( defaultOut ){
                            @Override
                            public void close() throws IOException {
                                flush();
                            }
                        };
                    }
                    outputWriter.write( out );
                } catch ( IOException ioe ) {
                    handleIOError( "output", ioe );
                } finally {
                    ResourceUtils.closeQuietly( out );
                }
            }
        }

        @SuppressWarnings({ "unchecked" })
        protected final <T extends ManagedObject> T cast( final ManagedObject object, final Class<T> accessor ) throws CommandException {
            if ( accessor.isInstance( object )) {
                return (T) object;
            } else {
                throw new CommandException( "Unexpected type" );
            }
        }

        protected final void handleIOError( final String description, final IOException ioe ) throws CommandException {
            throw new CommandException( "Error processing "+description+" '"+ExceptionUtils.getMessage( ioe )+"'." );
        }

        protected interface OutputWriter {
            void write( OutputStream out ) throws IOException, CommandException;
        }

        //- PRIVATE

        private final boolean requireFileInput;
        private final boolean requireFileOutput;
        private InputStream defaultIn;
        private OutputStream defaultOut;
        private String url;
        private Arguments arguments;
        private List<String> extraArguments;

        private void checkArgs() throws CommandException {
            if ( requireFileInput ) checkInput( true );
            if ( requireFileOutput ) checkOutput();
        }

        private Client buildClient() throws CommandException {
            return buildClient(
                    url,
                    arguments.username,
                    getArgValue(arguments.password, arguments.passwordFile, "UTF-8", "-passwordFile"),
                    arguments.verifyHostname,
                    arguments.verifyCertificate,
                    arguments.proxyUsername,
                    getArgValue(arguments.proxyPassword, arguments.proxyPasswordFile, "UTF-8", "-proxyPasswordFile")
            );
        }

        private Client buildClient( final String url,
                                    final String username,
                                    final String password,
                                    final boolean verifyHostname,
                                    final boolean verifyCertificate,
                                    final String proxyUsername,
                                    final String proxyPassword ) {
            final ClientFactory clientFactory = ClientFactory.newInstance();

            try {
                clientFactory.setAttribute( ClientFactory.ATTRIBUTE_USERNAME, username );
                clientFactory.setAttribute( ClientFactory.ATTRIBUTE_PASSWORD, password );
                clientFactory.setFeature( ClientFactory.FEATURE_HOSTNAME_VALIDATION, verifyHostname );
                clientFactory.setFeature( ClientFactory.FEATURE_CERTIFICATE_VALIDATION, verifyCertificate );
                clientFactory.setAttribute( ClientFactory.ATTRIBUTE_PROXY_USERNAME, proxyUsername );
                clientFactory.setAttribute( ClientFactory.ATTRIBUTE_PROXY_PASSWORD, proxyPassword );
                clientFactory.setAttribute( ClientFactory.ATTRIBUTE_NET_CONNECT_TIMEOUT, 30000 );
                clientFactory.setAttribute( ClientFactory.ATTRIBUTE_NET_READ_TIMEOUT, 330000 );  // > server operation timeout
            } catch ( ClientFactory.InvalidOptionException ioe ) {
                throw ExceptionUtils.wrap( ioe );               
            }

            return clientFactory.createClient( url );
        }

        private <AO extends AccessibleObject, AT extends Accessor<AO>> AT getAccessor( final Client client,
                                                                                       final Class<AO> objectType,
                                                                                       final Class<AT> accessorType ) throws CommandException {
            try {
                return client.getAccessor( objectType, accessorType );
            } catch ( AccessorException ae ) {
                throw new CommandException("Command not applicable for type '"+arguments.type+"'");
            }
        }

        @SuppressWarnings( { "unchecked" } )
        private Collection<Class<? extends AccessibleObject>> getAccessibleObjectTypes() {
            final Collection<Class<? extends AccessibleObject>> typeClasses = new ArrayList<Class<? extends AccessibleObject>>();
            try {
                String packageResource = AccessibleObject.class.getPackage().getName().replace( '.', '/' );
                for ( URL url : ClassUtils.listResources( AccessibleObject.class, "jaxb.index" ) ) {
                    final String path = url.getPath();
                    final int index = path.indexOf( packageResource );
                    if ( index > 0 ) {
                        final String className = path.substring( index + packageResource.length() + 1 );
                        final Class<?> aoClass = Class.forName( AccessibleObject.class.getPackage().getName() + "." + className );
                        if ( AccessibleObject.class.isAssignableFrom( aoClass ) ) {
                            typeClasses.add( (Class<? extends AccessibleObject>) aoClass );
                        }
                    }
                }
            } catch ( IOException e ) {
                throw ExceptionUtils.wrap( e );
            } catch ( ClassNotFoundException e ) {
                throw ExceptionUtils.wrap( e );
            }
            return typeClasses;
        }

        private void checkInput() throws CommandException {
            checkInput( false );
        }

        private void checkInput( final boolean requireFileInput ) throws CommandException {
            final String inText = arguments.in;
            final File inFile = arguments.inFile;

            if ( requireFileInput ) {
                if ( inFile == null ) {
                    throw new CommandException("Invalid options: 'inFile' is required.");
                }
            } else {
                if ( inText != null && inFile != null ) {
                    throw new CommandException("Invalid options: only one of 'in' or 'inFile' should be used.");
                } else if ( inText == null && inFile == null ) {
                    throw new CommandException("Invalid options: either 'in' or 'inFile' is required.");
                }
            }
        }

        private void checkOutput() throws CommandException {
            if ( requireFileOutput ) {
                final File outFile = arguments.outFile;

                if ( outFile == null ) {
                    throw new CommandException( "Invalid options: 'outFile' is required." );
                }
            }
        }        

        private String getArgValue( final String argText,
                                    final File argFile,
                                    final String encoding,
                                    final String description ) throws CommandException {
            String value = null;

            if ( argText != null ) {
                value = argText;
            } else if ( argFile != null ) {
                try {
                    byte[] data = IOUtils.slurpFile( argFile );
                    String dataEncoding = encoding;
                    if ( dataEncoding == null ) {
                        dataEncoding = getEncoding(data);
                    }
                    value = new String( data, dataEncoding );
                } catch ( IOException ioe ) {
                    handleIOError( description, ioe );
                }
            }

            return value;
        }
    }

    private static final class EnumerateCommand extends Command {
        private static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
        private static final String ENUM_START = "<enumeration>\n";
        private static final String ENUM_EMPTY = "<enumeration/>\n";
        private static final String ENUM_END = "</enumeration>\n";

        @Override
        protected void doRun( final Client client ) throws CommandException {
            final Accessor<?> accessor = client.getAccessor( getAccessibleObjectType() );

            try {
                final Iterator<? extends ManagedObject> iterator = accessor.enumerate();
                writeOutput( new OutputWriter(){
                    @Override
                    public void write( final OutputStream out ) throws IOException, CommandException {
                        final StreamResult outResult = new StreamResult( out );
                        boolean first = true;
                        while ( iterator.hasNext() ) {
                            ManagedObject mo;
                            try {
                                mo = iterator.next();
                            } catch ( RuntimeException re ) {
                                endOutput( out, first, true );
                                throw re;
                            }
                            if ( first ) {
                                writeBytes( out, XML_DECL );
                                writeBytes( out, ENUM_START );
                                first = false;
                            }
                            MarshallingUtils.marshal( mo, outResult, true );
                        }
                        endOutput( out, first, false );
                    }
                } );
            } catch ( Accessor.AccessorException e ) {
                throw new CommandException( "Accessor error", e );
            }
        }

        private void endOutput( final OutputStream out,
                                final boolean first,
                                final boolean suppressError ) throws IOException {
            try {
                if ( first ) {
                    writeBytes( out, XML_DECL );
                    writeBytes( out, ENUM_EMPTY );
                } else {
                    writeBytes( out, ENUM_END );
                }
            } catch ( IOException ioe ) {
                if ( !suppressError ) throw ioe;
            }
        }

        private void writeBytes( final OutputStream out, final String data ) throws IOException {
            out.write( data.getBytes( Charsets.UTF8 ));
        }
    }

    private static final class GetCommand extends Command {
        @Override
        protected void doRun( final Client client )  throws CommandException {
            final Accessor<?> accessor = client.getAccessor( getAccessibleObjectType() );

            try {
                ManagedObject mo = accessor.get( getSelectors() );
                writeOutput( mo );
            } catch ( Accessor.AccessorException e ) {
                throw new CommandException( "Accessor error", e );
            }
        }
    }

    private static final class PutCommand extends Command {
        @Override
        protected void doRun( final Client client )  throws CommandException {
            doPut( client, getAccessibleObjectType() );
        }

        private <AO extends AccessibleObject> void doPut( final Client client,
                                                          final Class<AO> type ) throws CommandException {
            final Accessor<AO> accessor = client.getAccessor( type );

            try {
                AO ao = accessor.put( cast( readInput(), type ) );
                writeOutput( ao );
            } catch ( Accessor.AccessorException e ) {
                throw new CommandException( "Accessor error", e );
            }
        }
    }

    private static final class CreateCommand extends Command {
        @Override
        protected void doRun( final Client client )  throws CommandException {
            doCreate( client, getAccessibleObjectType() );
        }

        private <AO extends AccessibleObject> void doCreate( final Client client,
                                                             final Class<AO> type ) throws CommandException {
            final Accessor<AO> accessor = client.getAccessor( type );

            try {
                AO mo = cast( readInput(), type );
                String id = accessor.create( mo );
                mo.setId( id );
                mo.setVersion( null );
                writeOutput( mo );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class DeleteCommand extends Command {
        @Override
        protected void doRun( final Client client )  throws CommandException {
            final Accessor<?> accessor = client.getAccessor( getAccessibleObjectType() );

            try {
                accessor.delete( getSelectors() );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class ExportCommand extends Command {
        @Override
        protected void doRun( final Client client )  throws CommandException {
            final PolicyAccessor<?> accessor = getPolicyAccessor( client, getAccessibleObjectType() );

            try {
                final String policy = accessor.exportPolicy( getId() );
                writeOutput( new OutputWriter(){
                    @Override
                    public void write( final OutputStream out ) throws IOException, CommandException {
                        out.write( policy.getBytes( getEncoding(policy) ) );
                    }
                } );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class ImportCommand extends Command {
        @SuppressWarnings({ "AssignmentToForLoopParameter" })
        @Override
        protected void doRun( final Client client )  throws CommandException {
            final PolicyAccessor<?> policyAccessor = getPolicyAccessor( client, getAccessibleObjectType() );

            final List<PolicyReferenceInstruction> instructions = new ArrayList<PolicyReferenceInstruction>();

            // Build import instructions
            final List<String> instructionArguments = getExtraArguments();
            for ( int i=0; i<instructionArguments.size(); i++ ) {
                final String importOption = instructionArguments.get(i);
                if ( !"-import".equals(importOption) ) {
                    throw new CommandException("Invalid options: Unknown option '"+importOption+"'.");
                }

                final String importInstruction = getArg( instructionArguments, ++i );
                final String importType = getArg( instructionArguments, ++i );
                final String importId = getArg( instructionArguments, ++i );

                final PolicyReferenceInstruction instruction = ManagedObjectFactory.createPolicyReferenceInstruction();
                instruction.setReferenceType( asExportType(importType) );
                instruction.setReferenceId( importId );

                if ( "accept".equals( importInstruction )) {
                    instruction.setPolicyReferenceInstructionType( PolicyReferenceInstruction.PolicyReferenceInstructionType.IGNORE );
                    instructions.add( instruction );
                } else if ( "remove".equals( importInstruction )) {
                    instruction.setPolicyReferenceInstructionType( PolicyReferenceInstruction.PolicyReferenceInstructionType.DELETE );
                    instructions.add( instruction );
                } else if ( "rename".equals( importInstruction )) {
                    final String importName = getArg( instructionArguments, ++i );
                    instruction.setPolicyReferenceInstructionType( PolicyReferenceInstruction.PolicyReferenceInstructionType.RENAME );
                    instruction.setMappedName( importName );
                    instructions.add( instruction );
                } else if ( "replace".equals( importInstruction )) {
                    final String importReplaceId = getArg( instructionArguments, ++i );
                    instruction.setPolicyReferenceInstructionType( PolicyReferenceInstruction.PolicyReferenceInstructionType.MAP );
                    instruction.setMappedReferenceId( importReplaceId );
                    instructions.add( instruction );
                } else {
                    throw new CommandException("Invalid options: Unknown import option '"+importInstruction+"'.");
                }
            }

            try {
                final Map<String,Object> properties = new HashMap<String,Object>();
                if ( useForce() ) {
                    properties.put( "force", true );
                }

                final PolicyImportResult result = policyAccessor.importPolicy( getId(), properties, readInputAsText(), instructions );
                writeOutput( result );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }

        /**
         * Allow reference types to be specified without the qualifying package.
         */
        private String asExportType( final String type ) {
            String exportType = type;

            if ( !exportType.contains("." ) ) {
                exportType = "com.l7tech.console.policy.exporter." + exportType;
            } else if ( exportType.startsWith( "." )) {
                // just in case someone later adds references in the default package   
                exportType = exportType.substring(1);
            }

            return exportType;
        }

        private String getArg( final List<String> arguments, final int index ) throws CommandException {
            return getArg( "import", arguments, index );
        }
    }

    private static final class ValidateCommand extends Command {
        @Override
        protected void doRun( final Client client )  throws CommandException {
            doValidate( client, getAccessibleObjectType() );
        }

        private <AO extends AccessibleObject> void doValidate( final Client client,
                                                               final Class<AO> type ) throws CommandException {
            final PolicyAccessor<AO> accessor = getPolicyAccessor( client, type );

            try {
                final PolicyValidationResult result = hasInput() ?
                        accessor.validatePolicy( cast(readInput(), accessor.getType()), null ) :
                        accessor.validatePolicy( getId() );
                writeOutput( result );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class SetKeySpecialPurposesCommand extends Command {
        @SuppressWarnings({ "AssignmentToForLoopParameter" })
        @Override
        protected void doRun( final Client client ) throws CommandException {
            final PrivateKeyMOAccessor accessor = getPrivateKeyAccessor( client, getAccessibleObjectType() );

            // Get extra args
            final Set<String> specialPurposes = new HashSet<String>();
            final List<String> instructionArguments = getExtraArguments();
            for ( int i=0; i<instructionArguments.size(); i++ ) {
                final String option = instructionArguments.get(i);

                if ( "-keyPurpose".equals(option) ) {
                    specialPurposes.add( getArg( "keyPurpose", instructionArguments, ++i ) );
                } else {
                    throw new CommandException("Invalid options: Unknown option '"+option+"'.");
                }
            }

            try {
                final PrivateKeyMO result = accessor.setSpecialPurposes( getId(), specialPurposes );
                writeOutput( result );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class GenerateCsrCommand extends Command {
        private GenerateCsrCommand() {
            super( false, true );
        }

        @SuppressWarnings({ "AssignmentToForLoopParameter" })
        @Override
        protected void doRun( final Client client ) throws CommandException {
            final PrivateKeyMOAccessor accessor = getPrivateKeyAccessor( client, getAccessibleObjectType() );

            // Get extra args
            String dn = null;
            final List<String> instructionArguments = getExtraArguments();
            for ( int i=0; i<instructionArguments.size(); i++ ) {
                final String option = instructionArguments.get(i);

                if ( "-csrDn".equals(option) ) {
                    dn = getArg( "csrDn", instructionArguments, ++i );
                } else {
                    throw new CommandException("Invalid options: Unknown option '"+option+"'.");
                }
            }

            try {
                final byte[] csrBytes = accessor.generateCsr( getId(), dn );
                writeOutput( csrBytes );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class CreateKeyCommand extends Command {
        @Override
        protected void doRun( final Client client ) throws CommandException {
            final PrivateKeyMOAccessor accessor = getPrivateKeyAccessor( client, getAccessibleObjectType() );

            final PrivateKeyCreationContext context = cast(readInput(), PrivateKeyCreationContext.class);
            context.setId( getId() );
            try {
                final PrivateKeyMO result = accessor.createKey( context );
                writeOutput( result );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class ExportKeyCommand extends Command {
        private ExportKeyCommand() {
            super( false, true );
        }

        @SuppressWarnings({ "AssignmentToForLoopParameter" })
        @Override
        protected void doRun( final Client client ) throws CommandException {
            final PrivateKeyMOAccessor accessor = getPrivateKeyAccessor( client, getAccessibleObjectType() );

            // Get extra args
            String alias = null;
            String password = null;
            final List<String> instructionArguments = getExtraArguments();
            for ( int i=0; i<instructionArguments.size(); i++ ) {
                final String option = instructionArguments.get(i);

                if ( "-keyAlias".equals(option) ) {
                    alias = getArg( "keyAlias", instructionArguments, ++i );
                } else if ( "-keyPassword".equals(option) ) {
                    password = getArg( "keyPassword", instructionArguments, ++i );
                } else {
                    throw new CommandException("Invalid options: Unknown option '"+option+"'.");
                }
            }

            try {
                final byte[] keystoreBytes = accessor.exportKey( getId(), alias, password );
                writeOutput( keystoreBytes );
            } catch ( Accessor.AccessorException ioe ) {
                    throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class ImportKeyCommand extends Command {
        private ImportKeyCommand() {
            super( true, false );
        }

        @SuppressWarnings({ "AssignmentToForLoopParameter" })
        @Override
        protected void doRun( final Client client ) throws CommandException {
            final PrivateKeyMOAccessor accessor = getPrivateKeyAccessor( client, getAccessibleObjectType() );

            // Get extra args
            String alias = null;
            String password = null;
            final List<String> instructionArguments = getExtraArguments();
            for ( int i=0; i<instructionArguments.size(); i++ ) {
                final String option = instructionArguments.get(i);

                if ( "-keyAlias".equals(option) ) {
                    alias = getArg( "keyAlias", instructionArguments, ++i );
                } else if ( "-keyPassword".equals(option) ) {
                    password = getArg( "keyPassword", instructionArguments, ++i );
                } else {
                    throw new CommandException("Invalid options: Unknown option '"+option+"'.");
                }
            }

            try {
                final byte[] keystoreBytes = readInputAsBytes();
                final PrivateKeyMO result = accessor.importKey( getId(), alias, password, keystoreBytes );
                writeOutput( result );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class Arguments {
        @Cli.Arg(name="-id", description="The identifier for the target item", required=false)
        private String id;

        @Cli.Arg(name="-name", description="The name for the target item", required=false)
        private String name;

        @Cli.Arg(name="-guid", description="The guid for the target item", required=false)
        private String guid;

        @Cli.Arg(name="-genericEntityType", description="The type of the generic entity", required=false)
        private String genericEntityType;

        @Cli.Arg(name="-in", description="The input item", required=false)
        private String in;

        @Cli.Arg(name="-inFile", description="The input file", required=false)
        private File inFile;

        @Cli.Arg(name="-outFile", description="The output file", required=false)
        private File outFile;

        @Cli.Arg(name="-type", description="The type for the target item", required=false)
        private String type;

        @Cli.Arg(name="-help", description="Show help for the command or global help if no\ncommand is specified", value="true")
        private boolean help = false;

        @Cli.Arg(name="-safe", description="Do not overwrite files", value="true")
        private boolean safe = false;

        @Cli.Arg(name="-password", description="The password to use", required=false)
        private String password;

        @Cli.Arg(name="-passwordFile", description="The file to read the password from", required=false)
        private File passwordFile;

        @Cli.Arg(name="-skipVerifyHostname", description="Disable hostname verification", value="false")
        private boolean verifyHostname = true;

        @Cli.Arg(name="-skipVerifyCertificate", description="Disable server certificate validation", value="false")
        private boolean verifyCertificate = true;

        @Cli.Arg(name="-force", description="Skip version and conflict checks", value="true")
        private boolean force = false;

        @Cli.Arg(name="-username", description="The username to use", required=false)
        private String username;

        @Cli.Arg(name="-version", description="Display version information", value="true")
        private boolean version = false;

        @Cli.Arg(name="-proxyHost", description="HTTP proxy hostname", required=false)
        private String proxyHost;

        @Cli.Arg(name="-proxyPort", description="HTTP proxy port", required=false)
        private String proxyPort;

        @Cli.Arg(name="-proxyUsername", description="HTTP proxy username", required=false)
        private String proxyUsername;

        @Cli.Arg(name="-proxyPassword", description="HTTP proxy password", required=false)
        private String proxyPassword;

        @Cli.Arg(name="-proxyPasswordFile", description="The file to read the HTTP proxy password from", required=false)
        private File proxyPasswordFile;
    }
}

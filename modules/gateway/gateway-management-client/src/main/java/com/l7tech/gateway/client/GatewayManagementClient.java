package com.l7tech.gateway.client;

import com.l7tech.gateway.api.Accessor;
import com.l7tech.gateway.api.Client;
import com.l7tech.gateway.api.ClientFactory;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ManagementRuntimeException;
import com.l7tech.gateway.api.PolicyAccessor;
import com.l7tech.gateway.api.PolicyImportResult;
import com.l7tech.gateway.api.PolicyValidationResult;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ClassUtils;
import com.l7tech.util.Cli;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ValidationUtils;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;

/**
 * Client utility for the SecureSpan Gateway management service.
 */
public class GatewayManagementClient {

    //- PUBLIC

    public static void main( final String[] args ) {
        JdkLoggerConfigurator.configure("com.l7tech.gateway.client", "com/l7tech/gateway/client/logging.properties", "logging.properties", true, true);
        int exitCode = 1;
        try {
            GatewayManagementClient gmc = new GatewayManagementClient(args);
            exitCode = gmc.run();
        } catch ( Exception e ) {
            handleErrorException( e );
        }
        System.err.flush();
        System.exit( exitCode );
    }

    public GatewayManagementClient( final String args[] )  {
        final Arguments arguments = new Arguments();
        final Map<Integer,String> extraArgs = new TreeMap<Integer,String>();

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
                System.err.println( ce.getMessage() );
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
                    System.out.println( tabReplace(MessageFormat.format( help, types, common )) );
                    exitCode = 0;
                } catch ( MissingResourceException mre ) {
                    System.out.println("Help not available for command '"+commandName+"'.");
                }
            }
        } else if ( arguments.version ) {
            printInfo( "message.version", getVersion() );
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

    private final String[] commandLineArguments;
    private final Arguments arguments;
    private final Command command;
    private final String error;

    private void usage() throws CommandException {
        printInfo( "usage" );
        try {
            Cli.usage( new Arguments(), System.out );
        } catch ( IOException ioe ) {
            throw new CommandException("Error writing usage", ioe);
        }
    }

    private static String getVersion() {
        return BuildInfo.getProductVersion() + " (" + BuildInfo.getBuildNumber() + ")";
    }

    private static void printError( final String messageKey, final Object... parameters ) {
        System.err.println( tabReplace(MessageFormat.format( resources.getString(messageKey), parameters )) );
    }

    private static void printInfo( final String messageKey, final Object... parameters ) {
        System.out.println( tabReplace(MessageFormat.format( resources.getString(messageKey), parameters )) );
    }

    private static String tabReplace( final String text ) {
        return text.replaceAll("\t", "    ");        
    }

    private Command buildCommand( final String commandName,
                                  final String hostPortOrUrl,
                                  final Arguments arguments,
                                  final List<String> extraArguments ) throws CommandException {
        final Command command = getCommand( commandName );


        if ( command != null ) {
            command.init( buildUrl( hostPortOrUrl ), arguments, Collections.unmodifiableList(extraArguments) );
        } else if ( commandName != null ) {
            usage();
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
                final String[] hostAndPort = hostPortOrUrl.split(":");
                String host = null;
                String port = null;

                if ( hostAndPort.length == 1 ) {
                    if ( ValidationUtils.isValidDomain(hostAndPort[0]) ) {
                        host = hostAndPort[0];
                        port = DEFAULT_HTTPS_PORT;
                    }
                } else if ( hostAndPort.length == 2 ) {
                    if ( ValidationUtils.isValidDomain(hostAndPort[0]) &&
                         ValidationUtils.isValidInteger(hostAndPort[1], false, 1, 65535) ) {
                        host = hostAndPort[0];
                        port = hostAndPort[1];
                    }
                }

                if ( host == null ) {
                    throw new CommandException("Invalid host or port '" + hostPortOrUrl + "'.");
                }

                url = MessageFormat.format( URL_TEMPLATE, host, port );
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
            printError("message.networkerror", ExceptionUtils.getMessage( ce ));
        } else if ( ExceptionUtils.causedBy( ce, Accessor.AccessorNotFoundException.class )) {
            printError("message.notfound");
        } else if ( ce.getCause() == null ) {
            printError( ExceptionUtils.getMessage( ce ) );
        } else {
            handleErrorException( ce );   
        }
    }

    private void handleManagementException( final ManagementRuntimeException mre ) {
        if ( mre instanceof Accessor.AccessorNetworkException ) {
            printError("message.networkerror", ExceptionUtils.getMessage( mre ));
        } else if ( mre instanceof Accessor.AccessorSOAPFaultException ) {
            Accessor.AccessorSOAPFaultException soapFault = (Accessor.AccessorSOAPFaultException) mre;
            printError("message.soapfault", soapFault.getFault(), soapFault.getRole(), soapFault.getDetails());
        } else if ( ExceptionUtils.causedBy( mre, IOException.class )) {
            final IOException ioe = ExceptionUtils.getCauseIfCausedBy( mre, IOException.class );
            if ( ExceptionUtils.getMessage(ioe).equals( "Unauthorized" ) ) {
                printError("message.notauthorized");
            } else if ( ioe instanceof SSLHandshakeException ) {
                printError("message.servernottrusted");
            } else {
                printError("message.ioerror", ExceptionUtils.getMessage( ioe ));
            }
        } else {
            handleErrorException( mre );   
        }
    }

    private static void handleErrorException( final Exception ce ) {
        printError("message.unexpectederror", ExceptionUtils.getMessage( ce ));

        final StringBuilder sb = new StringBuilder();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss z");

        sb.append(MessageFormat.format(resources.getString("error.datetime"), dateFormat.format(new Date())));
        sb.append(MessageFormat.format(resources.getString("error.version"), getVersion()));
        sb.append(MessageFormat.format(resources.getString("error.systemproperties"),
                System.getProperty("java.version"),
                System.getProperty("java.specification.version"),
                System.getProperty("os.name"),
                System.getProperty("os.arch")));
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
            printError( "message.errorreport", filename );
        } catch ( IOException e ) {
            printError( "message.errorreporterror", filename, ExceptionUtils.getMessage(e) );
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

        public void init( final String url, final Arguments arguments, final List<String> extraArguments ) {
            this.url = url;
            this.arguments = arguments;
            this.extraArguments = extraArguments;
        }

        abstract public void run() throws CommandException;

        //- PROTECTED

        protected final Client buildClient() throws CommandException {
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

        @SuppressWarnings( { "unchecked" } )
        protected final Class<? extends ManagedObject> getManagedObjectType() throws CommandException {
            Class<? extends ManagedObject> typeClass = null;
            final String type = arguments.type;
            if ( type == null ) {
                throw new CommandException( "Invalid options: type is required.");
            }

            final Collection<Class<? extends ManagedObject>> typeClasses = getManagedObjectTypes();
            for ( Class<? extends ManagedObject> moClass : typeClasses ) {
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

        protected final Map<String,Object> getSelectors() throws CommandException {
            final Map<String,Object> selectors = new HashMap<String,Object>();

            if ( arguments.id != null && !arguments.id.trim().isEmpty() ) {
                selectors.put( "id", arguments.id.trim() );
            }

            if ( arguments.name != null && !arguments.name.trim().isEmpty() ) {
                selectors.put( "name", arguments.name.trim() );
            }

            if ( selectors.isEmpty() ) {
                throw new CommandException("Invalid options: name or id is required.");
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

        protected final List<String> getExtraArguments() {
            return extraArguments;
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

        protected final void writeOutput( final ManagedObject mo ) throws CommandException {
            writeOutput( new Functions.UnaryThrows<Void, OutputStream, CommandException>(){
                @Override
                public Void call( final OutputStream out ) throws CommandException {
                    try {
                        ManagedObjectFactory.write( mo, out );
                    } catch ( IOException ioe ) {
                        handleIOError( "output", ioe );
                    }
                    return null;
                }
            } );
        }

        protected final void writeOutput( final Functions.UnaryThrows<Void, OutputStream, CommandException> callback ) throws CommandException {
            final File outFile = arguments.outFile;

            if ( callback != null ) {
                OutputStream out = null;
                try {
                    if ( outFile != null ) {
                        out = new FileOutputStream( outFile );
                    } else {
                        out = new FilterOutputStream( System.out ){
                            @Override
                            public void close() throws IOException {
                                flush();
                            }
                        };
                    }
                    callback.call( out );
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

        //- PRIVATE

        private String url;
        private Arguments arguments;
        private List<String> extraArguments;

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
            } catch ( ClientFactory.InvalidOptionException ioe ) {
                throw ExceptionUtils.wrap( ioe );               
            }

            return clientFactory.createClient( url );
        }

        @SuppressWarnings( { "unchecked" } )
        private Collection<Class<? extends ManagedObject>> getManagedObjectTypes() {
            final Collection<Class<? extends ManagedObject>> typeClasses = new ArrayList<Class<? extends ManagedObject>>();
            try {
                String packageResource = ManagedObject.class.getPackage().getName().replace( '.', '/' );
                for ( URL url : ClassUtils.listResources( ManagedObject.class, "jaxb.index" ) ) {
                    final String path = url.getPath();
                    final int index = path.indexOf( packageResource );
                    if ( index > 0 ) {
                        final String className = path.substring( index + packageResource.length() + 1 );
                        final Class<?> moClass = Class.forName( ManagedObject.class.getPackage().getName() + "." + className );
                        if ( ManagedObject.class.isAssignableFrom( moClass ) ) {
                            typeClasses.add( (Class<? extends ManagedObject>) moClass );
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
            final String inText = arguments.in;
            final File inFile = arguments.inFile;

            if ( inText != null && inFile != null ) {
                throw new CommandException("Invalid options: only one of 'in' or 'inFile' should be used.");
            } else if ( inText == null && inFile == null ) {
                throw new CommandException("Invalid options: either 'in' or 'inFile' is required.");
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
        @Override
        public void run() throws CommandException {
            final Client client = buildClient();
            final Accessor<?> accessor = client.getAccessor( getManagedObjectType() );

            try {
                final Iterator<? extends ManagedObject> iterator = accessor.enumerate();
                writeOutput( new Functions.UnaryThrows<Void, OutputStream, CommandException>(){
                    @Override
                    public Void call( final OutputStream out ) throws CommandException {
                        try {
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
                                    out.write( "<enumeration>".getBytes( "UTF-8" ));
                                    first = false;
                                }
                                ManagedObjectFactory.write( mo, out );
                            }
                            endOutput( out, first, false );
                        } catch ( IOException ioe ) {
                            handleIOError( "output", ioe );
                        }
                        return null;
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
                    out.write( "<enumeration/>".getBytes( "UTF-8" ));
                } else {
                    out.write( "</enumeration>".getBytes( "UTF-8" ));
                }
            } catch ( IOException ioe ) {
                if ( !suppressError ) throw ioe;
            }
        }
    }

    private static final class GetCommand extends Command {
        @Override
        public void run() throws CommandException {
            final Client client = buildClient();
            final Accessor<?> accessor = client.getAccessor( getManagedObjectType() );

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
        public void run() throws CommandException {
            doPut( getManagedObjectType() );
        }

        private <MO extends ManagedObject> void doPut( final Class<MO> type ) throws CommandException {
            final Client client = buildClient();
            final Accessor<MO> accessor = client.getAccessor( type );

            try {
                accessor.put( cast( readInput(), type) );
            } catch ( Accessor.AccessorException e ) {
                throw new CommandException( "Accessor error", e );
            }
        }
    }

    private static final class CreateCommand extends Command {
        @Override
        public void run() throws CommandException {
            doCreate( getManagedObjectType() );
        }

        private <MO extends ManagedObject> void doCreate( final Class<MO> type ) throws CommandException {
            final Client client = buildClient();
            final Accessor<MO> accessor = client.getAccessor( type );

            try {
                MO mo = cast( readInput(), type );
                String id = accessor.create( mo );
                mo.setId( id );
                writeOutput( mo );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class DeleteCommand extends Command {
        @Override
        public void run() throws CommandException {
            final Client client = buildClient();
            final Accessor<?> accessor = client.getAccessor( getManagedObjectType() );

            try {
                accessor.delete( getSelectors() );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class ExportCommand extends Command {
        @Override
        public void run() throws CommandException {
            final Client client = buildClient();
            final PolicyAccessor<?> accessor = (PolicyAccessor<?>) client.getAccessor( getManagedObjectType() );

            try {
                final String policy = accessor.exportPolicy( getId() );
                writeOutput( new Functions.UnaryThrows<Void, OutputStream, CommandException>(){
                    @Override
                    public Void call( final OutputStream out ) throws CommandException {
                        try {
                            out.write( policy.getBytes( getEncoding(policy) ) );
                        } catch ( IOException ioe ) {
                            handleIOError( "output", ioe );
                        }
                        return null;
                    }
                } );
            } catch ( Accessor.AccessorException ioe ) {
                throw new CommandException( "Accessor error", ioe );
            }
        }
    }

    private static final class ImportCommand extends Command {
        @Override
        public void run() throws CommandException {
            final Client client = buildClient();
            final PolicyAccessor<?> accessor = (PolicyAccessor<?>) client.getAccessor( getManagedObjectType() );

            final List<PolicyAccessor.PolicyReferenceInstruction> instructions = new ArrayList<PolicyAccessor.PolicyReferenceInstruction>();

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

                final PolicyAccessor.PolicyReferenceInstruction instruction = ManagedObjectFactory.createPolicyReferenceInstruction();
                instruction.setReferenceType( asExportType(importType) );
                instruction.setReferenceId( importId );

                if ( "accept".equals( importInstruction )) {
                    instruction.setPolicyReferenceInstructionType( PolicyAccessor.PolicyReferenceInstructionType.IGNORE );
                } else if ( "remove".equals( importInstruction )) {
                    instruction.setPolicyReferenceInstructionType( PolicyAccessor.PolicyReferenceInstructionType.DELETE );
                } else if ( "rename".equals( importInstruction )) {
                    final String importName = getArg( instructionArguments, ++i );
                    instruction.setPolicyReferenceInstructionType( PolicyAccessor.PolicyReferenceInstructionType.RENAME );
                    instruction.setMappedName( importName );
                    instructions.add( instruction );
                } else if ( "replace".equals( importInstruction )) {
                    final String importReplaceId = getArg( instructionArguments, ++i );
                    instruction.setPolicyReferenceInstructionType( PolicyAccessor.PolicyReferenceInstructionType.MAP );
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

                final PolicyImportResult result = accessor.importPolicy( getId(), properties, readInputAsText(), instructions );
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
            if ( index >= arguments.size() ) {
                throw new CommandException("Invalid options: '-import' option missing parameters.");
            }
            return arguments.get( index );
        }
    }

    private static final class ValidateCommand extends Command {
        @Override
        public void run() throws CommandException {
            doValidate( getManagedObjectType() );
        }

        private <MO extends ManagedObject> void doValidate( final Class<MO> type ) throws CommandException {
            final Client client = buildClient();
            final PolicyAccessor<MO> accessor = (PolicyAccessor<MO>) client.getAccessor( type );

            try {
                PolicyValidationResult result = hasInput() ?
                        accessor.validatePolicy( cast(readInput(), accessor.getType()), null ) :
                        accessor.validatePolicy( getId() );
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

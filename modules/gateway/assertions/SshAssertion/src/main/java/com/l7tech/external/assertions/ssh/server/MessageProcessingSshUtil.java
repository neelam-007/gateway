package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.*;
import com.l7tech.message.SshKnob.FileMetadata;
import com.l7tech.message.SshKnob.PublicKeyAuthentication;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import org.apache.sshd.server.session.ServerSession;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.mime.ContentTypeHeader.XML_DEFAULT;
import static com.l7tech.common.mime.ContentTypeHeader.parseValue;
import static com.l7tech.external.assertions.ssh.server.SshServerModule.*;
import static com.l7tech.gateway.common.transport.SsgConnector.PROP_OVERRIDE_CONTENT_TYPE;
import static com.l7tech.gateway.common.transport.SsgConnector.PROP_REQUEST_SIZE_LIMIT;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.server.message.PolicyEnforcementContextFactory.createPolicyEnforcementContext;
import static com.l7tech.util.TextUtils.isNotEmpty;

/**
 * Utility methods for SSH support.
 */
public class MessageProcessingSshUtil {

    /**
     * Read any remaining input after policy evaluation to ensure input can close without error.
     * @param inputStream The input to read
     * @param logger Log any IOException
     */
    public static void prepareInputStreamForClosing(InputStream inputStream, Logger logger) {
        try {
            IOUtils.copyStream(inputStream, new NullOutputStream());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception while preparing to close input stream.  " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Build a policy execution context for processing a message with an SSH knob
     *
     * @param connector The connector in use
     * @param session The current SSH session
     * @param stashManagerFactory The stash manager to use for messages
     * @param messageInputStream The stream for reading message content
     * @param file The file being transfered
     * @param path The path for the file transfer
     * @return The PolicyEnforcementContext
     * @throws IOException If the content type for the connector is invalid or there is an error reading the message
     */
    public static PolicyEnforcementContext buildPolicyExecutionContext( final SsgConnector connector,
                                                                        final ServerSession session,
                                                                        final StashManagerFactory stashManagerFactory,
                                                                        final InputStream messageInputStream,
                                                                        final String file,
                                                                        final String path,
                                                                        final FileMetadata fileMetadata) throws IOException {
        final Message request = new Message();
        final PolicyEnforcementContext context = createPolicyEnforcementContext( request, null );
        final long requestSizeLimit = connector.getLongProperty( PROP_REQUEST_SIZE_LIMIT, getMaxBytes());
        final String ctypeStr = connector.getProperty( PROP_OVERRIDE_CONTENT_TYPE);
        final ContentTypeHeader ctype = ctypeStr == null ? XML_DEFAULT : parseValue( ctypeStr );

        request.initialize(
                stashManagerFactory.createStashManager(),
                ctype,
                messageInputStream,
                requestSizeLimit);

        // attach ssh knob
        final PublicKeyAuthentication publicKeyAuthentication;
        final PasswordAuthentication passwordAuthentication;
        final Option<String> userName = session.getAttribute( MINA_SESSION_ATTR_CRED_USERNAME);
        final Option<String> userPublicKey = session.getAttribute( MINA_SESSION_ATTR_CRED_PUBLIC_KEY );
        final Option<String> userPassword = session.getAttribute( MINA_SESSION_ATTR_CRED_PASSWORD );
        if ( userName.exists(isNotEmpty()) && userPublicKey.isSome() ) {
            publicKeyAuthentication = new PublicKeyAuthentication( userName.some(), userPublicKey.some() );
            passwordAuthentication = null;
        } else if ( userName.exists(isNotEmpty()) && userPassword.exists(isNotEmpty()) ) {
            publicKeyAuthentication = null;
            passwordAuthentication = new PasswordAuthentication(userName.some(), userPassword.some().toCharArray());
        } else {
            publicKeyAuthentication = null;
            passwordAuthentication = null;
        }

        final SshKnob knob = buildSshKnob(
                        session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(),
                        file,
                        path,
                        publicKeyAuthentication,
                        passwordAuthentication, fileMetadata );
        request.attachKnob( knob, SshKnob.class, UriKnob.class, TcpKnob.class );

        final long hardwiredServiceOid = connector.getLongProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, -1L);
        if (hardwiredServiceOid != -1L) {
            request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
        }

        return context;
    }

    /*
     * Create an SshKnob for SCP and SFTP.
     */
    static SshKnob buildSshKnob( final SocketAddress localSocketAddress,
                                 final SocketAddress remoteSocketAddress,
                                 final String file,
                                 final String path,
                                 final PublicKeyAuthentication publicKeyCredential,
                                 final PasswordAuthentication passwordCredential,
                                 @Nullable final FileMetadata fileMetadata) {

        // SocketAddress requires us to parse for host and port (e.g. /127.0.0.1:22)
        final Pair<String,String> localHostPortPair = getHostAndPort(localSocketAddress.toString());
        final Pair<String,String> remoteHostPortPair = getHostAndPort(remoteSocketAddress.toString());

        final String localHostFinal = localHostPortPair.getKey();
        final int localPortFinal = Integer.parseInt(localHostPortPair.getValue());
        final String remoteHostFinal = remoteHostPortPair.getKey();
        final int remotePortFinal = Integer.parseInt(remoteHostPortPair.getValue());

        return buildSshKnob(localHostFinal, localPortFinal, remoteHostFinal, remotePortFinal, file, path,
                publicKeyCredential, passwordCredential, fileMetadata);
    }

    /*
     * Create an SshKnob for SCP and SFTP.
     */
    public static SshKnob buildSshKnob( @Nullable final String localHost,
                                        final int localPort,
                                        @Nullable final String remoteHost,
                                        final int remotePort,
                                        final String file,
                                        final String path,
                                        @Nullable final PublicKeyAuthentication publicKeyCredential,
                                        @Nullable final PasswordAuthentication passwordCredential,
                                        @Nullable final FileMetadata fileMetadata) {

        return new SshKnob(){
            @Override
            public String getLocalAddress() {
                return localHost;
            }
            @Override
            public String getLocalHost() {
                return localHost;
            }
            @Override
            public int getLocalPort() {
                return getLocalListenerPort();
            }
            @Override
            public int getLocalListenerPort() {
                return localPort;
            }
            @Override
            public String getRemoteAddress() {
                return remoteHost;
            }
            @Override
            public String getRemoteHost() {
                return remoteHost;
            }
            @Override
            public int getRemotePort() {
                return remotePort;
            }
            @Override
            public String getFile() {
                return file;
            }
            @Override
            public String getPath() {
                return path;
            }
            @Override
            public String getRequestUri() {
                return path;
            }
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return passwordCredential;
            }
            @Override
            public PublicKeyAuthentication getPublicKeyAuthentication() {
                return publicKeyCredential;
            }

            @Override
            public FileMetadata getFileMetadata() {
                return fileMetadata;
            }
        };
    }

    /**
     * Get the remote IP address for the given session (the client IP)
     *
     * @param session The server session (required)
     * @return The address or an empty string if not available (never null)
     */
    static String getRemoteAddress( final ServerSession session ) {
        String address = "";
        final SocketAddress socketAddress = session.getIoSession().getRemoteAddress();

        if ( socketAddress instanceof InetSocketAddress ) {
            final InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            if ( !inetSocketAddress.isUnresolved() ) {
                address = inetSocketAddress.getAddress().getHostAddress();
            }
        }

        return address;
    }

    /**
     * Parses the host and port from a "host[:port]" string.
     *
     * Similar to InetAddressUtil.getHostAndPort(String hostAndPossiblyPort, String defaultPort),
     * but does not return unwanted square bracket around the host name (e.g. [hostname]) like InetAddressUtil
     *
     * @param hostAndPossiblyPort string containing a host and optionally a port (delimited from the host part with ":")
     * @return the host and port determined as described above
     */
    private static Pair<String,String> getHostAndPort(String hostAndPossiblyPort) {
        boolean startsWithForwardSlash = (int) hostAndPossiblyPort.charAt( 0 ) == (int) '/';
        int colonIndex = hostAndPossiblyPort.indexOf(':');
        String host;
        if (startsWithForwardSlash && colonIndex > 1) {
            host = hostAndPossiblyPort.substring(1, colonIndex);
        } else if (startsWithForwardSlash) {
            host = hostAndPossiblyPort.substring(1);
        } else if (colonIndex > -1) {
            host = hostAndPossiblyPort.substring(0, colonIndex);
        } else {
            host = hostAndPossiblyPort;
        }
        String port = null;
        if (colonIndex > -1) {
            port = hostAndPossiblyPort.substring(colonIndex + 1);
        }

        return new Pair<String, String>(host, port);
    }
}

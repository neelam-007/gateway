package com.l7tech.external.assertions.ssh.server;

import com.l7tech.message.SshKnob;
import com.l7tech.util.Pair;

import java.net.PasswordAuthentication;
import java.net.SocketAddress;

/**
 * Utility methods for SSH support.
 */
public class MessageProcessingSshUtil {

    /*
     * Create an SshKnob for SCP and SFTP using public key credentials.
     */
    public static SshKnob buildSshKnob(final SocketAddress localSocketAddress, final SocketAddress remoteSocketAddress,
                                       final String file, final String path, final SshKnob.PublicKeyAuthentication publicKeyCredential) {
        return buildSshKnob(localSocketAddress, remoteSocketAddress, file, path, publicKeyCredential, null);
    }

    /*
     * Create an SshKnob for SCP and SFTP using password credentials.
     */
    public static SshKnob buildSshKnob(final SocketAddress localSocketAddress, final SocketAddress remoteSocketAddress,
                                       final String file, final String path, final PasswordAuthentication passwordCredential) {
        return buildSshKnob(localSocketAddress, remoteSocketAddress, file, path, null, passwordCredential);
    }

    /*
     * Create an SshKnob for SCP and SFTP.
     */
    public static SshKnob buildSshKnob(final SocketAddress localSocketAddress, final SocketAddress remoteSocketAddress,
                                       final String file, final String path,
                                       final SshKnob.PublicKeyAuthentication publicKeyCredential,
                                       final PasswordAuthentication passwordCredential) {

        // SocketAddress requires us to parse for host and port (e.g. /127.0.0.1:22)
        Pair<String,String> localHostPortPair = MessageProcessingSshUtil.getHostAndPort(localSocketAddress.toString());
        Pair<String,String> remoteHostPortPair = MessageProcessingSshUtil.getHostAndPort(remoteSocketAddress.toString());

        final String localHostFinal = localHostPortPair.getKey();
        final int localPortFinal = Integer.parseInt(localHostPortPair.getValue());
        final String remoteHostFinal = remoteHostPortPair.getKey();
        final int remotePortFinal = Integer.parseInt(remoteHostPortPair.getValue());

        return buildSshKnob(localHostFinal, localPortFinal, remoteHostFinal, remotePortFinal, file, path,
                publicKeyCredential, passwordCredential);
    }

    /*
     * Create an SshKnob for SCP and SFTP.
     */
    public static SshKnob buildSshKnob(final String localHost, final int localPort,
                                       final String remoteHost, final int remotePort,
                                       final String file, final String path,
                                       final SshKnob.PublicKeyAuthentication publicKeyCredential,
                                       final PasswordAuthentication passwordCredential) {

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
        };
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
        boolean startsWithForwardSlash = hostAndPossiblyPort.charAt(0) == '/';
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

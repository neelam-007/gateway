package com.l7tech.server.ssh;

import com.l7tech.util.Either;

import java.util.List;

/**
 * This is the key used to identify and create an ssh session.
 * TODO: investigate if all these items need to be here.
 *
 * @author Victor Kazakov
 */
public class SshSessionKey {
    private final String user;
    private final String host;
    private final int port;
    private final Either<String, String> passwordOrPrivateKey;
    private final int socketTimeout;
    private final String fingerPrint;
    private final List<String> encryptionAlgorithms;
    private final List<String> macAlgorithms;
    private final List<String> compressionAlgorithms;

    public SshSessionKey(String user, String host, int port, Either<String, String> passwordOrPrivateKey, int socketTimeout, String fingerPrint, List<String> encryptionAlgorithms, List<String> macAlgorithms, List<String> compressionAlgorithms) {
        this.user = user;
        this.host = host;
        this.port = port;
        this.passwordOrPrivateKey = passwordOrPrivateKey;
        this.socketTimeout = socketTimeout;
        this.fingerPrint = fingerPrint;
        this.encryptionAlgorithms = encryptionAlgorithms;
        this.macAlgorithms = macAlgorithms;
        this.compressionAlgorithms = compressionAlgorithms;
    }

    public String getUser() {
        return user;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Either<String, String> getPasswordOrPrivateKey() {
        return passwordOrPrivateKey;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public String getFingerPrint() {
        return fingerPrint;
    }

    public List<String> getEncryptionAlgorithms() {
        return encryptionAlgorithms;
    }

    public List<String> getMacAlgorithms() {
        return macAlgorithms;
    }

    public List<String> getCompressionAlgorithms() {
        return compressionAlgorithms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SshSessionKey)) return false;

        SshSessionKey that = (SshSessionKey) o;

        if (port != that.port) return false;
        if (socketTimeout != that.socketTimeout) return false;
        if (compressionAlgorithms != null ? !compressionAlgorithms.equals(that.compressionAlgorithms) : that.compressionAlgorithms != null)
            return false;
        if (encryptionAlgorithms != null ? !encryptionAlgorithms.equals(that.encryptionAlgorithms) : that.encryptionAlgorithms != null)
            return false;
        if (fingerPrint != null ? !fingerPrint.equals(that.fingerPrint) : that.fingerPrint != null) return false;
        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (macAlgorithms != null ? !macAlgorithms.equals(that.macAlgorithms) : that.macAlgorithms != null)
            return false;
        if (passwordOrPrivateKey != null ? !passwordOrPrivateKey.equals(that.passwordOrPrivateKey) : that.passwordOrPrivateKey != null)
            return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + port;
        result = 31 * result + (passwordOrPrivateKey != null ? passwordOrPrivateKey.hashCode() : 0);
        result = 31 * result + socketTimeout;
        result = 31 * result + (fingerPrint != null ? fingerPrint.hashCode() : 0);
        result = 31 * result + (encryptionAlgorithms != null ? encryptionAlgorithms.hashCode() : 0);
        result = 31 * result + (macAlgorithms != null ? macAlgorithms.hashCode() : 0);
        result = 31 * result + (compressionAlgorithms != null ? compressionAlgorithms.hashCode() : 0);
        return result;
    }
}

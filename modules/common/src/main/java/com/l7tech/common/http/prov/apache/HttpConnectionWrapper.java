package com.l7tech.common.http.prov.apache;

import java.net.InetAddress;
import java.net.SocketException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * Wrapper for HttpConnections (only used when bound).
 *
 * @author Steve Jones
 */
final class HttpConnectionWrapper extends HttpConnection {

    interface ConnectionListener {
        void wrap();
        boolean release();
    }

    // the wrapped connection
    private HttpConnection wrappedConnection;
    private final ConnectionListener listener;

    /**
     * Creates a new HttpConnectionAdapter.
     * @param connection the connection to be wrapped
     */
    public HttpConnectionWrapper(HttpConnection connection, ConnectionListener listener) {
        super(connection.getHost(), connection.getPort(), connection.getProtocol());
        this.wrappedConnection = connection;
        this.listener = listener;
        this.listener.wrap();
    }

    /**
     * Tests if the wrapped connection is still available.
     * @return boolean
     */
    protected boolean hasConnection() {
        return wrappedConnection != null;
    }

    /**
     *
     */
    HttpConnection getWrappedConnection() {
        return wrappedConnection;
    }

    /**
     *
     */
    ConnectionListener getConnectionListener() {
        return listener;
    }

    public void close() {
        if (hasConnection()) {
            wrappedConnection.close();
        } else {
            // do nothing
        }
    }

    public InetAddress getLocalAddress() {
        if (hasConnection()) {
            return wrappedConnection.getLocalAddress();
        } else {
            return null;
        }
    }

    /**
     * @deprecated
     */
    public boolean isStaleCheckingEnabled() {
        if (hasConnection()) {
            return wrappedConnection.isStaleCheckingEnabled();
        } else {
            return false;
        }
    }

    public void setLocalAddress(InetAddress localAddress) {
        if (hasConnection()) {
            wrappedConnection.setLocalAddress(localAddress);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public void setStaleCheckingEnabled(boolean staleCheckEnabled) {
        if (hasConnection()) {
            wrappedConnection.setStaleCheckingEnabled(staleCheckEnabled);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public String getHost() {
        if (hasConnection()) {
            return wrappedConnection.getHost();
        } else {
            return null;
        }
    }

    public HttpConnectionManager getHttpConnectionManager() {
        if (hasConnection()) {
            return wrappedConnection.getHttpConnectionManager();
        } else {
            return null;
        }
    }

    public InputStream getLastResponseInputStream() {
        if (hasConnection()) {
            return wrappedConnection.getLastResponseInputStream();
        } else {
            return null;
        }
    }

    public int getPort() {
        if (hasConnection()) {
            return wrappedConnection.getPort();
        } else {
            return -1;
        }
    }

    public Protocol getProtocol() {
        if (hasConnection()) {
            return wrappedConnection.getProtocol();
        } else {
            return null;
        }
    }

    public String getProxyHost() {
        if (hasConnection()) {
            return wrappedConnection.getProxyHost();
        } else {
            return null;
        }
    }

    public int getProxyPort() {
        if (hasConnection()) {
            return wrappedConnection.getProxyPort();
        } else {
            return -1;
        }
    }

    public OutputStream getRequestOutputStream()
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            return wrappedConnection.getRequestOutputStream();
        } else {
            return null;
        }
    }

    public InputStream getResponseInputStream()
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            return wrappedConnection.getResponseInputStream();
        } else {
            return null;
        }
    }

    public boolean isOpen() {
        if (hasConnection()) {
            return wrappedConnection.isOpen();
        } else {
            return false;
        }
    }

    public boolean isProxied() {
        if (hasConnection()) {
            return wrappedConnection.isProxied();
        } else {
            return false;
        }
    }

    public boolean isResponseAvailable() throws IOException {
        if (hasConnection()) {
            return  wrappedConnection.isResponseAvailable();
        } else {
            return false;
        }
    }

    public boolean isResponseAvailable(int timeout) throws IOException {
        if (hasConnection()) {
            return  wrappedConnection.isResponseAvailable(timeout);
        } else {
            return false;
        }
    }

    public boolean isSecure() {
        if (hasConnection()) {
            return wrappedConnection.isSecure();
        } else {
            return false;
        }
    }

    public boolean isTransparent() {
        if (hasConnection()) {
            return wrappedConnection.isTransparent();
        } else {
            return false;
        }
    }

    public void open() throws IOException {
        if (hasConnection()) {
            wrappedConnection.open();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public void print(String data)
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.print(data);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void printLine()
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.printLine();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public void printLine(String data)
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.printLine(data);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public String readLine() throws IOException, IllegalStateException {
        if (hasConnection()) {
            return wrappedConnection.readLine();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void releaseConnection() {
        if (!isLocked() && hasConnection()) {
            HttpConnection wrappedConnection = this.wrappedConnection;
            this.wrappedConnection = null;
            if(this.listener.release())
                wrappedConnection.releaseConnection();
        } else {
            // do nothing
        }
    }

    /**
     * @deprecated
     */
    public void setConnectionTimeout(int timeout) {
        if (hasConnection()) {
            wrappedConnection.setConnectionTimeout(timeout);
        } else {
            // do nothing
        }
    }

    public void setHost(String host) throws IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.setHost(host);
        } else {
            // do nothing
        }
    }

    public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
        if (hasConnection()) {
            wrappedConnection.setHttpConnectionManager(httpConnectionManager);
        } else {
            // do nothing
        }
    }

    public void setLastResponseInputStream(InputStream inStream) {
        if (hasConnection()) {
            wrappedConnection.setLastResponseInputStream(inStream);
        } else {
            // do nothing
        }
    }

    public void setPort(int port) throws IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.setPort(port);
        } else {
            // do nothing
        }
    }

    public void setProtocol(Protocol protocol) {
        if (hasConnection()) {
            wrappedConnection.setProtocol(protocol);
        } else {
            // do nothing
        }
    }

    public void setProxyHost(String host) throws IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.setProxyHost(host);
        } else {
            // do nothing
        }
    }

    public void setProxyPort(int port) throws IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.setProxyPort(port);
        } else {
            // do nothing
        }
    }

    /**
     * @deprecated
     */
    public void setSoTimeout(int timeout)
        throws SocketException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.setSoTimeout(timeout);
        } else {
            // do nothing
        }
    }

    /**
     * @deprecated
     */
    public void shutdownOutput() {
        if (hasConnection()) {
            wrappedConnection.shutdownOutput();
        } else {
            // do nothing
        }
    }

    public void tunnelCreated() throws IllegalStateException, IOException {
        if (hasConnection()) {
            wrappedConnection.tunnelCreated();
        } else {
            // do nothing
        }
    }

    public void write(byte[] data, int offset, int length)
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.write(data, offset, length);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void write(byte[] data)
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.write(data);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void writeLine()
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.writeLine();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void writeLine(byte[] data)
        throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.writeLine(data);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void flushRequestOutputStream() throws IOException {
        if (hasConnection()) {
            wrappedConnection.flushRequestOutputStream();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public int getSoTimeout() throws SocketException {
        if (hasConnection()) {
            return wrappedConnection.getSoTimeout();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public String getVirtualHost() {
        if (hasConnection()) {
            return wrappedConnection.getVirtualHost();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public void setVirtualHost(String host) throws IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.setVirtualHost(host);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public int getSendBufferSize() throws SocketException {
        if (hasConnection()) {
            return wrappedConnection.getSendBufferSize();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    /**
     * @deprecated
     */
    public void setSendBufferSize(int sendBufferSize) throws SocketException {
        if (hasConnection()) {
            wrappedConnection.setSendBufferSize(sendBufferSize);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public boolean closeIfStale() throws IOException {
        if (hasConnection()) {
            return wrappedConnection.closeIfStale();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public HttpConnectionParams getParams() {
        if (hasConnection()) {
            return wrappedConnection.getParams();
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void print(String string, String string1) throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.print(string, string1);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void printLine(String string, String string1) throws IOException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.printLine(string, string1);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public String readLine(String string) throws IOException, IllegalStateException {
        if (hasConnection()) {
            return wrappedConnection.readLine(string);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void setParams(HttpConnectionParams httpConnectionParams) {
        if (hasConnection()) {
            wrappedConnection.setParams(httpConnectionParams);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }

    public void setSocketTimeout(int i) throws SocketException, IllegalStateException {
        if (hasConnection()) {
            wrappedConnection.setSocketTimeout(i);
        } else {
            throw new IllegalStateException("Connection has been released");
        }
    }
}

/**
 * Utility class for building Ftp ONLY clients in the connected state, ready to use for file transfers.
 *
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: May 25, 2009
 * Time: 12:17:55 PM
 */
package com.l7tech.gateway.common.transport.ftp;

import java.net.UnknownHostException;
import java.io.*;

import com.jscape.inet.ftp.Ftp;
import com.jscape.inet.ftp.FtpException;

public class FtpUtils {
    /**
     * Wrapper method to call {@link Ftp#connect} that throws a better exception.
     *
     * The problem with calling {@link Ftp#connect} directly is that when the
     * host is unavailable, it throws an exception with a message containing
     * just the host name with no description. This wrapper replaces that with a
     * clearer message.
     * @param ftp Connect to Ftp server represented by FTP
     * @throws com.jscape.inet.ftp.FtpException if any ftp exception occurs
     */
    public static void ftpConnect(Ftp ftp) throws FtpException {
        try {
            ftp.connect();
        } catch (FtpException e) {
            final Exception cause = e.getException();
            if (cause instanceof UnknownHostException) {
                e = new FtpException("Unknown host: " + ftp.getHostname(), cause);
            }
            throw e;
        }
    }

    /**
     * Creates a new, connected FTP client using the provided FTP configuration.
     * @param config FtpClientConfig containing all ftp parameters required to create Ftp object. Cannot be null
     * @return Ftp configured as per config, in the connected state
     * @throws com.jscape.inet.ftp.FtpException if any ftp exception occurs when trying to connect
     * @throws NullPointerException if config is null
     */
    public static Ftp newFtpClient(FtpClientConfig config) throws FtpException {
        if(config == null) throw new NullPointerException("config cannot be null");
        if (FtpCredentialsSource.SPECIFIED != config.getCredentialsSource())
            throw new IllegalStateException("Cannot create FTP connection if crediantials are not specified.");

        final Ftp ftp = new Ftp(config.getHost(), config.getUser(), config.getPass(), config.getPort());
        if (config.getDebugStream() != null) {
            ftp.setDebugStream(config.getDebugStream());
            ftp.setDebug(true);
        }
        ftp.setTimeout(config.getTimeout());
        ftpConnect(ftp);

        String directory = config.getDirectory();
        try {
            if (directory != null && config.getDirectory().length() != 0) {
                ftp.setDir(config.getDirectory());
            }
            ftp.setAuto(false);
            ftp.setBinary();
        } catch (FtpException e) {
            ftp.disconnect();   // Closes connection before letting exception bubble up.
            throw e;
        }
        return ftp;
    }

    /**
     * Creates a new, connected FTP client connected to the specified hostname.
     * @param host The host name of the ftp server to connect to
     * @return Ftp object, with an active connection to the host ftp server
     * @throws com.jscape.inet.ftp.FtpException if any ftp exceptions occur
     */
    public static Ftp newFtpClient(String host) throws FtpException {
        return newFtpClient(FtpClientConfigImpl.newFtpConfig(host));
    }

    /**
     * Tests connection to FTP server and tries "cd" into remote directory.
     *
     * @param config FtpClientConfig containing all ftp parameters required to create Ftp object. Cannot be null 
     * @throws FtpTestException if connection test failed
     */
    public static void testFtpConnection(FtpClientConfig config) throws FtpTestException {
        if(config == null) throw new NullPointerException("config cannot be null");

        // provide our own debug if the client is not watching the logs
        ByteArrayOutputStream baos = null;
        if (config.getDebugStream() == null) {
            baos = new ByteArrayOutputStream();
            config.setDebugStream(new PrintStream(baos));
        }

        Ftp ftp = null;
        try {
            ftp = newFtpClient(config);
        } catch (FtpException e) {
            throw new FtpTestException(e.getMessage(), baos != null ? baos.toString() : null);
        } finally {
            if (ftp != null) ftp.disconnect();
            if (baos != null) {
                config.getDebugStream().close();
                config.setDebugStream(null);
            }
        }
    }

    /**
     *
     * @param config FtpClientConfig containing all ftp parameters required to create Ftp object. Cannot be null
     * @param is the InputStream to upload to FTP server
     * @param filename the name of the file on the ftp server after it has been uploaded
     * @throws FtpException if any ftp exceptions occur
     * @throws IllegalArgumentException if config.getSecurity != FtpSecurity.FTP_UNSECURED 
     */
    public static void upload(FtpClientConfig config, InputStream is, String filename)
            throws FtpException {
        upload(config, is, filename, false);
    }

    /**
     *
     * @param config FtpClientConfig containing all ftp parameters required to create Ftp object. Cannot be null
     * @param is the InputStream to upload to FTP server
     * @param filename the name of the file on the ftp server after it has been uploaded
     * @param allowDirChange if true, then if filename contains directory information, then the ftp session will
     * attemp to change into that directory. Note the assumption is that the ftp server is windows based
     * @throws FtpException if any ftp exceptions occur
     * @throws IllegalArgumentException if config.getSecurity != FtpSecurity.FTP_UNSECURED
     */
    public static void upload(FtpClientConfig config, InputStream is, String filename, boolean allowDirChange)
            throws FtpException {
        if(config == null) throw new NullPointerException("config cannot be null");

        if (FtpSecurity.FTP_UNSECURED != config.getSecurity())
            throw new IllegalArgumentException("Secured FTP is not supported");

        final Ftp ftp = FtpUtils.newFtpClient(config);

        try {
            ftp.upload(is, filename);
        } finally {
            ftp.disconnect();
        }
    }

}

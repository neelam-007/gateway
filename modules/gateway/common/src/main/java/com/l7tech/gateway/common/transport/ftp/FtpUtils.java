/**
 * Utility class for building Ftp ONLY clients in the connected state, ready to use for file transfers.
 *
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: May 25, 2009
 * Time: 12:17:55 PM
 */
package com.l7tech.gateway.common.transport.ftp;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;

import com.jscape.inet.ftp.Ftp;
import com.jscape.inet.ftp.FtpException;
import com.jscape.inet.ftp.FtpAdapter;
import com.jscape.inet.ftp.FtpUploadEvent;
import com.jscape.inet.ftp.FtpResponseEvent;
import com.l7tech.util.ResourceUtils;

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
            throw new IllegalStateException("Cannot create FTP connection if credentials are not specified.");

        final Ftp ftp = new Ftp(config.getHost(), config.getUser(), config.getPass(), config.getPort());
        if (config.getDebugStream() != null) {
            ftp.setDebugStream(config.getDebugStream());
            ftp.setDebug(true);
        }
        ftp.setTimeout(config.getTimeout());

        ftpConnect(ftp);

        try {
            ftp.setUseEPSV(InetAddress.getByName(config.getHost()) instanceof Inet6Address);
        } catch (UnknownHostException e) {
            // do nothing, go with PASV / assume IPv4
        }

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
     * @param count The minimum number of bytes to upload for a successful upload
     * @param filename the name of the file on the ftp server after it has been uploaded
     * @throws FtpException if any ftp exceptions occur
     * @throws IllegalArgumentException if config.getSecurity != FtpSecurity.FTP_UNSECURED
     */
    public static void upload( final FtpClientConfig config,
                               final InputStream is,
                               final long count,
                               final String filename )
            throws FtpException {
        if(config == null) throw new NullPointerException("config cannot be null");

        if (FtpSecurity.FTP_UNSECURED != config.getSecurity())
            throw new IllegalArgumentException("Secured FTP is not supported");

        final Ftp ftp = FtpUtils.newFtpClient(config);

        final FtpUploadSizeListener listener = new FtpUploadSizeListener();
        try {
            ftp.addFtpListener( listener );
            ftp.upload(is, filename);
        } finally {
            ftp.disconnect();
        }

        if ( listener.isError() ) {
            throw new FtpException("File '"+filename+"' upload error '"+listener.getError()+"'.");
        } else if ( listener.getSize() < count ) {
           throw new FtpException("File '"+filename+"' upload truncated to " + listener.getSize() + " bytes.");
        }
    }

    /**
     * Download a file from a FTP server and store locally
     * @param config FtpClientConfig ftp client config
     * @param fileNameAndPath Name of where to download the file to locally
     * @param fileToDownload Name of the file after it has been downloaded. if this String contains path information
     * then all directories up to the file name should exist. The file should not exist and will be created
     * @throws FtpException
     * @throws IOException if any IO exception writing downloaded file to disk
     */
    public static void download(FtpClientConfig config, String fileNameAndPath, String fileToDownload )
            throws FtpException, IOException{

        File saveFile = new File(fileNameAndPath);
        File localParentDir = new File(saveFile.getParent());
        if(!localParentDir.exists()) throw new IllegalArgumentException("Parent directory of fileNameAndPath does not exist");
        if(!localParentDir.isDirectory()) throw new IllegalArgumentException("Parent directory is not a directory");

        if(saveFile.exists()) saveFile.delete();
        saveFile.createNewFile();
        FileOutputStream fos = null;

        final Ftp ftp = FtpUtils.newFtpClient(config);

        try{
            fos = new FileOutputStream(saveFile);
            ftp.connect();
            ftp.download(fos, fileToDownload);
        } finally{
            ftp.disconnect();
            ResourceUtils.closeQuietly(fos);
        }

    }

    public static final class FtpUploadSizeListener extends FtpAdapter {
        private static final String CODE_CONN_OPEN_START_TRANS = "125";
        private static final String CODE_FILE_STATUS_OK_DATA_OPEN = "150";
        private static final String CODE_FILE_ACTION_NOT_TAKEN = "450";
        private static final String CODE_FILE_ACTION_ABORT_ERROR = "451";
        private static final String CODE_FILE_ACTION_NO_SPACE = "452";

        private boolean checkError = false;
        private String error;
        private long size;

        public long getSize() {
            return size;
        }

        public boolean isError() {
            return error != null;            
        }

        public String getError() {
            return error;
        }

        @Override
        public void responseReceived( final FtpResponseEvent ftpResponseEvent ) {
            final String response = ftpResponseEvent.getResponse();
            if ( response != null ) {
                if ( !checkError ) {
                    if ( response.startsWith( CODE_CONN_OPEN_START_TRANS ) ||
                         response.startsWith( CODE_FILE_STATUS_OK_DATA_OPEN ) ) {
                        checkError = error == null;
                    }
                } else {
                    if ( response.startsWith( CODE_FILE_ACTION_NOT_TAKEN ) ||
                         response.startsWith( CODE_FILE_ACTION_ABORT_ERROR ) ||
                         response.startsWith( CODE_FILE_ACTION_NO_SPACE ) ) {
                        error = response;
                    }

                    checkError = false;
                }
            }
        }

        @Override
        public void upload( final FtpUploadEvent ftpUploadEvent ) {
            size = ftpUploadEvent.getSize();
        }
    }
}

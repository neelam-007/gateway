package com.l7tech.common.message;

import java.net.PasswordAuthentication;

/**
 * Information about a request that arrived over HTTP.
 *
 * @author Steve Jones
 */
public interface FtpRequestKnob extends TcpKnob {

    /**
     * The path of the file being uploaded.
     *
     * @return the file path
     */
    String getPath();

    /**
     * The name of the file being uploaded.
     *
     * @return the file name
     */
    String getFile();

    /**
     * URI part of the URL for this request (e.g. /ssg/soap). Never null or empty.
     *
     * <p>This is used for service resolution.</p>
     *
     * @return the uri
     */
    String getRequestUri();

    /**
     * The (constructed) URL for this request (e.g. ftps://gateway:2121/ssg/soap/file.xml).
     *
     * <p>Never null or empty.</p>
     *
     * @return the url
     */
    String getRequestUrl();

    /**
     * True for STOU, false for STOR
     *
     * @return true if storing with a unique file name
     */
    boolean isUnique();

    /**
     * Check if this request arrived over a secure connection.
     *
     * @return true iff. this request (control and data) arrived over SSL
     */
    boolean isSecure();

    /**
     * Get the credentials for the session.
     *
     * @return The credentials or null if anonymous
     */
    PasswordAuthentication getCredentials();
}

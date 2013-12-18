package com.l7tech.message;

import java.net.PasswordAuthentication;

/**
 * Information about a request that arrived over FTP.
 *
 * @author Steve Jones
 */
public interface FtpRequestKnob extends TcpKnob, UriKnob {

    /**
     * The raw FTP command.
     *
     * @return the FTP command
     */
    String getCommand();

    /**
     * The argument string for the command.
     *
     * @return the argument string, or null if none given
     */
    String getArgument();

    /**
     * The current working directory of the FTP session.
     *
     * @return the working directory
     */
    String getPath();

    /**
     * The (constructed) URL for this request (e.g. ftps://gateway:2121/ssg/soap/file.xml).
     *
     * <p>Never null or empty.</p>
     *
     * @return the url
     */
    String getRequestUrl();

    /**
     * True for STOU, false for any other command.
     *
     * DEPRECATED: Use {@link #getCommand()} to determine if the command is STOU.
     *
     * @return true iff command is STOU
     */
    @Deprecated
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

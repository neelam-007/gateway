package com.l7tech.message;

import java.net.PasswordAuthentication;
import java.security.PublicKey;

/**
 * Information about a message over SSH.
 */
public interface SshKnob extends TcpKnob {

    /**
     * The path of the file being uploaded/downloaded.
     *
     * @return the file path
     */
    String getPath();

    /**
     * The name of the file being uploaded/downloaded.
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
     * Get the user name and password for the session.
     *
     * @return The user name and password or null if anonymous
     */
    PasswordAuthentication getPasswordAuthentication();

    /**
     * Get the user public key for the session.
     *
     * @return The public key or null if anonymous
     */
    PublicKey getPublicKey();
}

package com.l7tech.message;

import java.net.PasswordAuthentication;

/**
 * Information about a message over SSH.
 */
public interface SshKnob extends TcpKnob {

    /**
     * This class is a data holder that is used by SshKnob.
     * It is simply a repository for a user name and a public key.
     */
    public class PublicKeyAuthentication {
        private String userName;
        private String publicKey;

        public PublicKeyAuthentication(String userName, String publicKey) {
            this.userName = userName;
            this.publicKey = publicKey;
        }

        public String getUserName() {
            return userName;
        }

        public String getPublicKey() {
            return publicKey;
        }
    }

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
     * Get the user name and public key for the session.
     *
     * @return The user name and public key
     */
    PublicKeyAuthentication getPublicKeyAuthentication();
}

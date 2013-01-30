package com.l7tech.message;

import java.net.PasswordAuthentication;

/**
 * Information about a message over SSH.
 */
public interface SshKnob extends TcpKnob, UriKnob, CommandKnob {

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

    public class FileMetadata {
        
        private final long accessTime;
        private final long modificationTime;
        private final int  permission;

        public FileMetadata(final long accessTime, final long modificationTime, final int mode) {
            this.accessTime = accessTime;
            this.modificationTime = modificationTime;
            this.permission = mode;
        }

        public long getAccessTime() {
            return accessTime;
        }

        public long getModificationTime() {
            return modificationTime;
        }

        public int getPermission() {
            return permission;
        }
    }

    /**
     * The metadata associated with the file.
     * @return the file metadata if present, null otherwise.
     */
    FileMetadata getFileMetadata();

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

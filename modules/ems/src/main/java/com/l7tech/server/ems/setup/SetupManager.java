package com.l7tech.server.ems.setup;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.DeleteException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ArrayList;

/**
 * Encapsulates behavior for setup of an ESM instance.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface SetupManager {

    /**
     * Get the ID for the ESM instance.
     *
     * @return The ID (which is a GUID).
     */
    String getEsmId();

    /**
     * Delete the current license.
     *
     * @throws DeleteException if an error occurs.
     */
    void deleteLicense() throws DeleteException;

    /**
     * Configure an HTTPS listener for the given ip / port.
     *
     * @param ipaddress The ipaddress to listen on
     * @param port The port to listen on
     * @throw SetupException if an error occurs.
     */
    void configureListener( String ipaddress, int port ) throws SetupException;

    /**
     * Save the given key / cert and return the generated alias.
     *
     * @param key The private key to use
     * @param certificateChain The certificate to use
     * @return The generated alias value
     * @throws SetupException If an error occurs
     */
    String saveSsl( PrivateKey key, X509Certificate[] certificateChain ) throws SetupException;

    /**
     * Enum of the valid rsa key sizes available when generating a SSL certificate
     */
    public enum RsaKeySize {
        rsa512(512), rsa768(768), rsa1024(1024), rsa2048(2048);

        RsaKeySize(int keySize){
            this.keySize = keySize;           
        }

        /**
         * Convenience method to get a RsaKeySize from a string representing the required key size
         * @param keySizeString
         * @return the RsakeySize enum constant representing the supplied key size
         */
        public static RsaKeySize getRsaKeySize(String keySizeString){
            int keySize = Integer.parseInt(keySizeString);

            switch (keySize){
                case 512:
                    return rsa512;
                case 768:
                    return rsa768;
                case 1024:
                    return rsa1024;
                case 2048:
                    return rsa2048;
                default:
                    throw new IllegalArgumentException("Supplied key size string must represent one of the following" +
                            "key sizes: 512, 768, 1024 or 2048");
            }
        }

        public int getKeySize() {
            return keySize;
        }

        @Override
        public String toString() {
            return Integer.toString(keySize);
        }

        /**
         * Convenience method to return a list of Strings, which represent all valid key sizes
         * @return
         */
        public static List<String> getAllKeySizes(){
            List<String> returnList = new ArrayList<String>();
            returnList.add(rsa512.toString());
            returnList.add(rsa768.toString());
            returnList.add(rsa1024.toString());
            returnList.add(rsa2048.toString());
            return returnList;
        }

        private final int keySize;
    }

    /**
     * Generate an SSL certificate for the given host returning the generated alias.
     *
     * @param hostname The hostname to generate a certificate for.
     * @param rsaKeySize
     * @return The generated alias value
     * @throws SetupException If an error occurs
     */
    String generateSsl(String hostname, RsaKeySize rsaKeySize) throws SetupException;

    /**
     * Use the given alias for the SSL listener.
     *
     * @param alias The alias to use.
     * @throws SetupException If an error occurs
     */
    void setSslAlias( String alias ) throws SetupException;

    /**
     * Set the timeout to use for any new HTTP sessions.
     *
     * @param sessionTimeout The timeout in seconds
     * @throws SetupException If an error occurs.
     */
    void setSessionTimeout( int sessionTimeout ) throws SetupException;
}

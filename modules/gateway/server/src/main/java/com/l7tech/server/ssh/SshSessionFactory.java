package com.l7tech.server.ssh;

import com.jcraft.jsch.*;
import com.l7tech.util.Functions;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

import java.util.HashMap;
import java.util.logging.Level;

import static com.l7tech.util.Functions.reduce;

/**
 * This is the session factory. It is used to create SshSession's. This should be the only way SshSessions are created.
 *
 * @author Victor Kazakov
 */
public class SshSessionFactory implements KeyedPoolableObjectFactory<SshSessionKey, SshSession> {
    protected static final java.util.logging.Logger jschLogger = java.util.logging.Logger.getLogger(JSch.class.getName());
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SshSessionFactory.class.getName());

    //This is the default keep alive time for passive/idle ssh sessions
    private static final int DEFAULT_SOCKET_KEEP_ALIVE = 300000;

    static {
        //Sets the JSch logger so that we can appropriately log messages from JSch
        JSch.setLogger(new Logger() {

            @Override
            public boolean isEnabled(int level) {
                return jschLogger.isLoggable(getLevel(level));
            }

            private Level getLevel(int level) {
                //Our logging level standards are more restrictive then the Jsch ones so this adjusts them appropriately
                switch (level) {
                    case Logger.FATAL:
                    case Logger.ERROR:
                        return Level.WARNING;
                    case Logger.WARN:
                        return Level.INFO;
                    case Logger.INFO:
                        return Level.FINE;
                    case Logger.DEBUG:
                        return Level.FINEST;
                    default:
                        //Should never get here.
                        return Level.FINEST;
                }
            }

            @Override
            public void log(int level, String message) {
                jschLogger.log(getLevel(level), message);
            }
        });
    }

    /**
     * Creates a new SshSession associated with the given key. This session must be connected before it can be used.
     *
     * @param sshSessionKey The session key to create the session for
     * @return The SshSession. It has not been connected yet. It must be connected before it can be used.
     */
    public SshSession makeObject(SshSessionKey sshSessionKey) throws JSchException {
        logger.log(Level.FINE, "Making a new session for key {0}", sshSessionKey);
        JSch jsch = new JSch();
        final DefaultUI userInfo = new DefaultUI();

        // check if it is connecting with password authentication of with private key authentication
        if (sshSessionKey.getPasswordOrPrivateKey().isRight()) {
            //add the private key to the identity provider.
            jsch.addIdentity("", sshSessionKey.getPasswordOrPrivateKey().right().getBytes(), null, null);
        } else {
            //add the password to the user info
            userInfo.setPassword(sshSessionKey.getPasswordOrPrivateKey().left());
        }
        //create the session
        Session session = jsch.getSession(sshSessionKey.getUser(), sshSessionKey.getHost(), sshSessionKey.getPort());

        //set strict host key checking by default
        session.setConfig("StrictHostKeyChecking", "yes");
        if (sshSessionKey.getFingerPrint() != null) {
            //Create a new host key checking repository for checking the given fingerprint.
            final FingerprintCheckingHostKeyRepository hostKeyRepository = new FingerprintCheckingHostKeyRepository(jsch);
            jsch.setHostKeyRepository(hostKeyRepository);
            //add the fingerprint to the host key repository.
            hostKeyRepository.add(sshSessionKey.getHost(), sshSessionKey.getFingerPrint());
        } else {
            //if no fingerprint is provided we need to turn off strict host key checking.
            session.setConfig("StrictHostKeyChecking", "no");
        }

        //add the user info to the session to provide a password if one is used.
        session.setUserInfo(userInfo);
        //set the session timeout
        session.setTimeout(sshSessionKey.getSocketTimeout());

        //add the ciphers that are used on the session
        String ciphersString = reduce(sshSessionKey.getEncryptionAlgorithms(), "", new Functions.Binary<String, String, String>() {
            @Override
            public String call(String s, String s1) {
                return s.concat(",").concat(s1);
            }
        });
        //remove the starting ','
        ciphersString = ciphersString.substring(1);
        session.setConfig("cipher.s2c", ciphersString);
        session.setConfig("cipher.c2s", ciphersString);
        session.setConfig("CheckCiphers", ciphersString);

        // add the macs used on the session
        String macsString = reduce(sshSessionKey.getMacAlgorithms(), "", new Functions.Binary<String, String, String>() {
            @Override
            public String call(String s, String s1) {
                return s.concat(",").concat(s1);
            }
        });
        //remove the starting ','
        macsString = macsString.substring(1);
        session.setConfig("mac.c2s", macsString);
        session.setConfig("mac.s2c", macsString);

        //Add the compression methods used.
        String compressionsString = reduce(sshSessionKey.getCompressionAlgorithms(), "", new Functions.Binary<String, String, String>() {
            @Override
            public String call(String s, String s1) {
                return s.concat(",").concat(s1);
            }
        });
        //remove the starting ','
        compressionsString = compressionsString.substring(1);
        session.setConfig("compression.c2s", compressionsString);
        session.setConfig("compression.s2c", compressionsString);

        session.connect(sshSessionKey.getConnectionTimeout());
        return new SshSession(sshSessionKey, session);
    }

    @Override
    public void destroyObject(SshSessionKey key, SshSession session) throws Exception {
        logger.log(Level.FINE, "Destroying session for key {0}", key);
        session.close();
    }

    @Override
    public boolean validateObject(SshSessionKey key, SshSession session) {
        logger.log(Level.FINEST, "Validating session for key {0}", key);
        return session.isConnected();
    }

    @Override
    public void activateObject(SshSessionKey key, SshSession session) throws Exception {
        logger.log(Level.FINER, "Activating session for key {0}", key);
        session.setSocketTimeout(key.getSocketTimeout());
    }

    @Override
    public void passivateObject(SshSessionKey key, SshSession session) throws Exception {
        logger.log(Level.FINER, "Passivating session for key {0}", key);
        session.setSocketTimeout(DEFAULT_SOCKET_KEEP_ALIVE);
    }

    /**
     * This implements the user info needed by JSch to return the user password. No other methods should be called.
     */
    private class DefaultUI implements UserInfo {
        private String password;

        @Override
        public String getPassphrase() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public boolean promptPassword(String message) {
            return password != null;
        }

        @Override
        public boolean promptPassphrase(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean promptYesNo(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void showMessage(String message) {
            throw new UnsupportedOperationException();
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * This is a hostkey repository that checks that the host key fingerprint matches the given fingerprint.
     */
    private class FingerprintCheckingHostKeyRepository implements HostKeyRepository {

        private HashMap<String, String> hostFingerPrints = new HashMap<>();
        private JSch jsch;

        private FingerprintCheckingHostKeyRepository(JSch jsch) {
            this.jsch = jsch;
        }

        /**
         * Check if the fingerprint for the given host matches the key given.
         *
         * @param host The host to match the fingerprint for.
         * @param key  The key to match
         * @return 1 If there was an error or if the fingerprints don't match. 0 if the fingerprint matches
         */
        @Override
        public int check(String host, byte[] key) {
            HostKey hk;
            try {
                hk = new HostKey(host, key);
            } catch (JSchException e) {
                return 1;
            }
            String fingerPrint = hostFingerPrints.get(host);
            if (fingerPrint != null && fingerPrint.equals(hk.getFingerPrint(jsch))) {
                return 0;
            }
            return 1;
        }

        /**
         * This is needed to be implemented but it is not used by us.
         */
        @Override
        public void add(HostKey hostkey, UserInfo ui) {
            throw new UnsupportedOperationException();
        }

        /**
         * This will add a fingerprint to the repository for the given host.
         *
         * @param host        The host the the fingerprint belongs to
         * @param fingerprint The fingerprint of the host.
         */
        public void add(String host, String fingerprint) {
            hostFingerPrints.put(host, fingerprint);
        }

        /**
         * This is never used
         */
        @Override
        public void remove(String host, String type) {
            throw new UnsupportedOperationException();
        }

        /**
         * This is never used
         */
        @Override
        public void remove(String host, String type, byte[] key) {
            throw new UnsupportedOperationException();
        }

        /**
         * This is never used. But it is called by Jsch returning null is fine here.
         */
        @Override
        public String getKnownHostsRepositoryID() {
            return null;
        }

        /**
         * This is never used
         */
        @Override
        public HostKey[] getHostKey() {
            throw new UnsupportedOperationException();
        }

        /**
         * This is never used
         */
        @Override
        public HostKey[] getHostKey(String host, String type) {
            throw new UnsupportedOperationException();
        }
    }
}

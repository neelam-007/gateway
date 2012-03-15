package com.l7tech.external.assertions.ssh.server.client;

import com.jscape.inet.ssh.SshConfiguration;
import com.jscape.inet.ssh.transport.AlgorithmFactory;
import com.jscape.inet.ssh.types.SshNameList;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Functions;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.CollectionUtils.toSet;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.Functions.*;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;

/**
 * Configure SSH client including supported algorithms.
 */
public class SshClientConfiguration extends SshConfiguration {
    public static final String defaultCipherOrder = "aes128-ctr, aes128-cbc, 3des-cbc, blowfish-cbc, aes192-ctr, aes192-cbc, aes256-ctr, aes256-cbc";

    private static final boolean enableMacNone = ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.server.enableMacNone", false);
    private static final boolean enableMacMd5 = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.ssh.server.enableMacMd5", false );

    public SshClientConfiguration (final SshParameters sshParams, Set<String> ciphers) {
        super();
        final AlgorithmFactory algorithmFactory = buildAlgorithmFactory( ciphers );
        getTransportConfiguration().setAlgorithmFactory( algorithmFactory );
        getTransportConfiguration().setHostKeyVerifier( sshParams.getHostKeyVerifier() );
    }

    private AlgorithmFactory buildAlgorithmFactory( Set<String> ciphers ) {

        final List<String> cipherList = map( grep( map( ciphers, SshCipher.bySshName() ), SshCipher.available() ), SshCipher.sshName() );

        final AlgorithmFactory algorithmFactory = new AlgorithmFactory(){
            @Override
            public SshNameList getAllCiphers() {
                // overridden to return list in priority order
                return new SshNameList( cipherList.toArray( new String[cipherList.size()] ) );
            }
        };

        if ( !enableMacNone ) {
            algorithmFactory.removeMac( "none" );
        }
        // Remove MD5 hash by default, always prefer SHA-1
        if ( !enableMacMd5 ) {
            algorithmFactory.removeMac( "hmac-md5" );
        }
        algorithmFactory.setPrefferedMac( "hmac-sha1" );

        // Register all available supported ciphers
        for ( final SshCipher cipher : SshCipher.values() ) {
            if ( !cipher.isRegisteredByDefault() && cipherList.contains( cipher.getSshName() ) ) {
                algorithmFactory.addCipher( cipher.getSshName(), cipher.getJavaCipherName(), cipher.getBlockSize() );
            } else if ( cipher.isRegisteredByDefault() && !cipherList.contains( cipher.getSshName() )) {
                algorithmFactory.removeCipher( cipher.getSshName() );
            }
        }

        return algorithmFactory;
    }

    private enum SshCipher {
        None("none"),
        AES128CBC("aes128-cbc", "AES", "AES/CBC/NoPadding", 16),
        AES128CTR("aes128-ctr", "AES", "AES/CTR/NoPadding", 16),
        AES192CBC("aes192-cbc", "AES", "AES/CBC/NoPadding", 24),
        AES192CTR("aes192-ctr", "AES", "AES/CTR/NoPadding", 24),
        AES256CBC("aes256-cbc", "AES", "AES/CBC/NoPadding", 32),
        AES256CTR("aes256-ctr", "AES", "AES/CTR/NoPadding", 32),
        BlowfishCBC("blowfish-cbc", "Blowfish", "Blowfish/CBC/NoPadding", 16),
        TripleDESCBC("3des-cbc", "DESede", "DESede/CBC/NoPadding", 24);

        public boolean isAvailable() {
            return available;
        }

        public boolean isRegisteredByDefault() {
            return registeredByDefault;
        }

        public int getBlockSize() {
            return blockSize;
        }

        public String getJavaCipherName() {
            return javaCipherName;
        }

        public String getSshName() {
            return sshName;
        }

        public static Functions.Unary<String,SshCipher> sshName() {
            return new Functions.Unary<String,SshCipher>() {
                @Override
                public String call( final SshCipher sshCipher ) {
                    return sshCipher.getSshName();
                }
            };
        }

        public static Functions.Unary<Boolean,SshCipher> available() {
            return new Functions.Unary<Boolean,SshCipher>() {
                @Override
                public Boolean call( final SshCipher sshCipher ) {
                    return sshCipher!=null && sshCipher.isAvailable();
                }
            };
        }

        public static Functions.Unary<SshCipher,String> bySshName() {
            return new Functions.Unary<SshCipher,String>() {
                @Override
                public SshCipher call( final String sshName ) {
                    return grepFirst( list( SshCipher.values() ), equality( sshName(), sshName ) );
                }
            };
        }

        private final Logger logger = Logger.getLogger(SshCipher.class.getName()); // static logger not initialized early enough
        private final String sshName;
        private final String javaAlgorithmName;
        private final String javaCipherName;
        private final int blockSize;
        private final boolean available;
        private final boolean registeredByDefault;

        private SshCipher( final String sshName ) {
            this.sshName = sshName;
            this.javaAlgorithmName = null;
            this.javaCipherName = null;
            this.blockSize = -1;
            this.available = true;
            this.registeredByDefault = true;
        }

        private SshCipher( final String sshName,
                           final String javaAlgorithmName,
                           final String javaCipherName,
                           final int blockSize ) {
            this.sshName = sshName;
            this.javaAlgorithmName = javaAlgorithmName;
            this.javaCipherName = javaCipherName;
            this.blockSize = blockSize;
            this.available = checkCipherAvailable();
            this.registeredByDefault = false;
        }

        private boolean checkCipherAvailable() {
            boolean available = false;
            try {
                final Cipher cipher = Cipher.getInstance( javaCipherName );
                final byte[] key = new byte[blockSize];
                cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec( key, javaAlgorithmName ));
                available = true;
            } catch (Exception e) {
                logger.log( Level.FINE, "SSH cipher not available: " + sshName, getDebugException( e ) );
            }
            return available;
        }
    }
}

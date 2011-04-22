package com.l7tech.server.ems.util;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPMarker;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Date;

/**
 * Simple encryption / decryption utility for PGP with passwords.
 */
public class PgpUtil {

    //- PUBLIC

    /**
     * Decrypt the given input stream to the given output stream.
     *
     * <p>This method will fail if integrity is present but invalid but will
     * NOT fail if integrity is missing. The resulting metadata must be checked
     * if integrity is required.</p>
     *
     * @param inputStream The encrypted input stream
     * @param outputStream The clear output stream
     * @param password The password (encryption key)
     * @return The metadata for the decryption
     * @throws IOException If an IO error occurs
     * @throws PgpException If a decryption error occurs
     */
    public static DecryptionMetadata decrypt( final InputStream inputStream,
                                              final OutputStream outputStream,
                                              final char[] password ) throws IOException, PgpException {
        if ( inputStream == null ) throw new PgpException("Input is required");
        if ( outputStream == null ) throw new PgpException("Output is required");
        if ( password == null ) throw new PgpException("Password is required");

        final InputStream in = PGPUtil.getDecoderStream( inputStream );
        final boolean asciiArmour = in instanceof ArmoredInputStream;
        final PGPObjectFactory pgpF = new PGPObjectFactory( in );
        final PGPEncryptedDataList enc = findObject( pgpF, PGPEncryptedDataList.class );

        if ( enc.size() < 1 ) {
            failInvalid();
        }

        try {
            final PGPPBEEncryptedData pbe = cast( enc.get( 0 ), PGPPBEEncryptedData.class );
            final InputStream clear = pbe.getDataStream( password, securityProvider );
            PGPObjectFactory pgpFact = new PGPObjectFactory( clear );

            // Compression is optional
            final Object literalOrCompressed = pgpFact.nextObject();
            final PGPLiteralData ld;
            if ( literalOrCompressed instanceof PGPCompressedData ) {
                final PGPCompressedData cData = (PGPCompressedData) literalOrCompressed;
                pgpFact = new PGPObjectFactory( cData.getDataStream() );
                ld = cast( pgpFact.nextObject(), PGPLiteralData.class );
            } else {
                ld = cast( literalOrCompressed, PGPLiteralData.class );
            }

            final InputStream unc = ld.getInputStream();
            int ch;
            while ((ch = unc.read()) >= 0) {
                outputStream.write( ch );
            }

            if ( pbe.isIntegrityProtected() && !pbe.verify() ) {
                return failInvalid();
            }

            return new DecryptionMetadata( asciiArmour, pbe.isIntegrityProtected(), ld.getFileName(), ld.getModificationTime().getTime() );
        } catch ( PGPException e ) {
            throw new PgpException( ExceptionUtils.getMessage( e ), e );
        } catch  ( ArrayIndexOutOfBoundsException e ) {
            // See bug 10325, this can occur when a file is incorrectly treated as Base64
            throw new PgpException( ExceptionUtils.getMessage( e ), e );
        }
    }

    /**
     * Encrypt the given input stream to the given output stream.
     *
     * @param inputStream The clear input stream
     * @param outputStream The encrypted output stream
     * @param filename The name for the "file"
     * @param fileModified The modified date for the "file"
     * @param password The password to use for encryption
     * @param asciiArmour True to output with ASCII armour
     * @param integrityCheck True to output with an integrity check
     * @throws IOException If an IO error occurs
     * @throws PGPException If an encryption error occurs
     */
    public static void encrypt( final InputStream inputStream,
                                final OutputStream outputStream,
                                final String filename,
                                final long fileModified,
                                final char[] password,
                                final boolean asciiArmour,
                                final boolean integrityCheck )
            throws IOException, PgpException {
        if ( inputStream == null ) throw new PgpException("Input is required");
        if ( outputStream == null ) throw new PgpException("Output is required");
        if ( filename == null ) throw new PgpException("File name is required");
        if ( password == null ) throw new PgpException("Password is required");

        final PGPCompressedDataGenerator compressedDataGenerator =
                new PGPCompressedDataGenerator( PGPCompressedData.ZIP );
        final PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
        final PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(
                PGPEncryptedData.AES_256,
                integrityCheck,
                secureRandom,
                securityProvider );

        try {
            encryptedDataGenerator.addMethod( password, PGPEncryptedDataGenerator.S2K_SHA512 );
        } catch ( NoSuchProviderException e ) {
            throw new PgpException( ExceptionUtils.getMessage( e ), e );
        } catch ( PGPException e ) {
            throw new PgpException( ExceptionUtils.getMessage( e ), e );
        }

        final byte[] clearData = IOUtils.slurpStream( inputStream );
        OutputStream pgpOut = null;
        OutputStream compressionOut = null;
        OutputStream literalOut = null;
        OutputStream encryptionOut = null;
        try {
            final ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
            compressionOut = compressedDataGenerator.open( compressedOutputStream );
            literalOut = literalDataGenerator.open(
                    compressionOut,
                    PGPLiteralData.BINARY,
                    filename,
                    (long)clearData.length,
                    new Date( fileModified ) );
            literalOut.write( clearData );
            literalOut.close();
            literalOut = null;
            compressionOut.close();
            compressionOut = null;

            final byte[] compressedData = compressedOutputStream.toByteArray();
            pgpOut = asciiArmour ? new ArmoredOutputStream( outputStream ) : outputStream;
            encryptionOut = encryptedDataGenerator.open( pgpOut, (long)compressedData.length );
            encryptionOut.write( compressedData );
        } catch ( PGPException e ) {
            throw new PgpException( ExceptionUtils.getMessage( e ), e );
        } finally {
            // Closing streams closes the StreamGenerator
            ResourceUtils.closeQuietly( literalOut );
            ResourceUtils.closeQuietly( compressionOut );
            ResourceUtils.closeQuietly( encryptionOut );
            ResourceUtils.closeQuietly( pgpOut );
        }
    }

    public static final class PgpException extends Exception {
        public PgpException( final String message,
                             final Throwable cause ) {
            super( message, cause );
        }

        public PgpException( final String message ) {
            super( message );
        }
    }

    /**
     * Metadata for decryption.
     */
    public static final class DecryptionMetadata {
        private final boolean asciiArmoured;
        private final boolean integrityChecked;
        private final String filename;
        private final long fileModified;

        public DecryptionMetadata ( final boolean asciiArmoured,
                                    final boolean integrityChecked,
                                    final String filename,
                                    final long fileModified ) {
            this.asciiArmoured = asciiArmoured;
            this.integrityChecked = integrityChecked;
            this.filename = filename;
            this.fileModified = fileModified;
        }

        /**
         * Was the input ASCII armoured?
         *
         * @return True if armoured
         */
        public boolean isAsciiArmoured() {
            return asciiArmoured;
        }

        /**
         * Was the input integrity checked.
         *
         * @return True if integrity checked
         */
        public boolean isIntegrityChecked() {
            return integrityChecked;
        }

        /**
         * Get the name of the encrypted "file"
         *
         * @return The file name
         */
        public String getFilename() {
            return filename;
        }

        /**
         * Get the modified date of the encrypted "file"
         *
         * @return The modified date
         */
        public long getFileModified() {
            return fileModified;
        }
    }

    //- PRIVATE

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Provider securityProvider = new BouncyCastleProvider();

    private static DecryptionMetadata failInvalid() throws PgpException {
        throw new PgpException( "Invalid encrypted data" );
    }

    private static <T> T findObject( final PGPObjectFactory pgpObjectFactory,
                                     final Class<T> objectClass ) throws PgpException, IOException {
        T result;

        while ( true ) {
            final Object nextObject = pgpObjectFactory.nextObject();
            if ( nextObject == null ) {
                failInvalid();
            }

            if ( nextObject instanceof PGPMarker ) continue;

            if ( objectClass.isInstance( nextObject ) ) {
                result = cast( nextObject, objectClass );
                break;
            }

            failInvalid();
        }

        return result;
    }

    @SuppressWarnings({ "unchecked" })
    private static <T> T cast( final Object object,
                               final Class<T> objectClass ) throws PgpException {
        if ( objectClass.isInstance( object ) ) {
            return (T) object;
        } else {
            failInvalid();
            return null; // not reached
        }
    }
}

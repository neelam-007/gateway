package com.l7tech.external.assertions.symmetrickeyencryptiondecryption.server;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.server.audit.Auditor;
import com.l7tech.external.assertions.symmetrickeyencryptiondecryption.SymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.springframework.context.ApplicationContext;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the SymmetricKeyEncryptionDecryptionAssertion.
 *
 * @see com.l7tech.external.assertions.symmetrickeyencryptiondecryption.SymmetricKeyEncryptionDecryptionAssertion
 */
public class ServerSymmetricKeyEncryptionDecryptionAssertion extends AbstractServerAssertion<SymmetricKeyEncryptionDecryptionAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSymmetricKeyEncryptionDecryptionAssertion.class.getName());

    private final SymmetricKeyEncryptionDecryptionAssertion assertion;
    private final Auditor auditor;
    private final String[] variablesUsed;

    public ServerSymmetricKeyEncryptionDecryptionAssertion(SymmetricKeyEncryptionDecryptionAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.auditor = new Auditor(this, context, logger);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        Map<String, Object> vars = context.getVariableMap(this.variablesUsed, auditor);
        String toReturn = "";
        final String key = ExpandVariables.process(assertion.getKey(), vars, auditor, true);
        // This can be unset (and subsequently null), especially in unit tests!
        // (Like when a policy is pasted from XML into a gateway, and no UI is ever used)
        // Because this field is OPTIONAL, it shouldn't be treated as it's always present.
        final String iv = (assertion.getIv() != null ? ExpandVariables.process(assertion.getIv(), vars, auditor, true) : "");
        final String transformation = assertion.getAlgorithm();
        final String text = ExpandVariables.process(assertion.getText(), vars, auditor, true);
        final String pgpPassPhrase = (assertion.getPgpPassPhrase() != null ? ExpandVariables.process(assertion.getPgpPassPhrase(), vars, auditor, true) : "");
        final String variableName = assertion.getOutputVariableName();
        final boolean isEncrypt = assertion.getIsEncrypt();

        // Block of code to verify all required data has been submitted
        if (transformation == null || transformation.length() == 0) {
            logger.log(Level.WARNING, "Algorithm is not set");
            return AssertionStatus.FAILED;
        }
        if (text == null || text.length() == 0) {
            logger.log(Level.WARNING, "text is not set");
            return AssertionStatus.FAILED;
        }
        if (variableName == null || variableName.length() == 0) {
            logger.log(Level.WARNING, "variable is not set");
            return AssertionStatus.FAILED;
        }


        /*-----------------------------------------------------------*/

        /*Set things up: PREP START------------------------------ */
        //prep the input
        byte[] inputbytes = HexUtils.decodeBase64(text);

        // Lets make sure a proper input was base64 decoded.  In this case "proper" = inputbytes.length() > 0
        if (inputbytes == null || inputbytes.length <= 0) {
            logger.log(Level.WARNING, "Text is not properly set");
            return AssertionStatus.FAILED;
        }


        if (transformation.equals(SymmetricKeyEncryptionDecryptionAssertion.TRANS_PGP)) {
            byte[] output = new byte[0];
            InputStream is = new ByteArrayInputStream(inputbytes);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // first setup the pass phrase properly
            if (pgpPassPhrase == null || pgpPassPhrase.length() == 0) {
                logger.log(Level.WARNING, "PGP PassPhrase is not set");
                return AssertionStatus.FAILED;
            }

            byte[] pgpPassPhraseBytes = HexUtils.decodeBase64(pgpPassPhrase);
            char[] passwordCharArray = new String(pgpPassPhraseBytes).toCharArray();

            // Lets make sure a proper pgpPassPhraseBytes was base 64 decoded.  In this case "proper" = pgpPassPhraseBytes.length() > 0
            if (pgpPassPhraseBytes == null || pgpPassPhraseBytes.length <= 0) {
                logger.log(Level.WARNING, "PGP PassPhrase  is not properly set");
                return AssertionStatus.FAILED;
            }

            if (isEncrypt) {
                try {
                    PgpUtil.encrypt(is, out, "encrypt.pgp", new Date().getTime(), passwordCharArray, false, true);
                    output = out.toByteArray();
                    return this.doFinalCheckAndFinishCrypto(output, variableName, context);
                } catch (PgpUtil.PgpException e) {
                    logger.log(Level.WARNING, "Error encrypting text", ExceptionUtils.getDebugException(e));
                    return AssertionStatus.FAILED;
                }
            } else {
                try {
                    if (key != null && !key.equals(""))
                    {
                        // setup the key properly
                        byte[] keyBytes = HexUtils.decodeBase64(key);

                        // Lets make sure a proper key was base 64 decoded.  In this case "proper" = keyBytes.length() > 0
                        if (keyBytes == null || keyBytes.length <= 0) {
                            logger.log(Level.WARNING, "Key is not properly set");
                            return AssertionStatus.FAILED;
                        }
                        InputStream keyStream = new ByteArrayInputStream(keyBytes);

                        final PgpUtil.DecryptionMetadata metadata = PgpUtil.decrypt(is,keyStream,out,passwordCharArray);
                    }
                    else
                    {
                       final PgpUtil.DecryptionMetadata metadata = PgpUtil.decrypt(is,out,passwordCharArray);
                    }
                    output = out.toByteArray();
                    return this.doFinalCheckAndFinishCrypto(output, variableName, context);
                } catch (PgpUtil.PgpException e) {
                    logger.log(Level.WARNING, "Error decrypting text", ExceptionUtils.getDebugException(e));
                    return AssertionStatus.FAILED;
                }
            }
        } else {
            // assume the chunk of data before the first "/" of the algorithm portion of the Assertion class is the proper algorithm name
            String algorithmName = "";
            String blockMode = "";
            String[] temp;
            temp = transformation.split(SymmetricKeyEncryptionDecryptionAssertion.DEFAULT_TRANS_SEPERATOR);
            algorithmName = temp[0];
            // we hope and assume the "/" will always be used as the delimiter for the transformation
            // if not, the split method will return the entire string right back which will just end up being the algorithm Name
            if (temp.length > 1) {
                blockMode = temp[1];
            }
            /*Algorithm:
            1) generate the SecretKeyspec
            2) decide if we are going to encrypt or decrypt
            3) get the cipher object
            4) prep the input: does it need to be base 64 decoded
            5) If decrypting and the block mode is CBC, get the IV from the beginning portin of the cipher text.
                Depending on the encryption algorithm, the block size will differ
            6) finish the process
            7) base 64 encode
            done!*/
            // setup the key properly
            if (key == null || key.length() == 0) {
                logger.log(Level.WARNING, "Key is not set");
                return AssertionStatus.FAILED;
            }

            byte[] keyBytes = HexUtils.decodeBase64(key);

            // Lets make sure a proper key was base 64 decoded.  In this case "proper" = keyBytes.length() > 0
            if (keyBytes == null || keyBytes.length <= 0) {
                logger.log(Level.WARNING, "Key is not properly set");
                return AssertionStatus.FAILED;
            }
            SecretKeySpec skeySpec = new SecretKeySpec(keyBytes, algorithmName);

            // We don't check the IV value, if it's not present, it's not used.
            // Only checked on decryption, though...not used on encryption in this case.
            byte[] ivBytes = null;
            if ( (iv != null && iv.length() > 0) && (isEncrypt != true)) {
                // We have an IV value to use.
                ivBytes = HexUtils.decodeBase64(iv);
                if (ivBytes == null || ivBytes.length <= 0) {
                    logger.log(Level.WARNING, "IV was specified, but wasn't properly decoded from base64.");
                    return AssertionStatus.FAILED;
                }
            }

            // initialize the cipher for encrypt mode
            Cipher cipher = null;
            int mode;
            try {
                // get the proper Cipher
                cipher = JceProvider.getCipher(transformation, null);

                /* ----------------------- PREP OVER -------------------*/

                byte[] output = new byte[0];
                if (isEncrypt) {
                    output = encrypt(cipher, skeySpec, inputbytes, algorithmName, blockMode);
                } else {
                    output = decrypt(cipher, skeySpec, inputbytes, algorithmName, blockMode, ivBytes);
                }

                return this.doFinalCheckAndFinishCrypto(output, variableName, context);

            } catch (NoSuchAlgorithmException
                    nsaE) {
                logger.log(Level.WARNING, "Missing transformation: " + transformation, ExceptionUtils.getDebugException(nsaE));
                return AssertionStatus.FAILED;
            } catch (NoSuchPaddingException
                    nspE) {
                logger.log(Level.WARNING, "There is no padding specified in the transformation: " + transformation, ExceptionUtils.getDebugException(nspE));
                return AssertionStatus.FAILED;
            } catch (InvalidKeyException
                    ikE) {
                logger.log(Level.WARNING, "Problem with the key specified", ExceptionUtils.getDebugException(ikE));
                return AssertionStatus.FAILED;
            } catch (BadPaddingException
                    bpE) {
                logger.log(Level.WARNING, "There is a problem with the padding specified in the transformation: " + transformation, ExceptionUtils.getDebugException(bpE));
                return AssertionStatus.FAILED;
            } catch (IllegalBlockSizeException
                    ibsE) {
                logger.log(Level.WARNING, "There is a problem with the block size.  Please monitor the transformation: " + transformation, ExceptionUtils.getDebugException(ibsE));
                return AssertionStatus.FAILED;
            } catch (InvalidAlgorithmParameterException
                    e) {
                logger.log(Level.WARNING, "There is a problem with the parameters passed into the encryption process.  Please monitor the transformation: " + transformation, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            } catch (ShortBufferException e) {
                logger.log(Level.WARNING, "There is a problem with the Encryption/Decryption process.  The internal buffer has run out of space.", ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            }
        }

    }

    /*
    * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
    * that would otherwise keep our instances from getting collected.
    */

    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerSymmetricKeyEncryptionDecryptionAssertion is preparing itself to be unloaded");
    }

    private int getProperBlockSize(String algorithmName) {
        int toReturn = 0;

        if (algorithmName.equalsIgnoreCase(SymmetricKeyEncryptionDecryptionAssertion.ALGORITHM_AES)) {
            // if its AES, then the IV has to be 16 blocks
            toReturn = 16;
        } else if (algorithmName.equalsIgnoreCase(SymmetricKeyEncryptionDecryptionAssertion.ALGORITHM_DES)
                || algorithmName.equalsIgnoreCase(SymmetricKeyEncryptionDecryptionAssertion.ALGORITHM_TRIPLE_DES)) {
            toReturn = 8;
        }
        // else stick with zero which means it will probably fail.  Not sure if this scenario will ever be reached as AES,
        // DES, tripleDES is what this assertions is designed for

        return toReturn;
    }


    /*
    * The only supported Block Mode right now is CBC.  If this method returns a byte array with size 0, its because it ran into a block mode problem.
    * Namely an incorrect block mode (ie: not CBC) is being used.
    */
    private byte[] encrypt(Cipher cipher, SecretKeySpec skeySpec, byte[] inputbytes, String algorithmName, String blockMode) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, ShortBufferException, BadPaddingException {
        byte[] toReturn = new byte[0];

        if (inputbytes == null) {
            return toReturn;
        }

        int ivBytesSize = getProperBlockSize(algorithmName);

        if (blockMode.equals(SymmetricKeyEncryptionDecryptionAssertion.BLOCK_MODE_CBC) && inputbytes.length > 0 && ivBytesSize > 0) {
            // CBC means we need an IV
            byte[] ivBytes = new byte[ivBytesSize];
            byte[] interimOutput;

            // do the proper checks for the IV.
            // Most importantly, generate it and then feed it into the beginning of the cipher.
            // this will be slurped up later by CBC version of the algorithm.
            // generate it using SecureRandom.
            SecureRandom random = JceProvider.getInstance().getSecureRandom();
            random.nextBytes(ivBytes);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            interimOutput = cipher.doFinal(inputbytes);
            // Lets make sure interimOutput actually contains something.
            if (interimOutput == null || interimOutput.length <= 0) {
                return toReturn;
            }
            //now we will append IV to the beginning of the byte[] that we will actually return
            toReturn = new byte[interimOutput.length + ivBytesSize];
            System.arraycopy(ivBytes, 0, toReturn, 0, ivBytesSize); // ivyBytes is ivByteSize big and toReturn will be atleast ivBytesSize big
            System.arraycopy(interimOutput, 0, toReturn, ivBytesSize, interimOutput.length); //now toReturn is ready :), toReturn is atleast ivByteSize + interimOutput.length big.
        }

        return toReturn;
    }

    /*
    * the decrypt method will decrypt based on the information given to it.
    * Currently it is only developed to work with CBC block mode.
    * If using anything else, it will return a byte array of size 0.
    *
    * If anything goes wrong, a byte array of size 0 will be returned
    */
    private byte[] decrypt(Cipher cipher, SecretKeySpec skeySpec, byte[] inputbytes, String algorithmName, String blockMode, byte[] passedIvBytes) throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, ShortBufferException, BadPaddingException {
        byte[] toReturn = new byte[0];

        if (inputbytes == null) {
            return toReturn;
        }

        int ivBytesSize = getProperBlockSize(algorithmName);
        int sizeOfTextToDecrypt = inputbytes.length - ivBytesSize;
        boolean fetchIvFromBytes = true;

        // Check if ivBytes is the proper length compared to ivBytesSize for the algorithm
        if (passedIvBytes != null) {
            if (passedIvBytes.length != ivBytesSize) {
                logger.log(Level.WARNING, "The user-declared IV is the incorrect size for the selected algorithm.");
                return toReturn;
            }

            // It is a proper IV.  Guess this means we need to pre-pend it to the passed data to decrypt.
            sizeOfTextToDecrypt = inputbytes.length;
            fetchIvFromBytes = false;
        }

        // We will only try to decrypt if the block mode is CBC, the IV size is greater than 0, the input size is greater than 0, and the difference between the IV size and the input size is greater than zero (the amount of text to decrypt)
        if (blockMode.equals(SymmetricKeyEncryptionDecryptionAssertion.BLOCK_MODE_CBC)
                && ivBytesSize > 0 && inputbytes.length > 0 && sizeOfTextToDecrypt > 0) {

            // CBC means we need an IV
            byte[] ivBytes = null;
            byte[] intermedBytes= null;
            IvParameterSpec ivSpec;

            if (fetchIvFromBytes) {
                ivBytes = new byte[ivBytesSize];
                System.arraycopy(inputbytes, 0, ivBytes, 0, ivBytesSize);
                ivSpec = new IvParameterSpec(ivBytes);
            } else {
                ivSpec = new IvParameterSpec(passedIvBytes);
            }

            // remove the IV if not already specified...
            intermedBytes = new byte[sizeOfTextToDecrypt];
            System.arraycopy(inputbytes, (fetchIvFromBytes ? ivBytesSize : 0), intermedBytes, 0, sizeOfTextToDecrypt);

            // run the decrypt
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            toReturn = cipher.doFinal(intermedBytes);
        }

        return toReturn;
    }


    private AssertionStatus doFinalCheckAndFinishCrypto(byte[] toOutput, String variableName, PolicyEnforcementContext context) {
        String toReturn = "";

        if (toOutput.length <= 0) {
            logger.log(Level.WARNING, "Encryption/Decryption failed most likely due to improper block mode.  Failing Assertion now.");
            return AssertionStatus.FAILED;
        }
        toReturn = HexUtils.encodeBase64(toOutput);
        context.setVariable(variableName, toReturn);
        return AssertionStatus.NONE;
    }

    /*
   a helper method used to output the JCE providers on this instance of the gateway.
    */
    private void outputJCEProviders() {
        Provider[] provs = Security.getProviders();

        StringBuilder toOutput = new StringBuilder();
        toOutput.append("Security Providers: \n");

        for (Provider prov : provs) {
            toOutput.append("\nProvider: " + prov.getName() + " (" + prov + ")");
            List<Provider.Service> servs = new ArrayList<Provider.Service>(prov.getServices());
            Collections.sort(servs, new Comparator<Provider.Service>() {
                @Override
                public int compare(Provider.Service a, Provider.Service b) {
                    int r = a.getType().compareTo(b.getType());
                    return r != 0 ? r : a.getAlgorithm().compareTo(b.getAlgorithm());
                }
            });

            for (Provider.Service serv : servs) {
                toOutput.append("   " + serv.getType() + "\t" + serv.getAlgorithm() + "\t" + serv.getClassName() + "\n");
            }
        }

        logger.log(Level.WARNING, toOutput.toString());
    }

}

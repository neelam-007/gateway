package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature;

import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.JwsNone;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.hmac.JwsHs256;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.rsa.JwsRs256;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public abstract class JsonWebSignature implements Comparable {

    // Use this to set the signature name of the class/object.
    // Used to name the object and register it in the registry, so it needs to match the JWS type.
    protected static Logger logger;
    protected String jwsSignatureType;
    protected String jwsSignatureDescription;
    protected String javaAlgorithm;

    protected byte availableSignatureOptions;

    /*
      There needs to be a way to make this a "plug-in" type, where new signature algorithms can be specified.
      They all have one thing in common - they all need data to sign, and a key to sign it against.
    */
    private static TreeMap<String, JsonWebSignature> supportedAlgorithms = new TreeMap<String, JsonWebSignature>() {{
        // Add supported signature classes here.
        //  Algorithm name constant, new instance of algorithm
        put(JwsNone.jwsAlgorithmName, new JwsNone());
        put(JwsHs256.jwsAlgorithmName, new JwsHs256());
//        put(JwsHs384.jwsAlgorithmName, new JwsHs384());
//        put(JwsHs512.jwsAlgorithmName, new JwsHs512());
        put(JwsRs256.jwsAlgorithmName, new JwsRs256());
//        put(JwsRs384.jwsAlgorithmName, new JwsRs384());
//        put(JwsRs512.jwsAlgorithmName, new JwsRs512());
//  Elliptical Curve Algorithms
//        put(JwsEs256.jwsAlgorithmName, new JwsEs256());
//        put(JwsEs384.jwsAlgorithmName, new JwsEs384());
//        put(JwsEs512.jwsAlgorithmName, new JwsEs512());
    }};

    public static JsonWebSignature getAlgorithm(String jwsSignatureType) throws IllegalJwtSignatureException {
        if (jwsSignatureType == null) {
            throw new IllegalJwtSignatureException("A signature type must be specified");
        }
        if (supportedAlgorithms.containsKey(jwsSignatureType)) {
            return supportedAlgorithms.get(jwsSignatureType);
        }

        throw new IllegalJwtSignatureException("The requested signature algorithm (" + jwsSignatureType + ") is not available");
    }

    public static TreeSet<JsonWebSignature> getRegisteredSignatureTypes() {
        return new TreeSet<>(supportedAlgorithms.values());
    }

    /**
     * This constructor needs to be copied to each derived child from this class, and
     * its details filled in.
     */
    public JsonWebSignature(String algorithmName, String description, String javaAlgorithm, Logger theLogger, int availableSignatureOptions) {
        this.jwsSignatureType = algorithmName;
        this.jwsSignatureDescription = description;
        this.javaAlgorithm = javaAlgorithm;
        logger = theLogger;
        this.availableSignatureOptions = (byte) availableSignatureOptions;
    }

    public final String getAlgorithmName() {
        return jwsSignatureType;
    }

    /**
     * This should output the JWSSignatureType and JWSSignatureDescription in the following format:
     * [type]  ([description])
     * There should be two spaces between type and description.
     * This is used for the Algorithm Drop down UI.
     */
    public final String toString() {
        return jwsSignatureType + "   (" + jwsSignatureDescription + ")";
    }

    // These are used for the UI.  These need to be overridden if they ARE used by the signature type to return true.
    public final byte getUIAvailableSecrets() {
        return availableSignatureOptions;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof JsonWebSignature) {
            return jwsSignatureType.compareTo(((JsonWebSignature) o).getAlgorithmName());   // Sort naturally.
        }

        return 1;   // Place last.
    }

    protected byte[] createSecuredInput(byte[] header, byte[] payload) {

        byte separator[] = ".".getBytes();
        byte jwsSecuredInput[] = new byte[header.length + separator.length + payload.length];

        System.arraycopy(header, 0, jwsSecuredInput, 0, header.length);
        System.arraycopy(separator, 0, jwsSecuredInput, header.length, separator.length);
        System.arraycopy(payload, 0, jwsSecuredInput, header.length + separator.length, payload.length);


        return jwsSecuredInput;
    }

}

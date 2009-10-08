package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.SignatureMethod;
import com.l7tech.util.ExceptionUtils;
import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;

/**
 *
 */
class EcdsaSignatureMethod extends SignatureMethod {
    private final Signature signature;
    private final String uri;

    public EcdsaSignatureMethod(String sigMethod, String uri, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        signature = provider != null ? Signature.getInstance(sigMethod, provider) : Signature.getInstance(sigMethod);
        this.uri = uri;
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public void initSign(Key privateKey) throws InvalidKeyException {
        signature.initSign((PrivateKey)privateKey);
    }

    @Override
    public void initVerify(Key publicKey) throws InvalidKeyException {
        signature.initVerify((PublicKey)publicKey);
    }

    @Override
    public void update(byte data[]) throws SignatureException {
        signature.update(data);
    }

    @Override
    public byte[] sign() throws SignatureException {
        return decodeAsn1(signature.sign());
    }

    @Override
    public boolean verify(byte signature[]) throws SignatureException {
        return this.signature.verify(encodeAsn1(signature));
    }

    static byte[] toRawOctetString(DERInteger i) {
        byte[] rb = i.getPositiveValue().toByteArray();

        if (rb[0] != 0)
            return rb;

        // Skip first octet
        byte[] ret = new byte[rb.length - 1];
        System.arraycopy(rb, 1, ret, 0, ret.length);
        return ret;
    }

    /**
     * Convert an ASN.1 DER SEQUENCE of two INTEGER values into a byte array containing the concatenation
     * of the two raw integer octet strings (with any leading nul octets omitted).
     *
     * @param asn an EC signature value in the form of the ASN.1 DER SEQUENCE of two tagged INTEGER values
     *            for the (r,s) values respectively.
     * @return the raw (r,s) value octet strings concatenated together.
     */
    static byte[] decodeAsn1(byte[] asn) {
        if (asn.length < 40)
            throw new IllegalArgumentException("Encoded signature value is too small; only " + asn.length + " byte");
        if (asn.length > 4096)
            throw new IllegalArgumentException("Encoded signature value is too large; " + asn.length + " byte");
        try {
            ASN1Sequence seq = (ASN1Sequence)ASN1Object.fromByteArray(asn);
            if (seq.size() != 2)
                throw new IllegalArgumentException("r,s sequence did not contain exactly two elements; count = " + seq.size());
            DERInteger r = (DERInteger)seq.getObjectAt(0);
            DERInteger s = (DERInteger)seq.getObjectAt(1);

            byte[] rb = toRawOctetString(r);
            byte[] sb = toRawOctetString(s);

            byte[] out = new byte[rb.length + sb.length];
            System.arraycopy(rb, 0, out, 0, rb.length);
            System.arraycopy(sb, 0, out, rb.length, sb.length);
            return out;

        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid encoded signature value: " + ExceptionUtils.getMessage(e), e);
        }
    }

    static byte[] encodeAsn1(byte[] rs) {
        if (rs.length < 40)
            throw new IllegalArgumentException("Raw signature value is too small; only " + rs.length + " byte");
        if (rs.length > 4096)
            throw new IllegalArgumentException("Raw signature value is too large; " + rs.length + " byte");
        int half = rs.length / 2;
        byte[] rb = new byte[half];
        byte[] sb = new byte[half];
        System.arraycopy(rs, 0, rb, 0, half);
        System.arraycopy(rs, half, sb, 0, half);
        DERInteger r = new DERInteger(new BigInteger(1, rb));
        DERInteger s = new DERInteger(new BigInteger(1, sb));

        DERSequence seq = new DERSequence(new ASN1Encodable[] { r, s });

        try {
            return seq.getEncoded();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.SignatureMethod;
import com.l7tech.util.ExceptionUtils;
import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;

/**
 * A SignatureMethod for ECDSA signatures that when signing converts the Java Signature.FOOwithECDSA signature value,
 * which is ASN.1 encoded, back into the raw SignatureValue required by xmldsig; and when verifying converts
 * the raw signature value into ASN.1 before passing it to the Java Signature.FOOwithECDSA to verify.
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

    static byte[] encodeAsn1(byte[] rs) throws SignatureException {
        if (rs.length < 40)
            throw new SignatureException("Invalid ECDSA SignatureValue: it is only " + rs.length + " bytes long");
        if (rs.length % 2 != 0)
            throw new SignatureException("Invalid ECDSA SignatureValue: it is an odd number of bytes long");

        int sidx = rs.length / 2;
        byte[] rb = new byte[sidx];
        byte[] sb = new byte[sidx];
        System.arraycopy(rs, 0, rb, 0, sidx);
        System.arraycopy(rs, sidx, sb, 0, sidx);

        DERInteger r = new DERInteger(new BigInteger(1, rb));
        DERInteger s = new DERInteger(new BigInteger(1, sb));
        DERSequence seq = new DERSequence(new ASN1Encodable[] { r, s });
        try {
            return seq.getEncoded(ASN1Encodable.DER);
        } catch (IOException e) {
            throw new SignatureException("Invalid ECDSA SignatureValue: Unable to encode (r,s) pair as DER: " + ExceptionUtils.getMessage(e), e);
        }
    }

    static byte[] getIntegerOctetStringFromSequence(ASN1Sequence seq, int index) throws SignatureException {
        Object obj = seq.getObjectAt(index);
        if (obj instanceof DERInteger) {
            DERInteger integer = (DERInteger) obj;
            return integer.getPositiveValue().toByteArray();
        } else {
            throw new SignatureException("Internal error generating ECDSA signature: Unable to decode (r,s) pair from DER: sequence element was not a DERInteger");
        }
    }

    static byte[] decodeAsn1(byte[] asn1) throws SignatureException {
        try {
            ASN1Object got = ASN1Sequence.fromByteArray(asn1);
            if (got instanceof ASN1Sequence) {
                ASN1Sequence seq = (ASN1Sequence) got;
                if (seq.size() != 2)
                    throw new SignatureException("Internal error generating ECDSA signature: Unable to decode (r,s) pair from DER: sequence was not of two elements");
                byte[] rb = getIntegerOctetStringFromSequence(seq, 0);
                byte[] sb = getIntegerOctetStringFromSequence(seq, 1);

                // Skip over leading zeroes
                int rlen = rb.length;
                while (rlen > 0 && rb[rb.length - rlen] == 0)
                        rlen--;

                int slen = sb.length;
                while (slen > 0 && sb[sb.length - slen] == 0)
                        slen--;

                // Find output size as multiple of 8 bytes
                int rblen = ((rlen + 7) / 8) * 8;
                int sblen = ((slen + 7) / 8) * 8;
                int outlen = rblen > sblen ? rblen : sblen;

                byte[] out = new byte[outlen * 2];
                System.arraycopy(rb, rb.length - rlen, out, outlen - rlen, rlen);
                System.arraycopy(sb, sb.length - slen, out, outlen + (outlen - slen), slen);
                return out;
                
            } else {
                throw new SignatureException("Internal error generating ECDSA signature: Unable to decode (r,s) pair from DER: result was not a sequence");
            }
        } catch (IOException e) {
            throw new SignatureException("Internal error generating ECDSA signature: Unable to decode (r,s) pair from DER: " + ExceptionUtils.getMessage(e), e);
        }
    }
}

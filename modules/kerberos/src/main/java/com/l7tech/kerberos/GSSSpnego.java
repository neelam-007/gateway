package com.l7tech.kerberos;

import java.io.IOException;
import java.util.logging.Logger;

import sun.security.util.DerValue;
import sun.security.util.DerInputStream;
import sun.security.util.ObjectIdentifier;

/**
 * TODO replace use of the sun ASN.1 classes with the apache harmony ones.
 *
 * Utility class for unwrapping SPNEGO tokens.
 *
 *   3.  DATA ELEMENTS
 *
 *   3.1.  Mechanism Type
 *
 *      MechType::= OBJECT IDENTIFIER
 *
 *      mechType
 *           Each security mechanism is as defined in [1].
 *
 *   3.2.  Negotiation Tokens
 *
 *      The syntax of the negotiation tokens follows the InitialContextToken
 *      syntax defined in [1]. The security mechanism of the initial
 *      negotiation token is identified by the Object Identifier
 *      iso.org.dod.internet.security.mechanism.snego (1.3.6.1.5.5.2).
 *
 *   3.2.1. Syntax
 *
 *      This section specifies the syntax of the corresponding
 *      "innerContextToken" field for the first token and subsequent
 *      negotiation tokens. During the mechanism negociation, the
 *      "innerContextToken" field contains the ASN.1 structure
 *      "NegociationToken" given below, encoded using the DER encoding
 *      conventions.
 *
 *   NegotiationToken ::= CHOICE {
 *                                 negTokenInit  [0]  NegTokenInit,
 *                                 negTokenTarg  [1]  NegTokenTarg }
 *
 *   MechTypeList ::= SEQUENCE OF MechType
 *
 *   NegTokenInit ::= SEQUENCE {
 *                               mechTypes       [0] MechTypeList  OPTIONAL,
 *                               reqFlags        [1] ContextFlags  OPTIONAL,
 *                               mechToken       [2] OCTET STRING  OPTIONAL,
 *                               mechListMIC     [3] OCTET STRING  OPTIONAL
 *                            }
 *
 *   ContextFlags ::= BIT STRING {
 *           delegFlag       (0),
 *           mutualFlag      (1),
 *           replayFlag      (2),
 *           sequenceFlag    (3),
 *           anonFlag        (4),
 *           confFlag        (5),
 *           integFlag       (6)
 *   }
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
class GSSSpnego {

    /**
     *
     */
    private static final Logger logger = Logger.getLogger(GSSSpnego.class.getName());

    /**
     * The Mechanism OID for SPNEGO
     */
    private static final String OID_SPNEGO = "1.3.6.1.5.5.2";

    /**
     * If the given token is SPNEGO then unwrap it, else return the original token.
     */
    static byte[] removeSpnegoWrapper(byte[] possiblySpnegoToken) {
        byte[] resultToken = null;

        ObjectIdentifier oid = null;
        DerInputStream dis = null;

        try {
            DerValue dv = new DerValue(possiblySpnegoToken);
            if (dv.isConstructed() && dv.isApplication() && (0==(dv.getTag()&31))) {
                dis = new DerInputStream(dv.getDataBytes());
                oid = dis.getOID();
            }
        }
        catch(IOException ioe) {
            logger.info("Error parsing token when checking for SPNEGO '"+ioe.getMessage()+"'.");
        }

        if (oid != null && OID_SPNEGO.equals(oid.toString())) {
            try {
                DerValue negotationTokenDv = dis.getDerValue();
                if (negotationTokenDv.isContextSpecific((byte)0) &&
                    negotationTokenDv.isConstructed()) {
                    DerInputStream negTokenInitSeqStr = new DerInputStream(negotationTokenDv.getDataBytes());

                    if ((negTokenInitSeqStr.peekByte() & 63)==48) {
                        DerValue[] negTokenInitDerValues = negTokenInitSeqStr.getSequence(4);

                        for (int i=0; i<negTokenInitDerValues.length; i++) {
                            DerValue contextualDerValue = negTokenInitDerValues[i];
                            if (contextualDerValue.isContextSpecific((byte)2)) {
                                resultToken = contextualDerValue.data.getOctetString();
                            }
                        }
                    }
                }
            }
            catch(IOException ioe) {
                logger.info("Error parsing SPNEGO token '"+ioe.getMessage()+"'.");
            }
        }

        if (resultToken == null) {
            // then the token is not SPNEGO
            resultToken = possiblySpnegoToken;
        }

        return resultToken;
    }

}

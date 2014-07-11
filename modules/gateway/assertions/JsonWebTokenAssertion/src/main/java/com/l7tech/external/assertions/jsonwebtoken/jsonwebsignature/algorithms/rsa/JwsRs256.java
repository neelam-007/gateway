package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.rsa;

import java.util.logging.Logger;

/**
 * User: rseminoff
 * Date: 12/12/12
 */
public class JwsRs256 extends JwsRsa {

    public static final String jwsAlgorithmName = "RS256";

    public JwsRs256() {
        super(JwsRs256.jwsAlgorithmName, "RSA SHA-256", "SHA256withRSA", Logger.getLogger(JwsRs256.class.getName()));
    }

}

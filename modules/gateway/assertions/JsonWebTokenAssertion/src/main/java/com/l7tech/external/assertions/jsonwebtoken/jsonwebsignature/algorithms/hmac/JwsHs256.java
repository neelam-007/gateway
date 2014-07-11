package com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.algorithms.hmac;

import java.util.logging.Logger;

/**
 * User: rseminoff
 * Date: 30/11/12
 */
public class JwsHs256 extends JwsHmac {

    public static final String jwsAlgorithmName = "HS256";

    public JwsHs256() {
        super(JwsHs256.jwsAlgorithmName, "HMAC SHA-256", "HMACSHA256", Logger.getLogger(JwsHs256.class.getName()));
    }

}

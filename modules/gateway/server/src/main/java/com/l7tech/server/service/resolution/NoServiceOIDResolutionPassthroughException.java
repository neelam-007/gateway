package com.l7tech.server.service.resolution;

/**
 * This exception is thrown by the OriginalUrlServiceOidResolver when
 * the request did not contain any serviceOID pattern but the resolver is
 * about to return the incumbant set of service of size 1 suggesting that
 * resolution is complete. This aberration is necessary to handle the
 * special case where only one service is published on the gateway.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 13, 2006<br/>
 */
public class NoServiceOIDResolutionPassthroughException extends ServiceResolutionException {
    public NoServiceOIDResolutionPassthroughException() {
        super("The request did not contain any service oid patterns");
    }
}

package com.l7tech.external.assertions.rawtcp;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;

/**
 * User: wlui
 */
@Secured
public interface SimpleRawTransportAdmin {
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    public long getDefaultResponseSizeLimit();
}

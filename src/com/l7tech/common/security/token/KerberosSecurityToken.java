/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.token;

import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;

/**
 * Holds a Kerberos ticket, and hopefully more later.
 *
 * TODO support for signing / principal information
 */
public interface KerberosSecurityToken extends XmlSecurityToken, SigningSecurityToken {
    public KerberosGSSAPReqTicket getTicket();
}

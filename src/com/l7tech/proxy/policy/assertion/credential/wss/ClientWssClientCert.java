/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.proxy.policy.assertion.xmlsec.ClientXmlDsigReqAssertion;

/**
 * todo, remove this class and patch corresponding UI to use ClientXmlDsigReqAssertion instead
 *
 * $Id$
 * @author flascell
 * @version $Revision$
 */
public class ClientWssClientCert extends ClientXmlDsigReqAssertion {
    public ClientWssClientCert( WssClientCert data ) {
        super(null);
    }
}

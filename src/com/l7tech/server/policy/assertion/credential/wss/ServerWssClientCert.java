/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.server.policy.assertion.xmlsec.ServerXmlDsigReqAssertion;

/**
 * This has moved to ServerXmlDsigReqAssertion
 * todo, removed this class and adjust the console accordingly
 *
 * @author alex, flascell
 */
public class ServerWssClientCert extends ServerXmlDsigReqAssertion{
    public ServerWssClientCert( WssClientCert data ) {
        super(null);
    }
}

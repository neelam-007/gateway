package com.l7tech.server.saml;

import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;

/**
 * @author emil
 * @version 4-Aug-2004
 */
public interface Authority {
    ResponseDocument process(RequestDocument request) throws SamlException;
}

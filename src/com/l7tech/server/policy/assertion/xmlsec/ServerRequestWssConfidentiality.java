package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ParsedElement;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;

import java.util.logging.Logger;

/**
 * Enforces that a specific element in a request is encrypted.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class ServerRequestWssConfidentiality extends ServerRequestWssOperation {
    private static final Logger logger = Logger.getLogger(ServerRequestWssConfidentiality.class.getName());

    public ServerRequestWssConfidentiality(RequestWssConfidentiality data) {
        super(logger, data);
    }

    protected String getPastTenseOperationName() {
        return "encrypted";
    }

    protected ParsedElement[] getElementsFoundByProcessor(ProcessorResult wssResults) {
        return wssResults.getElementsThatWereEncrypted();
    }

    protected boolean isAllowIfEmpty() {
        return true;
    }
}

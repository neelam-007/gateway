package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;

import java.util.logging.Logger;

/**
 * Enforces that a specific element in a request is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class ServerRequestWssIntegrity extends ServerRequestWssOperation {
    private static final Logger logger = Logger.getLogger(ServerRequestWssIntegrity.class.getName());

    public ServerRequestWssIntegrity(RequestWssIntegrity data) {
        super(logger, data);
    }

    protected String getPastTenseOperationName() {
        return "signed";
    }

    protected WssProcessor.ParsedElement[] getElementsFoundByProcessor(WssProcessor.ProcessorResult wssResults) {
        return wssResults.getElementsThatWereSigned();
    }

    protected boolean isAllowIfEmpty() {
        return false;
    }
}

package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.token.ParsedElement;
import com.l7tech.common.security.token.EncryptedElement;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;
import java.util.Collection;
import java.util.ArrayList;

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

    public ServerRequestWssConfidentiality(RequestWssConfidentiality data, ApplicationContext springContext) {
        super(logger, data, springContext);
    }

    protected String getPastTenseOperationName() {
        return "encrypted";
    }

    protected ParsedElement[] getElementsFoundByProcessor(ProcessorResult wssResults) {
        EncryptedElement[] elementsThatWereEncrypted = wssResults.getElementsThatWereEncrypted();
        if (elementsThatWereEncrypted.length == 0) {
            return elementsThatWereEncrypted;
        }
        RequestWssConfidentiality rwss = (RequestWssConfidentiality)data;
        String xencAlgorithm = rwss.getXEncAlgorithm();
        Collection elementsToReturn = new ArrayList();
        for (int i = elementsThatWereEncrypted.length - 1; i >= 0; i--) {
            EncryptedElement encryptedElement = elementsThatWereEncrypted[i];
            if (encryptedElement.getAlgorithm().equals(xencAlgorithm)) {
                elementsToReturn.add(encryptedElement);
            }
        }
        return (ParsedElement[])elementsToReturn.toArray(new ParsedElement[] {});
    }

    protected boolean isAllowIfEmpty() {
        return true;
    }
}

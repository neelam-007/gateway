package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement;
import com.l7tech.security.token.EncryptedElement;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.xml.processor.ProcessorResult;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Enforces that a specific element in a request is encrypted.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class ServerRequireWssEncryptedElement extends ServerRequireWssOperation<RequireWssEncryptedElement> {
    private static final Logger logger = Logger.getLogger(ServerRequireWssEncryptedElement.class.getName());

    public ServerRequireWssEncryptedElement(RequireWssEncryptedElement data, ApplicationContext springContext) {
        super(logger, data, springContext);
    }

    @Override
    protected String getPastTenseOperationName() {
        return "encrypted";
    }

    @Override
    protected ParsedElement[] getElementsFoundByProcessor(ProcessorResult wssResults) {
        if (wssResults == null) return new ParsedElement[0];
        EncryptedElement[] elementsThatWereEncrypted = wssResults.getElementsThatWereEncrypted();
        if (elementsThatWereEncrypted.length == 0) {
            return elementsThatWereEncrypted;
        }
        RequireWssEncryptedElement rwss = assertion;
        List<String> algList = rwss.getXEncAlgorithmList();
        if (algList == null) {
            algList = Collections.singletonList(rwss.getXEncAlgorithm());
        }
        Collection<ParsedElement> elementsToReturn = new ArrayList<ParsedElement>();
        for (int i = elementsThatWereEncrypted.length - 1; i >= 0; i--) {
            EncryptedElement encryptedElement = elementsThatWereEncrypted[i];

            if (algList.contains(encryptedElement.getAlgorithm())) {
                elementsToReturn.add(encryptedElement);
            }
        }
        return elementsToReturn.toArray(new ParsedElement[elementsToReturn.size()]);
    }

    @Override
    protected boolean isAllowIfEmpty() {
        return true;
    }
}

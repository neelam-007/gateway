package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import org.w3c.dom.Document;

/**
 * @author: vchan
 */
public abstract class EvidenceBlockResolver extends MessageValueResolver<Document> {


    protected EvidenceBlockResolver(final SamlProtocolAssertion assertion) {
        super(assertion);
    }
}
package com.l7tech.common.security.xml;

import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;

/**
 * An implementation of the WssProcessor for use in both the SSG and the SSA.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 5, 2004<br/>
 * $Id$<br/>
 */
public class WssProcessorImpl implements WssProcessor {
    public WssProcessor.ProcessorResult undecorateMessage(Document soapMsg,
                                                          X509Certificate recipientCert,
                                                          PrivateKey recipientKey) throws WssProcessor.ProcessorException {
        // todo
        return null;
    }
}

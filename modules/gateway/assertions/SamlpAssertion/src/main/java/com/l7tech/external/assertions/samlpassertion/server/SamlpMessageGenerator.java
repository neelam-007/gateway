package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;

import javax.xml.bind.JAXBElement;

/**
 * Interface definition for SAMLP request message generator.  Multiple implementations
 * of the generator will be needed for different version of SAML supported.
 *
 * Currently, the SSG will need support both versions: SAML 1.x and 2.x
 *
 * @author: vchan
 */
public interface SamlpMessageGenerator<ASSN extends SamlProtocolAssertion, SAMLP_MSG> {

    /**
     * Creates an instance of the SAMLP Request type <code>REQ</code>
     * as a DOM document populated with the assertion
     *
     * @param assertion the configured assertion properties
     * @return the SAMLP AuthnRequest message
     */
    public SAMLP_MSG create(ASSN assertion);


    /**
     * Returns a JAXBElement for the SAMLP message instance argument.
     *
     * @param samlpMsg the SAMLP message instance to wrap into a JAXBElement
     * @return a JAXBElement instance for the supplied SAMLP message
     */
    public JAXBElement<SAMLP_MSG> createJAXBElement(SAMLP_MSG samlpMsg);

}

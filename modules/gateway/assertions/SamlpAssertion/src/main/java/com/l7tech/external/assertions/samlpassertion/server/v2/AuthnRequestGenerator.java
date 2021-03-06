package com.l7tech.external.assertions.samlpassertion.server.v2;

import com.l7tech.external.assertions.samlpassertion.server.NameIdentifierResolver;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp2MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.SamlpAssertionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saml.v2.protocol.AuthnRequestType;

import javax.xml.bind.JAXBElement;
import java.util.Map;

/**
 * User: vchan
 */
public class AuthnRequestGenerator extends AbstractSamlp2MessageGenerator<AuthnRequestType> {

    public AuthnRequestGenerator(@NotNull final Map<String, Object> variablesMap,
                                 @NotNull final Audit auditor,
                                 @Nullable final NameIdentifierResolver issuerNameResolver)
        throws SamlpAssertionException
    {
        super(variablesMap, auditor, issuerNameResolver);
    }

    @Override
    protected AuthnRequestType createMessageInstance() {
        return samlpFactory.createAuthnRequestType();
    }

    @Override
    protected void buildSpecificMessageParts() {

        // build subject - common for all SAMLP requests
        samlpMessage.setSubject( buildSubject() );

    }

    @Override
    public JAXBElement<AuthnRequestType> createJAXBElement(AuthnRequestType samlpMsg) {
        return samlpFactory.createAuthnRequest(samlpMsg);
    }
}

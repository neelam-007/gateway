package com.l7tech.policy.assertion;

import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.xml.KeyInfoInclusionType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Interface for configuring common SAML elements which can contain common SAML elements / attribute such as
 * Issuer and Subject, including subject confirmations.
 *
 * Note: This interface could be split out further.
 *
 * @author jbufu
 */
public interface SamlElementGenericConfig extends SamlIssuerConfig, SamlVersionConfig {

    boolean isSignAssertion();
    void setSignAssertion(boolean selected);

    //todo Move decoration types out of this interface
    EnumSet<DecorationType> getDecorationTypes();
    void setDecorationTypes(EnumSet<DecorationType> decorationTypes);

    String getSubjectConfirmationMethodUri();
    void setSubjectConfirmationMethodUri(String uri);

    KeyInfoInclusionType getSubjectConfirmationKeyInfoType();
    void setSubjectConfirmationKeyInfoType(KeyInfoInclusionType cert);

    NameIdentifierInclusionType getNameIdentifierType();
    void setNameIdentifierType(NameIdentifierInclusionType fromCreds);

    String getNameIdentifierValue();
    void setNameIdentifierValue(@Nullable String text);

    String getNameIdentifierFormat();
    void setNameIdentifierFormat(@Nullable String s);

    int getConditionsNotBeforeSecondsInPast();
    void setConditionsNotBeforeSecondsInPast(int seconds);

    int getConditionsNotOnOrAfterExpirySeconds();
    void setConditionsNotOnOrAfterExpirySeconds(int seconds);

    String getSubjectConfirmationDataRecipient();
    void setSubjectConfirmationDataRecipient(String recipient);

    String getSubjectConfirmationDataAddress();
    void setSubjectConfirmationDataAddress(String address);

    String getSubjectConfirmationDataInResponseTo();
    void setSubjectConfirmationDataInResponseTo(String identifier);

    int getSubjectConfirmationDataNotBeforeSecondsInPast();
    void setSubjectConfirmationDataNotBeforeSecondsInPast(int seconds);

    int getSubjectConfirmationDataNotOnOrAfterExpirySeconds();
    void setSubjectConfirmationDataNotOnOrAfterExpirySeconds(int seconds);

    public static enum DecorationType {
        /** Apply decorations to request */
        REQUEST,

        /** Apply decorations to response */
        RESPONSE,

        /** Insert the assertion into the message as a child of the Security header, but don't sign it */
        ADD_ASSERTION,

        /** Insert the assertion into the message, and ensure it's signed with a message-level signature */
        SIGN_ASSERTION,

        /** Sign the SOAP Body */
        SIGN_BODY,
    }


}

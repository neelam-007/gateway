package com.l7tech.policy.assertion;

import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.xml.KeyInfoInclusionType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Interface for configuring the SamlTokenIssuerAssertion bean from core/UI classes.
 *
 * @author jbufu
 */
public interface SamlIssuerConfiguration {

    /**
     * Get the SAML version for this assertion
     *
     * <p>The value 0 means any version, null means unspecified (in which case 1 should
     * be used for backwards compatibility).</p>
     *
     * @return The saml version (0/1/2) or null.
     */
    public Integer getVersion();

    /**
     * Set the SAML version for this assertion.
     *
     * @param version (may be null)
     */
    public void setVersion(Integer version);

    /**
     * Determine if the Issuer element is applicable. For example SAML 1.1 protocol requests do not have an Issuer.
     * @return true if the Issuer element should be added, false otherwise
     */
    public boolean addIssuerElement();

    String getCustomIssuerValue();
    void setCustomIssuerValue(@Nullable String customIssuerValue);

    String getCustomIssuerFormat();
    void setCustomIssuerFormat(@Nullable String customIssuerFormat);

    String getCustomIssuerNameQualifier();
    void setCustomIssuerNameQualifier(@Nullable String customIssuerNameQualifier);

    boolean isSignAssertion();
    void setSignAssertion(boolean selected);

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

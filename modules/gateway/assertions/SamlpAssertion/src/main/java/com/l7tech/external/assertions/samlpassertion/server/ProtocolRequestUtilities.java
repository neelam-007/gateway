package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.util.ISO8601Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saml.v2.assertion.EncryptedElementType;
import saml.v2.assertion.NameIDType;
import saml.v2.assertion.SubjectType;
import saml.v2.protocol.AuthnRequestType;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;

public class ProtocolRequestUtilities {

    // Suffixes for variables set by SAML Protocol Process Assertions
    public static final String SUFFIX_SUBJECT = "subject";
    public static final String SUFFIX_SUBJECT_NAME_QUALIFIER = "subject.nameQualifier";
    public static final String SUFFIX_SUBJECT_SP_NAME_QUALIFIER = "subject.spNameQualifier";
    public static final String SUFFIX_SUBJECT_FORMAT = "subject.format";
    public static final String SUFFIX_SUBJECT_SP_PROVIDED_ID = "subject.spProvidedId";
    public static final String SUFFIX_X509CERT_BASE64 = "x509CertBase64";
    public static final String SUFFIX_X509CERT = "x509Cert";
    public static final String SUFFIX_ACS_URL = "acsUrl";
    public static final String SUFFIX_ID = "id";
    public static final String SUFFIX_VERSION = "version";
    public static final String SUFFIX_ISSUE_INSTANT = "issueInstant";
    public static final String SUFFIX_DESTINATION = "destination";
    public static final String SUFFIX_CONSENT = "consent";
    public static final String SUFFIX_ISSUER = "issuer";
    public static final String SUFFIX_ISSUER_NAME_QUALIFIER = "issuer.nameQualifier";
    public static final String SUFFIX_ISSUER_SP_NAME_QUALIFIER = "issuer.spNameQualifier";
    public static final String SUFFIX_ISSUER_FORMAT = "issuer.format";
    public static final String SUFFIX_ISSUER_SP_PROVIDED_ID = "issuer.spProvidedId";
    public static final String SUFFIX_REQUEST = "request";
    public static final String SUFFIX_ATTRIBUTES = "attributes";

    @SuppressWarnings({"unchecked"})
    @Nullable static String getSubject(@Nullable final SubjectType subjectType) {
        String subject = null;

        final NameIDType nameIDType = getSubjectNameID(subjectType);
        if (nameIDType != null) {
            subject = nameIDType.getValue();
        }

        return subject;
    }

    @SuppressWarnings({"unchecked"})
    @Nullable static NameIDType getSubjectNameID(@Nullable final SubjectType subjectType) {
        NameIDType subjectNameID = null;

        if (subjectType != null) {
            final List<JAXBElement<?>> content = subjectType.getContent();
            if (content != null && !content.isEmpty()) {
                JAXBElement<?> contentElement = content.get(0);
                if (NameIDType.class.isAssignableFrom(contentElement.getDeclaredType())) {
                    final JAXBElement<NameIDType> nameIDType = (JAXBElement<NameIDType>) contentElement;
                    subjectNameID = nameIDType.getValue();
                }
            }
        }

        return subjectNameID;
    }

    @SuppressWarnings({"unchecked"})
    @Nullable static EncryptedElementType getSubjectEncryptedID(@Nullable final SubjectType subjectType) {
        EncryptedElementType encryptedIdType = null;

        if (subjectType != null) {
            final List<JAXBElement<?>> content = subjectType.getContent();
            if (content != null && !content.isEmpty()) {
                JAXBElement<?> contentElement = content.get(0);
                if (EncryptedElementType.class.isAssignableFrom(contentElement.getDeclaredType())) {
                    final JAXBElement<EncryptedElementType> encryptedIdJaxbType = (JAXBElement<EncryptedElementType>) contentElement;
                    encryptedIdType = encryptedIdJaxbType.getValue();
                }
            }
        }

        return encryptedIdType;
    }

    @Nullable static String getName(@Nullable final NameIDType nameIDType) {
        String name = null;
        if (nameIDType != null) {
            name = nameIDType.getValue();
        }
        return name;
    }

    @Nullable static String getNameQualifier(@Nullable final NameIDType nameIDType) {
        String nameQualifier = null;
        if (nameIDType != null) {
            nameQualifier = nameIDType.getNameQualifier();
        }
        return nameQualifier;
    }

    @Nullable static String getSPNameQualifier(@Nullable final NameIDType nameIDType) {
        String spNameQualifier = null;
        if (nameIDType != null) {
            spNameQualifier = nameIDType.getSPNameQualifier();
        }
        return spNameQualifier;
    }

    @Nullable static String getSPProvidedID(@Nullable final NameIDType nameIDType) {
        String format = null;
        if (nameIDType != null) {
            format = nameIDType.getSPProvidedID();
        }
        return format;
    }

    @Nullable static String getNameFormat(@Nullable final NameIDType nameIDType) {
        String format = null;
        if (nameIDType != null) {
            format = nameIDType.getFormat();
        }
        return format;
    }

    @Nullable static String getIsoTime(@Nullable final XMLGregorianCalendar xmlCalendar) {
        String time = null;
        if (xmlCalendar != null) {
            time = ISO8601Date.format(xmlCalendar.toGregorianCalendar().getTime());
        }
        return time;
    }

    @NotNull static String getConsent(@NotNull final AuthnRequestType authnRequest) {
        String consent = authnRequest.getConsent();

        if (consent == null) {
            consent = CONSENT_UNSPECIFIED;
        }

        return consent;
    }

    // - PRIVATE
    private static final String CONSENT_UNSPECIFIED = "urn:oasis:names:tc:SAML:2.0:consent:unspecified";

}

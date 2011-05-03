package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.FullQName;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 *
 */
public class ServerNonSoapVerifyElementAssertionTest {
    static final String SIGNER_CERT =
            "MIIB8TCCAVqgAwIBAgIIJiwd8ruk6HcwDQYJKoZIhvcNAQEMBQAwGjEYMBYGA1UEAwwPZGF0YS5sN3RlY2guY29tMB4XDTA5MDYyNjIyMjMzOVoXDTE5MDYyNDIyMjMzOVowGjEYMBYGA1UEAwwPZGF0YS5sN3RlY2guY29tMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQCTEO6rGMn+X+lVNIEprLUi9a2e6VVBg1Ozr91TPaOhK8JeWDNtkXUych0PFN6YpBDEsiLSb8aiej5CwZFt/mmWdRn2qAKfutJ8F52SgW5ZLuYTtD4MFcOMySfC6aLk726VUUIipYKngNPOLHxUqnMapTWT4x2Ssi9+23TN6QH63QIBA6NCMEAwHQYDVR0OBBYEFEJIF1caGbInfcje2ODXnxszns+yMB8GA1UdIwQYMBaAFEJIF1caGbInfcje2ODXnxszns+yMA0GCSqGSIb3DQEBDAUAA4GBAFU/MTZm3TZACawEgBUKSJd04FvnLV99lGIBLp91rHcbCAL9roZJp7RC/w7sHVUve8khtYm5ynaZVTu7S++NTqnCWQI1Bx021zFhAeucFsx3tynfAdjW/xRre8pQLF9A7PoJTYcaS2tNQgS7um3ZHxjA/JV3dQWzeR1Kwepvzmk9";

    static final String SIGNATURE_VALUE =
            "G+S/sK4hg3rPxAsjXm8jS4twP54ltDJQf/EK7elQCNi2Vd2wDrGQ0vkcIWm+fdfh4nYMA0Th8m/cQbDaicp/0Z990S4oNUllNL4cLPlkcIEPWG9r9siZg5346hdi/W0xHsO199ukHm51I6v4qujK5tqajP0AbJl1fe8ly8CnAos=";

    static final String SIGNED =
            "<foo><bar Id=\"bar-1-3511c4c29ab6a196290a5f79a61417a6\"><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">" +
            "<ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
            "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#bar-1-3511c4c29ab6a196290a5f79a61417a6\">" +
            "<ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>" +
            "<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>" +
            "<ds:DigestValue>VU0equBu1QkCdTyzf6Dx6dulVxM=</ds:DigestValue></ds:Reference></ds:SignedInfo>" +
            "<ds:SignatureValue>" + SIGNATURE_VALUE + "</ds:SignatureValue>" +
            "<ds:KeyInfo><ds:X509Data><ds:X509Certificate>" + SIGNER_CERT + "</ds:X509Certificate>" +
            "</ds:X509Data></ds:KeyInfo></ds:Signature></bar><blat/></foo>";

    static final String SIGNED_WITH_CUSTOM_ID_GLOBAL_ATTR =
            "<foo xmlns:pfx=\"urn:other\"><bar pfx0:customId=\"bar-1-e284b01f7aa88d1dd1cb8ad9318d6eda\" xmlns:pfx0=\"urn:blatch\"><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationM" +
            "ethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#bar-1-e284b01f7aa88d1dd1cb8ad9318d6eda\"><ds:Transforms" +
            "><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org" +
            "/2000/09/xmldsig#sha1\"/><ds:DigestValue>Xy+FSrEU2C6X9PEad4XpRxubXTY=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>SB3FJxzRZtpAmgvQnNJ/qG6orW9GTTUjnLzxGVs8v/fcFCUzW4glRygTpfDZaM/+8fDNm+j+I+UFs52" +
            "tZI4qhos2dcdr38+L2VQIJLHFRq5Blfd8vbFo6srBHbAQz8svH5PV2X0EA/kgRlhBCalbj9c749JNFovNpXZ2JTv/M2w=</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIB8TCCAVqgAwIBAgIIJiwd8ruk6HcwDQYJKoZIhvcNAQEMBQAwGjEYM" +
            "BYGA1UEAwwPZGF0YS5sN3RlY2guY29tMB4XDTA5MDYyNjIyMjMzOVoXDTE5MDYyNDIyMjMzOVowGjEYMBYGA1UEAwwPZGF0YS5sN3RlY2guY29tMIGdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQCTEO6rGMn+X+lVNIEprLUi9a2e6VVBg1Ozr91TPaOhK8JeWDNtkXUych0PFN6YpBDE" +
            "siLSb8aiej5CwZFt/mmWdRn2qAKfutJ8F52SgW5ZLuYTtD4MFcOMySfC6aLk726VUUIipYKngNPOLHxUqnMapTWT4x2Ssi9+23TN6QH63QIBA6NCMEAwHQYDVR0OBBYEFEJIF1caGbInfcje2ODXnxszns+yMB8GA1UdIwQYMBaAFEJIF1caGbInfcje2ODXnxszns+yMA0GCSqGSIb3DQE" +
            "BDAUAA4GBAFU/MTZm3TZACawEgBUKSJd04FvnLV99lGIBLp91rHcbCAL9roZJp7RC/w7sHVUve8khtYm5ynaZVTu7S++NTqnCWQI1Bx021zFhAeucFsx3tynfAdjW/xRre8pQLF9A7PoJTYcaS2tNQgS7um3ZHxjA/JV3dQWzeR1Kwepvzmk9</ds:X509Certificate></ds:X509Data" +
            "></ds:KeyInfo></ds:Signature></bar></foo>";

    static final String SIGNED_WITH_CUSTOM_ID_LOCAL_ATTR =
            "<foo><bar customId=\"bar-1-0458e75189e78fb5b252c18d33a1d72c\"><child1/><child2>foo</child2><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#bar-1-0458e75189e78fb5b252c18d33a1d72c\">" +
            "<ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>NGt5v42qZBpUq214IzBYVb7eSQc=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>T8GcDYwojM/DfXUTyXgykZ3Yr" +
            "KAUBr5tsAVbViPoJHBx8DlE1gOtsrUnG95uOgD+Y8XsSo1ohDm8CWZEQJN0AZL+RHiAXyd9HK+jIo6H3/ejpPAedPCmZk72QmRlMfqe/ADgoVKqRDrFK4bMLDdjdN4VU8qxMbXGsRjt4GfywWY=</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIB8TCCAVqgAwIBAgIIJiwd8ruk6HcwDQYJKoZIhvcNAQEMBQAwGjEYMBYGA1UEAwwPZGF0YS5sN3RlY2guY29tMB4XDTA5MDYyNjIyMjMzOVoXDTE5MDYyNDIyMjMzOVowGjEYMBYGA1UEAwwPZGF0YS5sN3RlY2guY29tMI" +
            "GdMA0GCSqGSIb3DQEBAQUAA4GLADCBhwKBgQCTEO6rGMn+X+lVNIEprLUi9a2e6VVBg1Ozr91TPaOhK8JeWDNtkXUych0PFN6YpBDEsiLSb8aiej5CwZFt/mmWdRn2qAKfutJ8F52SgW5ZLuYTtD4MFcOMySfC6aLk726VUUIipYKngNPOLHxUqnMapTWT4x2Ssi9+23TN6QH63QIBA6NCMEAwHQYDVR0OBBYEFEJIF1caGbInfcje2ODXnxszns+yMB8GA1UdIwQYMBaAFEJIF1caGbInfcje2ODXnxszns+yMA0GCSqGSIb3DQEBDAUAA4GBAFU/MTZm3TZACawEgBUKSJd04FvnLV99lGIBLp91rHcbCAL9roZJp7RC" +
            "/w7sHVUve8khtYm5ynaZVTu7S++NTqnCWQI1Bx021zFhAeucFsx3tynfAdjW/xRre8pQLF9A7PoJTYcaS2tNQgS7um3ZHxjA/JV3dQWzeR1Kwepvzmk9</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature></bar></foo>";

    static final String LAYER7_SIGNED_ECDSA =
            "<foo><bar Id=\"bar-1-6200bc512237668f3a916eea7e6db597\"><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
            "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384\"/><ds:Reference URI=\"#bar-1-6200bc512237668f3a916eea7e6db597\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09" +
            "/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#sha384\"/><ds:DigestValue>TDLi" +
            "r+xDp8ydRMhCFsi8hQgeuP3wcdFo2TnZcclexTEQdHboH4eUoffxHrd0eNSw</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>kB/nhDuhzd2jHbUxZpvpRiYSx50TqRg07T8MCnDg+5LWgX7v5QfdhtNXjkLxdGHt/dV/17urqrT0z+gxWAM8ovws" +
            "PvsWQoi20+0LwaG6Dn40JPVD93+BdTkI299QQX4/</ds:SignatureValue><ds:KeyInfo><ds:X509Data><ds:X509Certificate>MIIBuzCCAUCgAwIBAgIJAM0yapFBK9FWMAoGCCqGSM49BAMDMBAxDjAMBgNVBAMTBWVjZHNhMB4XDTA5MTAxNDIxMDMyN1oXDTI5MTAwOTIxMD" +
            "MyN1owEDEOMAwGA1UEAxMFZWNkc2EwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAAR/ZimmaWYyniX7dxIaZNm1bs3L6AlZQdbsXF0abw8RaCUKxv0kj+G/66Rz2Zi1KQi0X0SshBp8dcrIHg6CdGUCg3ecG/90m6ZTQ40GYW/saniPCPlSmsAvyBWT3eeWAoqjZjBkMA4GA1UdDwEB/wQEAwIF4" +
            "DASBgNVHSUBAf8ECDAGBgRVHSUAMB0GA1UdDgQWBBSxICMdkP6HesLjcsKr//Q+yU4vvDAfBgNVHSMEGDAWgBSxICMdkP6HesLjcsKr//Q+yU4vvDAKBggqhkjOPQQDAwNpADBlAjBlh8gFJlrtXBvaRlxM/uPlpSVQNkjMK+JQlDFYZ7GvDXh2txez5HOatn/+3+nk7MkCMQD0sWMGiePG" +
            "WPAFvaneFzQ8UBp5K9kBeBNXZ7nXTDnYxh28bvcLvVbtnQ5wa1Q34cgA</ds:X509Certificate></ds:X509Data></ds:KeyInfo></ds:Signature></bar></foo>";

    static final String SAML_11_ASSERTION =
            "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\" AssertionID=\"SamlAssertion-5b3f1ec7e856441b9d61d8d4ffedf44d\" Issuer=\"data.l7tech.local\" IssueInstant=\"2010-09-29T21:00:19.167Z\"><saml:Conditions NotBefore=\"2010-09-29T20:58:19.000Z\" NotOnOrAfter=\"2010-09-29T21:05:19.361Z\"/><saml:AuthenticationStatement AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:unspecified\" AuthenticationInstant=\"2010-09-29T21:00:19.167Z\"><saml:Subject><saml:SubjectConfirmation><saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:bearer</saml:ConfirmationMethod></saml:SubjectConfirmation></saml:Subject><saml:SubjectLocality IPAddress=\"127.0.0.1\"/></saml:AuthenticationStatement><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#SamlAssertion-5b3f1ec7e856441b9d61d8d4ffedf44d\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>HfhX0I2kqRhbex7MlLGqUkQ3BlM=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>ebLcJu5ltRJc0q5+ws4Ju/cxhry/S07EuJQWlVtP3K1lV2ued8QVxNaB32xIFpjhVSt0XSKNZaovYnhrZOLcFY4UMlm632tezm3DZdpRmIjGqg4M8goqnUX3ONQAIwTxjE627J69tcVkdWF9mfhoCdKNlDtKsqjNMpihPAG3FsFcOD3zOTxPtG8ZlqxoKsV+z4LfBnkfJYDkQJv+uU1fU2D4ANHbU52KJunKiws5UniNjCdpk8I2i+B+xfPRfSgN29XSTY/63O7Asck2NGcFd/ghwdIhXssXfo7IFhQ37bMU/9lljChn8Tsws5I+0UT2DS5JY8SdpyBDK6R7UCd7og==</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509SubjectName>CN=data.l7tech.local</X509SubjectName><X509Certificate>MIIC/TCCAeWgAwIBAgIJALQ69EvSLyHRMA0GCSqGSIb3DQEBDAUAMBwxGjAYBgNVBAMTEWRhdGEubDd0ZWNoLmxvY2FsMB4XDTEwMDkyMjIzNDUxNloXDTIwMDkxOTIzNDUxNlowHDEaMBgGA1UEAxMRZGF0YS5sN3RlY2gubG9jYWwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCbsHn5S7bjQxAVqM9mwT9mF/xpUwkhQFDjdnaZ3EgEWMNogmD8w94JMlV9FThYhNNkMm4KsbldmW80Zgj3Wg61/U8oNGwMDhNSzl8A5HJZpQYcGr1f0f9pHty1K35WRPy3Kwj27s+A0xnl9mFpWqOstZDL/R73dUDTRjLiyt1T5gIBvOJLSyCBdm4EKEYxd5pccXE0dMjme/H+9zeF1ZZcdVeCy/4p89g5uH4oRr5JwZpE2t257vvDCm3JMZz+0cehyqLm1dp43aOn5meFVBQnnxSqwQsROql0B4QAHE8v2ey6XV1IdFR0xWAixc6rqFcydkZ5ZlCaAGpnHu9q31bxAgMBAAGjQjBAMB0GA1UdDgQWBBQZdpWMAS2VY398QD2tHIYcZ6xpDDAfBgNVHSMEGDAWgBQZdpWMAS2VY398QD2tHIYcZ6xpDDANBgkqhkiG9w0BAQwFAAOCAQEAV1W4yGe892/0kMNFoGFzMbshrKZIQzKiWIOg4EMGx6m4RPT0wh+biTCmz6p+ZfEZiQ3NooN7VFawgWfGjxvhZv+8+lLCircBvaL9xPBdswTx7QUgxEJIoU8SSwRMQjG/WdrBsfmjG21UyBtswsvhvnwqYpFGptHBt/oAlNe6EGOkR8AvZAim4M0S0TXzBdJekXYzsnshxKBMsqVnoaBN1WFnIdb/U3oGXB/x+IsGIqusJpMhjyzhdvN3RfXPk9Dr/sxNDXq+9uF0EZ0ea8NlSI5ngVIavjVSuktwW80ZrDAgVPeDvBV9n14HlJ5TWwQwMRk/dftUUjQN3xamJXl3BQ==</X509Certificate></X509Data></KeyInfo></ds:Signature></saml:Assertion>";

    // Unfortunately this includes an EC cert that uses explicit parameters rather than a named curve, so
    // requires the Bouncy Castle certificate factory to parse it.
    public static final String APACHE_SAMPLE_SIGNED_XML =
            "<!-- Comment before -->\n" +
            "<RootElement>Some simple text\n" +
            "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "<ds:SignedInfo>\n" +
            "\n" +
            "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod>\n" +
            "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1\"></ds:SignatureMethod>\n" +
            "<ds:Reference URI=\"\">\n" +
            "<ds:Transforms>\n" +
            "<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform>\n" +
            "<ds:Transform Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments\"></ds:Transform>\n" +
            "</ds:Transforms>\n" +
            "<ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod>\n" +
            "<ds:DigestValue>LKyUpNaZJ2joznVzwEup5JDwtS0=</ds:DigestValue>\n" +
            "</ds:Reference>\n" +
            "</ds:SignedInfo>\n" +
            "<ds:SignatureValue>\n" +
            "Qma5I5AZiSzQ6J4UZwjpteD2qvQclABKATPQ5MZ7mmOFYfj8xAlpXWu2u+Oa/4mpP9jK9OUUcTU9\n" +
            "Psfucz+qPA==\n" +
            "</ds:SignatureValue>\n" +
            "<ds:KeyInfo>\n" +
            "<ds:X509Data>\n" +
            "<ds:X509Certificate>\n" +
            "MIIC0zCCAlmgAwIBAgIGARJQ/UmbMAkGByqGSM49BAEwUDEhMB8GA1UEAxMYWE1MIEVDRFNBIFNp\n" +
            "Z25hdHVyZSBUZXN0MRYwFAYKCZImiZPyLGQBGRMGYXBhY2hlMRMwEQYKCZImiZPyLGQBGRMDb3Jn\n" +
            "MCAXDTA3MDUwMzA4MTAxNVoYDzQ3NDkwMzMwMTcyMzU0WjBQMSEwHwYDVQQDExhYTUwgRUNEU0Eg\n" +
            "U2lnbmF0dXJlIFRlc3QxFjAUBgoJkiaJk/IsZAEZEwZhcGFjaGUxEzARBgoJkiaJk/IsZAEZEwNv\n" +
            "cmcwggEzMIHsBgcqhkjOPQIBMIHgAgEBMCwGByqGSM49AQECIQD/////////////////////////\n" +
            "///////////////9lzBEBCD////////////////////////////////////////9lAQgAAAAAAAA\n" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKYEQQQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\n" +
            "AAAAAXJuG44fZ2Ml2CCvpbrA1InK1rDSINwcTt1TNmNhYN+DAiEA/////////////////////2xh\n" +
            "EHCZWtEARYQbCbdhuJMCAQEDQgAEZubz40WiQ+v/nrjhfizYmEIltKIr/n7hwGwpG3CDEk3OTDm8\n" +
            "kAaLKEgVfamdL/RaR8ExrP7vfRyVzkLIkfQEraNCMEAwHQYDVR0OBBYEFCGBVSkjUniioRMZg+2N\n" +
            "b1x/dadFMB8GA1UdIwQYMBaAFCGBVSkjUniioRMZg+2Nb1x/dadFMAkGByqGSM49BAEDaQAwZgIx\n" +
            "AL+Ff9YRyKHW/Iq5eA5dt7If9vp77YZjgQwWEWjQdgIxK1dwuSFZ1IQDNrMgdoMcvQIxANHKPAC5\n" +
            "qZ5v+y4srbPR7yg3x7rjIUj1pMDNqIpoHkgsWBJjV42iHIppNC5LFtzjhw==\n" +
            "</ds:X509Certificate>\n" +
            "</ds:X509Data>\n" +
            "</ds:KeyInfo>\n" +
            "</ds:Signature></RootElement>\n" +
            "<!-- Comment after -->";

    public static final String APACHE_SIGNER_CERT =
            "MIICEjCCAbegAwIBAgIGARJQ/UmbMAsGByqGSM49BAEFADBQMSEwHwYDVQQDExhYTUwgRUNEU0Eg\n" +
            "U2lnbmF0dXJlIFRlc3QxFjAUBgoJkiaJk/IsZAEZEwZhcGFjaGUxEzARBgoJkiaJk/IsZAEZEwNv\n" +
            "cmcwHhcNMDcwNTAzMDgxMDE1WhcNMTEwNTAzMDgxMDE1WjBQMSEwHwYDVQQDExhYTUwgRUNEU0Eg\n" +
            "U2lnbmF0dXJlIFRlc3QxFjAUBgoJkiaJk/IsZAEZEwZhcGFjaGUxEzARBgoJkiaJk/IsZAEZEwNv\n" +
            "cmcwgbQwgY0GByqGSM49AgEwgYECAQEwLAYHKoZIzj0BAQIhAP//////////////////////////\n" +
            "//////////////2XMCcEIQD////////////////////////////////////////9lAQCAKYEAgMB\n" +
            "AiEA/////////////////////2xhEHCZWtEARYQbCbdhuJMDIgADZubz40WiQ+v/nrjhfizYmEIl\n" +
            "tKIr/n7hwGwpG3CDEk2jIDAeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgGmMAsGByqGSM49\n" +
            "BAEFAANIADBFAiEA63Pq7/YfDDrnbCxXVX20T3dn77iL8dvC1Cb24Al9VFkCIHUeymf/N+H60OQL\n" +
            "v9Wg/X8Cbp2am42qjQvaKtb4+BFk";

    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        Security.addProvider(new BouncyCastleProvider()); // Needs to be BC, not RSA, because of that wacky Apache signing cert that isn't a named curve
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", NonSoapXmlSecurityTestUtils.makeSecurityTokenResolver());
            put("ssgKeyStoreManager", NonSoapXmlSecurityTestUtils.makeSsgKeyStoreManager());
            put("trustedCertCache", NonSoapXmlSecurityTestUtils.makeTrustedCertCache());
        }});
    }

    @Test
    public void testVerify() throws Exception {
        verifyAndCheck(ass(), SIGNED, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", SIGNATURE_VALUE);
    }

    @Test
    public void testVerifyFailed_badSignatureValue() throws Exception {
        AssertionStatus result = sass(ass()).checkRequest(context(SIGNED.replace(SIGNATURE_VALUE, SIGNATURE_VALUE.replace("m8jS4twP54ltD", "m8jS3twP54ltD"))));
        assertEquals(AssertionStatus.BAD_REQUEST, result);
    }

    @Test
    public void testVerifyFailed_badDigestValue() throws Exception {
        AssertionStatus result = sass(ass()).checkRequest(context(SIGNED.replace("VU0equBu1", "VU0ebuBu1")));
        assertEquals(AssertionStatus.BAD_REQUEST, result);
    }

    @Test
    public void testVerifyFailed_alteredDocument() throws Exception {
        AssertionStatus result = sass(ass()).checkRequest(context(SIGNED.replace("<bar ", "<bar evilAttr=\"added\" ")));
        assertEquals(AssertionStatus.BAD_REQUEST, result);
    }

    @Test
    public void testVerifyApacheSignedEcdsa() throws Exception {
        ServerNonSoapVerifyElementAssertion.CERT_PARSE_BC_FALLBACK = true;
        verifyAndCheck(ass(), APACHE_SAMPLE_SIGNED_XML, false, null,
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1", null);
    }

    @Test
    public void testVerifyLayer7SignedEcdsa() throws Exception {
        verifyAndCheck(ass(), LAYER7_SIGNED_ECDSA, false, NonSoapXmlSecurityTestUtils.getEcdsaKey().getCertificate(),
                "http://www.w3.org/2001/04/xmldsig-more#sha384", "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384", null);
    }

    @Test
    @BugNumber(9179)
    public void testVerifySignedSaml11Assertion() throws Exception {
        verifyAndCheck(ass(), SAML_11_ASSERTION, false, null,
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifyCustomIdAttr() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("{urn:blatch}customId") });
        verifyAndCheck(ass, SIGNED_WITH_CUSTOM_ID_GLOBAL_ATTR, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifyCustomIdAttr_duplicateLocalNames() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("{urn:blortch}customId"), FullQName.valueOf("{urn:blatch}customId"), FullQName.valueOf("customId"), FullQName.valueOf("{urn:bleetch}customId") });
        verifyAndCheck(ass, SIGNED_WITH_CUSTOM_ID_GLOBAL_ATTR, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifyCustomIdAttr_duplicateLocalNames_noLocalAttrs() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("{urn:blortch}customId"), FullQName.valueOf("{urn:blatch}customId"), FullQName.valueOf("{urn:bleetch}customId") });
        verifyAndCheck(ass, SIGNED_WITH_CUSTOM_ID_GLOBAL_ATTR, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifyCustomIdAttr_duplicateLocalNames_onlyLocalAttrs() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("otherId"), FullQName.valueOf("customId"), FullQName.valueOf("{urn:blatch}customId") });
        verifyAndCheck(ass, SIGNED_WITH_CUSTOM_ID_GLOBAL_ATTR, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifyCustomIdAttr_duplicateLocalNames_missingPrefix() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("customId") });
        AssertionStatus result = sass(ass).checkRequest(context(SIGNED_WITH_CUSTOM_ID_GLOBAL_ATTR));
        assertEquals(AssertionStatus.BAD_REQUEST, result);
    }

    @Test
    @BugNumber(9028)
    public void testVerifySimpleCustomIdAttr() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("customId") });
        verifyAndCheck(ass, SIGNED_WITH_CUSTOM_ID_LOCAL_ATTR, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifySimpleCustomIdAttr_dynamicAttrNode() throws Exception {
        PolicyEnforcementContext context = context(SIGNED_WITH_CUSTOM_ID_LOCAL_ATTR);

        // Replace the custom ID attribute with a dynamically-created node whose namespace URI is null instead of ""
        Document doc = context.getRequest().getXmlKnob().getDocumentWritable();
        Element signedElement = XmlUtil.findOnlyOneChildElement(doc.getDocumentElement());
        assertEquals("bar", signedElement.getNodeName());
        Attr idAttr = signedElement.getAttributeNodeNS(null, "customId");
        assertEquals(null, idAttr.getNamespaceURI());
        String idValue = idAttr.getValue();
        assertTrue(idValue != null && idValue.trim().length() > 0);
        signedElement.removeAttributeNode(idAttr);
        signedElement.setAttribute("customId", idValue); // use DOM level 1 method

        NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("customId") });
        AssertionStatus result = sass(ass).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
    }

    @Test
    @BugNumber(9028)
    public void testVerifySimleCustomIdAttr_duplicateLocalNames() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("{urn:blortch}customId"), FullQName.valueOf("{urn:blatch}customId"), FullQName.valueOf("customId"), FullQName.valueOf("{urn:bleetch}customId") });
        verifyAndCheck(ass, SIGNED_WITH_CUSTOM_ID_LOCAL_ATTR, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifySimpleCustomIdAttr_duplicateLocalNames_noLocalAttrs() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("{urn:blortch}customId"), FullQName.valueOf("{urn:blatch}customId"), FullQName.valueOf("{urn:bleetch}customId") });
        AssertionStatus result = sass(ass).checkRequest(context(SIGNED_WITH_CUSTOM_ID_LOCAL_ATTR));
        assertEquals(AssertionStatus.BAD_REQUEST, result);
    }

    @Test
    @BugNumber(9028)
    public void testVerifySimpleCustomIdAttr_duplicateLocalNames_onlyLocalAttrs() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("otherId"), FullQName.valueOf("customId"), FullQName.valueOf("{urn:blatch}customId") });
        verifyAndCheck(ass, SIGNED_WITH_CUSTOM_ID_LOCAL_ATTR, true, CertUtils.decodeFromPEM(SIGNER_CERT, false),
                "http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2000/09/xmldsig#rsa-sha1", null);
    }

    @Test
    @BugNumber(9028)
    public void testVerifySimpleCustomIdAttr_duplicateLocalNames_extraPrefix() throws Exception {
        final NonSoapVerifyElementAssertion ass = ass();
        ass.setCustomIdAttrs(new FullQName[] { FullQName.valueOf("{urn:specialNs}customId") });
        AssertionStatus result = sass(ass).checkRequest(context(SIGNED_WITH_CUSTOM_ID_LOCAL_ATTR));
        assertEquals(AssertionStatus.BAD_REQUEST, result);
    }

    void verifyAndCheck(NonSoapVerifyElementAssertion ass, String signedXml,
                        boolean signedElementIsBar, X509Certificate expectedSigningCert,
                        String expectedDigestMethod, String expectedSignatureMethod, String expectedSignatureValue) throws Exception
    {
        PolicyEnforcementContext context = context(signedXml);
        AssertionStatus result = sass(ass).checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        Document doc = context.getRequest().getXmlKnob().getDocumentReadOnly();

        Object[] elementsVerified = (Object[])context.getVariable("elementsVerified");
        assertNotNull(elementsVerified);
        assertEquals(1, elementsVerified.length);
        if (signedElementIsBar) {
            Element bar = (Element)doc.getElementsByTagName("bar").item(0);
            assertTrue(elementsVerified[0] == bar);
        }

        Object[] signingCertificates = (Object[])context.getVariable("signingCertificates");
        assertNotNull(signingCertificates);
        assertEquals(1, signingCertificates.length);
        if (expectedSigningCert != null)
            assertTrue(CertUtils.certsAreEqual((X509Certificate) signingCertificates[0], expectedSigningCert));

        Object[] digestMethodUris = (Object[])context.getVariable("digestMethodUris");
        assertNotNull(digestMethodUris);
        assertEquals(1, digestMethodUris.length);
        assertEquals(expectedDigestMethod, digestMethodUris[0]);

        Object[] signatureMethodUris = (Object[])context.getVariable("signatureMethodUris");
        assertNotNull(signatureMethodUris);
        assertEquals(1, signatureMethodUris.length);
        assertEquals(expectedSignatureMethod, signatureMethodUris[0]);

        Object[] signatureValues = (Object[])context.getVariable("signatureValues");
        assertNotNull(signatureValues);
        assertEquals(1, signatureValues.length);
        if (expectedSignatureValue != null)
            assertEquals(expectedSignatureValue, signatureValues[0]);

        Object[] signatureElements = (Object[])context.getVariable("signatureElements");
        assertNotNull(signatureElements);
        assertEquals(1, signatureElements.length);
        assertTrue(Element.class.isInstance(signatureElements[0]));
        Element sigElement = (Element) signatureElements[0];
        assertEquals("Signature", sigElement.getLocalName());
        assertEquals(SoapConstants.DIGSIG_URI, sigElement.getNamespaceURI());
    }

    private static ServerNonSoapVerifyElementAssertion sass(NonSoapVerifyElementAssertion ass) throws InvalidXpathException, ParseException {
        return new ServerNonSoapVerifyElementAssertion(ass, beanFactory, null);
    }

    private static PolicyEnforcementContext context(String signedXml) {
        final Message request = new Message(XmlUtil.stringAsDocument(signedXml));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
    }

    private static NonSoapVerifyElementAssertion ass() {
        NonSoapVerifyElementAssertion ass = new NonSoapVerifyElementAssertion();
        ass.setXpathExpression(new XpathExpression("//*[local-name()='Signature']"));
        ass.setTarget(TargetMessageType.REQUEST);
        return ass;
    }
}

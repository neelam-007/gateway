package com.l7tech.server.security;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.security.xml.XmlElementEncryptionResolvedConfig;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class XmlElementEncryptorConfigUtils {

    /**
     * Utility method to convert an {@link XmlElementEncryptionConfig} which may reference context variables into an
     * {@link XmlElementEncryptionResolvedConfig}
     *
     * @param rawConfig Config with raw values
     * @param variableMap all available variables which the raw config may reference
     * @param audit Auditor
     * @return a resolved config which can be used with {@link com.l7tech.security.xml.XmlElementEncryptor}
     * @throws IOException if problems creating X509Certificate
     * @throws CertificateException if problems creating X509Certificate
     * @throws NoSuchVariableException if a reference variable does not exist
     * @throws InvalidResolvedValueException if a resolved value is not value e.g. an invalid URI
     */
    @NotNull
    public static XmlElementEncryptionResolvedConfig getXmlElementEncryptorConfig(@NotNull final XmlElementEncryptionConfig rawConfig,
                                                                                  @NotNull final Map<String, Object> variableMap,
                                                                                  @NotNull final Audit audit)
            throws IOException, CertificateException, NoSuchVariableException, InvalidResolvedValueException {


        final X509Certificate recipientCert;
        if (rawConfig.getRecipientCertContextVariableName() != null) {
            final Object resolvedObjectForCert = ExpandVariables.processSingleVariableAsObject(
                    Syntax.getVariableExpression(rawConfig.getRecipientCertContextVariableName()), variableMap, audit);

            if (resolvedObjectForCert instanceof String) {
                recipientCert = getCertFromBase64(String.valueOf(resolvedObjectForCert));
            } else {
                if (resolvedObjectForCert instanceof X509Certificate) {
                    recipientCert = (X509Certificate) resolvedObjectForCert;
                } else {
                    throw new NoSuchVariableException(rawConfig.getRecipientCertContextVariableName(),
                            "Recipient certificate variable was neither an X509Certificate nor a string in PEM or Base-64 format.");
                }
            }
        } else {
            recipientCert = getCertFromBase64(rawConfig.getRecipientCertificateBase64());
        }

        final XmlElementEncryptionResolvedConfig resolvedConfig = new XmlElementEncryptionResolvedConfig(recipientCert, rawConfig.getXencAlgorithm(), rawConfig.isEncryptContentsOnly());

        if (rawConfig.isIncludeEncryptedDataTypeAttribute()) {
            final String typeAttribute = rawConfig.getEncryptedDataTypeAttribute();
            final String processTypeAttr = ExpandVariables.process(typeAttribute, variableMap, audit).trim();
            if (!ValidationUtils.isValidUri(processTypeAttr)) {
                throw new InvalidResolvedValueException("Type attribute for EncryptedData is not a valid URI: '" + processTypeAttr + "'");
            }
            resolvedConfig.setEncryptedDataTypeAttribute(processTypeAttr);
        }

        final String recipientAttribute = rawConfig.getEncryptedKeyRecipientAttribute();
        if (recipientAttribute != null) {
            final String resolvedRecipientAttr = ExpandVariables.process(recipientAttribute, variableMap, audit);
            resolvedConfig.setEncryptedKeyRecipientAttribute(resolvedRecipientAttr);
        }

        return resolvedConfig;
    }

    public static class InvalidResolvedValueException extends Exception {
        public InvalidResolvedValueException(String message) {
            super(message);
        }
    }


    private static X509Certificate getCertFromBase64(String base64) throws IOException, CertificateException {
        return CertUtils.decodeFromPEM(base64, false);
    }
}

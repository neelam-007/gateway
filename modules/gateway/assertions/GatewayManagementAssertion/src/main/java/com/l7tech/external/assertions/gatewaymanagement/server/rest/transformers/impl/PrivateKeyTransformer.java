package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PrivateKeyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.ParseException;

@Component
public class PrivateKeyTransformer implements EntityAPITransformer<PrivateKeyMO, SsgKeyEntry> {

    @Inject
    protected PrivateKeyResourceFactory factory;

    @NotNull
    @Override
    public Item<PrivateKeyMO> convertToItem(@NotNull PrivateKeyMO m) {
        return new ItemBuilder<PrivateKeyMO>(m.getAlias(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @Override
    @NotNull
    public String getResourceType() {
        return factory.getType().toString();
    }

    @NotNull
    @Override
    public PrivateKeyMO convertToMO(@NotNull EntityContainer<SsgKeyEntry> ssgKeyEntryEntityContainer, SecretsEncryptor secretsEncryptor) {
        return convertToMO(ssgKeyEntryEntityContainer.getEntity(), secretsEncryptor);
    }

    @NotNull
    public PrivateKeyMO convertToMO(@NotNull SsgKeyEntry e) {
        return convertToMO(e, null);
    }

    @NotNull
    @Override
    public PrivateKeyMO convertToMO(@NotNull SsgKeyEntry ssgKeyEntry, SecretsEncryptor secretsEncryptor) {
        //need to 'identify' the MO because by default the wsman factories will no set the id and version in the
        // asResource method
        PrivateKeyMO mo = factory.identify(factory.asResource(ssgKeyEntry), ssgKeyEntry);

        if (secretsEncryptor != null) {
            // Get encoded form of RSA, EC or DSA key
            final PrivateKey pk;
            try {
                pk = ssgKeyEntry.getPrivateKey();
            } catch (UnrecoverableKeyException e) {
                throw new RuntimeException("Could not retrieve private key for export. Key: " + ssgKeyEntry.getId() + " Message: " + ExceptionUtils.getMessage(e));
            }
            final String keyFormat = pk.getFormat().toUpperCase();
            if ("PKCS8".equals(keyFormat) || "PKCS#8".equals(keyFormat)) {
                final String encryptedKeyBytes = secretsEncryptor.encryptSecret(new Base64().encodeAsString(pk.getEncoded()));
                final AttributeExtensibleType.AttributeExtensibleString keyData = new AttributeExtensibleType.AttributeExtensibleString();
                keyData.setValue(encryptedKeyBytes);
                keyData.setAttributeExtensions(CollectionUtils.<QName, Object>mapBuilder()
                        .put(new QName("algorithm"), pk.getAlgorithm()) // either "RSA", "EC", or "DSA"
                        .put(new QName("bundleKey"), secretsEncryptor.getWrappedBundleKey()).map());
                mo.getProperties().put("keyData", keyData);
            } else {
                throw new RuntimeException("Could not retrieve private key for export. Key: " + ssgKeyEntry.getId() + " Message: Unexpected format " + keyFormat + ", expected PKCS8");
            }
        }

        return mo;
    }

    @NotNull
    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(@NotNull PrivateKeyMO m, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        return convertFromMO(m, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<SsgKeyEntry> convertFromMO(@NotNull PrivateKeyMO m, boolean strict, SecretsEncryptor secretsEncryptor) throws ResourceFactory.InvalidResourceException {
        SsgKeyEntry ssgKeyEntry = new SsgKeyEntry(Goid.parseGoid(m.getKeystoreId()), m.getAlias(), factory.toCertificateArray(m.getCertificateChain()), null);

        if (m.getProperties() != null && m.getProperties().containsKey("keyData")) {
            //If the key data is available in the properties then decrypt it.
            final AttributeExtensibleType.AttributeExtensibleString keyData = (AttributeExtensibleType.AttributeExtensibleString) m.getProperties().get("keyData");
            final String algorithm = (String) keyData.getAttributeExtensions().get(new QName("algorithm"));
            final String bundleKey = (String) keyData.getAttributeExtensions().get(new QName("bundleKey"));

            final String decryptedKeyString;
            try {
                decryptedKeyString = secretsEncryptor.decryptSecret(keyData.getValue(), bundleKey);
            } catch (ParseException e) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid private key data");
            }

            // Convert PKCS#8 bytes back into RSA, EC or DSA private key instance
            try {
                final PrivateKey privateKey = KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(new Base64().decode(decryptedKeyString)));
                ssgKeyEntry.setPrivateKey(privateKey);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new ResourceFactory.InvalidResourceException(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid private key data: " + ExceptionUtils.getMessage(e));
            }
        }

        return new EntityContainer<>(ssgKeyEntry);
    }
}

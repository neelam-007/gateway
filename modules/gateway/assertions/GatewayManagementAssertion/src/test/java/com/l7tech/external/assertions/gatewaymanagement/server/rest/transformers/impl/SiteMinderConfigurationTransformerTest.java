package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EntityManagerResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.SiteMinderConfigurationResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.util.Charsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.ParseException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author alee, 1/20/2015
 */
@RunWith(MockitoJUnitRunner.class)
public class SiteMinderConfigurationTransformerTest {
    private static final String UNENCRYPTED_SECRET = "top secret";
    private static final String ENCRYPTED_SECRET = "encrypted top secret";
    private static final String BUNDLE_KEY = "abc123";
    @Mock
    private SiteMinderConfigurationResourceFactory factory;
    @Mock
    private SecretsEncryptor encryptor;
    @Mock
    private EntityManagerResourceFactory.EntityBag<SiteMinderConfiguration> entityBag;
    private SiteMinderConfigurationTransformer transformer;
    private SiteMinderConfiguration config;
    private SiteMinderConfigurationMO mo;

    @Before
    public void setup() throws Exception {
        transformer = new SiteMinderConfigurationTransformer();
        transformer.setFactory(factory);
        config = new SiteMinderConfiguration();
        mo = ManagedObjectFactory.createSiteMinderConfiguration();
    }

    @Test
    public void convertToMOEncryptsSecret() throws Exception {
        config.setSecret(UNENCRYPTED_SECRET);
        when(factory.asResource(config)).thenReturn(ManagedObjectFactory.createSiteMinderConfiguration());
        when(encryptor.encryptSecret(UNENCRYPTED_SECRET.getBytes(Charsets.UTF8))).thenReturn(ENCRYPTED_SECRET);
        when(encryptor.getWrappedBundleKey()).thenReturn(BUNDLE_KEY);
        final SiteMinderConfigurationMO mo = transformer.convertToMO(config, encryptor);
        assertEquals(ENCRYPTED_SECRET, mo.getSecret());
        assertEquals(BUNDLE_KEY, mo.getSecretBundleKey());
    }

    @Test
    public void convertToMONulLEncryptor() throws Exception {
        config.setSecret(UNENCRYPTED_SECRET);
        when(factory.asResource(config)).thenReturn(ManagedObjectFactory.createSiteMinderConfiguration());
        final SiteMinderConfigurationMO mo = transformer.convertToMO(config, null);
        assertNull(mo.getSecret());
        assertNull(mo.getSecretBundleKey());
    }

    @Test
    public void convertToMONullSecret() throws Exception {
        config.setSecret(null);
        when(factory.asResource(config)).thenReturn(ManagedObjectFactory.createSiteMinderConfiguration());
        final SiteMinderConfigurationMO mo = transformer.convertToMO(config, encryptor);
        assertNull(mo.getSecret());
        assertNull(mo.getSecretBundleKey());
        verify(encryptor, never()).encryptSecret(any(byte[].class));
    }

    @Test
    public void convertFromMODecryptsSecret() throws Exception {
        mo.setEncryptedSecret(ENCRYPTED_SECRET, BUNDLE_KEY);
        config.setSecret(ENCRYPTED_SECRET);
        setupMOToEntity(mo, config);
        when(encryptor.decryptSecret(ENCRYPTED_SECRET, BUNDLE_KEY)).thenReturn(UNENCRYPTED_SECRET.getBytes(Charsets.UTF8));
        final SiteMinderConfiguration result = transformer.convertFromMO(mo, false, encryptor).getEntity();
        assertEquals(UNENCRYPTED_SECRET, result.getSecret());
    }

    @Test
    public void convertFromMONullEncryptor() throws Exception {
        mo.setEncryptedSecret(ENCRYPTED_SECRET, BUNDLE_KEY);
        config.setSecret(ENCRYPTED_SECRET);
        setupMOToEntity(mo, config);
        final SiteMinderConfiguration result = transformer.convertFromMO(mo, false, null).getEntity();
        assertEquals(ENCRYPTED_SECRET, result.getSecret());
    }

    @Test
    public void convertFromMONullSecret() throws Exception {
        mo.setSecret(null);
        config.setSecret(null);
        setupMOToEntity(mo, config);
        final SiteMinderConfiguration result = transformer.convertFromMO(mo, false, encryptor).getEntity();
        assertNull(result.getSecret());
        verify(encryptor, never()).decryptSecret(anyString(), anyString());
    }

    @Test
    public void convertFromMONullBundleKey() throws Exception {
        mo.setSecret(UNENCRYPTED_SECRET);
        config.setSecret(UNENCRYPTED_SECRET);
        setupMOToEntity(mo, config);
        final SiteMinderConfiguration result = transformer.convertFromMO(mo, false, encryptor).getEntity();
        assertEquals(UNENCRYPTED_SECRET, result.getSecret());
        verify(encryptor, never()).decryptSecret(anyString(), anyString());
    }

    @Test(expected= ResourceFactory.InvalidResourceException.class)
    public void convertFromMOErrorDecryptingSecret() throws Exception {
        mo.setEncryptedSecret(ENCRYPTED_SECRET, BUNDLE_KEY);
        setupMOToEntity(mo, config);
        when(encryptor.decryptSecret(ENCRYPTED_SECRET, BUNDLE_KEY)).thenThrow(new ParseException("mocked exception", 0));
        try {
            transformer.convertFromMO(mo, false, encryptor).getEntity();
            fail("Expected InvalidResourceException");
        } catch (final ResourceFactory.InvalidResourceException e) {
            assertEquals(ResourceFactory.InvalidResourceException.ExceptionType.INVALID_VALUES, e.getType());
            assertEquals("Resource validation failed due to 'INVALID_VALUES' Failed to decrypt password", e.getMessage());
            throw e;
        }
    }

    private void setupMOToEntity(final SiteMinderConfigurationMO expectedMO, final SiteMinderConfiguration resultConfig) throws ResourceFactory.InvalidResourceException {
        when(factory.fromResourceAsBag(expectedMO, false)).thenReturn(entityBag);
        when(entityBag.getEntity()).thenReturn(resultConfig);
    }
}

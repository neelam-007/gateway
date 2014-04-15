package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.io.*;
import com.l7tech.gateway.api.CertificateData;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.TrustedCertificateMO;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.ParamsKeyGenerator;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import junit.framework.Assert;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import org.junit.After;
import org.junit.Before;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class CertificateRestEntityResourceTest extends RestEntityTests<TrustedCert, TrustedCertificateMO> {
    private TrustedCertManager trustedCertManager;
    private List<TrustedCert> trustedCerts = new ArrayList<>();
    private RevocationCheckPolicyManager revocationCheckPolicyManager;
    private RevocationCheckPolicy revocationCheckPolicy;

    private CertGenParams c;
    private KeyGenParams k;

    private static final SecureRandom defaultRandom = new SecureRandom();
    private SecureRandom random;
    private String signatureProviderName;

    @Before
    public void before() throws SaveException, NoSuchAlgorithmException, CertificateException, InvalidAlgorithmParameterException, CertificateGeneratorException {
        init();

        trustedCertManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("trustedCertManager", TrustedCertManager.class);
        revocationCheckPolicyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("revocationCheckPolicyManager", RevocationCheckPolicyManager.class);

        revocationCheckPolicy = new RevocationCheckPolicy();
        revocationCheckPolicy.setName("My Revocation Check Policy");
        revocationCheckPolicyManager.save(revocationCheckPolicy);

        //Create the certs
        TrustedCert trustedCert = new TrustedCert();
        trustedCert.setName("Cert 1");
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.SPECIFIED);
        trustedCert.setRevocationCheckPolicyOid(revocationCheckPolicy.getGoid());
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_SERVER_CERTS, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ATTESTING_ENTITY, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ISSUER, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SSL, false);
        trustedCert.setTrustAnchor(true);
        trustedCert.setVerifyHostname(true);
        trustedCert.setCertificate(generateCert());

        trustedCertManager.save(trustedCert);
        trustedCerts.add(trustedCert);

        trustedCert = new TrustedCert();
        trustedCert.setName("Cert 2");
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.USE_DEFAULT);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS, true);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_SERVER_CERTS, true);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ATTESTING_ENTITY, true);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ISSUER, true);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SSL, true);
        trustedCert.setTrustAnchor(false);
        trustedCert.setVerifyHostname(false);
        trustedCert.setCertificate(generateCert());

        trustedCertManager.save(trustedCert);
        trustedCerts.add(trustedCert);

        trustedCert = new TrustedCert();
        trustedCert.setName("Cert 3");
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SIGNING_SERVER_CERTS, true);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ATTESTING_ENTITY, false);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SAML_ISSUER, true);
        trustedCert.setTrustedFor(TrustedCert.TrustedFor.SSL, false);
        trustedCert.setTrustAnchor(true);
        trustedCert.setVerifyHostname(false);
        trustedCert.setCertificate(generateCert());

        trustedCertManager.save(trustedCert);
        trustedCerts.add(trustedCert);

    }

    public void init() {
        random = defaultRandom;
        signatureProviderName = null;

        k = new KeyGenParams();
        k.setAlgorithm("RSA");
        k.setNamedParam("sect163k1");
        k.setKeySize(1024);

        c = new CertGenParams();
        c.setSerialNumber(new BigInteger(64, random).abs());
        c.setNotBefore(new Date(new Date().getTime() - (10 * 60 * 1000L))); // default: 10 min ago
        c.setDaysUntilExpiry(20 * 365);
        c.setNotAfter(null);
        c.setSignatureAlgorithm(null);
        c.setSubjectDn(new X500Principal("cn=test"));
        c.setIncludeBasicConstraints(false);
        c.setKeyUsageBits(X509KeyUsage.digitalSignature | X509KeyUsage.keyEncipherment | X509KeyUsage.nonRepudiation);
        c.setIncludeKeyUsage(true);
        c.setKeyUsageCritical(true);
        c.setIncludeSki(true);
        c.setIncludeAki(true);
        c.setIncludeExtendedKeyUsage(true);
        c.setExtendedKeyUsageCritical(true);
        c.setExtendedKeyUsageKeyPurposeOids(Arrays.asList(KeyPurposeId.anyExtendedKeyUsage.getId()));
        c.setIncludeSubjectDirectoryAttributes(false);
        c.setSubjectDirectoryAttributesCritical(false);
        c.setCountryOfCitizenshipCountryCodes(Collections.<String>emptyList());
        c.setCertificatePolicies(Collections.<String>emptyList());
        c.setSubjectAlternativeNames(Collections.<X509GeneralName>emptyList());
    }

    private X509Certificate generateCert() throws CertificateGeneratorException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, CertificateException {
        final KeyPair subjectKeyPair = new ParamsKeyGenerator(k, random).generateKeyPair();
        final PublicKey subjectPublicKey = subjectKeyPair.getPublic();
        final PrivateKey subjectPrivateKey = subjectKeyPair.getPrivate();

        ParamsCertificateGenerator certgen = new ParamsCertificateGenerator(c, random, signatureProviderName);
        return (X509Certificate) CertUtils.getFactory().generateCertificate(new ByteArrayInputStream(certgen.generateCertificate(subjectPublicKey, subjectPrivateKey, null).getEncoded()));
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<TrustedCert> all = trustedCertManager.findAll();
        for (TrustedCert trustedCert : all) {
            trustedCertManager.delete(trustedCert.getGoid());
        }

        revocationCheckPolicyManager.delete(revocationCheckPolicy);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(trustedCerts, new Functions.Unary<String, TrustedCert>() {
            @Override
            public String call(TrustedCert trustedCert) {
                return trustedCert.getId();
            }
        });
    }

    @Override
    public List<TrustedCertificateMO> getCreatableManagedObjects() {
        List<TrustedCertificateMO> trustedCertificateMOs = new ArrayList<>();

        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setId(getGoid().toString());
        trustedCertificateMO.setName("Created Trusted Cert");
        trustedCertificateMO.setRevocationCheckingPolicyId(revocationCheckPolicy.getId());
        trustedCertificateMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder()
                .put("trustedForSigningClientCerts", true)
                .put("trustedForSigningServerCerts", true)
                .put("trustedAsSamlAttestingEntity", true)
                .put("trustedAsSamlIssuer", true)
                .put("trustedForSsl", true)
                .put("trustAnchor", true)
                .put("verifyHostname", true)
                .put("revocationCheckingEnabled", true)
                .map());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        final X509Certificate x509Certificate;
        try {
            x509Certificate = generateCert();
            certificateData.setEncoded(x509Certificate.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        certificateData.setIssuerName(x509Certificate.getIssuerDN().getName());
        certificateData.setSubjectName(x509Certificate.getSubjectDN().getName());
        certificateData.setSerialNumber(x509Certificate.getSerialNumber());
        trustedCertificateMO.setCertificateData(certificateData);
        trustedCertificateMOs.add(trustedCertificateMO);

        return trustedCertificateMOs;
    }

    @Override
    public List<TrustedCertificateMO> getUpdateableManagedObjects() {
        List<TrustedCertificateMO> trustedCertificateMOs = new ArrayList<>();

        TrustedCert trustedCert = this.trustedCerts.get(0);
        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setId(trustedCert.getId());
        trustedCertificateMO.setName(trustedCert.getName() + " Updated");
        trustedCertificateMO.setRevocationCheckingPolicyId(trustedCert.getRevocationCheckPolicyOid().toString());
        trustedCertificateMO.setProperties(CollectionUtils.MapBuilder.<String,Object>builder()
                .put("trustedForSigningClientCerts", trustedCert.isTrustedForSigningClientCerts())
                .put("trustedForSigningServerCerts", trustedCert.isTrustedForSigningServerCerts())
                .put("trustedAsSamlAttestingEntity", trustedCert.isTrustedAsSamlAttestingEntity())
                .put("trustedAsSamlIssuer", trustedCert.isTrustedAsSamlIssuer())
                .put("trustedForSsl", trustedCert.isTrustedForSsl())
                .put("trustAnchor", trustedCert.isTrustAnchor())
                .put("verifyHostname", trustedCert.isVerifyHostname())
                .put("revocationCheckingEnabled", true)
                .map());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        try {
            certificateData.setEncoded(trustedCert.getCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        certificateData.setIssuerName(trustedCert.getIssuerDn());
        certificateData.setSubjectName(trustedCert.getSubjectDn());
        certificateData.setSerialNumber(trustedCert.getSerial());
        trustedCertificateMO.setCertificateData(certificateData);
        trustedCertificateMOs.add(trustedCertificateMO);

        return trustedCertificateMOs;
    }

    @Override
    public Map<TrustedCertificateMO, Functions.BinaryVoid<TrustedCertificateMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<TrustedCertificateMO, Functions.BinaryVoid<TrustedCertificateMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        TrustedCert trustedCert = this.trustedCerts.get(0);

        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName(trustedCert.getName() + " Updated");
        trustedCertificateMO.setRevocationCheckingPolicyId(revocationCheckPolicy.getId());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        try {
            certificateData.setEncoded(trustedCert.getCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        certificateData.setIssuerName(trustedCert.getIssuerDn());
        certificateData.setSubjectName(trustedCert.getSubjectDn());
        certificateData.setSerialNumber(trustedCert.getSerial());
        trustedCertificateMO.setCertificateData(certificateData);

        builder.put(trustedCertificateMO, new Functions.BinaryVoid<TrustedCertificateMO, RestResponse>() {
            @Override
            public void call(TrustedCertificateMO trustedCertificateMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });


        trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setName(trustedCert.getName() + " Updated");
        trustedCertificateMO.setRevocationCheckingPolicyId(new Goid(567,890).toString());
        trustedCertificateMO.setProperties(CollectionUtils.MapBuilder.<String,Object>builder()
                .put("trustedForSigningClientCerts", trustedCert.isTrustedForSigningClientCerts())
                .put("trustedForSigningServerCerts", trustedCert.isTrustedForSigningServerCerts())
                .put("trustedAsSamlAttestingEntity", trustedCert.isTrustedAsSamlAttestingEntity())
                .put("trustedAsSamlIssuer", trustedCert.isTrustedAsSamlIssuer())
                .put("trustedForSsl", trustedCert.isTrustedForSsl())
                .put("trustAnchor", trustedCert.isTrustAnchor())
                .put("verifyHostname", trustedCert.isVerifyHostname())
                .put("revocationCheckingEnabled", true)
                .map());
        certificateData = ManagedObjectFactory.createCertificateData();
        final X509Certificate x509Certificate;
        try {
            x509Certificate = generateCert();
            certificateData.setEncoded(x509Certificate.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        certificateData.setIssuerName(x509Certificate.getIssuerDN().getName());
        certificateData.setSubjectName(x509Certificate.getSubjectDN().getName());
        certificateData.setSerialNumber(x509Certificate.getSerialNumber());
        trustedCertificateMO.setCertificateData(certificateData);

        builder.put(trustedCertificateMO, new Functions.BinaryVoid<TrustedCertificateMO, RestResponse>() {
            @Override
            public void call(TrustedCertificateMO trustedCertificateMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<TrustedCertificateMO, Functions.BinaryVoid<TrustedCertificateMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<TrustedCertificateMO, Functions.BinaryVoid<TrustedCertificateMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        TrustedCert trustedCert = this.trustedCerts.get(0);

        TrustedCertificateMO trustedCertificateMO = ManagedObjectFactory.createTrustedCertificate();
        trustedCertificateMO.setId(this.trustedCerts.get(1).getId());
        trustedCertificateMO.setName(trustedCert.getName());
        trustedCertificateMO.setRevocationCheckingPolicyId(trustedCert.getRevocationCheckPolicyOid().toString());
        CertificateData certificateData = ManagedObjectFactory.createCertificateData();
        try {
            certificateData.setEncoded(trustedCert.getCertificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        certificateData.setIssuerName(trustedCert.getIssuerDn());
        certificateData.setSubjectName(trustedCert.getSubjectDn());
        certificateData.setSerialNumber(trustedCert.getSerial());
        trustedCertificateMO.setCertificateData(certificateData);

        builder.put(trustedCertificateMO, new Functions.BinaryVoid<TrustedCertificateMO, RestResponse>() {
            @Override
            public void call(TrustedCertificateMO trustedCertificateMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(trustedCerts, new Functions.Unary<String, TrustedCert>() {
            @Override
            public String call(TrustedCert trustedCert) {
                return trustedCert.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "trustedCertificates";
    }

    @Override
    public String getType() {
        return EntityType.TRUSTED_CERT.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        TrustedCert entity = trustedCertManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        TrustedCert entity = trustedCertManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, TrustedCertificateMO managedObject) throws FindException {
        TrustedCert entity = trustedCertManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getRevocationCheckPolicyOid() == null ? null : entity.getRevocationCheckPolicyOid().toString(), managedObject.getRevocationCheckingPolicyId());
            Assert.assertEquals(entity.getIssuerDn(), managedObject.getCertificateData().getIssuerName().toLowerCase());
            Assert.assertEquals(entity.getSubjectDn(), managedObject.getCertificateData().getSubjectName().toLowerCase());
            Assert.assertEquals(entity.getCertBase64(), HexUtils.encodeBase64(managedObject.getCertificateData().getEncoded()));
            Assert.assertEquals(entity.getSerial(), managedObject.getCertificateData().getSerialNumber());

            Assert.assertNotNull(managedObject.getProperties());
            Assert.assertEquals(entity.isTrustedFor(TrustedCert.TrustedFor.SIGNING_CLIENT_CERTS), managedObject.getProperties().get("trustedForSigningClientCerts"));
            Assert.assertEquals(entity.isTrustedFor(TrustedCert.TrustedFor.SIGNING_SERVER_CERTS), managedObject.getProperties().get("trustedForSigningServerCerts"));
            Assert.assertEquals(entity.isTrustedFor(TrustedCert.TrustedFor.SAML_ATTESTING_ENTITY), managedObject.getProperties().get("trustedAsSamlAttestingEntity"));
            Assert.assertEquals(entity.isTrustedFor(TrustedCert.TrustedFor.SAML_ISSUER), managedObject.getProperties().get("trustedAsSamlIssuer"));
            Assert.assertEquals(entity.isTrustedFor(TrustedCert.TrustedFor.SSL), managedObject.getProperties().get("trustedForSsl"));

            Assert.assertEquals(entity.isTrustAnchor(), managedObject.getProperties().get("trustAnchor"));
            Assert.assertEquals(entity.isVerifyHostname(), managedObject.getProperties().get("verifyHostname"));
            Assert.assertEquals(!TrustedCert.PolicyUsageType.NONE.equals(entity.getRevocationCheckPolicyType()), managedObject.getProperties().get("revocationCheckingEnabled"));

        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(trustedCerts, new Functions.Unary<String, TrustedCert>() {
                    @Override
                    public String call(TrustedCert trustedCert) {
                        return trustedCert.getId();
                    }
                }))
//                .put("name=" + URLEncoder.encode(activeConnectors.get(0).getName()), Arrays.asList(activeConnectors.get(0).getId()))
//                .put("name=" + URLEncoder.encode(activeConnectors.get(0).getName()) + "&name=" + URLEncoder.encode(activeConnectors.get(1).getName()), Functions.map(activeConnectors.subList(0, 2), new Functions.Unary<String, SsgActiveConnector>() {
//                    @Override
//                    public String call(SsgActiveConnector ssgActiveConnector) {
//                        return ssgActiveConnector.getId();
//                    }
//                }))
//                .put("name=banName", Collections.<String>emptyList())
//                .put("enabled=false", Arrays.asList(activeConnectors.get(1).getId()))
//                .put("enabled=true", Arrays.asList(activeConnectors.get(0).getId(), activeConnectors.get(2).getId()))
//                .put("type=MqNative", Arrays.asList(activeConnectors.get(0).getId(), activeConnectors.get(2).getId()))
//                .put("type=SFTP", Arrays.asList(activeConnectors.get(1).getId()))
//                .put("hardwiredServiceId=" + new Goid(123, 123).toString(), Arrays.asList(activeConnectors.get(0).getId()))
//                .put("hardwiredServiceId", Arrays.asList(activeConnectors.get(2).getId()))
//                .put("name=" + URLEncoder.encode(activeConnectors.get(0).getName()) + "&name=" + URLEncoder.encode(activeConnectors.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(activeConnectors.get(1).getId(), activeConnectors.get(0).getId()))
                .map();
    }
}

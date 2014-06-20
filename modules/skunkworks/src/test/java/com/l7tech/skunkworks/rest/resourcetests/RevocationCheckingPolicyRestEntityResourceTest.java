package com.l7tech.skunkworks.rest.resourcetests;


import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.identity.cert.RevocationCheckPolicyManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import sun.security.provider.certpath.OCSP;

import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.*;


@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class RevocationCheckingPolicyRestEntityResourceTest extends RestEntityTests<RevocationCheckPolicy, RevocationCheckingPolicyMO> {

    private RevocationCheckPolicyManager revocationCheckPolicyManager;
    private List<RevocationCheckPolicy> checkPolicies = new ArrayList<>();
    private TrustedCertManager trustedCertManager;
    private TrustedCert trustedCert = new TrustedCert();


    @Before
    public void before() throws Exception {
        revocationCheckPolicyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("revocationCheckPolicyManager", RevocationCheckPolicyManager.class);
        trustedCertManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("trustedCertManager", TrustedCertManager.class);

        // create cert
        X509Certificate certificate = new TestCertificateGenerator().subject("cn=test").generate();
        trustedCert.setName(CertUtils.extractFirstCommonNameFromCertificate(certificate));
        trustedCert.setCertificate(certificate);
        trustedCert.setTrustAnchor(false);
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);
        trustedCertManager.save(trustedCert);

        //Create the checkPolicies
        RevocationCheckPolicyItem checkPolicyItem = new RevocationCheckPolicyItem();
        checkPolicyItem.setAllowIssuerSignature(true);
        checkPolicyItem.setType(RevocationCheckPolicyItem.Type.CRL_FROM_CERTIFICATE);
        checkPolicyItem.setUrl("URL");
        checkPolicyItem.setTrustedSigners(CollectionUtils.list(trustedCert.getGoid()));

        RevocationCheckPolicy checkPolicy = new RevocationCheckPolicy();
        checkPolicy.setName("RevocationCheckPolicy1");
        checkPolicy.setRevocationCheckItems(CollectionUtils.list(checkPolicyItem));
        checkPolicy.setDefaultPolicy(true);
        checkPolicy.setContinueOnServerUnavailable(false);
        checkPolicy.setDefaultSuccess(true);

        revocationCheckPolicyManager.save(checkPolicy);
        checkPolicies.add(checkPolicy);

        checkPolicy = new RevocationCheckPolicy();
        checkPolicy.setName("RevocationCheckPolicy2");
        checkPolicy.setRevocationCheckItems(CollectionUtils.list(checkPolicyItem));
        checkPolicy.setDefaultPolicy(false);
        checkPolicy.setContinueOnServerUnavailable(true);
        checkPolicy.setDefaultSuccess(true);
        revocationCheckPolicyManager.save(checkPolicy);
        checkPolicies.add(checkPolicy);

        checkPolicy = new RevocationCheckPolicy();
        checkPolicy.setName("RevocationCheckPolicy3");
        checkPolicy.setRevocationCheckItems(CollectionUtils.list(checkPolicyItem));
        checkPolicy.setContinueOnServerUnavailable(false);
        checkPolicy.setDefaultSuccess(false);

        revocationCheckPolicyManager.save(checkPolicy);
        checkPolicies.add(checkPolicy);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<EntityHeader> all = revocationCheckPolicyManager.findAllHeaders();
        for (EntityHeader checkPolicy : all) {
            revocationCheckPolicyManager.delete(checkPolicy.getGoid());
        }
        trustedCertManager.delete(trustedCert);
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(checkPolicies, new Functions.Unary<String, RevocationCheckPolicy>() {
            @Override
            public String call(RevocationCheckPolicy checkPolicy) {
                return checkPolicy.getId();
            }
        });
    }

    @Override
    public List<RevocationCheckingPolicyMO> getCreatableManagedObjects() {
        List<RevocationCheckingPolicyMO> checkPolicies = new ArrayList<>();

        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName("Create Rev Check Policy");
        checkPolicyMO.setId(getGoid().toString());
        checkPolicyMO.setDefaultPolicy(true);
        RevocationCheckingPolicyItemMO checkItem = ManagedObjectFactory.createRevocationCheckingPolicyItem();
        checkItem.setType(RevocationCheckingPolicyItemMO.Type.CRL_FROM_CERTIFICATE);
        checkItem.setUrl(".*");
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        checkPolicies.add(checkPolicyMO);

        // with trusted cert reference
        checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName("Create Rev Check Policy 2");
        checkPolicyMO.setId(getGoid().toString());
        checkPolicyMO.setDefaultPolicy(true);
        checkItem = ManagedObjectFactory.createRevocationCheckingPolicyItem();
        checkItem.setType(RevocationCheckingPolicyItemMO.Type.CRL_FROM_CERTIFICATE);
        checkItem.setUrl(".*");
        checkItem.setTrustedSigners(CollectionUtils.list(trustedCert.getId()));
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        checkPolicies.add(checkPolicyMO);

        // no rev check items
        checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName("Create Rev Check Policy 3");
        checkPolicyMO.setId(getGoid().toString());
        checkPolicyMO.setDefaultPolicy(true);
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        checkPolicies.add(checkPolicyMO);

        return checkPolicies;
    }

    @Override
    public List<RevocationCheckingPolicyMO> getUpdateableManagedObjects() {
        List<RevocationCheckingPolicyMO> checkPolicies = new ArrayList<>();

        RevocationCheckPolicy checkPolicy = this.checkPolicies.get(0);
        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setId(checkPolicy.getId());
        checkPolicyMO.setName(checkPolicy.getName() + " Updated");
        checkPolicies.add(checkPolicyMO);

        //update twice
        checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setId(checkPolicy.getId());
        checkPolicyMO.setName(checkPolicy.getName() + " Updated");
        checkPolicies.add(checkPolicyMO);

        return checkPolicies;
    }

    @Override
    public Map<RevocationCheckingPolicyMO, Functions.BinaryVoid<RevocationCheckingPolicyMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<RevocationCheckingPolicyMO, Functions.BinaryVoid<RevocationCheckingPolicyMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setName(checkPolicies.get(0).getName());
        checkPolicyMO.setId(getGoid().toString());
        checkPolicyMO.setDefaultPolicy(true);
        RevocationCheckingPolicyItemMO checkItem = ManagedObjectFactory.createRevocationCheckingPolicyItem();
        checkItem.setType(RevocationCheckingPolicyItemMO.Type.CRL_FROM_CERTIFICATE);
        checkItem.setUrl(".*");
        checkPolicyMO.setRevocationCheckItems(CollectionUtils.list(checkItem));
        builder.put(checkPolicyMO, new Functions.BinaryVoid<RevocationCheckingPolicyMO, RestResponse>() {
            @Override
            public void call(RevocationCheckingPolicyMO checkPolicyMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<RevocationCheckingPolicyMO, Functions.BinaryVoid<RevocationCheckingPolicyMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<RevocationCheckingPolicyMO, Functions.BinaryVoid<RevocationCheckingPolicyMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();


        //same name as another connection
        RevocationCheckPolicy checkPolicy = checkPolicies.get(0);
        RevocationCheckingPolicyMO checkPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        checkPolicyMO.setId(checkPolicy.getId());
        checkPolicyMO.setName(checkPolicies.get(1).getName());
        builder.put(checkPolicyMO, new Functions.BinaryVoid<RevocationCheckingPolicyMO, RestResponse>() {
            @Override
            public void call(RevocationCheckingPolicyMO checkPolicyMO, RestResponse restResponse) {
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
        return Functions.map(checkPolicies, new Functions.Unary<String, RevocationCheckPolicy>() {
            @Override
            public String call(RevocationCheckPolicy checkPolicy) {
                return checkPolicy.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "revocationCheckingPolicies";
    }

    @Override
    public String getType() {
        return EntityType.REVOCATION_CHECK_POLICY.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        RevocationCheckPolicy entity = revocationCheckPolicyManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        RevocationCheckPolicy entity = revocationCheckPolicyManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, RevocationCheckingPolicyMO managedObject) throws FindException {
        RevocationCheckPolicy entity = revocationCheckPolicyManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.isContinueOnServerUnavailable(), managedObject.isContinueOnServerUnavailable().booleanValue());
            Assert.assertEquals(entity.isDefaultPolicy(), managedObject.isDefaultPolicy().booleanValue());
            Assert.assertEquals(entity.isDefaultSuccess(), managedObject.isDefaultSuccess().booleanValue());


            Assert.assertEquals(entity.getRevocationCheckItems().size(), managedObject.getRevocationCheckItems().size());
            for(int i = 0 ; i < entity.getRevocationCheckItems().size() ; ++i){
                RevocationCheckingPolicyItemMO itemMO = managedObject.getRevocationCheckItems().get(i);
                RevocationCheckPolicyItem item = entity.getRevocationCheckItems().get(i);
                Assert.assertEquals(item.getUrl(), itemMO.getUrl());
                Assert.assertEquals(item.isAllowIssuerSignature(), itemMO.isAllowIssuerSignature());
                Assert.assertEquals(item.getType().name(), itemMO.getType().name());
                org.junit.Assert.assertArrayEquals(Functions.map(item.getTrustedSigners(), new Functions.Unary<String, Goid>() {
                    @Override
                    public String call(Goid goid) {
                        return goid.toString();
                    }
                }).toArray(), itemMO.getTrustedSigners().toArray());
            }

        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {

        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Arrays.asList(checkPolicies.get(0).getId(),checkPolicies.get(1).getId(),checkPolicies.get(2).getId()))
                .put("sort=name", Arrays.asList(checkPolicies.get(0).getId(),checkPolicies.get(1).getId(),checkPolicies.get(2).getId()))
                .put("sort=id", Arrays.asList(checkPolicies.get(0).getId(),checkPolicies.get(1).getId(),checkPolicies.get(2).getId()))
                .put("sort=name&order=desc", Arrays.asList(checkPolicies.get(2).getId(),checkPolicies.get(1).getId(),checkPolicies.get(0).getId()))
                .put("name=" + URLEncoder.encode(checkPolicies.get(0).getName()), Arrays.asList(checkPolicies.get(0).getId()))
                .put("name=" + URLEncoder.encode(checkPolicies.get(0).getName()) + "&name=" + URLEncoder.encode(checkPolicies.get(1).getName()), Functions.map(checkPolicies.subList(0, 2), new Functions.Unary<String, RevocationCheckPolicy>() {
                    @Override
                    public String call(RevocationCheckPolicy checkPolicy) {
                        return checkPolicy.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .map();
    }
}

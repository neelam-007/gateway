package com.l7tech.server.encass;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.admin.EncapsulatedAssertionAdmin;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Implementation of {@link EncapsulatedAssertionAdmin}.
 */
public class EncapsulatedAssertionAdminImpl implements EncapsulatedAssertionAdmin {
    @Inject
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;

    @Inject
    private LicenseManager licenseManager;

    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> findAllEncapsulatedAssertionConfigs() throws FindException {
        Collection<EncapsulatedAssertionConfig> ret = encapsulatedAssertionConfigManager.findAll();
        return ret != null ? ret : Collections.<EncapsulatedAssertionConfig>emptyList();
    }

    @NotNull
    @Override
    public EncapsulatedAssertionConfig findByPrimaryKey(long oid) throws FindException {
        EncapsulatedAssertionConfig ret = encapsulatedAssertionConfigManager.findByPrimaryKey(oid);
        if (ret == null)
            throw new FindException("No encapsulated assertion config found with oid " + oid);
        return ret;
    }

    @NotNull
    @Override
    public EncapsulatedAssertionConfig findByGuid(@NotNull String guid) throws FindException {
        EncapsulatedAssertionConfig ret = encapsulatedAssertionConfigManager.findByGuid(guid);
        if (ret == null)
            throw new FindException("No encapsulated assertion config found with GUID  " + guid);
        return ret;
    }

    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> findByPolicyOid(long policyOid) throws FindException {
        Collection<EncapsulatedAssertionConfig> ret = encapsulatedAssertionConfigManager.findByPolicyOid(policyOid);
        if (ret == null) {
            ret = Collections.emptySet();
        }
        return ret;
    }

    @Override
    public long saveEncapsulatedAssertionConfig(@NotNull EncapsulatedAssertionConfig config) throws SaveException, UpdateException, VersionException {
        checkLicenseEncAss();
        long oid;

        if (config.getOid() == EncapsulatedAssertionConfig.DEFAULT_OID) {
            if (config.getGuid() != null) {
                // TODO it may end up being necessary to allow this case in order to import an encass created on a different cluster
                throw new SaveException("New encapsulated assertion config already has a GUID assigned");
            }
            config.setGuid(UUID.randomUUID().toString());
            oid = encapsulatedAssertionConfigManager.save(config);
        } else {
            if (config.getGuid() == null)
                throw new UpdateException("Unable to update existing encapsulated assertion to have a null GUID");
            encapsulatedAssertionConfigManager.update(config);
            oid = config.getOid();
        }
        return oid;
    }

    @Override
    public void deleteEncapsulatedAssertionConfig(long oid) throws FindException, DeleteException, ConstraintViolationException {
        checkLicenseEncAss();
        encapsulatedAssertionConfigManager.delete(oid);
    }


    private void checkLicenseEncAss() {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ENCAPSULATED_ASSERTION);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new LicenseRuntimeException(e);
        }
    }
}

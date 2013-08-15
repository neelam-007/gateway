package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.server.GoidEntityManagerStub;

/**
 *
 */
public class SecurityZoneManagerStub extends GoidEntityManagerStub<SecurityZone, EntityHeader> implements SecurityZoneManager {

    public SecurityZoneManagerStub(SecurityZone... securityZonesIn) {
        super(securityZonesIn);
    }

    @Override
    public void addReadSecurityZoneRole(SecurityZone zone) throws SaveException {

    }

    @Override
    public void addManageSecurityZoneRole(SecurityZone zone) throws SaveException {

    }
}

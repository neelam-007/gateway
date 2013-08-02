package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.OidEntityManagerStub;

/**
 *
 */
public class MockSsgNodeManager extends OidEntityManagerStub<SsgNode, EntityHeader> implements SsgNodeManager {

    @Override
    public SsgNode findByGuid(String guid) throws FindException {
        return null;
    }
}
/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.jms.JmsAdmin;
import com.l7tech.jms.JmsProvider;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.identity.StubDataStore;

import java.rmi.RemoteException;
import java.util.Map;

/**
 * Stub-mode JMS admin interface.
 */
public class JmsAdminStub implements JmsAdmin {
    private Map providers;

    public JmsAdminStub() {
        providers = StubDataStore.defaultStore().getJmsProviders();
    }

    public EntityHeader[] findAllProviders() throws RemoteException, FindException {
        return (EntityHeader[]) providers.values().toArray(new EntityHeader[0]);
    }

    public JmsProvider findProviderByPrimaryKey(long oid) throws RemoteException, FindException {
        return (JmsProvider) providers.get(new Long(oid));
    }

    public long saveProvider(JmsProvider provider) throws RemoteException, UpdateException, SaveException, VersionException {
        long oid = provider.getOid();
        if (oid == 0) {
            oid = StubDataStore.defaultStore().nextObjectId();
        }
        provider.setOid(oid);
        Long key = new Long(oid);
        providers.put(key, provider);
        return oid;
    }

    public void deleteProvider(long id) throws RemoteException, DeleteException {
        if (providers.remove(new Long(id)) == null) {
            throw new RemoteException("Could not find jms provider oid= " + id);
        }
    }
}

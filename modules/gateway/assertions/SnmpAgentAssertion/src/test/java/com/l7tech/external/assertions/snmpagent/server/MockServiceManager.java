package com.l7tech.external.assertions.snmpagent.server;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: rseminoff
 * Date: 14/05/12
 */
public class MockServiceManager implements ServiceManager {

    // This pretends to hand out polices defined by the gateway.

    @Override
    public String resolveWsdlTarget(String url) {
        System.out.println("*** CALL *** MockServiceManager: resolveWsdlTarget()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addManageServiceRole(PublishedService service) throws SaveException {
        System.out.println("*** CALL *** MockServiceManager: addManageServiceRole()");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<ServiceHeader> findAllHeaders(boolean includeAliases) throws FindException {
        // This is returning a new POLICY Header...indicating a policy and it's OIDs.
        // Its only relation to SNMPAgent is that the Agent needs the OIDs here to determine what policy we're talking to.
        // Multiple ones are returned to test the finding the OID in the group.
        return new ArrayList<ServiceHeader>() {{
            add(new ServiceHeader(false, false, MockSnmpValues.TEST_SERVICE_NAME, new Goid(0L, 49152L), MockSnmpValues.TEST_SERVICE_NAME, "Test Policy for Unit Testing", new Goid(0L, 0L), new Goid(0L, 49152L), 1L, 1, "/test", false, false, Goid.DEFAULT_GOID, Goid.DEFAULT_GOID));
            add(new ServiceHeader(false, false, MockSnmpValues.TEST_SERVICE_NAME, new Goid(0L, 65535L), MockSnmpValues.TEST_SERVICE_NAME, "Test Policy for Unit Testing", new Goid(0L, 0L), new Goid(0L, 65535L), 1L, 1, "/test", false, false, Goid.DEFAULT_GOID, Goid.DEFAULT_GOID));
            add(new ServiceHeader(false, false, MockSnmpValues.TEST_SERVICE_NAME, new Goid(0L, 131072L), MockSnmpValues.TEST_SERVICE_NAME, "Test Policy for Unit Testing", new Goid(0L, 0L), new Goid(0L, 131072L), 1L, 1, "/test", false, false, Goid.DEFAULT_GOID, Goid.DEFAULT_GOID));
        }};
    }

    @Override
    public Collection<PublishedService> findByRoutingUri(String routingUri) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findByRouting()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateFolder(Goid entityId, Folder folder) throws UpdateException {
        System.out.println("*** CALL *** MockServiceManager: updateFolder(long, Folder)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateFolder(PublishedService entity, Folder folder) throws UpdateException {
        System.out.println("*** CALL *** MockServiceManager: updateFolder(PublishedService, Folder)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateWithFolder(PublishedService entity) throws UpdateException {
        System.out.println("*** CALL *** MockServiceManager: updateWithFolder()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PublishedService findByPrimaryKey(Goid goid) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findByPrimaryKey()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<ServiceHeader> findAllHeaders() throws FindException {
        return this.findAllHeaders(false);
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<ServiceHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findAllHeaders(int, int)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<PublishedService> findAll() throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findAll()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        System.out.println("*** CALL *** MockServiceManager: getImpClass()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Goid save(PublishedService entity) throws SaveException {
        System.out.println("*** CALL *** MockServiceManager: save()");
        return Goid.DEFAULT_GOID;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void save(Goid id, PublishedService entity) throws SaveException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Integer getVersion(Goid goid) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: getVersion()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<Goid, Integer> findVersionMap() throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findVersionMap()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(PublishedService entity) throws DeleteException {
        System.out.println("*** CALL *** MockServiceManager: delete()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PublishedService getCachedEntity(Goid o, int maxAge) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: getCachedEntity()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        System.out.println("*** CALL *** MockServiceManager: getInterfaceClass()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public EntityType getEntityType() {
        System.out.println("*** CALL *** MockServiceManager: getEntityType()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getTableName() {
        System.out.println("*** CALL *** MockServiceManager: getTableName()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PublishedService findByUniqueName(String name) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findByUniqueName()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void delete(Goid oid) throws DeleteException, FindException {
        System.out.println("*** CALL *** MockServiceManager: delete()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void update(PublishedService entity) throws UpdateException {
        System.out.println("*** CALL *** MockServiceManager: update()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PublishedService findByHeader(EntityHeader header) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findByHeader()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<PublishedService> findPagedMatching(int offset, int count, @Nullable String sortProperty, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> matchProperties) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<ServiceHeader> findHeaders(int offset, int windowSize, Map<String, String> filters) throws FindException {
        System.out.println("*** CALL *** MockServiceManager: findHeaders()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createRoles(PublishedService entity) throws SaveException {
        System.out.println("*** CALL *** MockServiceManager: createRoles()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateRoles(PublishedService entity) throws UpdateException {
        System.out.println("*** CALL *** MockServiceManager: updateRoles()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<PublishedService> findByFolder(@NotNull Goid folderGoid) throws FindException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
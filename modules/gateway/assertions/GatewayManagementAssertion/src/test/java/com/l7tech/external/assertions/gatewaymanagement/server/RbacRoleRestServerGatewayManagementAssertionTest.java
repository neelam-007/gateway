package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RbacRoleAssignmentMO;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.server.security.rbac.MockRoleManager;
import com.l7tech.util.Functions;
import org.apache.http.entity.ContentType;
import org.junit.*;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * This was created: 10/23/13 as 4:47 PM
 *
 * @author Victor Kazakov
 */
public class RbacRoleRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(RbacRoleRestServerGatewayManagementAssertionTest.class.getName());

    private static final Role role = new Role();
    private static final Role roleCustom = new Role();
    private static MockRoleManager roleManager;
    private static final String roleBasePath = "roles/";

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Before
    public void before() throws Exception {
        super.before();
        roleManager = applicationContext.getBean("roleManager", MockRoleManager.class);
        role.setName("Role1");
        roleManager.save(role);
        roleCustom.setName("CustomRole");
        roleCustom.setUserCreated(true);
        roleManager.save(roleCustom);
    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> roles = new ArrayList<>(roleManager.findAllHeaders());
        for(EntityHeader role : roles){
            roleManager.delete(role.getGoid());
        }
    }

    @Test
    public void getRoleTest() throws Exception {
        Response response = processRequest(roleBasePath + role.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        RbacRoleMO roleReturned = ManagedObjectFactory.read(response.getBody(), RbacRoleMO.class);

        Assert.assertEquals(role.getId(), roleReturned.getId());
        Assert.assertEquals(role.getName(), roleReturned.getName());
    }

    @Test
    public void getRoleNotExistsTest() throws Exception {
        Response response = processRequest(roleBasePath + new Goid(123, 456), HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(404, response.getStatus());
        Assert.assertEquals("Resource not found {id=" + new Goid(123, 456) + "}", response.getBody());
    }

    @Test
    public void createRoleTest() throws Exception {
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setDescription("My Description");
        roleMO.setName("MyCreateRole");

        String roleMOString = writeMOToString(roleMO);

        Response response = processRequest(roleBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), roleMOString);
        logger.info(response.toString());

        Role roleSaved = roleManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        Assert.assertEquals(roleSaved.getDescription(), "My Description");
        Assert.assertEquals(roleSaved.getName(), "MyCreateRole");
    }

    @Test
    public void createRoleWithIDTest() throws Exception {
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setDescription("My Description");
        roleMO.setName("MyCreateRole");

        Goid id = new Goid(124124124, 1);

        String roleMOString = writeMOToString(roleMO);

        Response response = processRequest(roleBasePath + id, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), roleMOString);
        logger.info(response.toString());

        final Goid goidReturned = new Goid(getFirstReferencedGoid(response));
        Assert.assertEquals(id, goidReturned);

        Role roleSaved = roleManager.findByPrimaryKey(goidReturned);
        Assert.assertNotNull(roleSaved);
        Assert.assertEquals(roleSaved.getDescription(), "My Description");
        Assert.assertEquals(roleSaved.getName(), "MyCreateRole");
    }

    @Test
    public void createRoleWithIDInReservedRangeTest() throws Exception {
        RbacRoleMO roleMO = ManagedObjectFactory.createRbacRoleMO();
        roleMO.setDescription("My Description");
        roleMO.setName("MyCreateRole");

        Goid id = new Goid(5, 1);

        String roleMOString = writeMOToString(roleMO);

        Response response = processRequest(roleBasePath + id, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), roleMOString);
        logger.info(response.toString());

        Assert.assertEquals(403, response.getStatus());
        Assert.assertEquals("Cannot save entity with ID in reserved Range. ID: "+id, response.getBody());
    }

    @Test
    public void deleteRoleTest() throws Exception {
        Response response = processRequest(roleBasePath + roleCustom.getId(), HttpMethod.DELETE, null, "");
        logger.info(response.toString());

        Assert.assertNull(roleManager.findByPrimaryKey(roleCustom.getGoid()));
    }

    @Test
    public void updateRoleTest() throws Exception {
        Response response = processRequest(roleBasePath + roleCustom.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        RbacRoleMO roleReturned = ManagedObjectFactory.read(response.getBody(), RbacRoleMO.class);

        roleReturned.setDescription("My Custom Role Updated");

        String roleMOString = writeMOToString(roleReturned);

        response = processRequest(roleBasePath + roleCustom.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), roleMOString);
        logger.info(response.toString());

        Role roleSaved = roleManager.findByPrimaryKey(roleCustom.getGoid());
        Assert.assertEquals(roleSaved.getDescription(), "My Custom Role Updated");
        Assert.assertEquals(roleSaved.getName(), roleCustom.getName());
    }

    @Test
    public void updateManagedRoleTest() throws Exception {
        Response response = processRequest(roleBasePath + role.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        RbacRoleMO roleReturned = ManagedObjectFactory.read(response.getBody(), RbacRoleMO.class);

        roleReturned.setDescription("My Custom Role Updated");

        String roleMOString = writeMOToString(roleReturned);

        response = processRequest(roleBasePath + role.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), roleMOString);
        logger.info(response.toString());

        Assert.assertEquals(403, response.getStatus());
        Assert.assertEquals("Cannot update gateway managed role.", response.getBody());
    }

    @Test
    public void addAssignments() throws Exception {
        final RbacRoleAssignmentMO roleAssignmentMO = ManagedObjectFactory.createRbacRoleAssignmentMO();
        roleAssignmentMO.setEntityType("User");
        roleAssignmentMO.setIdentityName("adminNewAssignment");
        TestIdentityProvider.addUser(new UserBean(identityProviderConfig.getGoid(), "adminNewAssignment"), "adminNewAssignment", "password".toCharArray());
        roleAssignmentMO.setProviderId(identityProviderConfig.getId());
        roleAssignmentMO.setIdentityId("adminNewAssignment");

        AddAssignmentsContext addAssignmentsContext = new AddAssignmentsContext();
        addAssignmentsContext.setAssignments(Arrays.asList(roleAssignmentMO));

        String assignmentsMOString = writeMOToString(addAssignmentsContext);

        Response response = processRequest(roleBasePath + role.getId() + "/assignments", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), assignmentsMOString);

        Assert.assertEquals(204, response.getStatus());

        Role updatedRole = roleManager.findByPrimaryKey(role.getGoid());


        Assert.assertTrue(Functions.exists(updatedRole.getRoleAssignments(), new Functions.Unary<Boolean, RoleAssignment>() {
            @Override
            public Boolean call(RoleAssignment roleAssignment) {
                return roleAssignmentMO.getIdentityId().equals(roleAssignment.getIdentityId());
            }
        }));

    }

    @Test
    public void listRoles() throws Exception {
        Response response = processRequest(roleBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource( new StringReader(response.getBody()) );

        JAXBContext jsxb = JAXBContext.newInstance(References.class, Reference.class);
        References references = jsxb.createUnmarshaller().unmarshal(source, References.class).getValue();

        Assert.assertEquals(2, references.getReferences().size());
    }
}

package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RbacRoleAssignmentMO;
import com.l7tech.gateway.api.RbacRoleMO;
import com.l7tech.gateway.api.impl.AddAssignmentsContext;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.server.security.rbac.MockRoleManager;
import com.l7tech.util.Functions;
import org.apache.http.entity.ContentType;
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        roleManager.delete(role);
        roleManager.delete(roleCustom);
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

        String body = response.getBody();
        Pattern pattern = Pattern.compile(".*xlink:href=\"([^\"]+).*");
        Matcher matcher = pattern.matcher(body);
        Assert.assertTrue(matcher.matches());
        String uri = matcher.group(1);

        Role roleSaved = roleManager.findByPrimaryKey(new Goid(uri.substring(uri.lastIndexOf('/')+1)));
        Assert.assertEquals(roleSaved.getDescription(), "My Description");
        Assert.assertEquals(roleSaved.getName(), "MyCreateRole");
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
    }

    private String writeMOToString(ManagedObject roleMO) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ManagedObjectFactory.write(roleMO, bout);
        return bout.toString();
    }
}

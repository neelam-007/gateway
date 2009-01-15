package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.*;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityTypeRegistry;
import com.l7tech.objectmodel.migration.MigrationUtils;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.UsesEntities;
import com.l7tech.gateway.common.service.ServiceDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.util.Random;
import java.util.Collections;
import java.util.Set;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import junit.framework.TestCase;


/**
 * @author jbufu
 */
public class MigrationTest extends TestCase {

    private static final String MIGRATION_JAXB_PACKAGE = "com.l7tech.server.management.migration.bundle";

    private static Random random = new Random();

    private static EntityHeader generateHeader() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        return new EntityHeader(newRandomString(15), EntityType.ANY, "name", "desc");
    }

    private static String newRandomString(int length) {
        byte[] randomBytes = new byte[length];

        random.nextBytes(randomBytes);

        for (int i = 0; i < length; i++) {
            byte b = randomBytes[i];

            int c = Math.abs(b % 36);

            if (c < 26) c += 97;// map (0..25) to 'a' .. 'z'
            else c += (48 - 26);// map (26..35) to '0'..'9'

            randomBytes[i] = (byte) c;
        }

        try {
            return new String(randomBytes, "US-ASCII");
        }
        catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public Set<EntityHeader> headerSetMethod() { return null; }
    public Set<Entity> entitySetMethod() { return null; }

    public void testDependency() throws Exception {
        Method property = UsesEntities.class.getMethod("getEntitiesUsed");
        assertTrue(MigrationUtils.isDependency(property));

        property = MigrationTest.class.getMethod("headerSetMethod");
        assertTrue(MigrationUtils.isDependency(property));

        property = MigrationTest.class.getMethod("entitySetMethod");
        assertTrue(MigrationUtils.isDependency(property));
    }

    public void testMigrationApi() throws Exception {
    }

    public void testEntitiesJaxbAble() throws Exception {
        for (Class entityClass : EntityTypeRegistry.getAllEntityClasses()) {
            //entityClass.
        }
    }

    public void testServiceDoc() throws Exception {
        ServiceDocument doc = new ServiceDocument();
        doc.setContentType("bla");
        doc.setServiceId(87432l);
        doc.setContents("some content");
        doc.setUri("http://some.uri/");
        doc.setVersion(3);
    
    }

    public void testGetMethod() throws Exception {
        Method method = ServiceDocument.class.getMethod("setServiceId", Class.forName("java.lang.long"));
        System.out.println(method);
        method = MigrationUtils.setterForPropertyName(new ServiceDocument(), "ServiceId", Long.class);
        System.out.println(method);
    }

    public static void main(String[] args) throws Exception {

        JAXBContext jaxbc = JAXBContext.newInstance(MIGRATION_JAXB_PACKAGE);
        Marshaller marshaller = jaxbc.createMarshaller();                                     
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

/*
        for (int i = 1; i < 10; i++) {
            metadata.addHeader(generateHeader());
        }
*/

        MigrationManager manager = new MigrationManagerImpl(null, null, null);

        MigrationMetadata metadata = manager.findDependencies(
            Collections.<EntityHeader>singleton(new PolicyHeader(-1l, true, "a policy", "desc", "abc-123", -1L, false, 0)));

        marshaller.marshal(metadata, System.out);

    }
}

package com.l7tech.server.migration;

import com.l7tech.server.management.migration.bundle.*;
import com.l7tech.server.management.migration.MigrationManager;
import com.l7tech.server.EntityFinderImpl;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.PolicyHeader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.util.Random;
import java.util.Collections;
import java.io.UnsupportedEncodingException;


/**
 * @author jbufu
 */
public class MigrationTest {

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

    public static void main(String[] args) throws Exception {

        JAXBContext jaxbc = JAXBContext.newInstance(MIGRATION_JAXB_PACKAGE);
        Marshaller marshaller = jaxbc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

/*
        for (int i = 1; i < 10; i++) {
            metadata.addHeader(generateHeader());
        }
*/

        MigrationManager manager = new MigrationManagerImpl(new EntityFinderImpl());

        MigrationMetadata metadata = manager.findDependencies(
            Collections.<EntityHeader>singleton(new PolicyHeader(-1,true,"a policy", "desc", "abc-123", -1L, false)));

        marshaller.marshal(metadata, System.out);

    }
}

package com.l7tech.objectmodel;

import org.junit.Assert;
import org.junit.Test;

public class EntityHeaderTest {
    @Test
    public void testCompareToId() throws Exception {
        EntityHeader header1 = new EntityHeader((String)null, null, null, null);
        EntityHeader header2 = new EntityHeader((String)null, null, null, null);
        Assert.assertEquals("nulls are equal", 0, header1.compareTo(header2));
        Assert.assertSame("nulls are equal", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", null, null, null);
        header2 = new EntityHeader((String)null, null, null, null);
        Assert.assertEquals("diff id's but null types", 0, header1.compareTo(header2));
        Assert.assertSame("diff id's but null types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader((String)null, null, null, null);
        header2 = new EntityHeader("id2", null, null, null);
        Assert.assertEquals("diff id's but null types", 0, header1.compareTo(header2));
        Assert.assertSame("diff id's but null types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", null, null, null);
        header2 = new EntityHeader("id2", null, null, null);
        Assert.assertEquals("diff id's but null types", 0, header1.compareTo(header2));
        Assert.assertSame("diff id's but null types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader((String)null, EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader((String)null, EntityType.CUSTOM_KEY_VALUE_STORE, null, null);
        Assert.assertEquals("null id's are not compared", header1.compareTo(header2), header1.getType().compareTo(header2.getType()));
        Assert.assertEquals("null id's are not compared", header1.compareTo(header2), EntityType.SECURE_PASSWORD.compareTo(EntityType.CUSTOM_KEY_VALUE_STORE));
        Assert.assertEquals("null id's are not compared", header2.compareTo(header1), header2.getType().compareTo(header1.getType()));
        Assert.assertNotSame("null id's are not compared", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader((String)null, EntityType.SECURE_PASSWORD, null, null);
        Assert.assertEquals("diff id's but same types", 0, header1.compareTo(header2));
        Assert.assertSame("diff id's but same types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader((String)null, EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, null, null);
        Assert.assertEquals("diff id's but same types", 0, header1.compareTo(header2));
        Assert.assertSame("diff id's but same types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, null, null);
        Assert.assertEquals("diff id's but same types", 0, header1.compareTo(header2));
        Assert.assertSame("diff id's but same types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id2", EntityType.CUSTOM_KEY_VALUE_STORE, null, null);
        Assert.assertEquals("diff id's but diff types", header1.compareTo(header2), header1.getType().compareTo(header2.getType()));
        Assert.assertEquals("diff id's but diff types", header1.compareTo(header2), EntityType.SECURE_PASSWORD.compareTo(EntityType.CUSTOM_KEY_VALUE_STORE));
        Assert.assertEquals("diff id's but diff types", header2.compareTo(header1), header2.getType().compareTo(header1.getType()));
        Assert.assertNotSame("diff id's but diff types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id1", EntityType.CUSTOM_KEY_VALUE_STORE, null, null);
        Assert.assertEquals("same id's but different types", 0, header1.compareTo(header2));
        Assert.assertSame("same id's but different types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", null, null, null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, null, null);
        Assert.assertEquals("nulls are high --- diff id's, type1 null", 1, header1.compareTo(header2));
        Assert.assertEquals("nulls are high --- diff id's, type1 null", -1, header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id2", null, null, null);
        Assert.assertEquals("nulls are high --- diff id's, type2 null", -1, header1.compareTo(header2));
        Assert.assertEquals("nulls are high --- diff id's, type2 null", 1, header2.compareTo(header1));
    }

    @Test
    public void testCompareToName() {
        EntityHeader header1 = new EntityHeader("id1", null, null, null);
        EntityHeader header2 = new EntityHeader("id2", null, null, null);
        Assert.assertEquals("null names, null types", 0, header1.compareTo(header2));
        Assert.assertSame("null names, null types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, null, null);
        Assert.assertEquals("null names, same types", 0, header1.compareTo(header2));
        Assert.assertSame("null names, same types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id2", EntityType.CUSTOM_KEY_VALUE_STORE, null, null);
        Assert.assertEquals("null names, diff types", header1.compareTo(header2), header1.getType().compareTo(header2.getType()));
        Assert.assertEquals("null names, diff types", header1.compareTo(header2), EntityType.SECURE_PASSWORD.compareTo(EntityType.CUSTOM_KEY_VALUE_STORE));
        Assert.assertEquals("null names, diff types", header2.compareTo(header1), header2.getType().compareTo(header1.getType()));
        Assert.assertNotSame("null names, diff types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", null, "name", null);
        header2 = new EntityHeader("id2", null, "name", null);
        Assert.assertEquals("same names, null types", 0, header1.compareTo(header2));
        Assert.assertSame("same names, null types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, "name", null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, "name", null);
        Assert.assertEquals("same names, same types", 0, header1.compareTo(header2));
        Assert.assertSame("same names, same types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, "name", null);
        header2 = new EntityHeader("id2", EntityType.CUSTOM_KEY_VALUE_STORE, "name", null);
        Assert.assertEquals("same names, diff types", header1.compareTo(header2), header1.getType().compareTo(header2.getType()));
        Assert.assertEquals("same names, diff types", header1.compareTo(header2), EntityType.SECURE_PASSWORD.compareTo(EntityType.CUSTOM_KEY_VALUE_STORE));
        Assert.assertEquals("same names, diff types", header2.compareTo(header1), header2.getType().compareTo(header1.getType()));
        Assert.assertNotSame("same names, diff types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", null, "name1", null);
        header2 = new EntityHeader("id2", null, "name2", null);
        Assert.assertEquals("diff names, null types", header1.compareTo(header2), header1.getName().compareTo(header2.getName()));
        Assert.assertEquals("diff names, null types", header1.compareTo(header2), "name1".compareTo("name2"));
        Assert.assertEquals("diff names, null types", header2.compareTo(header1), header2.getName().compareTo(header1.getName()));
        Assert.assertNotSame("diff names, null types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, "name1", null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, "name2", null);
        Assert.assertEquals("diff names, same types", header1.compareTo(header2), header1.getName().compareTo(header2.getName()));
        Assert.assertEquals("diff names, same types", header1.compareTo(header2), "name1".compareTo("name2"));
        Assert.assertEquals("diff names, same types", header2.compareTo(header1), header2.getName().compareTo(header1.getName()));
        Assert.assertNotSame("diff names, same types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, "name1", null);
        header2 = new EntityHeader("id2", EntityType.CUSTOM_KEY_VALUE_STORE, "name2", null);
        Assert.assertEquals("diff names, diff types", header1.compareTo(header2), header1.getName().compareTo(header2.getName()));
        Assert.assertEquals("diff names, diff types", header1.compareTo(header2), "name1".compareTo("name2"));
        Assert.assertEquals("diff names, diff types", header2.compareTo(header1), header2.getName().compareTo(header1.getName()));
        Assert.assertNotSame("diff names, diff types", header1.compareTo(header2), header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, null, null);
        header2 = new EntityHeader("id2", EntityType.CUSTOM_KEY_VALUE_STORE, "name2", null);
        Assert.assertEquals("nulls are high --- name1 null, diff types", 1, header1.compareTo(header2));
        Assert.assertEquals("nulls are high --- name1 null, diff types", -1, header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.CUSTOM_KEY_VALUE_STORE, null, null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, "name2", null);
        Assert.assertEquals("nulls are high --- name1 null, diff types", 1, header1.compareTo(header2));
        Assert.assertEquals("nulls are high --- name1 null, diff types", -1, header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.SECURE_PASSWORD, "name1", null);
        header2 = new EntityHeader("id2", EntityType.CUSTOM_KEY_VALUE_STORE, null, null);
        Assert.assertEquals("nulls are high --- name2 null, diff types", -1, header1.compareTo(header2));
        Assert.assertEquals("nulls are high --- name2 null, diff types", 1, header2.compareTo(header1));

        header1 = new EntityHeader("id1", EntityType.CUSTOM_KEY_VALUE_STORE, "name1", null);
        header2 = new EntityHeader("id2", EntityType.SECURE_PASSWORD, null, null);
        Assert.assertEquals("nulls are high --- name2 null, diff types", -1, header1.compareTo(header2));
        Assert.assertEquals("nulls are high --- name2 null, diff types", 1, header2.compareTo(header1));
    }
}

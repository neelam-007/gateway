package com.l7tech.gateway.common.mapping;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 */
public class MappingIdentityTest {

    @Test
    public void testCustomMappingKeyEquality() {
        MessageContextMappingKeys mcmk1 = new MessageContextMappingKeys();
        mcmk1.setMapping1_key( "key1" );
        mcmk1.setMapping1_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk1.setMapping2_key( "key2" );
        mcmk1.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk1.setMapping3_key( "key3" );
        mcmk1.setMapping4_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        MessageContextMappingKeys mcmk2 = new MessageContextMappingKeys();
        mcmk2.setMapping1_key( "KEY1" );
        mcmk2.setMapping1_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk2.setMapping2_key( "key2" );
        mcmk2.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk2.setMapping3_key( "kEy3" );
        mcmk2.setMapping4_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        System.out.println(mcmk1.generateDigest());
        System.out.println(mcmk2.generateDigest());

        Assert.assertTrue( "Case insensitive", mcmk2.generateDigest().equals(mcmk1.generateDigest()) );
        Assert.assertTrue( "Case insensitive matches", mcmk1.matches(mcmk2) );
        Assert.assertTrue( "Case insensitive matches", mcmk2.matches(mcmk1) );
    }

    @Test
    public void testMixedMappingKeyEquality() {
        MessageContextMappingKeys mcmk1 = new MessageContextMappingKeys();
        mcmk1.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk1.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );
        mcmk1.setMapping2_key( "key2" );
        mcmk1.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk1.setMapping3_key( "key3" );
        mcmk1.setMapping3_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        MessageContextMappingKeys mcmk2 = new MessageContextMappingKeys();
        mcmk2.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk2.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );
        mcmk2.setMapping2_key( "key2" );
        mcmk2.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk2.setMapping3_key( "kEy3" );
        mcmk2.setMapping3_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        System.out.println(mcmk1.generateDigest());
        System.out.println(mcmk2.generateDigest());

        Assert.assertTrue( "Case insensitive (mixed)", mcmk2.generateDigest().equals(mcmk1.generateDigest()) );
        Assert.assertTrue( "Case insensitive (mixed) matches", mcmk1.matches(mcmk2) );
        Assert.assertTrue( "Case insensitive (mixed) matches", mcmk2.matches(mcmk1) );
    }

    @Test
    public void testOneMappingKeyEquality() {
        MessageContextMappingKeys mcmk1 = new MessageContextMappingKeys();
        mcmk1.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk1.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );
        mcmk1.setMapping2_key( "key2" );
        mcmk1.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk1.setMapping2_key( "key3" );
        mcmk1.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk1.setMapping2_key( "key4" );
        mcmk1.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk1.setMapping2_key( "key5" );
        mcmk1.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        MessageContextMappingKeys mcmk2 = new MessageContextMappingKeys();
        mcmk2.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk2.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );
        mcmk2.setMapping2_key( "key2" );
        mcmk2.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk2.setMapping2_key( "key3" );
        mcmk2.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk2.setMapping2_key( "key4" );
        mcmk2.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );
        mcmk2.setMapping2_key( "key5" );
        mcmk2.setMapping2_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        System.out.println(mcmk1.generateDigest());
        System.out.println(mcmk2.generateDigest());

        Assert.assertTrue( "One value", mcmk2.generateDigest().equals(mcmk1.generateDigest()) );
        Assert.assertTrue( "One value matches", mcmk1.matches(mcmk2) );
        Assert.assertTrue( "One value matches", mcmk2.matches(mcmk1) );
    }

    @Test
    public void testFiveMappingKeyEquality() {
        MessageContextMappingKeys mcmk1 = new MessageContextMappingKeys();
        mcmk1.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk1.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );

        MessageContextMappingKeys mcmk2 = new MessageContextMappingKeys();
        mcmk2.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk2.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );

        System.out.println(mcmk1.generateDigest());
        System.out.println(mcmk2.generateDigest());

        Assert.assertTrue( "One value", mcmk2.generateDigest().equals(mcmk1.generateDigest()) );
        Assert.assertTrue( "One value matches", mcmk1.matches(mcmk2) );
        Assert.assertTrue( "One value matches", mcmk2.matches(mcmk1) );
    }

    @Test
    public void testReallyMixedMappingKeyEquality() {
        MessageContextMappingKeys mcmk1 = new MessageContextMappingKeys();
        mcmk1.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk1.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );
        mcmk1.setMapping2_key( "(SYSTEM DEFINED)" );
        mcmk1.setMapping2_type( MessageContextMapping.MappingType.IP_ADDRESS );
        mcmk1.setMapping3_key( "key3" );
        mcmk1.setMapping3_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        MessageContextMappingKeys mcmk2 = new MessageContextMappingKeys();
        mcmk2.setMapping1_key( "(SYSTEM DEFINED)" );
        mcmk2.setMapping1_type( MessageContextMapping.MappingType.AUTH_USER );
        mcmk2.setMapping2_key( "(SYSTEM DEFINED)" );
        mcmk2.setMapping2_type( MessageContextMapping.MappingType.IP_ADDRESS );
        mcmk2.setMapping3_key( "kEy3" );
        mcmk2.setMapping3_type( MessageContextMapping.MappingType.CUSTOM_MAPPING );

        System.out.println(mcmk1.generateDigest());
        System.out.println(mcmk2.generateDigest());

        Assert.assertTrue( "Case insensitive (really mixed)", mcmk2.generateDigest().equals(mcmk1.generateDigest()) );
        Assert.assertTrue( "Case insensitive (really mixed) matches", mcmk1.matches(mcmk2) );
        Assert.assertTrue( "Case insensitive (really mixed) matches", mcmk2.matches(mcmk1) );
    }

    @Test
    public void testValueEquality() {
        MessageContextMappingValues mcmv1 = new MessageContextMappingValues();
        mcmv1.setGoid(new Goid(0, 123456));
        mcmv1.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv1.setAuthUserId("testuser1 [Provider A oldname]");
        mcmv1.setAuthUserUniqueId("testuser1");
        mcmv1.setAuthUserProviderId( new Goid(0,200031L) );
        mcmv1.setMapping1_value("A");
        mcmv1.setMapping2_value("B");
        mcmv1.setMapping2_value("C");

        MessageContextMappingValues mcmv2 = new MessageContextMappingValues();
        mcmv2.setGoid(new Goid(0, 123));
        mcmv2.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv2.setAuthUserId("testuser [Provider A]");
        mcmv2.setAuthUserUniqueId("testuser1");
        mcmv2.setAuthUserProviderId(  new Goid(0,200031L) );
        mcmv2.setMapping1_value("A");
        mcmv2.setMapping2_value("B");
        mcmv2.setMapping2_value("C");

        Assert.assertTrue( "Value digest equality test", mcmv1.generateDigest().equals(mcmv2.generateDigest()) );
        Assert.assertTrue( "Value matches test", mcmv1.matches(mcmv2) );
        Assert.assertTrue( "Value matches test", mcmv2.matches(mcmv1) );
    }

    @Test
    public void testUserValueEquality() {
        MessageContextMappingValues mcmv1 = new MessageContextMappingValues();
        mcmv1.setGoid(new Goid(0, 123456));
        mcmv1.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv1.setAuthUserId("testuser1 [Provider A oldname]");
        mcmv1.setAuthUserUniqueId("testuser1");
        mcmv1.setAuthUserProviderId(  new Goid(0,200031L) );

        MessageContextMappingValues mcmv2 = new MessageContextMappingValues();
        mcmv2.setGoid(new Goid(0, 123));
        mcmv2.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv2.setAuthUserId("testuser [Provider A]");
        mcmv2.setAuthUserUniqueId("testuser1");
        mcmv2.setAuthUserProviderId( new Goid(0, 200031L) );

        Assert.assertTrue( "Value user digest equality test", mcmv1.generateDigest().equals(mcmv2.generateDigest()) );
        Assert.assertTrue( "Value user matches test", mcmv1.matches(mcmv2) );
        Assert.assertTrue( "Value user matches test", mcmv2.matches(mcmv1) );
    }

    @Test
    public void testValuesEquality() {
        MessageContextMappingValues mcmv1 = new MessageContextMappingValues();
        mcmv1.setGoid(new Goid(0, 123456));
        mcmv1.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv1.setMapping1_value("A");
        mcmv1.setMapping2_value("B");
        mcmv1.setMapping3_value("C");
        mcmv1.setMapping4_value("D");
        mcmv1.setMapping5_value("e");

        MessageContextMappingValues mcmv2 = new MessageContextMappingValues();
        mcmv2.setGoid(new Goid(0, 123));
        mcmv2.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv2.setMapping1_value("A");
        mcmv2.setMapping2_value("B");
        mcmv2.setMapping3_value("C");
        mcmv2.setMapping4_value("D");
        mcmv2.setMapping5_value("e");

        Assert.assertTrue( "Values digest equality test", mcmv1.generateDigest().equals(mcmv2.generateDigest()) );
        Assert.assertTrue( "Values matches test", mcmv1.matches(mcmv2) );
        Assert.assertTrue( "Values matches test", mcmv2.matches(mcmv1) );
    }

    @Test
    public void testValuesCasesensitiveEquality() {
        MessageContextMappingValues mcmv1 = new MessageContextMappingValues();
        mcmv1.setGoid(new Goid(0, 123456));
        mcmv1.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv1.setMapping1_value("A");
        mcmv1.setMapping2_value("B");
        mcmv1.setMapping3_value("C");
        mcmv1.setMapping4_value("D");
        mcmv1.setMapping5_value("E");

        MessageContextMappingValues mcmv2 = new MessageContextMappingValues();
        mcmv2.setGoid(new Goid(0, 123));
        mcmv2.setMappingKeysGoid(new Goid(0, 2345L));
        mcmv2.setAuthUserId("testuser [Provider A]");
        mcmv2.setAuthUserUniqueId("testuser1");
        mcmv2.setAuthUserProviderId(  new Goid(0,200031L) );
        mcmv2.setMapping1_value("A");
        mcmv2.setMapping2_value("B");
        mcmv2.setMapping3_value("C");
        mcmv2.setMapping4_value("D");
        mcmv2.setMapping5_value("e");

        Assert.assertFalse( "Values mismatch digest equality test", mcmv1.generateDigest().equals(mcmv2.generateDigest()) );
        Assert.assertFalse( "Values mismatch matches test", mcmv1.matches(mcmv2) );
        Assert.assertFalse( "Values mismatch matches test", mcmv2.matches(mcmv1) );
    }

}

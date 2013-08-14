package com.l7tech.util;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.GoidRange;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for GoidUpgradeMapper.
 */
public class GoidUpgradeMapperTest {

    private static final long WRAP = GoidRange.WRAPPED_OID.getFirstHi();
    public static final long TRUSTED_CERT_PREFIX = 4848445555234456123L;

    @Before
    public void prepare() throws Exception {
        GoidUpgradeMapperTestUtil.clearAllPrefixes();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        GoidUpgradeMapperTestUtil.clearAllPrefixes();
    }

    @Test
    public void testMapOid() throws Exception {
        assertNull( GoidUpgradeMapper.mapOid( null, null ) );
        assertEquals(new Goid(WRAP, 44), GoidUpgradeMapper.mapOid(EntityType.TRUSTED_CERT, 44L));
        assertEquals(new Goid(WRAP, 45), GoidUpgradeMapper.mapOid(EntityType.SERVICE, 45L));

        GoidUpgradeMapperTestUtil.addPrefix("trusted_cert", TRUSTED_CERT_PREFIX);
        assertEquals(new Goid(TRUSTED_CERT_PREFIX, 44), GoidUpgradeMapper.mapOid(EntityType.TRUSTED_CERT, 44L));
        assertEquals(new Goid(WRAP, 45), GoidUpgradeMapper.mapOid(EntityType.SERVICE, 45L));
        assertEquals(new Goid(WRAP, 41), GoidUpgradeMapper.mapOid(null, 41L));
    }

    @Test
    public void testMapOids() throws Exception {
        GoidUpgradeMapperTestUtil.addPrefix( "trusted_cert", TRUSTED_CERT_PREFIX);

        assertNull(GoidUpgradeMapper.mapOids(EntityType.TRUSTED_CERT, null));

        Goid[] goids = GoidUpgradeMapper.mapOids(EntityType.TRUSTED_CERT, new Long[]{44L, 38383L, null, 222L});
        assertEquals(4, goids.length);
        assertEquals(new Goid(TRUSTED_CERT_PREFIX, 44L), goids[0]);
        assertEquals(new Goid(TRUSTED_CERT_PREFIX, 38383L), goids[1]);
        assertNull(goids[2]);
        assertEquals(new Goid(TRUSTED_CERT_PREFIX, 222L), goids[3]);
        assertFalse(GoidRange.WRAPPED_OID.isInRange(goids[3]));

        goids = GoidUpgradeMapper.mapOids(EntityType.SERVICE, new Long[]{44L, 38383L, null, 222L});
        assertEquals(4, goids.length);
        assertEquals(new Goid(WRAP, 44L), goids[0]);
        assertEquals(new Goid(WRAP, 38383L), goids[1]);
        assertNull(goids[2]);
        assertEquals(new Goid(WRAP, 222L), goids[3]);
        assertTrue(GoidRange.WRAPPED_OID.isInRange(goids[3]));

        goids = GoidUpgradeMapper.mapOids(null, new Long[]{44L, 38383L, null, 222L});
        assertEquals(4, goids.length);
        assertEquals(new Goid(WRAP, 44L), goids[0]);
        assertEquals(new Goid(WRAP, 38383L), goids[1]);
        assertNull(goids[2]);
        assertEquals(new Goid(WRAP, 222L), goids[3]);
        assertTrue(GoidRange.WRAPPED_OID.isInRange(goids[3]));
    }

    @Test
    public void test(){
        System.out.println(GoidEntity.DEFAULT_GOID.toString());
    }
}

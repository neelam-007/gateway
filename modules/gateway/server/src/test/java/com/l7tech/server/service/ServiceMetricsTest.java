package com.l7tech.server.service;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;
import org.junit.Assert;
import com.l7tech.identity.UserBean;
import com.l7tech.gateway.common.mapping.MessageContextMapping;

import java.util.*;

/**
 *
 */
public class ServiceMetricsTest {

    @Test
    public void testMetricsDetailKeyEqualityWithoutUserMapping() {
        // test equality with different users when there is no user mapping
        {
            UserBean ub1 = new UserBean(new Goid(0,-2), "admin");
            ub1.setUniqueIdentifier("3");
            ServiceMetrics.MetricsDetailKey mdk1 = new ServiceMetrics.MetricsDetailKey("getProductDetails", ub1, null);

            UserBean ub2 = new UserBean(new Goid(0,-2), "steve");
            ub2.setUniqueIdentifier("786432");
            ServiceMetrics.MetricsDetailKey mdk2 = new ServiceMetrics.MetricsDetailKey("getProductDetails", ub2, null);

            Assert.assertEquals( "detail keys without user mapping equality", mdk1, mdk2 );
            Assert.assertEquals( "detail keys without user mapping hashcode", mdk1.hashCode(), mdk2.hashCode() );
        }
        // test inequality with different operation
        {
            UserBean ub1 = new UserBean(new Goid(0,-2), "admin");
            ub1.setUniqueIdentifier("3");
            ServiceMetrics.MetricsDetailKey mdk1 = new ServiceMetrics.MetricsDetailKey("getProductDetail", ub1, null);

            UserBean ub2 = new UserBean(new Goid(0,-2), "steve");
            ub2.setUniqueIdentifier("786432");
            ServiceMetrics.MetricsDetailKey mdk2 = new ServiceMetrics.MetricsDetailKey("getProductDetails", ub2, null);

            Assert.assertNotSame( "detail keys without user mapping equality (different operations)", mdk1, mdk2 );
            Assert.assertNotSame( "detail keys without user mapping hashcode (different operations)", mdk1.hashCode(), mdk2.hashCode() );
        }
    }

    @Test
    public void testMetricsDetailKeyEqualityWithUserMapping() {
        // test inequality for different user mappings
        {
            UserBean ub1 = new UserBean(new Goid(0,-2), "admin");
            ub1.setUniqueIdentifier("3");
            MessageContextMapping mcm1 = new MessageContextMapping();
            mcm1.setMappingType(MessageContextMapping.MappingType.AUTH_USER);
            ServiceMetrics.MetricsDetailKey mdk1 = new ServiceMetrics.MetricsDetailKey("getProductDetails", ub1, Collections.singletonList(mcm1));

            UserBean ub2 = new UserBean(new Goid(0,-2), "steve");
            ub2.setUniqueIdentifier("786432");
            MessageContextMapping mcm2 = new MessageContextMapping();
            mcm2.setMappingType(MessageContextMapping.MappingType.AUTH_USER);
            ServiceMetrics.MetricsDetailKey mdk2 = new ServiceMetrics.MetricsDetailKey("getProductDetails", ub2, Collections.singletonList(mcm2));

            Assert.assertNotSame( "detail keys with user mapping equality (different users)", mdk1, mdk2 );
            Assert.assertNotSame( "detail keys with user mapping hashcode (different users)", mdk1.hashCode(), mdk2.hashCode() );
        }
        // test equality with the same user mapping
        {
            UserBean ub1 = new UserBean(new Goid(0,-2), "admin");
            ub1.setUniqueIdentifier("3");
            MessageContextMapping mcm1 = new MessageContextMapping();
            mcm1.setMappingType(MessageContextMapping.MappingType.AUTH_USER);
            ServiceMetrics.MetricsDetailKey mdk1 = new ServiceMetrics.MetricsDetailKey("getProductDetails", ub1, Collections.singletonList(mcm1));

            UserBean ub2 = new UserBean(new Goid(0,-2), "admin");
            ub2.setUniqueIdentifier("3");
            MessageContextMapping mcm2 = new MessageContextMapping();
            mcm2.setMappingType(MessageContextMapping.MappingType.AUTH_USER);
            ServiceMetrics.MetricsDetailKey mdk2 = new ServiceMetrics.MetricsDetailKey("getProductDetails", ub2, Collections.singletonList(mcm2));

            Assert.assertEquals( "detail keys with user mapping equality", mdk1, mdk2 );
            Assert.assertEquals( "detail keys with user mapping hashcode", mdk1.hashCode(), mdk2.hashCode() );
        }
    }
}

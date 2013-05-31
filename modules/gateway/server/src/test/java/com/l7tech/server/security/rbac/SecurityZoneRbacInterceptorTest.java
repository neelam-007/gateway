package com.l7tech.server.security.rbac;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class SecurityZoneRbacInterceptorTest {
    private SecurityZoneRbacInterceptor interceptor;
    @Mock
    private ZoneUpdateSecurityChecker checker;
    @Mock
    private MockAdmin mockAdmin;
    private List<Long> oids;
    private InternalUser user;
    private Method bulkUpdate;


    @Before
    public void setup() throws Exception {
        interceptor = new SecurityZoneRbacInterceptor();
        ApplicationContexts.inject(interceptor, CollectionUtils.<String, Object>mapBuilder()
                .put("zoneUpdateSecurityChecker", checker)
                .unmodifiableMap(), false);
        oids = new ArrayList<>();
        user = new InternalUser("test");
        interceptor.setUser(user);
        bulkUpdate = MockAdmin.class.getMethod("bulkUpdate", Long.class, EntityType.class, Collection.class);
    }


    @Test(expected = IllegalArgumentException.class)
    public void invalidCollectionType() throws Throwable {
        interceptor.invoke(new StubMethodInvocation(bulkUpdate, new Object[]{null, EntityType.POLICY, Arrays.asList("abc")}, null, mockAdmin));
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooFewArguments() throws Throwable {
        interceptor.invoke(new StubMethodInvocation(bulkUpdate, new Object[]{null, EntityType.POLICY}, null, mockAdmin));
    }

    @Test(expected = IllegalArgumentException.class)
    public void firstArgNotLong() throws Throwable {
        interceptor.invoke(new StubMethodInvocation(bulkUpdate, new Object[]{"1234", EntityType.POLICY, oids}, null, mockAdmin));
    }

    @Test(expected = IllegalArgumentException.class)
    public void secondArgNotEntityType() throws Throwable {
        interceptor.invoke(new StubMethodInvocation(bulkUpdate, new Object[]{null, "policy", oids}, null, mockAdmin));
    }

    private interface MockAdmin {
        void bulkUpdate(final Long securityZoneOid, final EntityType entityType, final Collection<Long> oids);
    }
}

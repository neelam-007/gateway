package com.l7tech.server.wsdm.subscription;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.wsdm.faults.FaultMappableException;
import com.l7tech.server.wsdm.method.Subscribe;
import com.l7tech.util.GoidUpgradeMapperTestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.Collections;

/**
 * This was created: 9/17/13 as 12:24 PM
 *
 * @author Victor Kazakov
 */
public class SubscriptionNotifierTest {


    private static final String NODE_ID = "my_node_id";
    private static final long SERVICE_PREFIX = 123L;
    private Goid serviceGoid = new Goid(SERVICE_PREFIX, 456L);

    @Mock
    private Subscribe subscribe;

    @Mock
    private ServiceCache serviceCache;

    @Mock
    private SubscriptionManager subscriptionManager;

    @InjectMocks
    private SubscriptionNotifier subscriptionNotifier;

    PublishedService service = new PublishedService(){{
        setGoid(serviceGoid);
    }};

    @Before
    public void before() throws FindException {
        subscriptionNotifier = new SubscriptionNotifier(NODE_ID);

        MockitoAnnotations.initMocks(this);

        Mockito.when(subscribe.getTermination()).thenReturn(System.currentTimeMillis()+100000);
        Mockito.when(subscribe.getTopicValue()).thenReturn("muws-ev:OperationalStatusCapability");
        Mockito.when(subscribe.getCallBackAddress()).thenReturn("https://example.com");
        GoidUpgradeMapperTestUtil.addPrefix("published_service", SERVICE_PREFIX);

        Mockito.when(serviceCache.getCachedService(Matchers.eq(serviceGoid))).thenReturn(service);
        Mockito.when(subscriptionManager.findBySubscriptionKey(Matchers.<SubscriptionKey>any())).thenReturn(Collections.<Subscription>emptySet());
    }

    @Test
    public void subscribeOidTest() throws FaultMappableException {

        Mockito.when(subscribe.getServiceId()).thenReturn(String.valueOf(serviceGoid.getLow()));

        Subscription rtn = subscriptionNotifier.subscribe(subscribe, "GUID");

        Assert.assertEquals(serviceGoid, rtn.getPublishedServiceGoid());
    }

    @Test
    public void subscribeGoidTest() throws FaultMappableException {

        Mockito.when(subscribe.getServiceId()).thenReturn(serviceGoid.toHexString());

        Subscription rtn = subscriptionNotifier.subscribe(subscribe, "GUID");

        Assert.assertEquals(serviceGoid, rtn.getPublishedServiceGoid());
    }
}

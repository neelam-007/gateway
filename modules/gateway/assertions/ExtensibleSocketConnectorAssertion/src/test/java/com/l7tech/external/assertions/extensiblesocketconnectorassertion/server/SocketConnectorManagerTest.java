package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExchangePatternEnum;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader.*;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 05/02/14
 * Time: 9:21 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NioSocketConnectorWrapper.class, OutboundIoHandlerAdapterWrapper.class, ProtocolCodecFilterWrapper.class})
public class SocketConnectorManagerTest {

    public static SocketConnectorManager socketConnectorManager = null;

    private ReadFutureWrapper readFutureWrapper = null;
    private IoSessionConfigWrapper ioSessionConfigWrapper = null;
    private IoSessionWrapper sessionWrapper = null;
    private NioSocketConnectorWrapper connectorWrapper = null;
    private Message message = null;

    //dns data for dns tests
    private String dnsService = "sip";
    private String dnsServiceBad = "badService";
    private String dnsDomainName = "somebusiness.com";
    private String dnsDomainNameBad = "badDomain";
    private String dnsHostName = "somesite.somebusiness.com";
    private String dnsHostNameBad = "badHost";
    private String dnsAttributeName = "dns:/_" + dnsService + "._tcp." + dnsDomainName;
    private String dnsAttributeNameBad = "dns:/_" + dnsServiceBad + "._tcp." + dnsDomainNameBad;
    private String dnsAttributeId = "SRV";
    private String[] dnsAttributeIds = new String[]{dnsAttributeId};
    private int dnsPort = 5060;
    private String dnsAttributeValue = "0 0 " + Long.toString(dnsPort) + " " + dnsHostName + ".";

    @BeforeClass
    public static void initialize() throws FindException {
        //setup the socket connector
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.findAll()).thenReturn(new ArrayList<ExtensibleSocketConnectorEntity>());

        ClusterPropertyManager clusterPropertyManager = mock(ClusterPropertyManager.class);
        SsgKeyStoreManager ssgKeyStoreManager = mock(SsgKeyStoreManager.class);
        TrustManager trustManager = mock(TrustManager.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        StashManagerFactory stashManagerFactory = mock(StashManagerFactory.class);
        MessageProcessor messageProcessor = mock(MessageProcessor.class);
        DefaultKey defaultKey = mock(DefaultKey.class);
        FirewallRulesManager firewallRulesManager = mock(FirewallRulesManager.class);

        SocketConnectorManager.createConnectionManager(entityManager, clusterPropertyManager, ssgKeyStoreManager,
                trustManager, secureRandom, stashManagerFactory, messageProcessor, defaultKey, firewallRulesManager);
        socketConnectorManager = SocketConnectorManager.getInstance();
        socketConnectorManager.start();
    }

    @Before
    public void setUp() throws Exception {
        //setup message data
        message = new Message();
        message.initialize(ContentTypeHeader.TEXT_DEFAULT, "BlahBlah".getBytes());

        //setup mock classes
        //mock up wrapper classes for Mina
        ioSessionConfigWrapper = mock(IoSessionConfigWrapper.class);

        readFutureWrapper = mock(ReadFutureWrapper.class);

        sessionWrapper = mock(IoSessionWrapper.class);
        when(sessionWrapper.read()).thenReturn(readFutureWrapper);
        when(sessionWrapper.getConfig()).thenReturn(ioSessionConfigWrapper);

        ConnectFutureWrapper futureWrapper = mock(ConnectFutureWrapper.class);
        when(futureWrapper.getSession()).thenReturn(sessionWrapper);

        DefaultIoFilterChainBuilderWrapper ioFilterChainWrapper = mock(DefaultIoFilterChainBuilderWrapper.class);

        OutboundIoHandlerAdapterWrapper outboundIoHandlerAdapterWrapper =
                mock(OutboundIoHandlerAdapterWrapper.class);
        when(outboundIoHandlerAdapterWrapper.getResponse()).thenReturn("response".getBytes());
        PowerMockito.mockStatic(OutboundIoHandlerAdapterWrapper.class);
        when(OutboundIoHandlerAdapterWrapper.create()).thenReturn(outboundIoHandlerAdapterWrapper);

        connectorWrapper = mock(NioSocketConnectorWrapper.class);
        when(connectorWrapper.connect()).thenReturn(futureWrapper);
        when(connectorWrapper.getFilterChain()).thenReturn(ioFilterChainWrapper);
        when(connectorWrapper.getHandler()).thenReturn(outboundIoHandlerAdapterWrapper);
        PowerMockito.mockStatic(NioSocketConnectorWrapper.class);
        when(NioSocketConnectorWrapper.create()).thenReturn(connectorWrapper);

        PowerMockito.mockStatic(ProtocolCodecFilterWrapper.class);
        when(ProtocolCodecFilterWrapper.create(any(Object.class))).thenReturn(null);

        //mock up classes for DnsLookup and assign to the directory context
        Attribute attribute = mock(Attribute.class);
        when(attribute.size()).thenReturn(1);
        when(attribute.get(0)).thenReturn(dnsAttributeValue);

        Attributes attributes = mock(Attributes.class);
        when(attributes.get(dnsAttributeId)).thenReturn(attribute);

        InitialDirContext dirContext = mock(InitialDirContext.class);
        when(dirContext.getAttributes(dnsAttributeName, dnsAttributeIds)).thenReturn(attributes);
        when(dirContext.getAttributes(dnsAttributeNameBad, dnsAttributeIds))
                .thenThrow(new NamingException("Test Exception for bad dnsAttributeName"));
        socketConnectorManager.setDirectoryContext(dirContext);
    }

    @AfterClass
    public static void tearDown() {
        socketConnectorManager.stop();
    }

    /**
     * Test that the session is closed after exchange
     *
     * @throws Exception
     */
    @Test
    public void testKeepAliveOff() throws Exception {

        //setup data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setKeepAlive(false);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, null, false);

        verify(sessionWrapper).close(true);  //test that the session is closed
    }

    /**
     * Test the session is not closed after exchange
     *
     * @throws Exception
     */
    @Test
    public void testKeepAliveOn() throws Exception {
        //setup test data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setKeepAlive(true);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, null, false);
        verify(sessionWrapper, never()).close(true); //test that the session is never closed
    }

    /**
     * Test the capture and reuse of session ids. First check that a sessionId is returned from sendMessage call,
     * then check that when a session id is sent into the send message call, a session is retrieved from the
     * connectors managed connections.
     *
     * @throws Exception
     */
    @Test
    public void testSessionIdReuse() throws Exception {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);

        //setup test data
        long sessionId = 1001;
        Map<Long, IoSessionWrapper> ioSessionWrapperMap = new HashMap<Long, IoSessionWrapper>();
        ioSessionWrapperMap.put(sessionId, sessionWrapper);

        when(sessionWrapper.getId()).thenReturn(sessionId);
        when(connectorWrapper.getManagedSessions()).thenReturn(ioSessionWrapperMap);
        when(connectorWrapper.getManagedSessionCount()).thenReturn(1);

        SocketConnectorManager.OutgoingMessageResponse outgoingMessageResponse = null;

        //execute test
        //test that the session id is returned..
        socketConnectorManager.connectionUpdated(entity);
        outgoingMessageResponse = socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, null, false);
        assertEquals(sessionId, outgoingMessageResponse.getSessionId());

        //test that when passed in the session is retrieved from the connector using the session id
        outgoingMessageResponse = socketConnectorManager.sendMessage(message,
                Goid.DEFAULT_GOID, Long.toString(outgoingMessageResponse.getSessionId()), false);
        verify(connectorWrapper).getManagedSessions(); //check that get managed session was called for the connection
        assertEquals(sessionId, outgoingMessageResponse.getSessionId());
    }

    /**
     * Test that a new connection is created when a an invalid session id is sent into sendMessage.
     *
     * @throws Exception
     * @throws ExtensibleSocketConnectorMinaClassException
     */
    @Test
    public void testInvalidSessionUsed() throws Exception, ExtensibleSocketConnectorMinaClassException {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);

        //setup test data
        String badSessionID = "BadSessionID"; //invalid session id, session id should be a number
        long goodSessionId = 1001L;
        when(sessionWrapper.getId()).thenReturn(goodSessionId);
        when(connectorWrapper.getManagedSessionCount()).thenReturn(1);

        SocketConnectorManager.OutgoingMessageResponse outgoingMessageResponse = null;

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        outgoingMessageResponse = socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, badSessionID, false);
        verify(connectorWrapper).connect(); // check that a new connection is created
        assertEquals(goodSessionId, outgoingMessageResponse.getSessionId()); //check that the good session id is returned
    }

    /**
     * Test that an exception is thrown if when a valid session is sent into sendMessage but the connector
     * does not contain a session with that id.
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testFailWhenSessionNotFound() throws Exception {

        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);

        //setup test data
        String badSessionId = "1111"; // valid session id, but no session with that id is available
        long sessionId = 1001;
        Map<Long, IoSessionWrapper> ioSessionWrapperMap = new HashMap<Long, IoSessionWrapper>();
        ioSessionWrapperMap.put(sessionId, sessionWrapper);

        when(connectorWrapper.getManagedSessionCount()).thenReturn(1);
        when(connectorWrapper.getManagedSessions()).thenReturn(ioSessionWrapperMap);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, badSessionId, true);
    }

    /**
     * Check that a new connection is not created when the port has not expired.  sendMessage should do a dns lookup
     * every minute to check if a the port has changed.  This test checks that a new session is not created
     * when a minute has not elapsed, and instead a living session is used.
     *
     * @throws Exception
     */
    @Test
    public void testDnsLookupPortNotExpiredTimeNotPassed() throws Exception {

        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setUseDnsLookup(true);
        entity.setDnsDomainName(dnsDomainName);
        entity.setDnsService(dnsService);
        entity.setHostname(dnsHostName);

        //setup test data
        //setup data for mock living session
        long sessionId = 1001;
        Map<Long, IoSessionWrapper> ioSessionWrapperMap = new HashMap<Long, IoSessionWrapper>();
        ioSessionWrapperMap.put(sessionId, sessionWrapper);
        when(connectorWrapper.getManagedSessionCount()).thenReturn(1);
        when(connectorWrapper.getManagedSessions()).thenReturn(ioSessionWrapperMap);

        //setup data for elapsed time
        long testExpireTime = Calendar.getInstance().getTimeInMillis();
        when(connectorWrapper.getPortCacheTime()).thenReturn(testExpireTime);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, Long.toString(sessionId), false);
        verify(connectorWrapper).getPortCacheTime(); // test that we tried to check that time has expired
        verify(connectorWrapper, never()).connect(); // time has not expired therefore no new connection was created
    }

    /**
     * Check that a new connection is not created when the port has not expired.  sendMessage should do a dns lookup
     * every minute to check if a the port has changed.  A port is considered expired if the time has elapsed and
     * the port has changed.  This test checks that a new session is not created when a minute has elapsed, but
     * the port has not changed.  Instead a living session is used.
     *
     * @throws Exception
     */
    @Test
    public void testDnsLookupPortNotExpiredTimePassedPortNotChanged() throws Exception {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setUseDnsLookup(true);
        entity.setDnsDomainName(dnsDomainName);
        entity.setDnsService(dnsService);
        entity.setHostname(dnsHostName);

        //setup test data
        //setup mock living session data
        long sessionId = 1001;
        Map<Long, IoSessionWrapper> ioSessionWrapperMap = new HashMap<Long, IoSessionWrapper>();
        ioSessionWrapperMap.put(sessionId, sessionWrapper);
        when(connectorWrapper.getManagedSessionCount()).thenReturn(1);
        when(connectorWrapper.getManagedSessions()).thenReturn(ioSessionWrapperMap);

        //setup mock time expiry data
        long testExpireTime = Calendar.getInstance().getTimeInMillis() - 60000;
        when(connectorWrapper.getPortCacheTime()).thenReturn(testExpireTime);

        //setup mock connector port
        when(connectorWrapper.getPort()).thenReturn(dnsPort);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, Long.toString(sessionId), false);
        verify(connectorWrapper).getPortCacheTime(); // test that we tried to check that time has expired
        verify(connectorWrapper).getPort(); // test that we tried to check the port value
        // test that we have set the cache time twice, once on connectionUpdated, then once during port
        // expiration testing
        verify(connectorWrapper, times(2)).setPortCacheTime(anyLong());
        // time has expired but port has not changed therefore no new connection was created
        verify(connectorWrapper, never()).connect();
    }

    /**
     * Check that a new connection is created when the port has expired.  sendMessage should do a dns lookup
     * every minute to check if a the port has changed.  A port is considered expired if the time has elapsed and
     * the port has changed.  This test checks that the code works according to the previous statements.
     *
     * @throws Exception
     */
    @Test
    public void testDnsLookupPortExpired() throws Exception {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setUseDnsLookup(true);
        entity.setDnsDomainName(dnsDomainName);
        entity.setDnsService(dnsService);
        entity.setHostname(dnsHostName);

        //setup test data
        //setup mock living session data
        long sessionId = 1001;
        Map<Long, IoSessionWrapper> ioSessionWrapperMap = new HashMap<Long, IoSessionWrapper>();
        ioSessionWrapperMap.put(sessionId, sessionWrapper);
        when(connectorWrapper.getManagedSessionCount()).thenReturn(1);
        when(connectorWrapper.getManagedSessions()).thenReturn(ioSessionWrapperMap);

        //setup mock elapsed time data
        long testExpireTime = Calendar.getInstance().getTimeInMillis() - 60000;
        when(connectorWrapper.getPortCacheTime()).thenReturn(testExpireTime);

        //setup mock connector port...
        //port is different value than what is return from mock dns classes, see setUp method above
        when(connectorWrapper.getPort()).thenReturn(5000);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, Long.toString(sessionId), false);
        verify(connectorWrapper).getPortCacheTime(); // test that we tried to check that time has expired
        verify(connectorWrapper).getPort(); // test that we tried to check the port value
        // test that we have set the port twice, once on connectionUpdated, then once during port
        // expiration testing
        verify(connectorWrapper, times(2)).setPort(anyInt());
        // test that we have set the cache time twice, once on connectionUpdated, then once during port
        // expiration testing
        verify(connectorWrapper, times(2)).setPortCacheTime(anyLong());
        // time has expired but port has not changed therefore no new connection was created
        verify(connectorWrapper).connect();
    }

    /**
     * Test that send message to throw exception if no dns entry is found for domain/service
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testDnsLookupFailNoEntryForServiceDomainName() throws Exception {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setUseDnsLookup(true);
        entity.setDnsDomainName(dnsDomainNameBad);
        entity.setDnsService(dnsServiceBad);
        entity.setHostname(dnsHostName);

        //setup test data
        long testExpireTime = Calendar.getInstance().getTimeInMillis() - 60000;
        when(connectorWrapper.getPortCacheTime()).thenReturn(testExpireTime);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, null, false);
    }

    /**
     * Test that send message to throw exception if no dns entry is found for domain/service/hostname
     *
     * @throws Exception
     */
    @Test(expected = Exception.class)
    public void testDnsLookupFailNoEntryForServiceDomainNameHostname() throws Exception {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setUseDnsLookup(true);
        entity.setDnsDomainName(dnsDomainNameBad);
        entity.setDnsService(dnsServiceBad);
        entity.setHostname(dnsHostNameBad);

        //setup test data
        long testExpireTime = Calendar.getInstance().getTimeInMillis() - 60000;
        when(connectorWrapper.getPortCacheTime()).thenReturn(testExpireTime);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, null, false);
    }

    /**
     * Test that for the OutIn exchange the session is configured to use read, and that write and read are
     * called on the session.
     *
     * @throws Exception
     */
    @Test
    public void testExchangeOutIn() throws Exception {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutIn);
        entity.setListenTimeout(5000L);

        //setup test data
        when(readFutureWrapper.awaitUninterruptibly(anyLong())).thenReturn(true);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, null, false);
        verify(sessionWrapper).getConfig(); // verify that the config is retieved from the session...
        verify(ioSessionConfigWrapper).setUseReadOperation(true); // ...and it is configured to use read operation
        //verify that write and read were called on the session
        verify(sessionWrapper).write(any(byte[].class));
        verify(sessionWrapper).read();
    }

    /**
     * Test that for the Out exchange the session is configured to suspend read, and that read is
     * never called on the session.
     *
     * @throws Exception
     */
    @Test
    public void testExchangeOutOnly() throws Exception {
        //setup connection data
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(Goid.DEFAULT_GOID);
        entity.setIn(false);
        entity.setExchangePattern(ExchangePatternEnum.OutOnly);
        entity.setListenTimeout(5000L);

        //execute test
        socketConnectorManager.connectionUpdated(entity);
        socketConnectorManager.sendMessage(message, Goid.DEFAULT_GOID, null, false);
        // ensure that suspend read is set, exchange is out only not expecting a return
        verify(sessionWrapper).suspendRead();
        verify(sessionWrapper).write(any(byte[].class)); // ensure that write is called
        verify(sessionWrapper, never()).read(); // ensure that read is never called
    }


}

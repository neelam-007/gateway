package com.l7tech.external.assertions.pbsmel.server;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.extension.registry.event.EventListenerRegistry;
import com.l7tech.server.polback.PolicyBackedServiceManager;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

import static com.l7tech.external.assertions.pbsmel.server.ServiceMetricsEventListenerManager.getExtensionKey;
import static com.l7tech.external.assertions.pbsmel.server.ServiceMetricsEventListenerManager.getInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test the ServiceMetricsEventListenerManager.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceMetricsEventListenerManagerTest {

    @Mock
    private ApplicationContext context;
    @Mock
    private ApplicationEventProxy appEventProxy;
    @Mock
    private PolicyBackedServiceRegistry pbsreg;
    @Mock
    private PolicyBackedServiceManager pbsManager;
    @Mock
    private EventListenerRegistry eventListenerReg;

    private PolicyBackedService pbs1;
    private final Goid pbs1Id = new Goid(0L, 1L);

    private PolicyBackedService pbs2;
    private final Goid pbs2Id = new Goid(0L, 2L);

    private PolicyBackedService pbs3; // policy-backed service with a different interface.
    private final Goid pbs3Id = new Goid(0L, 3L);

    @Before
    public void setUp() {
        when(context.getBean(anyString(), eq(ApplicationEventProxy.class))).thenReturn(appEventProxy);
        when(context.getBean(anyString(), eq(PolicyBackedServiceManager.class))).thenReturn(pbsManager);
        when(context.getBean(anyString(), eq(PolicyBackedServiceRegistry.class))).thenReturn(pbsreg);
        when(context.getBean(anyString(), eq(EventListenerRegistry.class))).thenReturn(eventListenerReg);

        pbs1 = new PolicyBackedService();
        pbs1.setGoid(pbs1Id);
        pbs1.setServiceInterfaceName(ServiceMetricsProcessor.class.getName());

        pbs2 = new PolicyBackedService();
        pbs2.setGoid(pbs2Id);
        pbs2.setServiceInterfaceName(ServiceMetricsProcessor.class.getName());

        pbs3 = new PolicyBackedService();
        pbs3.setGoid(pbs3Id);
        pbs3.setServiceInterfaceName("NotServiceMetricsProcessor");
    }

    @After
    public void cleanUp() {
        ServiceMetricsEventListenerManager.onModuleUnloaded();
    }

    @Test
    public void testOnModuleLoaded_NoExistingServiceMetricsProcessorPbs() throws Exception {
        this.loadSmelManager(pbs3);
        verify(eventListenerReg, times(0)).register(anyString(), any(ServiceMetricsEventListener.class));
    }

    @Test
    public void testOnModuleLoaded_ExistingServiceMetricsProcessorPbs() throws Exception {
        this.loadSmelManager(pbs1, pbs2, pbs3);
        verify(eventListenerReg, times(1)).register(eq(getExtensionKey(pbs1Id)), any(ServiceMetricsEventListener.class));
        verify(eventListenerReg, times(1)).register(eq(getExtensionKey(pbs2Id)), any(ServiceMetricsEventListener.class));

    }

    @Test
    public void testOnModuleUnLoaded_NoExistingServiceMetricsProcessorPbs() throws Exception {
        this.loadSmelManager(pbs3);
        ServiceMetricsEventListenerManager.onModuleUnloaded();
        verify(eventListenerReg, times(0)).unregister(anyString());
    }

    @Test
    public void testOnModuleUnLoaded_ExistingServiceMetricsProcessorPbs() throws Exception {
        this.loadSmelManager(pbs1, pbs2, pbs3);
        ServiceMetricsEventListenerManager.onModuleUnloaded();
        verify(eventListenerReg, times(1)).unregister(eq(getExtensionKey(pbs1Id)));
        verify(eventListenerReg, times(1)).unregister(eq(getExtensionKey(pbs2Id)));
    }

    @Test
    public void testEntityInvalidationEvent_Create() throws Exception {
        EntityInvalidationEvent eie = mock(EntityInvalidationEvent.class);
        Mockito.<Class<? extends Entity>>when(eie.getEntityClass()).thenReturn(PolicyBackedService.class);
        when(eie.getEntityOperations()).thenReturn(new char[]{ EntityInvalidationEvent.CREATE });
        when(eie.getEntityIds()).thenReturn(new Goid[]{ pbs1Id });
        when(pbsManager.findByPrimaryKey(eq(pbs1Id))).thenReturn(pbs1);

        this.loadSmelManager();
        ServiceMetricsEventListenerManager smelManager = getInstance();

        smelManager.processEntityInvalidationEvent(eie);
        verify(eventListenerReg, times(1)).register(eq(getExtensionKey(pbs1Id)), any(ServiceMetricsEventListener.class));
    }

    @Test
    public void testEntityInvalidationEvent_Delete() throws Exception {
        EntityInvalidationEvent eie = mock(EntityInvalidationEvent.class);
        Mockito.<Class<? extends Entity>>when(eie.getEntityClass()).thenReturn(PolicyBackedService.class);
        when(eie.getEntityOperations()).thenReturn(new char[]{ EntityInvalidationEvent.DELETE });
        when(eie.getEntityIds()).thenReturn(new Goid[]{ pbs1Id });
        when(pbsManager.findByPrimaryKey(eq(pbs1Id))).thenReturn(pbs1);

        this.loadSmelManager();
        ServiceMetricsEventListenerManager smelManager = getInstance();

        smelManager.processEntityInvalidationEvent(eie);
        verify(eventListenerReg, times(1)).unregister(eq(getExtensionKey(pbs1Id)));
    }

    @Test
    public void testEntityInvalidationEvent_Update() throws Exception {
        EntityInvalidationEvent eie = mock(EntityInvalidationEvent.class);
        Mockito.<Class<? extends Entity>>when(eie.getEntityClass()).thenReturn(PolicyBackedService.class);
        when(eie.getEntityOperations()).thenReturn(new char[]{ EntityInvalidationEvent.UPDATE });
        when(eie.getEntityIds()).thenReturn(new Goid[]{ pbs1Id });
        when(pbsManager.findByPrimaryKey(eq(pbs1Id))).thenReturn(pbs1);

        this.loadSmelManager();
        ServiceMetricsEventListenerManager smelManager = getInstance();

        smelManager.processEntityInvalidationEvent(eie);

        // No action should be taken when existing policy-backed service is updated.
        verify(eventListenerReg, times(0)).register(eq(getExtensionKey(pbs1Id)), any(ServiceMetricsEventListener.class));
        verify(eventListenerReg, times(0)).unregister(eq(getExtensionKey(pbs1Id)));
    }

    @Test
    public void testEntityInvalidationEvent_CreateDifferentEntity() throws Exception {
        EntityInvalidationEvent eie = mock(EntityInvalidationEvent.class);
        Mockito.<Class<? extends Entity>>when(eie.getEntityClass()).thenReturn(Folder.class);
        when(eie.getEntityOperations()).thenReturn(new char[]{ EntityInvalidationEvent.CREATE });
        when(eie.getEntityIds()).thenReturn(new Goid[]{ pbs1Id });
        when(pbsManager.findByPrimaryKey(eq(pbs1Id))).thenReturn(pbs1);

        this.loadSmelManager();
        ServiceMetricsEventListenerManager smelManager = getInstance();

        smelManager.processEntityInvalidationEvent(eie);

        // No action should be taken when entity other than policy-backed service is created.
        verify(eventListenerReg, times(0)).register(eq(getExtensionKey(pbs1Id)), any(ServiceMetricsEventListener.class));
    }

    @Test
    public void testEntityInvalidationEvent_DeleteDifferentEntity() throws Exception {
        EntityInvalidationEvent eie = mock(EntityInvalidationEvent.class);
        Mockito.<Class<? extends Entity>>when(eie.getEntityClass()).thenReturn(Folder.class);
        when(eie.getEntityOperations()).thenReturn(new char[]{ EntityInvalidationEvent.DELETE });
        when(eie.getEntityIds()).thenReturn(new Goid[]{ pbs1Id });
        when(pbsManager.findByPrimaryKey(eq(pbs1Id))).thenReturn(pbs1);

        this.loadSmelManager();
        ServiceMetricsEventListenerManager smelManager = getInstance();

        smelManager.processEntityInvalidationEvent(eie);

        // No action should be taken when entity other than policy-backed service is deleted.
        verify(eventListenerReg, times(0)).unregister(eq(getExtensionKey(pbs1Id)));
    }

    @Test
    public void testEntityInvalidationEvent_UpdateDifferentEntity() throws Exception {
        EntityInvalidationEvent eie = mock(EntityInvalidationEvent.class);
        Mockito.<Class<? extends Entity>>when(eie.getEntityClass()).thenReturn(Folder.class);
        when(eie.getEntityOperations()).thenReturn(new char[]{ EntityInvalidationEvent.UPDATE });
        when(eie.getEntityIds()).thenReturn(new Goid[]{ pbs1Id });
        when(pbsManager.findByPrimaryKey(eq(pbs1Id))).thenReturn(pbs1);

        this.loadSmelManager();
        ServiceMetricsEventListenerManager smelManager = getInstance();

        smelManager.processEntityInvalidationEvent(eie);

        // No action should be taken when entity other than policy-backed service is updated.
        verify(eventListenerReg, times(0)).register(eq(getExtensionKey(pbs1Id)), any(ServiceMetricsEventListener.class));
        verify(eventListenerReg, times(0)).unregister(eq(getExtensionKey(pbs1Id)));
    }

    private void loadSmelManager(PolicyBackedService... pbs) throws Exception {
        when(pbsManager.findAll()).thenReturn(Arrays.asList(pbs));
        ServiceMetricsEventListenerManager.onModuleLoaded(context);
        verify(pbsreg, times(1)).registerPolicyBackedServiceTemplate(Matchers.eq(ServiceMetricsProcessor.class));
    }
}

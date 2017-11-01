package com.l7tech.external.assertions.portaldeployer.server;

import static com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManagerImpl
        .PD_ENABLED_CP;
import static com.l7tech.external.assertions.portaldeployer.server.PortalDeployerModuleLoadListener.PD_STATUS_CP;
import static com.l7tech.external.assertions.portaldeployer.server.PortalDeployerModuleLoadListener
        .PortalDeployerStatus.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.server.GatewayState;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.util.ApplicationEventProxy;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Created by BAGJO04 on 2017-10-31.
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalDeployerModuleLoadListenerTest {

  @Mock
  private ApplicationContext context;
  @Mock
  private ApplicationEventProxy applicationEventProxy;
  @Mock
  private PortalDeployerClientManager portalDeployerClientManager;
  @Mock
  private PlatformTransactionManager transactionManager;
  @Mock
  private ClusterPropertyManager clusterPropertyManager;
  @Mock
  private GatewayState gatewayState;

  private ExecutorService executorService;
  private PortalDeployerModuleLoadListener portalDeployerModuleLoadListener;

  private boolean shutdownCalled = false;

  @Before
  public void setup() {
    when(context.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);
    when(context.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
    when(context.getBean("transactionManager", PlatformTransactionManager.class)).thenReturn(transactionManager);
    when(context.getBean("transactionManager", PlatformTransactionManager.class)).thenReturn(transactionManager);
    when(context.getBean("gatewayState", GatewayState.class)).thenReturn(gatewayState);
    executorService = new SynchronousExecutorService();

    portalDeployerModuleLoadListener = new PortalDeployerModuleLoadListener(context, portalDeployerClientManager, executorService);
  }

  @Test
  public void test_destroy() throws Exception {
    portalDeployerModuleLoadListener = new PortalDeployerModuleLoadListener(context, portalDeployerClientManager, executorService);
    portalDeployerModuleLoadListener.destroy();

    verify(applicationEventProxy, times(1)).removeApplicationListener(any(PortalDeployerModuleLoadListener.class));
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, STOPPED.name());
    verify(portalDeployerClientManager, times(1)).stop();
    assertTrue(shutdownCalled);
  }

  @Test
  public void test_loadModule_gatewayReady_portalDeployerStarted() throws Exception {
    when(gatewayState.isReadyForMessages()).thenReturn(true);
    when(clusterPropertyManager.getProperty(PD_ENABLED_CP)).thenReturn("true");
    when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
    portalDeployerModuleLoadListener.checkIfPortalDeployerEnabledOnceGatewayReady();

    verify(portalDeployerClientManager, times(1)).start();
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, STARTED.name());
  }

  @Test
  public void test_loadModule_gatewayNotReady_portalDeployerNotStarted() throws Exception {
    when(gatewayState.isReadyForMessages()).thenReturn(false);
    when(clusterPropertyManager.getProperty(PD_ENABLED_CP)).thenReturn("false");
    portalDeployerModuleLoadListener.checkIfPortalDeployerEnabledOnceGatewayReady();

    verify(portalDeployerClientManager, never()).start();
    verify(clusterPropertyManager, never()).putProperty(PD_STATUS_CP, STARTED.name());
  }

  @Test
  public void test_onApplicationEvent_portalDeployerEnabled_createdCpTrue() throws Exception {
    Created cpCreatedEvent = mock(Created.class);
    when(cpCreatedEvent.getEntity()).thenReturn(new ClusterProperty(PD_ENABLED_CP, "true"));
    portalDeployerModuleLoadListener.onApplicationEvent(cpCreatedEvent);

    verify(portalDeployerClientManager, times(1)).start();
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, STARTED.name());
  }

  @Test
  public void test_onApplicationEvent_portalDeployerDisabled_createdCpFalse() throws Exception {
    Created cpCreatedEvent = mock(Created.class);
    when(cpCreatedEvent.getEntity()).thenReturn(new ClusterProperty(PD_ENABLED_CP, "false"));
    portalDeployerModuleLoadListener.onApplicationEvent(cpCreatedEvent);

    verify(portalDeployerClientManager, times(1)).stop();
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, STOPPED.name());
  }

  @Test
  public void test_onApplicationEvent_portalDeployerEnabled_updatedCpTrue() throws Exception {
    Updated cpUpdatedEvent = mock(Updated.class);
    when(cpUpdatedEvent.getEntity()).thenReturn(new ClusterProperty(PD_ENABLED_CP, "true"));
    portalDeployerModuleLoadListener.onApplicationEvent(cpUpdatedEvent);

    verify(portalDeployerClientManager, times(1)).start();
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, STARTED.name());
  }

  @Test
  public void test_onApplicationEvent_portalDeployerDisabled_updatedCpFalse() throws Exception {
    Updated cpUpdatedEvent = mock(Updated.class);
    when(cpUpdatedEvent.getEntity()).thenReturn(new ClusterProperty(PD_ENABLED_CP, "false"));
    portalDeployerModuleLoadListener.onApplicationEvent(cpUpdatedEvent);

    verify(portalDeployerClientManager, times(1)).stop();
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, STOPPED.name());
  }

  @Test
  public void test_onApplicationEvent_portalDeployerFailed_exceptionDuringStart() throws Exception {
    Updated cpUpdatedEvent = mock(Updated.class);
    when(cpUpdatedEvent.getEntity()).thenReturn(new ClusterProperty(PD_ENABLED_CP, "true"));
    doThrow(mock(PortalDeployerConfigurationException.class)).when(portalDeployerClientManager).start();
    portalDeployerModuleLoadListener.onApplicationEvent(cpUpdatedEvent);

    verify(portalDeployerClientManager, times(1)).start();
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, START_FAILED.name());
  }

  @Test
  public void test_onApplicationEvent_portalDeployerFailed_exceptionDuringStop() throws Exception {
    Updated cpUpdatedEvent = mock(Updated.class);
    when(cpUpdatedEvent.getEntity()).thenReturn(new ClusterProperty(PD_ENABLED_CP, "false"));
    doThrow(mock(PortalDeployerConfigurationException.class)).when(portalDeployerClientManager).stop();
    portalDeployerModuleLoadListener.onApplicationEvent(cpUpdatedEvent);

    verify(portalDeployerClientManager, times(1)).stop();
    verify(clusterPropertyManager, times(1)).putProperty(PD_STATUS_CP, STOP_FAILED.name());
  }

  class SynchronousExecutorService implements ExecutorService {

    @Override
    public void execute(@NotNull Runnable command) {
      command.run();
    }

    @Override
    public void shutdown() {
      shutdownCalled = true;
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
      return null;
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
      return false;
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
      return null;
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
      return null;
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
      return null;
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
      return null;
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
      return null;
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
      return null;
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return null;
    }
  }
}

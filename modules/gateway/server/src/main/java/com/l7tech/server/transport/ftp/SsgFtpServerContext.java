package com.l7tech.server.transport.ftp;

import org.apache.ftpserver.ConnectionConfig;
import org.apache.ftpserver.command.CommandFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.ftpletcontainer.FtpletContainer;
import org.apache.ftpserver.ftpletcontainer.impl.DefaultFtpletContainer;
import org.apache.ftpserver.impl.DefaultFtpStatistics;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.message.MessageResource;
import org.apache.ftpserver.message.MessageResourceFactory;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * SSG implementation of Apache FtpServerContext.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SsgFtpServerContext implements FtpServerContext {
    private final ConnectionConfig connectionConfig;
    private final FtpRequestProcessor requestProcessor;

    private final UserManager userManager = new FtpUserManager();
    private final FtpStatistics statistics = new DefaultFtpStatistics();
    private final CommandFactory commandFactory = new FtpCommandFactory();
    private final FtpletContainer ftpletContainer = new DefaultFtpletContainer();
    private final FileSystemFactory fileSystemManager = new VirtualFileSystemManager(); // TODO jwilliams: VirtualFileSystemManager needs rewriting
    private final MessageResource messageResource = new MessageResourceFactory().createMessageResource();

    private final Map<String, Listener> listeners = new HashMap<>();

    /**
     * The thread pool executor to be used by the server using this context
     */
    private ThreadPoolExecutor threadPoolExecutor = null;

    public SsgFtpServerContext(@NotNull ConnectionConfig connectionConfig,
                               @NotNull FtpRequestProcessor requestProcessor) {
        this.connectionConfig = connectionConfig;
        this.requestProcessor = requestProcessor;
    }

    @Override
    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    public FileSystemFactory getFileSystemManager() {
        return fileSystemManager;
    }

    @Override
    public MessageResource getMessageResource() {
        return messageResource;
    }

    @Override
    public FtpStatistics getFtpStatistics() {
        return statistics;
    }

    @Override
    public FtpletContainer getFtpletContainer() {
        return ftpletContainer;
    }

    @Override
    public CommandFactory getCommandFactory() {
        return commandFactory;
    }

    public FtpRequestProcessor getRequestProcessor() {
        return requestProcessor;
    }

    public void addFtplet(String name, Ftplet ftplet) {
        ftpletContainer.getFtplets().put(name, ftplet);
    }

    @Override
    public Ftplet getFtplet(String name) {
        return ftpletContainer.getFtplet(name);
    }

    public void addListener(String name, Listener listener) {
        listeners.put(name, listener);
    }

    @Override
    public Listener getListener(String name) {
        return listeners.get(name);
    }

    @Override
    public Map<String, Listener> getListeners() {
        return listeners;
    }

    @Override
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    @Override
    public void dispose() {
        listeners.clear();
        ftpletContainer.getFtplets().clear();

        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();

            try {
                threadPoolExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // nothing
            }
        }
    }

    @Override
    public synchronized ThreadPoolExecutor getThreadPoolExecutor() {
        if (threadPoolExecutor == null) {
            int maxThreads = connectionConfig.getMaxThreads();

            if (maxThreads < 1) {
                int maxLogins = connectionConfig.getMaxLogins();

                if(maxLogins > 0) {
                    maxThreads = maxLogins;
                } else {
                    maxThreads = 16;
                }
            }

            threadPoolExecutor = new OrderedThreadPoolExecutor(maxThreads);
        }

        return threadPoolExecutor;
    }
}

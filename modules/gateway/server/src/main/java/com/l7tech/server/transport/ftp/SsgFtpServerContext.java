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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSG implementation of Apache FtpServerContext.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SsgFtpServerContext implements FtpServerContext {
    private static final Logger logger = Logger.getLogger(FtpServerContext.class.getName());

    private final CommandFactory commandFactory;
    private final ConnectionConfig connectionConfig;
    private final FtpRequestProcessor requestProcessor;
    private final UserManager userManager;

    private final FtpStatistics statistics = new DefaultFtpStatistics();
    private final FtpletContainer ftpletContainer = new DefaultFtpletContainer();
    private final FileSystemFactory fileSystemManager = new VirtualFileSystemManager();
    private final MessageResource messageResource = new MessageResourceFactory().createMessageResource();

    private final Map<String, Listener> listeners = new HashMap<>();

    /**
     * The thread pool executor to be used by the server using this context
     */
    private ThreadPoolExecutor threadPoolExecutor = null;

    public SsgFtpServerContext(@NotNull CommandFactory commandFactory,
                               @NotNull ConnectionConfig connectionConfig,
                               @NotNull FtpRequestProcessor requestProcessor,
                               @NotNull UserManager userManager) {
        this.commandFactory = commandFactory;
        this.connectionConfig = connectionConfig;
        this.requestProcessor = requestProcessor;
        this.userManager = userManager;
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
                logger.log(Level.FINE, "Thread pool shutdown interrupted: " + e.getMessage());
            }
        }

        requestProcessor.dispose();
    }

    @Override
    public synchronized ThreadPoolExecutor getThreadPoolExecutor() {
        if (threadPoolExecutor == null) {
            threadPoolExecutor = new OrderedThreadPoolExecutor(connectionConfig.getMaxThreads());
        }

        return threadPoolExecutor;
    }
}

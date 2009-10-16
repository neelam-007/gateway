package com.l7tech.server.processcontroller;

import org.apache.cxf.interceptor.InInterceptors;
import org.apache.cxf.interceptor.OutFaultInterceptors;

import javax.jws.WebService;

import com.l7tech.common.io.ProcResult;
import com.l7tech.common.io.ProcUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.io.File;
import java.util.concurrent.*;

/**
 * @author jbufu
 */
@WebService(name="OSApi",
            targetNamespace="http://ns.l7tech.com/secureSpan/5.0/component/processController/osApi",
            endpointInterface="com.l7tech.server.processcontroller.OSApi")
@InInterceptors(interceptors = "org.apache.cxf.interceptor.LoggingInInterceptor")
@OutFaultInterceptors(interceptors = "org.apache.cxf.interceptor.LoggingOutInterceptor")
public class OSApiImpl implements OSApi {

    @Override
    public ProcResult execute(final String cwd, final String program, final String[] args, final byte[] stdin, long timeoutMillis) throws IOException {

        if (timeoutMillis > 0) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<ProcResult> taskResult = executor.submit(new Callable<ProcResult>() {
                    @Override
                    public ProcResult call() throws Exception {
                        return ProcUtils.exec(new File(cwd), new File(program), args, stdin);
                    }
                });

            try {
                return taskResult.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new CausedIOException("OS API error while executing: " + program + ": " + ExceptionUtils.getMessage(e), e);
            } finally {
                taskResult.cancel(true);
                executor.shutdownNow();
            }
        } else {
            return ProcUtils.exec(new File(cwd), new File(program), args, stdin);
        }
    }
}

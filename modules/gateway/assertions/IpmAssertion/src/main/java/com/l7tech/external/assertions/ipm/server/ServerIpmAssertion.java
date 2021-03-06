package com.l7tech.external.assertions.ipm.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.Message;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.util.BufferPool;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.external.assertions.ipm.IpmAssertion;
import com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Server side implementation of the IpmAssertion.
 *
 * @see com.l7tech.external.assertions.ipm.IpmAssertion
 */
public class ServerIpmAssertion extends AbstractServerAssertion<IpmAssertion> {
    private static final int DEFAULT_BUFF_SIZE = 131040;
    private static final int DEFAULT_MAX_BUFFERS = 120;

    private static final AtomicInteger curBuffSize = new AtomicInteger(-1);
    private static final AtomicInteger bytesBufferSize = new AtomicInteger(DEFAULT_BUFF_SIZE);
    private static final AtomicBoolean sharedByteBuffers = new AtomicBoolean(false);
    private static ObjectPool charBufferPool = null;
    private static ObjectPool byteBufferPool = null;


    private final String varname;
    private final ThreadLocal<CompiledTemplate> threadLocalCompiledTemplate;
    private final Config config;

    public ServerIpmAssertion(IpmAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context==null ? null : context.getBean( "auditFactory", AuditFactory.class ));

        varname = assertion.getSourceVariableName();

        Class<? extends CompiledTemplate> ctClass = null;
        try {
            CompiledTemplate ct = new TemplateCompiler(assertion.template()).compile();
            ctClass = ct.getClass();
        } catch (TemplateCompilerException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                                new String[] { "Unable to compile template; assertion will always fail: " + ExceptionUtils.getMessage(e) }, e);
        }

        config = context == null ? null : context.getBean("serverConfig", Config.class);

        if (ctClass == null) {
            threadLocalCompiledTemplate = null;
        } else {
            final Class<? extends CompiledTemplate> ctClass1 = ctClass;
            threadLocalCompiledTemplate = new ThreadLocal<CompiledTemplate>() {
                @Override
                protected CompiledTemplate initialValue() {
                    try {
                        return ctClass1.newInstance();
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (threadLocalCompiledTemplate == null) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Template compilation failed; assertion fails");
            return AssertionStatus.SERVER_ERROR;
        }

        try {
            final Object got = context.getVariable(varname);
            final char[] inputBuffer = got.toString().toCharArray();

            final CompiledTemplate compiledTemplate = this.threadLocalCompiledTemplate.get();

            final String targetVariable = assertion.getTargetVariableName();
            if (targetVariable == null) {
                expandBytesToMessage(inputBuffer, compiledTemplate, context);
            } else {
                expandCharsToVariable(inputBuffer, compiledTemplate, context, targetVariable);
            }

            return AssertionStatus.NONE;
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Missing variable: " + varname }, e);
            return AssertionStatus.FAILED;
        } catch (CompiledTemplate.InputBufferEmptyException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "IPM to XML input DATA_BUFF contains fewer characters than required by this template");
            return AssertionStatus.FAILED;
        } catch (CompiledTemplate.OutputBufferFullException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "IPM to XML output buffer is too small to expand this DATA_BUFF with this template; increase value of ipm.outputBuffer cluster property");
            return AssertionStatus.SERVER_ERROR;
        } catch (OutputBufferException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to obtain character output buffer: " + ExceptionUtils.getMessage(e) }, e); // can't happen
            return AssertionStatus.SERVER_ERROR;
        } catch (NoSuchPartException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { "Unable to initialize message: " + ExceptionUtils.getMessage(e) }, e);
            return AssertionStatus.FAILED;
        }
    }

    private void expandBytesToMessage(char[] inputBuffer,
                                      CompiledTemplate compiledTemplate,
                                      PolicyEnforcementContext context)
            throws NoSuchPartException, IOException, CompiledTemplate.InputBufferEmptyException, CompiledTemplate.OutputBufferFullException, OutputBufferException
    {
        Message targetMessage = assertion.isUseResponse() ? context.getResponse() : context.getRequest();
        final byte[] outputBuffer = getByteOutputBuffer();
        context.runOnClose(new Runnable() {
            @Override
            public void run() {
                try {
                    returnByteOutputBuffer(outputBuffer);
                } catch (OutputBufferException e) {
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e); // can't happen
                }
            }
        });
        int numBytes = compiledTemplate.expandBytes(inputBuffer, outputBuffer);
        InputStream inputStream = new ByteArrayInputStream(outputBuffer, 0, numBytes);
        targetMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, inputStream);
    }

    private void expandCharsToVariable(char[] inputBuffer,
                                       CompiledTemplate compiledTemplate,
                                       PolicyEnforcementContext context,
                                       String targetVariableName)
            throws CompiledTemplate.InputBufferEmptyException, CompiledTemplate.OutputBufferFullException, OutputBufferException
    {
        final char[] outputBuffer = getCharOutputBuffer();
        try {
            String stringResult = compiledTemplate.expand(inputBuffer, outputBuffer);
            context.setVariable(targetVariableName, stringResult);
        } finally {
            returnCharOutputBuffer(outputBuffer);
        }
    }

    private static void closeByteBufferPool() throws OutputBufferException {
        if (byteBufferPool != null) {
            try {
                byteBufferPool.close();
            } catch (Exception e) {
                throw new OutputBufferException(e); // can't happen
            } finally {
                byteBufferPool = null;
            }
        }
    }

    private static void closeCharBufferPool() throws OutputBufferException {
        if (charBufferPool != null) {
            try {
                charBufferPool.close();
            } catch (Exception e) {
                throw new OutputBufferException(e); // can't happen
            } finally {
                charBufferPool = null;
            }
        }
    }

    private synchronized static void resetCharBufferPool(Config config) throws OutputBufferException {
        final int newBuffSize = lookupBuffSize(config);
        if (newBuffSize == curBuffSize.get())
            return; // another thread got here first

        closeCharBufferPool();
        closeByteBufferPool();

        final int maxActive = config == null
                              ? DEFAULT_MAX_BUFFERS
                              : config.getIntProperty( IpmAssertion.PARAM_IPM_MAXBUFFERS, DEFAULT_MAX_BUFFERS );

        charBufferPool = new GenericObjectPool(new BasePoolableObjectFactory() {
            @Override
            public Object makeObject() throws Exception {
                return new char[newBuffSize];
            }

            @Override
            public boolean validateObject(Object o) {
                return o instanceof char[] && ((char[])o).length == newBuffSize;
            }
        }, makePoolConfig(maxActive));

        final int byteSize = (int)(newBuffSize * 1.15);
        bytesBufferSize.set( byteSize );
        sharedByteBuffers.set(config != null &&
                              config.getBooleanProperty( IpmAssertion.PARAM_IPM_SHAREBYTEBUFFERS, false ));

        byteBufferPool = new GenericObjectPool(new BasePoolableObjectFactory() {
            @Override
            public Object makeObject() throws Exception {
                return new byte[byteSize];
            }

            @Override
            public boolean validateObject(Object o) {
                return o instanceof byte[] && ((byte[])o).length == byteSize;
            }
        }, makePoolConfig(maxActive));

        curBuffSize.set(newBuffSize);
    }

    private static GenericObjectPool.Config makePoolConfig(int maxActive) {
        GenericObjectPool.Config config = new GenericObjectPool.Config();
        config.maxActive = maxActive;
        config.maxIdle = Math.max(maxActive / 3, 10);
        config.minIdle = Math.max(maxActive / 8, 3);
        config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
        config.maxWait = -1;
        config.testOnBorrow = false;
        config.testWhileIdle = false;
        config.testOnReturn = true;
        config.timeBetweenEvictionRunsMillis = 17271L;
        config.numTestsPerEvictionRun = config.maxActive;
        return config;
    }

    private static int lookupBuffSize(Config config) {
        int buffSize = config == null
               ? DEFAULT_BUFF_SIZE
               : config.getIntProperty( IpmAssertion.PARAM_IPM_OUTPUTBUFFER, DEFAULT_BUFF_SIZE );
        if (buffSize < 4096) // 4k minimum, for sanity
            buffSize = 4096;
        return buffSize;
    }

    private static class OutputBufferException extends Exception {
        private OutputBufferException(Throwable cause) {
            super(cause);
        }
    }

    private byte[] getByteOutputBuffer() throws OutputBufferException {
        int buffSize = lookupBuffSize(config);
        if (buffSize != curBuffSize.get())
            resetCharBufferPool(config);

        if (sharedByteBuffers.get()) {
            return BufferPool.getBuffer(bytesBufferSize.get());
        }

        try {
            return (byte[])byteBufferPool.borrowObject();
        } catch (Exception e) {
            throw new OutputBufferException(e); // can't happen
        }
    }

    private void returnByteOutputBuffer(byte[] buffer) throws OutputBufferException {
        try {
            if (curBuffSize.get() < 0)
                resetCharBufferPool(config);

            if (sharedByteBuffers.get()) {
                BufferPool.returnBuffer(buffer);
                return;
            }

            byteBufferPool.returnObject(buffer);
        } catch (Exception e) {
            throw new OutputBufferException(e); // can't happen
        }
    }

    private char[] getCharOutputBuffer() throws OutputBufferException {
        try {
            int buffSize = lookupBuffSize(config);
            if (buffSize != curBuffSize.get())
                resetCharBufferPool(config);

            return (char[])charBufferPool.borrowObject();
        } catch (Exception e) {
            throw new OutputBufferException(e); // can't happen
        }
    }

    private void returnCharOutputBuffer(char[] buffer) throws OutputBufferException {
        if (curBuffSize.get() < 0)
            resetCharBufferPool(config);

        try {
            charBufferPool.returnObject(buffer);
        } catch (Exception e) {
            throw new OutputBufferException(e); // can't happen
        }
    }
}

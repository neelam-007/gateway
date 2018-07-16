package com.l7tech.gateway.common.spring.remoting.http;

import com.l7tech.util.ClassFilter;
import org.junit.Assert;
import org.mockito.Mockito;

public class SecureHttpInvokerServiceExporterStub {
    /**
     * Creates a {@link SecureHttpInvokerServiceExporter} mock with the specified {@code classFilterOverride}.
     */
    public static SecureHttpInvokerServiceExporter mockWithClassFilterOverride(final ClassFilter classFilterOverride) {
        Assert.assertNotNull(classFilterOverride);
        final SecureHttpInvokerServiceExporter exporter = Mockito.spy(new SecureHttpInvokerServiceExporter());
        Mockito.doReturn(classFilterOverride).when(exporter).getDeserializationClassFilter();
        return exporter;
    }
}
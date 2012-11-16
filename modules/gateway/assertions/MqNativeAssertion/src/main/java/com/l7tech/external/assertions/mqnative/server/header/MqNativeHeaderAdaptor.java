package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQException;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MqNativeHeaderAdaptor {

    void applyHeaderBytesToMessage(final byte[] headerBytes) throws IOException, MQDataException;

    void applyHeaderValuesToMessage(@NotNull final Map<String, Object> messageHeaderValueMap) throws IOException, MQException;

    void applyPropertiesToMessage(@NotNull final Map<String, Object> messagePropertyMap) throws IOException, MQException;

    String getMessageFormat();

    void writeHeaderToMessage() throws IOException;

    Pair<List<String>, List<String>> exposeHeaderValuesToContextVariables(@Nullable final Map<String, Object> messageHeaderValues,
                                                                          @NotNull final PolicyEnforcementContext context) throws IOException;

    Pair<List<String>, List<String>> exposePropertiesToContextVariables(@Nullable final Map<String, Object> messageProperties,
                                                                        @NotNull final PolicyEnforcementContext context) throws IOException;

    byte[] parseHeaderAsBytes() throws IOException;

    Map<String, Object> parseHeaderValues() throws IOException;

    Map<String, Object> parseProperties() throws IOException, MQDataException, MQException;
}

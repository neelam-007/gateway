package com.l7tech.external.assertions.mqnative.server.header;

import com.ibm.mq.MQMessage;
import com.ibm.mq.headers.MQDataException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MqUnsupportedHeader extends MqNoHeader {

    public MqUnsupportedHeader(@NotNull final MQMessage mqMessage) {
        super(mqMessage);
    }

    /**
     * Apply non specific header bytes to message.  For a specific header type (e.g. RFH2) create subclass to override the implementation.
     *
     * @param headerBytes header bytes to apply
     * @throws java.io.IOException
     */
    public void applyHeaderBytesToMessage(final byte[] headerBytes) throws IOException, MQDataException {
        if (headerBytes != null && headerBytes.length > 0) {
            mqMessage.write(headerBytes);
        }
    }
}

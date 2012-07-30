package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.DefaultIcapResponse;
import ch.mimo.netty.handler.codec.icap.IcapResponse;
import ch.mimo.netty.handler.codec.icap.IcapResponseStatus;
import ch.mimo.netty.handler.codec.icap.IcapVersion;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.util.IOUtils;

import java.io.IOException;
import java.util.Map;

/**
 * A mock implementation of the {@link AbstractIcapResponseHandler} to mimic an ICAP server.  This return
 * canned responses.
 *
 * @author Ken Diep
 */
public class MockIcapResponseHandler extends AbstractIcapResponseHandler {

    private static final String EICAR_VIRUS = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    @Override
    public IcapResponse sendOptionsCommand(final String icapUri, final String host) {
        IcapResponseStatus status = IcapResponseStatus.OK;
        return new DefaultIcapResponse(IcapVersion.ICAP_1_0, status);
    }

    @Override
    public IcapResponse scan(final String icapUri, final String host, PartInfo partInfo, Map<String, String> headers) throws NoSuchPartException, IOException {
        byte[] test = IOUtils.slurpStream(partInfo.getInputStream(false));
        String string = new String(test);
        IcapResponseStatus status;
        IcapResponse response;
        if (string.contains(EICAR_VIRUS)) {
            status = IcapResponseStatus.OK;
            response = new DefaultIcapResponse(IcapVersion.ICAP_1_0, status);
            response.setHeader("Service", "testService");
            response.setHeader("X-Infection-Found", "Type=0; Resolution=2; Threat=EICAR-AV-Test;");
        } else {
            status = IcapResponseStatus.NO_CONTENT;
            response = new DefaultIcapResponse(IcapVersion.ICAP_1_0, status);
        }
        return response;
    }

}
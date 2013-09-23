package com.l7tech.external.assertions.icapantivirusscanner.server;

import ch.mimo.netty.handler.codec.icap.DefaultIcapResponse;
import ch.mimo.netty.handler.codec.icap.IcapResponse;
import ch.mimo.netty.handler.codec.icap.IcapResponseStatus;
import ch.mimo.netty.handler.codec.icap.IcapVersion;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.util.IOUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;

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
    public IcapResponse getResponse() {
        IcapResponseStatus status = IcapResponseStatus.OK;
        IcapResponse response = new DefaultIcapResponse(IcapVersion.ICAP_1_0, status);
        response.setHeader("Service", "testService");
        response.setHeader("X-Infection-Found", "Type=0; Resolution=2; Threat=EICAR-AV-Test;");
        return response;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        System.out.println(e.toString());
    }
}
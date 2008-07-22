package com.l7tech.server.util;

import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;

/**
 * @author steve
 */
public class WSSecurityProcessorUtils {
    public static ProcessorResult getWssResults(final Message msg, final String what,
                                                final SecurityTokenResolver securityTokenResolver, final Audit audit)
    {
        final ProcessorResult wssResults;
        final SecurityKnob sk = (SecurityKnob)msg.getKnob(SecurityKnob.class);
        if (sk != null) {
            wssResults = sk.getProcessorResult();
            if (wssResults == null && audit != null) audit.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_NO_WSS, what);
            return wssResults;
        } else {
            try {
                final WssProcessorImpl impl = new WssProcessorImpl(msg);
                impl.setSecurityTokenResolver(securityTokenResolver);
                wssResults = impl.processMessage();
                msg.getSecurityKnob().setProcessorResult(wssResults); // In case someone else needs it later
                return wssResults;
            } catch (Exception e) {
                if (audit != null) audit.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_BAD_WSS, new String[] { what, ExceptionUtils.getMessage(e) }, e);
                return null;
            }
        }
    }
}

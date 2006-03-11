package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpFormPost;
import com.l7tech.policy.assertion.InverseHttpFormPost;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.logging.Logger;

/**
 * Extracts fields from an HTML form submission and constructs MIME parts in the current
 * request out of them.  The request must have been received via HTTP.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current request and replaces it
 * with new content!
 */
public class ServerInverseHttpFormPost implements ServerAssertion {
    private static Logger logger = Logger.getLogger(ServerInverseHttpFormPost.class.getName());
    private final Auditor auditor;
    private final InverseHttpFormPost assertion;
    private static final String ENCODING = "UTF-8";

    public ServerInverseHttpFormPost(InverseHttpFormPost assertion, ApplicationContext springContext) {
        this.auditor = new Auditor(this, springContext, logger);
        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        MimeKnob reqMime = request.getMimeKnob();
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream(512);
        for (int i = 0; i < assertion.getFieldNames().length; i++) {
            String fieldName = assertion.getFieldNames()[i];
            try {
                PartInfo partInfo = reqMime.getPart(i);
                ContentTypeHeader ctype = partInfo.getContentType();
                InputStream partStream = partInfo.getInputStream(true);
                byte[] partBytes = HexUtils.slurpStreamLocalBuffer(partStream);
                if (partBytes.length >= HexUtils.LOCAL_BUFFER_SIZE) {
                    auditor.logAndAudit(AssertionMessages.INVERSE_HTTPFORM_TOO_BIG, new String[] { Integer.toString(i) });
                    baos.close();
                    return AssertionStatus.FAILED;
                }

                baos.write(fieldName.getBytes(ENCODING));
                baos.write("=".getBytes());
                baos.write(URLEncoder.encode(new String(partBytes, ctype.getEncoding()), ENCODING).getBytes(ENCODING));
                if (assertion.getFieldNames().length < i) baos.write("&".getBytes());
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(AssertionMessages.INVERSE_HTTPFORM_NO_SUCH_PART, new String[] { Integer.toString(i) });
                baos.close();
                return AssertionStatus.FAILED;
            }
        }
        baos.close();

        try {
            request.initialize(StashManagerFactory.createStashManager(),
                    ContentTypeHeader.parseValue("application/" + HttpFormPost.X_WWW_FORM_URLENCODED),
                    new ByteArrayInputStream(baos.toByteArray()));
            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            throw new PolicyAssertionException(assertion, e);
        }
    }
}

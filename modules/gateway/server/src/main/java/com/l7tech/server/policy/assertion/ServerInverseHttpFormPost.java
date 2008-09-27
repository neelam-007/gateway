package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.IOUtils;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
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
 * Extracts MIE parts out of the current request and formats them as an HTML form submission.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current request and replaces it with new content!
 */
public class ServerInverseHttpFormPost extends AbstractServerAssertion<InverseHttpFormPost> {
    private static Logger logger = Logger.getLogger(ServerInverseHttpFormPost.class.getName());
    private final Auditor auditor;
    private final StashManagerFactory stashManagerFactory;
    private static final String ENCODING = "UTF-8";
    private final ContentTypeHeader contentType;

    public ServerInverseHttpFormPost(InverseHttpFormPost assertion, ApplicationContext springContext) {
        super(assertion);
        this.auditor = new Auditor(this, springContext, logger);
        this.stashManagerFactory = (StashManagerFactory) springContext.getBean("stashManagerFactory", StashManagerFactory.class);
        try {
            this.contentType = ContentTypeHeader.parseValue("application/" + HttpFormPost.X_WWW_FORM_URLENCODED);
        } catch (IOException e) {
            throw new RuntimeException(e); // Something has gone horribly wrong
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        MimeKnob reqMime = request.getMimeKnob();
        BufferPoolByteArrayOutputStream baos = null;
        try {
            baos = new BufferPoolByteArrayOutputStream(512);
            for (int i = 0; i < assertion.getFieldNames().length; i++) {
                String fieldName = assertion.getFieldNames()[i];
                try {
                    PartInfo partInfo = reqMime.getPart(i);
                    ContentTypeHeader ctype = partInfo.getContentType();
                    InputStream partStream = partInfo.getInputStream(false);
                    byte[] partBytes = IOUtils.slurpStreamLocalBuffer(partStream);

                    baos.write(fieldName.getBytes(ENCODING));
                    baos.write("=".getBytes());
                    baos.write(URLEncoder.encode(new String(partBytes, ctype.getEncoding()), ENCODING).getBytes(ENCODING));
                    if (i < assertion.getFieldNames().length - 1) baos.write("&".getBytes());
                } catch (NoSuchPartException e) {
                    auditor.logAndAudit(AssertionMessages.INVERSE_HTTPFORM_NO_SUCH_PART, Integer.toString(i));
                    return AssertionStatus.FAILED;
                }
            }

            request.initialize(stashManagerFactory.createStashManager(),
                        contentType,
                        new ByteArrayInputStream(baos.toByteArray()));
            return AssertionStatus.NONE;
        }
        finally {
            if (baos != null) baos.close();
        }
    }
}

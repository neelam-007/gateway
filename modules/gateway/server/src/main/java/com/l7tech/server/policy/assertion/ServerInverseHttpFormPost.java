package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpFormPost;
import com.l7tech.policy.assertion.InverseHttpFormPost;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * Extracts MIE parts out of the current request and formats them as an HTML form submission.
 * <p>
 * <b>NOTE</b>: This assertion destroys the current request and replaces it with new content!
 */
public class ServerInverseHttpFormPost extends AbstractServerAssertion<InverseHttpFormPost> {
    private final StashManagerFactory stashManagerFactory;
    private static final Charset ENCODING = Charsets.UTF8;
    private final ContentTypeHeader contentType;

    public ServerInverseHttpFormPost(InverseHttpFormPost assertion, ApplicationContext springContext) {
        super(assertion);
        this.stashManagerFactory = springContext.getBean("stashManagerFactory", StashManagerFactory.class);
        try {
            this.contentType = ContentTypeHeader.parseValue("application/" + HttpFormPost.X_WWW_FORM_URLENCODED);
        } catch (IOException e) {
            throw new RuntimeException(e); // Something has gone horribly wrong
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request = context.getRequest();
        MimeKnob reqMime = request.getMimeKnob();
        PoolByteArrayOutputStream baos = null;
        try {
            baos = new PoolByteArrayOutputStream(512);
            for (int i = 0; i < assertion.getFieldNames().length; i++) {
                String fieldName = assertion.getFieldNames()[i];
                try {
                    PartInfo partInfo = reqMime.getPart(i);
                    ContentTypeHeader ctype = partInfo.getContentType();
                    InputStream partStream = partInfo.getInputStream(false);
                    byte[] partBytes = IOUtils.slurpStream(partStream);

                    baos.write(fieldName.getBytes(ENCODING));
                    baos.write("=".getBytes());
                    baos.write(URLEncoder.encode(new String(partBytes, ctype.getEncoding()), ENCODING.name()).getBytes(ENCODING));
                    if (i < assertion.getFieldNames().length - 1) baos.write("&".getBytes());
                } catch (NoSuchPartException e) {
                    logAndAudit( AssertionMessages.INVERSE_HTTPFORM_NO_SUCH_PART, Integer.toString( i ) );
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

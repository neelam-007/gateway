package com.l7tech.policy.validator;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * AssertionValidator for HardcodedResponseAssertion.
 */
public class HardcodedResponseAssertionValidator implements AssertionValidator {
    private final HardcodedResponseAssertion ass;
    private String ctypeErr;
    private String xmlErr;
    private boolean useContextVariable;

    public HardcodedResponseAssertionValidator(final HardcodedResponseAssertion ass) {
        this.ass = ass;

        ContentTypeHeader ctype = null;
        final String contentType = ass.getResponseContentType();
        if ( contentType != null && Syntax.getReferencedNames(contentType).length == 0 ) {
            try {
                ctype = ContentTypeHeader.parseValue( contentType );
            } catch (IOException e) {
                this.ctypeErr = ExceptionUtils.getMessage(e);
            }
        }

        final String body = ass.responseBodyString();
        if (body != null) {
            useContextVariable = Syntax.getReferencedNames(body).length > 0;
            if (useContextVariable) return;
        }

        try {
            if (ctype != null && ctype.isXml()) {
                if (body == null || body.trim().length() < 1)
                    xmlErr = "it is completely empty";
                else
                    XmlUtil.stringToDocument(body);
            }
        } catch (SAXException e) {
            this.xmlErr = ExceptionUtils.getMessage(e);
        }
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        if (ctypeErr != null)
            result.addError(new PolicyValidatorResult.Error(ass, path, "The content type is invalid: " + ctypeErr, null));
        if (useContextVariable)
            result.addWarning(new PolicyValidatorResult.Warning(ass, path, "Response Body using context variable may result in invalid body content.", null));
        if (xmlErr != null)
            result.addWarning(new PolicyValidatorResult.Warning(ass, path, "XML response is not well-formed: " + xmlErr, null));
    }
}

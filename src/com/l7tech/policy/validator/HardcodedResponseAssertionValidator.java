package com.l7tech.policy.validator;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * AssertionValidator for HardcodedResponseAssertion.
 */
public class HardcodedResponseAssertionValidator implements AssertionValidator {
    private final HardcodedResponseAssertion ass;
    private String ctypeErr;
    private String xmlErr;

    public HardcodedResponseAssertionValidator(HardcodedResponseAssertion ass) {
        this.ass = ass;
        ContentTypeHeader ctype = null;
        try {
            ctype = ContentTypeHeader.parseValue(ass.getResponseContentType());
        } catch (IOException e) {
            this.ctypeErr = ExceptionUtils.getMessage(e);
        }

        try {
            if (ctype != null && ctype.isXml()) {
                final String body = ass.responseBodyString();
                if (body == null || body.trim().length() < 1)
                    xmlErr = "it is completely empty";
                else
                    XmlUtil.stringToDocument(body);
            }
        } catch (SAXException e) {
            this.xmlErr = ExceptionUtils.getMessage(e);
        }
    }

    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        if (ctypeErr != null)
            result.addError(new PolicyValidatorResult.Error(ass, path, "The content type is invalid: " + ctypeErr, null));
        if (xmlErr != null)
            result.addWarning(new PolicyValidatorResult.Warning(ass, path, "XML response is not well-formed: " + xmlErr, null));
    }
}

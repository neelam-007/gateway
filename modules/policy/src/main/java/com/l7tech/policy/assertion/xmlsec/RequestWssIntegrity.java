package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspUpgradeUtilFrom21;

import java.util.HashMap;

/**
 * Enforces that a specific element in a request is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
@ProcessesRequest
public class RequestWssIntegrity extends XmlSecurityAssertionBase {
    public RequestWssIntegrity() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public RequestWssIntegrity(XpathExpression xpath) {
        setXpathExpression(xpath);
    }


    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put(WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping.getExternalName(),
                WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping);            
        }});

        return meta;
    }
}

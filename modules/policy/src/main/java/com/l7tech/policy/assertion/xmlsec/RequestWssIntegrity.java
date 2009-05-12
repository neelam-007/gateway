package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspUpgradeUtilFrom21;
import com.l7tech.util.Functions;

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


    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Sign Request Element");
        meta.put(AssertionMetadata.DESCRIPTION, "Requestor must sign an element of the SOAP request.");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, RequestWssIntegrity>() {
            @Override
            public String call( final RequestWssIntegrity requestWssIntegrity ) {
                StringBuilder name = new StringBuilder("Sign request element ");                
                if (requestWssIntegrity.getXpathExpression() == null) {
                    name .append("[XPath expression not set]");
                } else {
                    name.append(requestWssIntegrity.getXpathExpression().getExpression());
                }
                name.append(SecurityHeaderAddressableSupport.getActorSuffix(requestWssIntegrity));
                return name.toString();
            }
        });
        meta.put(AssertionMetadata.WSP_COMPATIBILITY_MAPPINGS, new HashMap<String, TypeMapping>() {{
            put(WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping.getExternalName(),
                WspUpgradeUtilFrom21.xmlRequestSecurityCompatibilityMapping);            
        }});

        return meta;
    }
}

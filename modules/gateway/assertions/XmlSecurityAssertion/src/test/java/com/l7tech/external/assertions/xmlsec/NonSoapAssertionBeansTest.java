package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import org.junit.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class NonSoapAssertionBeansTest {
    final Map<Class<? extends Assertion>, String> expectedFeatureSetNames = new HashMap<Class<? extends Assertion>,String>(){{
        put(NonSoapSignElementAssertion.class, "assertion:NonSoapSignElement");
        put(NonSoapVerifyElementAssertion.class, "assertion:NonSoapVerifyElement");
        put(NonSoapCheckVerifyResultsAssertion.class, "assertion:NonSoapCheckVerifyResults");
        put(NonSoapEncryptElementAssertion.class, "assertion:NonSoapEncryptElement");
        put(NonSoapDecryptElementAssertion.class, "assertion:NonSoapDecryptElement");
        put(IndexLookupByItemAssertion.class, "assertion:IndexLookupByItem");
        put(ItemLookupByIndexAssertion.class, "assertion:ItemLookupByIndex");
        put(SelectElementAssertion.class, "assertion:SelectElement");
        put(VariableCredentialSourceAssertion.class, "assertion:VariableCredentialSource");
    }};

    @Test
    public void testFeatureSetNames() throws Exception {
        for (Map.Entry<Class<? extends Assertion>, String> entry : expectedFeatureSetNames.entrySet()) {
            assertEquals(entry.getValue(), entry.getKey().newInstance().getFeatureSetName());
        }
    }
}

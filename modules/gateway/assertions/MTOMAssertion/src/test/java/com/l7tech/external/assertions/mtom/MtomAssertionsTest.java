package com.l7tech.external.assertions.mtom;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.util.IOUtils;

/**
 * 
 */
public class MtomAssertionsTest {

    @Test
    public void testDecodeAssertionPolicy() throws Exception {
        Assertion ass = reader(MtomDecodeAssertion.class).parsePermissively( resource("policy_decode.xml"), WspReader.Visibility.includeDisabled );

        assertTrue( "All assertion", ass instanceof AllAssertion );
        final AllAssertion allAss = (AllAssertion) ass;

        assertTrue( "One child of expected type", allAss.getChildren()!=null && allAss.getChildren().size()==1 && allAss.getChildren().get(0) instanceof MtomDecodeAssertion );
        MtomDecodeAssertion mtomDecode = (MtomDecodeAssertion) allAss.getChildren().get(0);

        assertEquals("removePackaging", false, mtomDecode.isRemovePackaging() );
        assertEquals("requireEncoded", true, mtomDecode.isRequireEncoded() );
        assertEquals("processSecuredOnly", true, mtomDecode.isProcessSecuredOnly() );
        assertEquals("sourceMessage", new MessageTargetableSupport( TargetMessageType.RESPONSE ), new MessageTargetableSupport(mtomDecode) );
        assertEquals("targetMessage", new MessageTargetableSupport( "var" , true), new MessageTargetableSupport(mtomDecode.getOutputTarget()));
        assertNotNull("variablesSet not null", mtomDecode.getVariablesSet());
        assertEquals("variablesSet length", 1, mtomDecode.getVariablesSet().length);
        assertEquals("variablesSet name 0", "var", mtomDecode.getVariablesSet()[0].getName());
    }

    @Test
    public void testEncodeAssertionPolicy() throws Exception {
        Assertion ass = reader(MtomEncodeAssertion.class).parsePermissively( resource("policy_encode.xml"), WspReader.Visibility.includeDisabled );

        assertTrue( "All assertion", ass instanceof AllAssertion );
        final AllAssertion allAss = (AllAssertion) ass;

        assertTrue( "One child of expected type", allAss.getChildren()!=null && allAss.getChildren().size()==1 && allAss.getChildren().get(0) instanceof MtomEncodeAssertion );
        MtomEncodeAssertion mtomEncode = (MtomEncodeAssertion) allAss.getChildren().get(0);

        assertEquals("alwaysEncode", false, mtomEncode.isAlwaysEncode() );
        assertEquals("failIfNotFound", true, mtomEncode.isFailIfNotFound() );
        assertEquals("processSecuredOnly", 1024, mtomEncode.getOptimizationThreshold() );
        assertNull( "xpathExpressions", mtomEncode.getXpathExpressions() );
        assertEquals("sourceMessage", new MessageTargetableSupport( TargetMessageType.RESPONSE ), new MessageTargetableSupport(mtomEncode) );
        assertEquals("targetMessage", new MessageTargetableSupport( "var" , true), new MessageTargetableSupport(mtomEncode.getOutputTarget()));
        assertNotNull("variablesSet not null", mtomEncode.getVariablesSet());
        assertEquals("variablesSet length", 1, mtomEncode.getVariablesSet().length);
        assertEquals("variablesSet name 0", "var", mtomEncode.getVariablesSet()[0].getName());
    }

    @Test
    public void testValidateAssertionPolicy() throws Exception {
        Assertion ass = reader(MtomValidateAssertion.class).parsePermissively( resource("policy_validate.xml"), WspReader.Visibility.includeDisabled );

        assertTrue( "All assertion", ass instanceof AllAssertion );
        final AllAssertion allAss = (AllAssertion) ass;

        assertTrue( "One child of expected type", allAss.getChildren()!=null && allAss.getChildren().size()==1 && allAss.getChildren().get(0) instanceof MtomValidateAssertion );
        MtomValidateAssertion mtomValidate = (MtomValidateAssertion) allAss.getChildren().get(0);

        assertEquals("requireEncoded", false, mtomValidate.isRequireEncoded() );
        assertEquals("sourceMessage", new MessageTargetableSupport( "var" ), new MessageTargetableSupport(mtomValidate) );
    }

    @Test
    public void testDecodeDefaults() {
        MtomDecodeAssertion mtomDecode = new MtomDecodeAssertion();

        assertEquals("removePackaging", true, mtomDecode.isRemovePackaging() );
        assertEquals("requireEncoded", false, mtomDecode.isRequireEncoded() );
        assertEquals("processSecuredOnly", false, mtomDecode.isProcessSecuredOnly() );
        assertEquals("sourceMessage", new MessageTargetableSupport(), new MessageTargetableSupport(mtomDecode) );
        assertNull("targetMessage", mtomDecode.getOutputTarget());
    }

    @Test
    public void testEncodeDefaults() {
        MtomEncodeAssertion mtomEncode = new MtomEncodeAssertion();

        assertEquals("alwaysEncode", true, mtomEncode.isAlwaysEncode() );
        assertEquals("failIfNotFound", false, mtomEncode.isFailIfNotFound() );
        assertEquals("processSecuredOnly", 0, mtomEncode.getOptimizationThreshold() );
        assertNull( "xpathExpressions", mtomEncode.getXpathExpressions() );
        assertEquals("sourceMessage", new MessageTargetableSupport(), new MessageTargetableSupport(mtomEncode) );
        assertNull("targetMessage", mtomEncode.getOutputTarget());
    }

    @Test
    public void testValidateDefaults() {
        MtomValidateAssertion mtomValidate = new MtomValidateAssertion();

        assertEquals("requireEncoded", true, mtomValidate.isRequireEncoded() );
        assertEquals("sourceMessage", new MessageTargetableSupport(), new MessageTargetableSupport(mtomValidate) );
    }

    private WspReader reader( final Class<? extends Assertion> assClass ) {
        return  new WspReader(new AssertionRegistry(){{
            registerAssertion( assClass );
        }});
    }

    private String resource( final String resourceName ) throws Exception {
        return new String(IOUtils.slurpStream( MtomAssertionsTest.class.getResourceAsStream(resourceName) ));
    }
}

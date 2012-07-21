package com.l7tech.xml;

import com.l7tech.util.CollectionUtils;
import org.junit.Test;

import java.util.Map;

import static com.l7tech.xml.xpath.XpathVersion.XPATH_2_0;

/**
 * Unit tests for {@link SaxonUtils}.
 */
public class SaxonUtilsTest {
    @Test
    public void testValidateSyntaxAndNamespacePrefixes() throws Exception {
        Map<String,String> nsmap = CollectionUtils.MapBuilder.<String,String>builder()
            .put("foo", "urn:foons")
            .put("baz", "urn:bazns")
            .put("bleet", "urn:bleetns")
            .put("notused", "urn:sirnotappearinginthisfilm")
            .map();

        SaxonUtils.validateSyntaxAndNamespacePrefixes("(//foo:bar, //baz:quux//bleet:blof)", XPATH_2_0, nsmap);
    }

    @Test(expected = InvalidXpathException.class)
    public void testValidateSyntaxAndNamespacePrefixes_badPrefix() throws Exception {
        Map<String,String> nsmap = CollectionUtils.MapBuilder.<String,String>builder()
            .put("foo", "urn:foons")
            .put("baz", "urn:bazns")
            .put("bleet", "urn:bleetns")
            .put("notused", "urn:sirnotappearinginthisfilm")
            .map();

        SaxonUtils.validateSyntaxAndNamespacePrefixes("(//foo:bar, //baz:quux//bleet2:blof)", XPATH_2_0, nsmap);
    }

    @Test(expected = InvalidXpathException.class)
    public void testValidateSyntaxAndNamespacePrefixes_badSyntax() throws Exception {
        Map<String,String> nsmap = CollectionUtils.MapBuilder.<String,String>builder()
            .put("foo", "urn:foons")
            .put("baz", "urn:bazns")
            .put("bleet", "urn:bleetns")
            .put("notused", "urn:sirnotappearinginthisfilm")
            .map();

        SaxonUtils.validateSyntaxAndNamespacePrefixes("(//foo:bar, //baz:quux//bleet2blof", XPATH_2_0, nsmap);
    }
}

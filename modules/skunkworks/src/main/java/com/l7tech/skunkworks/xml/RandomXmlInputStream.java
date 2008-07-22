package com.l7tech.skunkworks.xml;

/**
 *
 */
class RandomXmlInputStream extends StringSourceInputStream {
    public RandomXmlInputStream(long randomSeed, long maxIndent, long numStanzas) {
        super(new XmlDocumentStringSource("d", randomSeed, maxIndent, numStanzas));
    }
}

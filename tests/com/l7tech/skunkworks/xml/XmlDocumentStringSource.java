package com.l7tech.skunkworks.xml;

/**
 * A StringSource that produces a random XML document.
 */
class XmlDocumentStringSource extends ListStringSource {
    public XmlDocumentStringSource(String documentElementName, final long randomSeed, final long maxIndent, long maxStanzas) {
        super(
                "<" + documentElementName + ">\n",
                new RepeatedStringSource(new StringSource() {
                    private long curSeed = randomSeed;
                    private StringSource source = null;

                    public String next() {
                        if (source == null)
                            source = new XmlStanzaStringSource(++curSeed, maxIndent - 1);
                        return source.next();
                    }

                    public void reset() {
                        source = null;
                    }
                }, maxStanzas),
                "</" + documentElementName + ">\n"
        );
    }
}

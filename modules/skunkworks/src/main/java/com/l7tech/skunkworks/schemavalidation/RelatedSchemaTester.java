/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 2, 2005<br/>
 */
package com.l7tech.skunkworks.schemavalidation;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * This tests using EntityResolver to do software schema validation through multiple schemas that relate to each other
 *
 * @author flascelles@layer7-tech.com
 */
public class RelatedSchemaTester {
    private static final String POXSD = "com/l7tech/service/resources/relatedschemas/purchaseOrder.xsd";
    private static final String MSG = "com/l7tech/service/resources/relatedschemas/validSample.xml";
    private static final String ACCNTXSD = "com/l7tech/service/resources/relatedschemas/account.xsd";

    private final Logger logger = Logger.getLogger(RelatedSchemaTester.class.getName());
    private final Validator validator = new Validator();

    public static void main(String[] args) throws Exception {
        RelatedSchemaTester tester = new RelatedSchemaTester();
        tester.dothetest();
    }

    public void dothetest() throws Exception {
        // get root schema
        InputStream schema = getRes(POXSD);
        // get sample doc
        InputStream doc = getRes(MSG);
        // validate the doc
        validator.validate(schema, doc, getRealEntityResolver());
    }

    protected EntityResolver getRealEntityResolver() {
        return new EntityResolver () {
            private final String HOMEDIR = System.getProperty("user.dir");
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                // by default, the parser constructs a systemId in the form of a url "file:///user.dir/filename"
                String schemaId = systemId;
                if (systemId != null && HOMEDIR != null) {
                    int pos = systemId.indexOf(HOMEDIR);
                    if (pos > -1) {
                        schemaId = systemId.substring(pos+HOMEDIR.length()+1);
                    }
                }
                logger.info("asking for resource " + schemaId);
                // todo, get schema based on the schemaId instead of the hardcoded example below
                return new InputSource(getRes(ACCNTXSD));
            }
        };
    }

    protected InputStream getRes(String resname) {
        return getClass().getClassLoader().getResourceAsStream(resname);
    }
}

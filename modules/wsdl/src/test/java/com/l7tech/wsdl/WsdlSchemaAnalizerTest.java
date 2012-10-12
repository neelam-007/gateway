package com.l7tech.wsdl;

import com.l7tech.common.TestDocuments;
import junit.framework.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Map;

public class WsdlSchemaAnalizerTest {
    
    private static final String WAREHOUSE_IMPORT = "com/l7tech/wsdl/warehouse_import.wsdl";
    private static final String WAREHOUSE_IMPORT_SCHEMA = "com/l7tech/wsdl/schema/warehouse_schema.xsd";
    private static final String WAREHOUSE_MIX = "com/l7tech/wsdl/warehouse_mix.wsdl";
    private static final String WAREHOUSE_IMPORT_SCHEMA_RES = "com/l7tech/wsdl/schema/warehouse_schema_res.xsd";

    @Test
    public void testImportSchema() throws Exception {

        Document wsdl = TestDocuments.getTestDocument(WAREHOUSE_IMPORT);
        Document schema = TestDocuments.getTestDocument(WAREHOUSE_IMPORT_SCHEMA);

        Map<String,Document> docs = new HashMap<String, Document>();
        docs.put(WAREHOUSE_IMPORT, wsdl);
        docs.put(WAREHOUSE_IMPORT_SCHEMA, schema);
        WsdlSchemaAnalizer sa = new WsdlSchemaAnalizer(wsdl, docs);
        Assert.assertEquals(1, sa.getFullSchemas().length);
    }

    @Test
    public void testMixSchema() throws Exception {

        Document wsdl = TestDocuments.getTestDocument(WAREHOUSE_MIX);
        Document schema = TestDocuments.getTestDocument(WAREHOUSE_IMPORT_SCHEMA_RES);

        Map<String,Document> docs = new HashMap<String, Document>();
        docs.put(WAREHOUSE_MIX, wsdl);
        docs.put(WAREHOUSE_IMPORT_SCHEMA_RES, schema);
        WsdlSchemaAnalizer sa = new WsdlSchemaAnalizer(wsdl, docs);
        Assert.assertEquals(2, sa.getFullSchemas().length);
    }


}

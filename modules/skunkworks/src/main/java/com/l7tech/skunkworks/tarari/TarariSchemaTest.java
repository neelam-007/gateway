package com.l7tech.skunkworks.tarari;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;

import com.tarari.xml.rax.schema.SchemaLoader;
import com.tarari.xml.rax.schema.SchemaResolver;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.XmlSource;

import com.l7tech.common.io.IOUtils;

/**
 * Utility for testing tarari schema validation.
 *
 * To run this use something like:
 *
 *   java -cp ...:/usr/local/Tarari/lib/tarari_raxj.jar com.l7tech.skunkworks.tarari.TarariSchemaTest warehouse_bad.xml warehouse_imports.xsd soap.xsd
 *
 * So first arg is the document to validate and the rest are schemas to load.
 *
 * Imports/includes that are relative to the working directory should just work.
 *
 * @author Steve Jones
 */
public class TarariSchemaTest {

    public static void main(String[] args) throws Exception {
        SchemaLoader.unloadAllSchemas();

        SchemaLoader.setSchemaResolver(new SchemaResolver() {
            public byte[] resolveSchema(String namespaceUri, String locationHint, String baseUri) {
                try {
                    return IOUtils.slurpFile(new File(locationHint));
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                return null;
            }
        });

        for (int s=1; s<args.length; s++) {
            SchemaLoader.loadSchema(new FileInputStream(new File(args[s])), args[s]);
        }

        RaxDocument document = RaxDocument.createDocument(new XmlSource(new FileInputStream(args[0])));

        System.out.println("Document is valid? : " + document.validate());
    }
}

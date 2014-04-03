package com.l7tech.external.assertions.gatewaymanagement.tools;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;

/**
 * This is used to build the html rest api documentation using the wadl and an xsl transformation file
 */
public class DocBuilder {

    /**
     * This requires 3 parameters: The xsl file, The wadl file, and the output file.
     * @param args 3 paths: The xsl file, The wadl file, and the output file.
     * @throws TransformerException
     */
    public static void main(String args[]) throws TransformerException {

        if(args.length != 3) {
            throw new IllegalArgumentException("Usage: DocBuilder <XSL path> <WADL path> <Output path>");
        }
        final File xslFile = new File(args[0]);
        if(!xslFile.exists() || !xslFile.canRead()){
            throw new IllegalArgumentException("Invalid xsl file path: " + args[0]);
        }
        final File wadlFile = new File(args[1]);
        if(!wadlFile.exists() || !wadlFile.canRead()){
            throw new IllegalArgumentException("Invalid wadl file path: " + args[1]);
        }
        final File outputFile = new File(args[2]);
        if(outputFile.exists()){
            throw new IllegalArgumentException("Invalid output file path, already exists: " + args[2]);
        }

        final TransformerFactory factory = TransformerFactory.newInstance();
        final Source xslt = new StreamSource(xslFile);
        final Transformer transformer = factory.newTransformer(xslt);

        final Source text = new StreamSource(wadlFile);
        transformer.transform(text, new StreamResult(outputFile));
    }
}

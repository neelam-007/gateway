package com.l7tech.skunkworks;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.common.xml.TestDocuments;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;
import javax.activation.DataSource;

import org.apache.axis.attachments.MultiPartRelatedInputStream;
import org.apache.axis.attachments.AttachmentPart;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class SOAPWithAttachmentsTest extends TestCase {

    public SOAPWithAttachmentsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(SOAPWithAttachmentsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testParseMIMEMessage() throws Exception {
        System.out.println("start reading SOAP message with attachment");

        InputStream is = getSOAPRequest();

        MultiPartRelatedInputStream multipart = new MultiPartRelatedInputStream("text/xml", is);

        // SOAP Message Part
        String fname = "c:\\temp\\soap\\soap.txt";
        System.out.println("\n********* SOAP Message Part *********");
        System.out.println("Content-ID: " + multipart.getContentId());
        printInputStream(multipart, fname);

        Collection attachments = multipart.getAttachments();
        for (Iterator iterator = attachments.iterator(); iterator.hasNext();) {
            System.out.println("\n********* Attachement Part *********");
            Object o = (Object) iterator.next();
            if (o instanceof AttachmentPart) {
                AttachmentPart ap = (AttachmentPart) o;
                System.out.println("Content-ID: " + ap.getContentId());

                fname = "c:\\temp\\soap\\attachment.txt";
                printDataSource(ap.getDataHandler().getDataSource(), fname);
            }
        }
        System.out.println("\n\n********* End Of Document *********");
    }


    private InputStream getSOAPRequest() throws Exception {
        return TestDocuments.getInputStream(TestDocuments.SOAP_WITH_ATTACHMENTS_REQUEST);
    }

    private void printDataSource(DataSource dataSource, String fname) {
        System.out.println("Content-Type : " + dataSource.getContentType());
        try {

            InputStream is = dataSource.getInputStream();

            printInputStream(is, fname);

        } catch (IOException ioe) {
            System.out.println("Caught exception writing to file: " + ioe);
            ioe.printStackTrace(System.err);
        }
    }

    private void printInputStream(InputStream is, String fname) {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] data;
            byte buf[] = new byte[4096];
            int length;

            while (true) {
                length = is.read(buf);
                if (length < 0) {
                    break;
                }
                outputStream.write(buf, 0, length);
            }

            data = outputStream.toByteArray();
            System.out.write(data);

/*            System.out.println(" Writing attachment to file: " + fname);
            FileOutputStream fos = new FileOutputStream(fname);
            fos.write(data);*/

        } catch (IOException ioe) {
            System.out.println("Caught exception writing to file: " + ioe);
            ioe.printStackTrace(System.err);
        }
    }

}

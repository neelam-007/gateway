package com.l7tech.skunkworks.xml;

import com.infonyte.ds.fds.FileDataServerFactory;
import com.infonyte.ds.fds.SharedIndexPageCache;
import com.infonyte.ds.fds.SharedFileDataCache;
import com.infonyte.pdom.*;
import com.infonyte.xpath.XPathFactory;
import com.infonyte.xpath.XPathExpression;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

import org.w3c.dom.Document;

/**
 * @author jbufu
 */
public class MyPDOMTest
{
    private static final Logger logger = Logger.getLogger(MyPDOMTest.class.getName());
    private static final String FILE = "/tmp/test.xml";

    public static void main(String[] args) throws Exception {
        MyPDOMTest.testParse();
    }

    public static void testParse() throws Exception {
        // global file cache settings
        SharedFileDataCache.setSize(30);                     // default = 3
        SharedIndexPageCache.setFirstLevelCacheSize(40);     // default = 4
        SharedIndexPageCache.setSecondLevelCacheSize(80);    // default = 8
        SharedNodePageCache.setSize(1000);                   // default = 100

        FileDataServerFactory fdsfac = new FileDataServerFactory();
        fdsfac.setCacheFileData(true);                     // default = false
        System.out.println("FDS factory cacheFileData: " + fdsfac.getCacheFileData());

        PDOMFactory pfac = new PDOMFactory(fdsfac);
        pfac.setMaintainStructureIndex(false);              // default = true

        File tempPdomFile = File.createTempFile("tmp", ".pdom", new File("/tmp"));
        PDOM.setCacheSize(1000);                            // default = 100
        PDOM pdom = pfac.create(tempPdomFile);
//        pdom.deferStructureIndexUpdate();

        PDOMParserFactory ppfac = new PDOMParserFactory();
        System.out.println("parser DTD: " + ppfac.getCreateDTDNodes());
        System.out.println("parser entityRef: " + ppfac.getCreateEntityReferenceNodes());
        ppfac.getSAXParserFactory().setNamespaceAware(true);
        PDOMParser parser = ppfac.newParser();

        final String fn = FILE;
        logger.info("PDOM parsing start: " + fn);
        long time = System.currentTimeMillis();
        final FileInputStream fis = new FileInputStream(fn);
        parser.parse(fis, pdom);
        time = System.currentTimeMillis() - time;
        logger.info("PDOM parsing end; time: " + time / 1000 + " seconds.");
        fis.close();

        time = System.currentTimeMillis();
        pdom.commit();
        time = System.currentTimeMillis() - time;
        logger.info("PDOM commit done; time: " + time / 1000 + " seconds.");

        testXPath(pdom.getDocument());

        pdom.close();
    }

    public static void testXPath(Document dom) throws Exception {
        XPathExpression xpe = XPathFactory.newExpression("//ea3");
        logger.info("PDOM XPath start ");
        long time = System.currentTimeMillis();
        xpe.eval(dom);
        time = System.currentTimeMillis() - time;
        logger.info("PDOM XPath end; time: " + time + " milliseconds.");
    }
}

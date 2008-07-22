/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks.tarari;

import com.l7tech.message.Message;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.TarariLoader;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.test.BenchmarkRunner;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class TarariWssSignatureTest {
    private static final Logger logger = Logger.getLogger(TarariWssSignatureTest.class.getName());

    public static void main(String[] args) throws Exception {
        System.setProperty("com.l7tech.common.xml.tarari.enable", args[0]);
        TarariLoader.compile();

        //WssProcessorTest.TestDocument testdoc = WssProcessorTest.makeDotNetTestDocument("thing", TestDocuments.DOTNET_SIGNED_REQUEST);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlUtil.nodeToOutputStream(null/*testdoc.document*/, baos);
        final byte[] bytes = baos.toByteArray();

        Runnable runnable = new Runnable() {
            public void run() {
                Message msg = null;
                try {
                    msg = null;//new Message(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(bytes));
                    new TarariWssProcessingContext(msg).process();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, ExceptionUtils.getMessage(e), e);
                    System.exit(1);
                } finally {
                    if (msg != null) msg.close();
                }
            }
        };

        BenchmarkRunner brun = new BenchmarkRunner(runnable, Integer.parseInt(args[1]), "Tarari=" + args[0]);
        brun.setThreadCount(Integer.parseInt(args[2]));
        brun.run();
    }



}

package com.l7tech.server.transport.ftp;

import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import com.l7tech.util.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nilic
 * @author jwilliams
 */
public class FtpListUtil {

    private static List<String> LIST_COMMANDS =
            Arrays.asList(FtpMethod.FTP_LIST.getWspName(),
                    FtpMethod.FTP_MDTM.getWspName(),
                    FtpMethod.FTP_MLSD.getWspName(),
                    FtpMethod.FTP_MLST.getWspName(),
                    FtpMethod.FTP_NLST.getWspName(),
                    FtpMethod.FTP_SIZE.getWspName(),
                    //FtpMethod.FTP_USER.getWspName(),
                    //FtpMethod.FTP_PASS.getWspName(),
                    FtpMethod.FTP_PWD.getWspName(),
                    FtpMethod.FTP_SYST.getWspName(),
                    FtpMethod.FTP_FEAT.getWspName(),
                    FtpMethod.FTP_HELP.getWspName());

    private static final Logger logger = Logger.getLogger(FtpListUtil.class.getName());
    private InputStream responseStream;

    public FtpListUtil(InputStream responseStream) {
        this.responseStream = responseStream;
    }

    public String writeMessageToOutput(){
        String messageOut;

        try {
            messageOut = new String(IOUtils.slurpStream(responseStream));
        } catch (SocketException ex) {
            logger.log(Level.WARNING, "Socket exception during list transfer", ex);
            return null;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "IOException during list transfer", ex);
            return null;
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Illegal list syntax");
            return null;
        }

        return messageOut;
    }

    public static Document createDoc(String responseMessage) {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder;
        Document document;

        try {
            builder = factory.newDocumentBuilder();

            // Use String reader
            document = builder.parse(new InputSource(new StringReader(responseMessage)));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to parse String to Xml");
            return null;
        }

        return document;
    }

    public static String getRawListData(Document doc) {
        StringBuilder sb = new StringBuilder();
        NodeList nodes = doc.getDocumentElement().getElementsByTagName("raw");

        for (int i = 0; i < nodes.getLength(); i++){
            Node node = nodes.item(i);
            sb.append("\r\n");
            sb.append(node.getTextContent());
        }

        sb.append("\r\n");

        return sb.toString();
    }

    public static boolean isInputStreamCommand(FtpMethod ftpMethod){
        return LIST_COMMANDS.contains(ftpMethod.getWspName());
    }
}

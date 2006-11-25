package com.l7tech.server.config.commands;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.partition.PartitionInformation;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 10:51:03 AM
 */
public class PartitionConfigCommand extends BaseConfigurationCommand{
    private static final Logger logger = Logger.getLogger(PartitionConfigCommand.class.getName());
    PartitionConfigBean partitionBean;

    public PartitionConfigCommand(ConfigurationBean bean) {
        super(bean);
        partitionBean = (PartitionConfigBean) configBean;
    }

    public boolean execute() {
        boolean success = true;
        PartitionInformation pInfo = partitionBean.getPartitionInfo();
        try {
            updatePartitionEndpoints(pInfo);
        } catch (Exception e) {
            success = false;
        }
        return success;
    }

    private void updatePartitionEndpoints(PartitionInformation pInfo) throws IOException, SAXException {

        List<PartitionInformation.EndpointHolder> newEndpoints = pInfo.getEndpointsList();
        Document serverConfigDom = pInfo.getOriginalDom();
        if (serverConfigDom == null) {
            serverConfigDom = getDomFromServerConfig(pInfo);
        }

        NodeList connectors = serverConfigDom.getElementsByTagName("Connector");
        if (connectors != null && connectors.getLength() > 0) {
            for (int index = 0; index < connectors.getLength(); index++) {
                Element connector = (Element) connectors.item(index);
            }
        }

    }

    private Document getDomFromServerConfig(PartitionInformation pInfo) throws IOException, SAXException {
        Document doc = null;
        FileInputStream fis = null;
        String errorMessage = "Could not read the server.xml for partition \"" + pInfo + "\": ";
        try {
            OSSpecificFunctions osf = pInfo.getOSSpecificFunctions();
            String serverConfigPath = osf.getTomcatServerConfig();
            fis = new FileInputStream(serverConfigPath);
            doc = XmlUtil.parse(fis);

        } catch (FileNotFoundException e) {
            logger.severe(errorMessage + e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe(errorMessage + e.getMessage());
            throw e;
        } catch (SAXException e) {
            logger.severe(errorMessage + e.getMessage());
            throw e;
        } finally {
            if (fis != null)
            try {
                fis.close();
            } catch (IOException e) {
            }
        }
        return doc;
    }

}

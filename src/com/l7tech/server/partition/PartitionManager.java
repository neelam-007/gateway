package com.l7tech.server.partition;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.config.OSDetector;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manage the partition configurations on an SSG.
 * Partitions are physically separate instances of the gateway running from one codebase (the WAR) with different configurations.
 *
 * Partition configurations are located in $SSG_HOME/etc/conf/partitions/partitionName where partitionName is a normal String.
 * All configuration for an SSG is under this directory
 *
 * Date: Nov 10, 2006
 * Time: 10:00:01 AM
 */
public class PartitionManager {

    private static final Logger logger = Logger.getLogger(PartitionManager.class.getName());

    private static PartitionManager instance;

    private Map<String, PartitionInformation> partitions;

    public static PartitionManager getInstance() {
            if (instance == null)
                instance = new PartitionManager();
            return instance;
        }

    //private constructor - must access this class through getInstance()
    private PartitionManager() {
        partitions = new HashMap<String, PartitionInformation>();
        enumeratePartitions();
    }

    private void enumeratePartitions() {
        String partitionRoot = OSDetector.getOSSpecificFunctions().getPartitionBase();

        File partitionBaseDir = new File(partitionRoot);

        if (partitionBaseDir.exists() && partitionBaseDir.isDirectory()) {
            File[] partitionDirectories = partitionBaseDir.listFiles();
            if (partitionDirectories != null) {
                for (File partitionDir : partitionDirectories) {
                    if (partitionDir.isDirectory()) {
                        if (!partitionDir.getName().equals(PartitionInformation.TEMPLATE_PARTITION_NAME))
                            addExistingPartition(partitionDir);
                    }
                }
            }
        }
    }

    private void addExistingPartition(File partitionDir) {
        //inside partitionDir is a server.xml that we need to parse
        String partitionName = partitionDir.getName();
        String serverXmlPath = OSDetector.getOSSpecificFunctions(partitionName).getTomcatServerConfig();
        InputStream is = null;
        try {
            is = new FileInputStream(serverXmlPath);
            Document dom = XmlUtil.parse(is);
            PartitionInformation pi = null;
            pi = new PartitionInformation(partitionName, dom, false);
            partitions.put(new String(pi.getPartitionId()), pi);
        } catch (FileNotFoundException e) {
            logger.warning("Could not find a server.xml for partition \"" + partitionDir.getName() + "\". This partition " +
                    "will not be enumerated");
            logger.warning(e.getMessage());
        } catch (XPathExpressionException e) {
            logger.warning("There was an error while reading the configuration of partition \"" + partitionName + "\". This partition will not be enumerated");
            logger.warning(e.getMessage());
        } catch (SAXException e) {
            logger.warning("There was an error while reading the configuration of partition \"" + partitionName + "\". This partition will not be enumerated");
            logger.warning(e.getMessage());
        } catch (IOException e) {
            logger.warning("There was an error while reading the configuration of partition \"" + partitionName + "\". This partition will not be enumerated");
            logger.warning(e.getMessage());
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {}
        }
    }

    public boolean isPartitioned() {
        return !partitions.isEmpty();
    }

    public Set<String> getPartitionNames() {
            return partitions.keySet();
        }

    public PartitionInformation getPartition(String partitionId) {
        return partitions.get(partitionId);
    }

    public List<PartitionInformation.EndpointHolder> getEndpoints() {
        List<PartitionInformation.EndpointHolder> allEndpoints = new ArrayList<PartitionInformation.EndpointHolder>();

        Set<Map.Entry<String,PartitionInformation>> entries = partitions.entrySet();
        for (Map.Entry<String, PartitionInformation> entry : entries) {
            PartitionInformation pInfo = entry.getValue();
            allEndpoints.addAll(pInfo.getEndpointsList());
        }
        return allEndpoints;
    }

}

package com.l7tech.server.partition;

import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 10, 2006
 * Time: 10:15:56 AM
 */
public class PartitionInformation{

    private static final Logger logger = Logger.getLogger(PartitionInformation.class.getName());

    public static final String PARTITIONS_BASE = "etc/conf/partitions/";
    public static final String TEMPLATE_PARTITION_NAME = "partitiontemplate_";
    public static final String DEFAULT_PARTITION_NAME = "default_";
    public static final String ENABLED_FILE = "enabled";

    public static final String SYSTEM_PROP_PARTITIONNAME = "com.l7tech.server.partitionName";

    private String partitionId;
    private String oldPartitionId;
    boolean isNewPartition = false;
    boolean isEnabled = false;
    private boolean shouldDisable = false;
    
    private List<HttpEndpointHolder> httpEndpointsList;
    private List<FtpEndpointHolder> ftpEndpointsList;
    private List<OtherEndpointHolder> otherEndpointsList;

    OSSpecificFunctions osf;
    Document originalDom;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    private static final String ENABLED = " (Enabled)";
    private static final String DISABLED = " (Disabled)";

    private static int DEFAULT_HTTP_PORT = 8080;
    private static int DEFAULT_SSL_PORT = 8443;
    private static int DEFAULT_NOAUTH_PORT = 9443;
    private static int DEFAULT_RMI_PORT = 2124;

    public static final String ALLOWED_PARTITION_NAME_PATTERN = "[^\\p{Punct}\\s]{1,128}";
    public static final int MAX_PARTITIONS = 8;
    public enum OtherEndpointType {
        RMI_ENDPOINT("Inter-Node Communication Port"),
        ;

        private String endpointName;

        OtherEndpointType(String endpointName) {this.endpointName = endpointName;}
        public String getName() {return endpointName;}
        public String toString() {return endpointName;}
    }

    public enum HttpEndpointType {
        BASIC_HTTP("Basic HTTP Endpoint"),
        SSL_HTTP("SSL Endpoint"),
        SSL_HTTP_NOCLIENTCERT("SSL Endpoint (No Client Certificate)"),
        ;

        private String endpointName;

        HttpEndpointType(String endpointName) {this.endpointName = endpointName;}
        public String getName() {return endpointName;}
        public String toString() {return endpointName;}
    }

    public enum FtpEndpointType {
        BASIC_FTP("Basic FTP Endpoint"),
        SSL_FTP_NOCLIENTCERT("SSL FTP Endpoint"),
        ;

        private String endpointName;

        FtpEndpointType(String endpointName) {this.endpointName = endpointName;}
        public String getName() {return endpointName;}
        public String toString() {return endpointName;}
    }

    public PartitionInformation() {
    }

    public PartitionInformation(String partitionName) {
        this.partitionId = partitionName;
        isNewPartition = true;
        httpEndpointsList = new ArrayList<HttpEndpointHolder>();
        ftpEndpointsList = new ArrayList<FtpEndpointHolder>();
        otherEndpointsList = new ArrayList<OtherEndpointHolder>();
        osf = OSDetector.getOSSpecificFunctions(partitionName);
    }

    public PartitionInformation copy() {
        PartitionInformation theCopy = new PartitionInformation(this.getPartitionId());
        theCopy.setEnabled(this.isEnabled());
        theCopy.setNewPartition(this.isNewPartition());
        theCopy.setOriginalDom(this.getOriginalDom());
        theCopy.setShouldDisable(this.isShouldDisable());
        theCopy.setHttpEndpointsList(this.getHttpEndpoints());
        theCopy.setFtpEndpointsList(this.getFtpEndpoints());
        theCopy.setOtherEndpointsList(this.getOtherEndpoints());
        return theCopy;
    }

    public Collection<SsgConnector> getConnectorsFromServerXml() {
        if (new File(getOSSpecificFunctions().getTomcatServerConfig()).exists())
            return DefaultHttpConnectors.makeFallbackConnectors(getOSSpecificFunctions().getTomcatServerConfig());
        else
            return Collections.emptyList();
    }

    public List<SsgConnector> parseFtpEndpointsAsSsgConnectors() {
        File ftpServerProps = new File(OSDetector.getOSSpecificFunctions(partitionId).getFtpServerConfig());
        if (!ftpServerProps.exists()) return Collections.emptyList();

        FileInputStream fis = null;
        try {
            Properties props = new Properties();
            fis = new FileInputStream(ftpServerProps);
            props.load(fis);

            List<SsgConnector> ret = new ArrayList<SsgConnector>();
            {
                //do the default ftp connector config
                FtpEndpointHolder defaultHolder = new FtpEndpointHolder(FtpEndpointType.BASIC_FTP);
                updateFtpEndpoint(defaultHolder, props, "default");
                SsgConnector basic = new SsgConnector();
                basic.setScheme(SsgConnector.SCHEME_FTP);
                basic.setPort(defaultHolder.getPort());
                basic.setEnabled(defaultHolder.isEnabled());
                basic.setName("Legacy FTP " + basic.getPort());
                basic.setSecure(false);
                basic.setEndpoints(SsgConnector.Endpoint.MESSAGE_INPUT.toString());
                String addr = defaultHolder.getIpAddress();
                if (addr != null && addr.length() < 1 || addr.equals("*")) addr = null;
                basic.putProperty(SsgConnector.PROP_BIND_ADDRESS, addr);
                basic.putProperty(SsgConnector.PROP_PORT_RANGE_START, defaultHolder.getPassivePortStart().toString());
                basic.putProperty(SsgConnector.PROP_PORT_RANGE_COUNT, defaultHolder.getPassivePortCount().toString());
                ret.add(basic);
            }

            {
                //do the secure ftp connector config
                FtpEndpointHolder secureHolder = new FtpEndpointHolder(FtpEndpointType.SSL_FTP_NOCLIENTCERT);
                updateFtpEndpoint(secureHolder, props, "secure");
                SsgConnector sec = new SsgConnector();
                sec.setScheme(SsgConnector.SCHEME_FTPS);
                sec.setPort(secureHolder.getPort());
                sec.setSecure(true);
                sec.setEnabled(secureHolder.isEnabled());
                sec.setClientAuth(SsgConnector.CLIENT_AUTH_NEVER);
                sec.setEndpoints(SsgConnector.Endpoint.MESSAGE_INPUT.toString());
                String addr = secureHolder.getIpAddress();
                if (addr != null && addr.length() < 1 || addr.equals("*")) addr = null;
                sec.putProperty(SsgConnector.PROP_BIND_ADDRESS, addr);
                sec.putProperty(SsgConnector.PROP_PORT_RANGE_START, secureHolder.getPassivePortStart().toString());
                sec.putProperty(SsgConnector.PROP_PORT_RANGE_COUNT, secureHolder.getPassivePortCount().toString());
                sec.setName("Legacy FTPS " + sec.getPort());
                ret.add(sec);
            }

            return ret;

        } catch (FileNotFoundException e) {
            logger.warning("no FTP server properties file found for partition: " + partitionId);
        } catch (IOException e) {
            logger.warning("Error while reading the FTP server properties file for partition: " + partitionId);
            logger.warning(e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
        return Collections.emptyList();
    }

    private void updateFtpEndpoint(FtpEndpointHolder ftpEndpointHolder, Properties props, String name) {
        String prefix = "ssgftp." + name + ".";
        Boolean enabledVal = parseBoolean(props.getProperty(prefix + "enabled"), "Invalid enabled flag");
        String addressVal = props.getProperty(prefix + "address");
        Integer controlPortVal = parseInteger(props.getProperty(prefix + "controlPort"), "Invalid FTP server control port ''{0}''.");
        Integer passiveStartVal = parseInteger(props.getProperty(prefix + "passivePortStart"), "Invalid FTP server passive start port ''{0}''.");
        Integer passiveEndVal = parseInteger(props.getProperty(prefix + "passivePortEnd"), "Invalid FTP server passive end port ''{0}''.");

        if (enabledVal != null)
            ftpEndpointHolder.setEnabled(enabledVal.booleanValue());

        if (addressVal != null) {
            if ("0.0.0.0".equals(addressVal))
                ftpEndpointHolder.setIpAddress("*");
            else
                ftpEndpointHolder.setIpAddress(addressVal);
        }

        if (controlPortVal != null)
            ftpEndpointHolder.setPort(controlPortVal);

        if (passiveStartVal != null && passiveEndVal != null) {
            ftpEndpointHolder.setPassivePortStart(passiveStartVal);
            ftpEndpointHolder.setPassivePortCount(Integer.valueOf((passiveEndVal.intValue() - passiveStartVal.intValue()) + 1));
        }
    }

    public OSSpecificFunctions getOSSpecificFunctions() {
        if (osf == null) osf = OSDetector.getOSSpecificFunctions(partitionId);
        return osf;
    }

    public void setPartitionId(String newId) {
        oldPartitionId = partitionId;
        partitionId = newId;
        getOSSpecificFunctions().setPartitionName(partitionId);
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getOldPartitionId() {
        return oldPartitionId;
    }

    public boolean isNewPartition() {
        return isNewPartition;
    }

    public void setNewPartition(boolean newPartition) {
        isNewPartition = newPartition;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isShouldDisable() {
        return shouldDisable;
    }

    public void setShouldDisable(boolean shouldDisable) {
        this.shouldDisable = shouldDisable;
    }

    public List<EndpointHolder> getEndpoints() {
        List<PartitionInformation.EndpointHolder> allHolders = new ArrayList<PartitionInformation.EndpointHolder>();
        allHolders.addAll(getHttpEndpoints());
        allHolders.addAll(getFtpEndpoints());
        allHolders.addAll(getOtherEndpoints());
        return allHolders;
    }
    
    public List<HttpEndpointHolder> getHttpEndpoints() {
        return httpEndpointsList;
    }

    public List<FtpEndpointHolder> getFtpEndpoints() {
        return getFtpEndpoints(false);
    }

    public List<FtpEndpointHolder> getFtpEndpoints(boolean activeOnly) {
        List<FtpEndpointHolder> endpoints = new ArrayList<FtpEndpointHolder>();

        for (FtpEndpointHolder ftpEndpointHolder : ftpEndpointsList) {
            if (!activeOnly || ftpEndpointHolder.isEnabled()) {
                endpoints.add(ftpEndpointHolder);
            }
        }

        return endpoints;
    }

    public List<OtherEndpointHolder> getOtherEndpoints() {
        return otherEndpointsList;
    }

    public void setHttpEndpointsList(List<HttpEndpointHolder> httpEndpointsList) {
        this.httpEndpointsList = httpEndpointsList;
    }

    public void setFtpEndpointsList(List<FtpEndpointHolder> ftpEndpointsList) {
        this.ftpEndpointsList = ftpEndpointsList;
    }

    public void setOtherEndpointsList(List<OtherEndpointHolder> otherEndpointsList) {
        this.otherEndpointsList = otherEndpointsList;
    }

    public String toString() {
        if (getOSSpecificFunctions().isUnix())
            return partitionId + (isEnabled?"":" (Disabled)");

        return partitionId;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PartitionInformation that = (PartitionInformation) o;

        if (partitionId != null ? !partitionId.equals(that.partitionId) : that.partitionId != null) return false;

        return true;
    }

    public int hashCode() {
        return (partitionId != null ? partitionId.hashCode() : 0);
    }

    private Boolean parseBoolean(String booleanValue, String warningMessage) {
        Boolean result = null;

        if (booleanValue != null) {
            if ("true".equalsIgnoreCase(booleanValue)) {
                result = Boolean.TRUE;
            }
            else if ("false".equalsIgnoreCase(booleanValue)) {
                result = Boolean.FALSE;
            }
            else {
                logger.log(Level.WARNING, warningMessage, booleanValue);
            }
        }

        return result;
    }

    private Integer parseInteger(String intValue, String warningMessage) {
        Integer result = null;

        if (intValue != null) {
            try {
                result = new Integer(intValue);
            }
            catch(NumberFormatException nfe) {
                logger.log(Level.WARNING, warningMessage, intValue);
            }
        }

        return result;
    }

    public Document getOriginalDom() {
        return originalDom;
    }

    public void setOriginalDom(Document originalDom) {
        this.originalDom = originalDom;
    }

    public static abstract class EndpointHolder {
        private String ipAddress = "";
        private Integer port;
        private String validationMessaqe;

        public String toString() {
            return describe();
        }

        public boolean isEnabled() {
            return true;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Integer[] getPorts() {
            return new Integer[]{port};
        }

        public String getValidationMessaqe() {
            return validationMessaqe;
        }

        public void setValidationMessaqe(String validationMessaqe) {
            this.validationMessaqe = validationMessaqe;
        }

        public boolean equals(Object o) {
            return isEquals(o);
        }

        public void avoidPorts(Integer[] ports) {
            Integer port = getPort();

            while (ArrayUtils.contains(ports, port)) {
                int value = port.intValue();
                if (value < MAX_PORT) {
                    port = Integer.valueOf(value + 1);
                }
                else {
                    break;
                }
            }

            setPort(port);
        }

        abstract boolean isEquals(Object o);

        public int hashCode() {
            return getHashCode();
        }

        abstract int getHashCode();

        public abstract String describe();
        public abstract void setValueAt(int columnIndex, Object aValue);
    }

    public static class OtherEndpointHolder extends EndpointHolder{
        public OtherEndpointType endpointType;

        public OtherEndpointHolder(OtherEndpointType endpointType) {
            this.endpointType = endpointType;
        }

        public OtherEndpointHolder(Integer port, OtherEndpointType endpointType) {
            this.setPort(port);
            this.endpointType = endpointType;
        }

        public boolean isEquals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OtherEndpointHolder that = (OtherEndpointHolder) o;

            if (getPort() != null ? !getPort().equals(that.getPort()) : that.getPort()!= null) return false;

            return true;
        }

        public int getHashCode() {
            int result;
            result = 31 * (getPort() != null ? getPort().hashCode() : 0);
            return result;
        }

        public String describe() {
            return endpointType.getName() + " = " + getPort();
        }

        public static void populateDefaultEndpoints(List<OtherEndpointHolder> endpoints) {
            OtherEndpointHolder holder = new OtherEndpointHolder(OtherEndpointType.RMI_ENDPOINT);
            holder.setPort(DEFAULT_RMI_PORT);
            endpoints.add(holder);
        }

        public void setValueAt(int columnIndex, Object aValue) {
            switch(columnIndex) {
                case 0:
                    break;
                case 1:
                    this.setPort(Integer.parseInt(String.valueOf(aValue)));
                    break;
            }
        }

        public static Class<?> getClassAt(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return OtherEndpointType.class;
                case 1:
                    return Integer.class;
                default:
                    return String.class;
            }
        }
    }

    public static class HttpEndpointHolder extends EndpointHolder {
        private static String[] headings = new String[] {
            "Endpoint Type",
            "IP Address",
            "Port",
        };

        public HttpEndpointType endpointType;

        public HttpEndpointHolder(HttpEndpointType type) {
            this.endpointType = type;
            setIpAddress("*");
        }

        public boolean isEquals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HttpEndpointHolder that = (HttpEndpointHolder) o;

            if (getIpAddress() != null ? !getIpAddress().equals(that.getIpAddress()) : that.getIpAddress() != null) return false;
            if (getPort() != null ? !getPort().equals(that.getPort()) : that.getPort() != null) return false;

            return true;
        }

        public int getHashCode() {
            int result;
            result = (getIpAddress() != null ? getIpAddress().hashCode() : 0);
            result = 31 * result + (getPort() != null ? getPort().hashCode() : 0);
            return result;
        }


        public String describe() {
            if (StringUtils.isEmpty(getIpAddress()) || getPort()==null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(endpointType.getName()).append(" = ");
            sb.append(getIpAddress().equals("*")?"* (all interfaces)":getIpAddress()).append(", ");
            sb.append(getPort());
            return sb.toString();
        }

        public static String[] getHeadings() {
            return headings;
        }

        public Object getValue(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return endpointType.getName();
                case 1:
                    return getIpAddress();
                case 2:
                    return getPort();
                default:
                    return null;
            }
        }

        public void setValueAt(int columnIndex, Object aValue) {
            switch(columnIndex) {
                case 0:
                    break;
                case 1:
                    this.setIpAddress(String.valueOf(aValue));
                    break;
                case 2:
                    this.setPort(Integer.parseInt(String.valueOf(aValue)));
                    break;
            }
        }

        public static Class<?> getClassAt(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return HttpEndpointType.class;
                case 1:
                    return String.class;
                case 2:
                    return Integer.class;
                default:
                    return String.class;
            }
        }

        public static void populateDefaultEndpoints(List<HttpEndpointHolder> endpoints) {
            HttpEndpointHolder holder = new HttpEndpointHolder(HttpEndpointType.BASIC_HTTP);
            holder.setPort(DEFAULT_HTTP_PORT);
            endpoints.add(holder);

            holder = new PartitionInformation.HttpEndpointHolder(PartitionInformation.HttpEndpointType.SSL_HTTP);
            holder.setPort(DEFAULT_SSL_PORT);
            endpoints.add(holder);

            holder = new PartitionInformation.HttpEndpointHolder(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT);
            holder.setPort(DEFAULT_NOAUTH_PORT);
            endpoints.add(holder);
        }
    }

    public static class FtpEndpointHolder extends EndpointHolder {
        private static String[] headings = new String[] {
            "Endpoint Type",
            "Enabled",
            "IP Address",
            "Port",
            "Passive Port Start",
            "Passive Port Count",
        };

        private final FtpEndpointType endpointType;
        private boolean enabled;
        private Integer passivePortStart;
        private Integer passivePortCount;

        public FtpEndpointHolder(FtpEndpointType type) {
            this.endpointType = type;
            setIpAddress("*");
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer[] getPorts() {
            Integer[] ports = new Integer[passivePortCount.intValue()+1];
            ports[0] = getPort().intValue();
            for (int i=0; i<ports.length-1; i++) {
                ports[i+1] = passivePortStart.intValue() + i;                               
            }
            return ports;
        }

        public Integer[] getPassivePorts() {
            Integer[] ports = new Integer[passivePortCount.intValue()];
            for (int i=0; i<ports.length; i++) {
                ports[i] = passivePortStart.intValue() + i;
            }
            return ports;
        }

        public void avoidPorts(Integer[] ports) {
            while (ArrayUtils.contains(ports, getPort())) {
                int value = getPort().intValue();
                if (value < MAX_PORT) {
                    setPort(Integer.valueOf(value + 1));
                }
                else {
                    break;
                }
            }

            while (ArrayUtils.containsAny(ports, getPassivePorts())) {
                int value = passivePortStart.intValue() + (passivePortCount.intValue()-1);
                if (value < MAX_PORT) {
                    passivePortStart = Integer.valueOf(passivePortStart.intValue() + 1);
                }
                else {
                    break;
                }
            }
        }

        public FtpEndpointType getEndpointType() {
            return endpointType;
        }

        public Integer getPassivePortStart() {
            return passivePortStart;
        }

        public void setPassivePortStart(Integer passivePortStart) {
            this.passivePortStart = passivePortStart;
        }

        public Integer getPassivePortCount() {
            return passivePortCount;
        }

        public void setPassivePortCount(Integer passivePortCount) {
            this.passivePortCount = passivePortCount;
        }

        public boolean isEquals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FtpEndpointHolder that = (FtpEndpointHolder) o;

            if (getIpAddress() != null ? !getIpAddress().equals(that.getIpAddress()) : that.getIpAddress() != null) return false;
            if (getPort() != null ? !getPort().equals(that.getPort()) : that.getPort() != null) return false;
            if (getPassivePortStart() != null ? !getPassivePortStart().equals(that.getPassivePortStart()) : that.getPassivePortStart() != null) return false;
            if (getPassivePortCount() != null ? !getPassivePortCount().equals(that.getPassivePortCount()) : that.getPassivePortCount() != null) return false;

            return true;
        }

        public int getHashCode() {
            int result;
            result = (getIpAddress() != null ? getIpAddress().hashCode() : 0);
            result = 31 * result + (getPort() != null ? getPort().hashCode() : 0);
            return result;
        }

        public String describe() {
            if (StringUtils.isEmpty(getIpAddress()) || getPort()==null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(endpointType.getName()).append(isEnabled() ? ENABLED : DISABLED).append(" = ");
            sb.append(getIpAddress().equals("*")?"* (all interfaces)":getIpAddress()).append(", ");
            sb.append(getPort());
            sb.append(",");
            sb.append(getPassivePortStart());
            sb.append("-");
            sb.append((getPassivePortStart().intValue() + getPassivePortCount().intValue() -1));
            return sb.toString();
        }

        public static String[] getHeadings() {
            return headings;
        }

        public Object getValue(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return endpointType.getName();
                case 1:
                    return Boolean.valueOf(isEnabled());
                case 2:
                    return getIpAddress();
                case 3:
                    return getPort();
                case 4:
                    return getPassivePortStart();
                case 5:
                    return getPassivePortCount();
                default:
                    return null;
            }
        }

        public void setValueAt(int columnIndex, Object aValue) {
            switch(columnIndex) {
                case 0:
                    break;
                case 1:
                    this.setEnabled(Boolean.valueOf(String.valueOf(aValue)));
                    break;
                case 2:
                    this.setIpAddress(String.valueOf(aValue));
                    break;
                case 3:
                    this.setPort(Integer.parseInt(String.valueOf(aValue)));
                    break;
                case 4:
                    this.setPassivePortStart(Integer.parseInt(String.valueOf(aValue)));
                    break;
                case 5:
                    this.setPassivePortCount(Integer.parseInt(String.valueOf(aValue)));
                    break;
            }
        }

        public static Class<?> getClassAt(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return FtpEndpointType.class;
                case 1:
                    return Boolean.class;
                case 2:
                    return String.class;
                case 3:
                    return Integer.class;
                case 4:
                    return Integer.class;
                case 5:
                    return Integer.class;
                default:
                    return String.class;
            }
        }
    }

    public static class IpPortPair {
        private EndpointHolder endpointHolder;

        public IpPortPair(PartitionInformation.EndpointHolder holder) {
            this.endpointHolder = holder;
        }

        public String getIpAddress() {
            return StringUtils.isEmpty(endpointHolder.getIpAddress())?"*":endpointHolder.getIpAddress();
        }

        public void setIpAddress(String ipAddress) {
            if (endpointHolder != null) {
                endpointHolder.ipAddress = ipAddress;
            }
        }

        public Integer[] getPorts() {
            Integer[] ports = new Integer[0];

            if (endpointHolder != null) {
                ports = endpointHolder.getPorts();
            }

            return ports;
        }

        public boolean conflictsWith(IpPortPair o, String[] messageHolder) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IpPortPair that = (IpPortPair) o;

            if (endpointHolder != null) {
                if (StringUtils.equals(getIpAddress(), "*") ||
                     StringUtils.equals(that.getIpAddress(), "*") ||
                     StringUtils.equals(that.getIpAddress(), getIpAddress())) {

                    Integer[] ports1 = endpointHolder.getPorts();
                    Integer[] ports2 = that.endpointHolder.getPorts();

                    for (Integer port : ports2) {
                        if (ArrayUtils.contains(ports1, port)) {
                            if (messageHolder!=null)
                                messageHolder[0] = that.getIpAddress() + ":" + port;
                            return true;
                        }
                    }
                }
            }

            if (endpointHolder != null ? !endpointHolder.equals(that.endpointHolder) : that.endpointHolder != null)
                 return false;

            return true;
        }

        public int hashCode() {
            return (endpointHolder != null ? endpointHolder.hashCode() : 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotEmpty(getIpAddress())) {
                sb.append(getIpAddress()).append(":");
            }
            if (endpointHolder != null) {
                if(endpointHolder.getPorts().length == 1) {
                    sb.append(endpointHolder.getPort());
                } else {
                    sb.append(Arrays.asList(endpointHolder.getPorts()));
                }
            }

            return sb.toString();
        }
    }
}
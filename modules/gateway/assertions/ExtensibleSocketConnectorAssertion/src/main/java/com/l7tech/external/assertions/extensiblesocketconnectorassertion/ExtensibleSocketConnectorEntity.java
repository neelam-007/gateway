package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.CodecConfiguration;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.XmlSafe;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 29/11/11
 * Time: 4:09 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlSafe
public class ExtensibleSocketConnectorEntity extends GenericEntity {
    private String name;
    private boolean in = true;
    private String hostname;
    private int defaultPort = 8888;
    private int port = defaultPort;
    private boolean useSsl = false;
    private String sslKeyId;
    private SSLClientAuthEnum clientAuthEnum;
    private int threadPoolMin = 10;
    private int threadPoolMax = 20;
    private String bindAddress;
    private Goid serviceGoid;
    private String contentType = null;
    private int maxMessageSize = 1024 * 1024;
    private boolean enabled = false;
    private CodecConfiguration codecConfiguration;
    private ExchangePatternEnum exchangePattern = ExchangePatternEnum.OutIn;
    private boolean keepAlive = false;
    private long listenTimeout = 0L;

    private boolean usePortValue = true;
    private boolean useDnsLookup = false;
    private String dnsDomainName = "";
    private String dnsService = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIn() {
        return in;
    }

    public void setIn(boolean in) {
        this.in = in;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public String getSslKeyId() {
        return sslKeyId;
    }

    public void setSslKeyId(String sslKeyId) {
        this.sslKeyId = sslKeyId;
    }

    public SSLClientAuthEnum getClientAuthEnum() {
        return clientAuthEnum;
    }

    public void setClientAuthEnum(SSLClientAuthEnum clientAuthEnum) {
        this.clientAuthEnum = clientAuthEnum;
    }

    public int getThreadPoolMin() {
        return threadPoolMin;
    }

    public void setThreadPoolMin(int threadPoolMin) {
        this.threadPoolMin = threadPoolMin;
    }

    public int getThreadPoolMax() {
        return threadPoolMax;
    }

    public void setThreadPoolMax(int threadPoolMax) {
        this.threadPoolMax = threadPoolMax;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    @XmlSafe
    public void setServiceOid(long serviceOid) {
        this.serviceGoid = GoidUpgradeMapper.mapOid(EntityType.SERVICE, serviceOid);
    }

    @XmlSafe
    public Goid getServiceGoid() {
        return serviceGoid;
    }

    @XmlSafe
    public void setServiceGoid(Goid serviceGoid) {
        this.serviceGoid = serviceGoid;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public CodecConfiguration getCodecConfiguration() {
        return codecConfiguration;
    }

    public void setCodecConfiguration(CodecConfiguration codecConfiguration) {
        this.codecConfiguration = codecConfiguration;
    }

    public boolean requireListenerRestart(ExtensibleSocketConnectorEntity newConfig) {
        if (in != newConfig.isIn()) {
            return true;
        }

        if (port != newConfig.getPort()) {
            return true;
        }

        if (useSsl != newConfig.isUseSsl()) {
            return true;
        }

        if (useSsl) {
            if (sslKeyId == null && newConfig.getSslKeyId() != null || !sslKeyId.equals(newConfig.getSslKeyId())) {
                return true;
            }

            if (clientAuthEnum != newConfig.getClientAuthEnum()) {
                return true;
            }
        }

        if (threadPoolMin != newConfig.getThreadPoolMin()) {
            return true;
        }

        if (threadPoolMax != newConfig.getThreadPoolMax()) {
            return true;
        }

        if (!bindAddress.equals(newConfig.getBindAddress())) {
            return true;
        }

        if (!serviceGoid.equals(newConfig.getServiceGoid())) {
            return true;
        }

        if (!contentType.equals(newConfig.getContentType())) {
            return true;
        }

        if (maxMessageSize != newConfig.getMaxMessageSize()) {
            return true;
        }

        if (enabled != newConfig.isEnabled()) {
            return true;
        }

        return codecConfiguration.requiresListenerRestart(newConfig.getCodecConfiguration());
    }

    public ExchangePatternEnum getExchangePattern() {
        return exchangePattern;
    }

    public void setExchangePattern(ExchangePatternEnum exchangePattern) {
        this.exchangePattern = exchangePattern;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public long getListenTimeout() {
        return listenTimeout;
    }

    public void setListenTimeout(long listenTimeout) {
        this.listenTimeout = listenTimeout;
    }

    public boolean isUsePortValue() {
        return usePortValue;
    }

    public void setUsePortValue(boolean usePortValue) {
        this.usePortValue = usePortValue;
    }

    public boolean isUseDnsLookup() {
        return useDnsLookup;
    }

    public void setUseDnsLookup(boolean useDnsLookup) {
        this.useDnsLookup = useDnsLookup;
    }

    public String getDnsDomainName() {
        return dnsDomainName;
    }

    public void setDnsDomainName(String dnsDomainName) {
        this.dnsDomainName = dnsDomainName;
    }

    public String getDnsService() {
        return dnsService;
    }

    public void setDnsService(String dnsService) {
        this.dnsService = dnsService;
    }
}

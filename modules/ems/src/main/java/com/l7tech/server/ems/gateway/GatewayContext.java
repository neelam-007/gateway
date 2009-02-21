package com.l7tech.server.ems.gateway;

import com.l7tech.server.DefaultKey;
import com.l7tech.server.management.api.node.GatewayApi;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.SyspropUtil;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * GatewayContext provides access from the ESM to APIs offered by a Gateway cluster (represented by either a
 * single Gateway node or a front-end load balancer URL).
 * <p/>
 * For access to Process Controller APIs, see {@link ProcessControllerContext}.
 */
public class GatewayContext extends ApiContext {
    private static final Logger logger = Logger.getLogger( GatewayContext.class.getName() );
    
    private static final String PROP_GATEWAY_URL = "com.l7tech.esm.gatewayUrl";
    private static final String PROP_REPORT_URL = "com.l7tech.esm.reportUrl";
    private static final String PROP_MIGRATION_URL = "com.l7tech.esm.migrationUrl";
    private static final String REPORT_URL = SyspropUtil.getString(PROP_REPORT_URL, "https://{0}:{1}/ssg/services/reportApi");
    private static final String MIGRATION_URL = SyspropUtil.getString(PROP_MIGRATION_URL, "https://{0}:{1}/ssg/services/migrationApi");
    private static final String GATEWAY_URL = SyspropUtil.getString(PROP_GATEWAY_URL, "https://{0}:{1}/ssg/services/gatewayApi");

    private final AtomicReference<GatewayApi> api = new AtomicReference<GatewayApi>();
    private final AtomicReference<ReportApi> reportApi = new AtomicReference<ReportApi>();
    private final AtomicReference<MigrationApi> migrationApi = new AtomicReference<MigrationApi>();
    private String gatewayUrl;
    private String reportUrl;
    private String migrationUrl;

    /**
     * Create a GatewayContext that uses the given host/port for services.
     *
     * @param defaultKey the ESM's SSL private key.  Required so the ESM can authenticate to the API servers.
     * @param host The gateway SSL hostname, or the node IP address.  Required.
     * @param gatewayPort The gateway port.  Required.
     * @param esmId The ID for the EM
     * @param userId The ID for the EM user (null for none)
     */
    public GatewayContext(final DefaultKey defaultKey, final String host, final int gatewayPort, final String esmId, final String userId) {
        super(logger, defaultKey, host, esmId, userId);
        gatewayUrl = MessageFormat.format(GATEWAY_URL, host, Integer.toString(gatewayPort));
        reportUrl = MessageFormat.format(REPORT_URL, host, Integer.toString(gatewayPort));
        migrationUrl = MessageFormat.format(MIGRATION_URL, host, Integer.toString(gatewayPort));
    }

    public GatewayApi getApi() {
        return getApi(api, GatewayApi.class, gatewayUrl);
    }

    public ReportApi getReportApi() {
        return getApi(reportApi, ReportApi.class, reportUrl);
    }

    public MigrationApi getMigrationApi() {
        return getApi(migrationApi, MigrationApi.class, migrationUrl);
    }
}

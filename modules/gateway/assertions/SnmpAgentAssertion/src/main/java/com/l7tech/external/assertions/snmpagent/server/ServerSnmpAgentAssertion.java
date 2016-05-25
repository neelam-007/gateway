package com.l7tech.external.assertions.snmpagent.server;

import com.l7tech.external.assertions.snmpagent.SnmpAgentAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ServiceUsageManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Pair;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the SnmpAgentAssertion.
 *
 * @see com.l7tech.external.assertions.snmpagent.SnmpAgentAssertion
 */
public class ServerSnmpAgentAssertion extends AbstractServerAssertion<SnmpAgentAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSnmpAgentAssertion.class.getName());
    private SnmpAgentAssertion assertion;
    private String[] variablesUsed;

    //from original snmp agent servlet impl.
    private static final String BASE = ".1.3.6.1.4.1.17304";
    private static final String BASE_PUBLISHED_SERVICES = ".7.1";
    private ServiceUsage[] serviceTable = null;
    private long serviceTableTime = 0;

    // Regex pattern for: "/ssg/management/VERB/OBJECTID"
    private static final Pattern URL_PATTERN = Pattern.compile("^/snmp/management/([^/]*)/" + BASE + "([-\\d.]*)(/.*)?$");

    // Regex pattern for: ".1.4"
    private final Pattern SUB_URL_PATTERN_WITH_GOID = Pattern.compile("^\\.(\\d+)\\.([-\\d]+\\.[-\\d]+\\.[-\\d]+\\.[-\\d]+)$");
    private final Pattern SUB_URL_PATTERN_WITHOUT_GOID = Pattern.compile("^\\.(\\d+)$");


    private ApplicationContext springContext;
    private PolicyEnforcementContext pec;

    public static final String SNMP_RESPONSE_CONTEXT_VAR = "snmp.agent.response";

    private ClusterInfoManager clusterInfoManager;

    private ServiceMetricsManager metricsManager;

    private ServiceMetricsServices metricsServices;

    public ServerSnmpAgentAssertion(SnmpAgentAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
        this.springContext = context;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        HttpRequestKnob requestKnob = context.getRequest().getHttpRequestKnob();
        HttpResponseKnob responseKnob = context.getResponse().getHttpResponseKnob();
        this.pec = context;

        clusterInfoManager = (ClusterInfoManager) springContext.getBean("clusterInfoManager");
        metricsManager = (ServiceMetricsManager)springContext.getBean("serviceMetricsManager");
        metricsServices = (ServiceMetricsServices)springContext.getBean("serviceMetricsServices");

        // Initialize context variable to null.
        // This is done here because there are many branches in this code where this assertion returns
        // "AssertionStatus.NONE" without setting this context variable.
        //
        context.setVariable(ServerSnmpAgentAssertion.SNMP_RESPONSE_CONTEXT_VAR, null);

        try {
            String url = requestKnob.getRequestUrl();
            URL myurl = new URL(url);

            if(!InetAddress.getByName(myurl.getHost()).isLoopbackAddress()){
                throw new UnknownHostException();
            }
        }
        catch(UnknownHostException uhe) {
            logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {"The configured SNMP server hostname could not be found: " + uhe.getMessage()}, uhe);
            context.getResponse().getHttpResponseKnob().setStatus(HttpServletResponse.SC_FORBIDDEN);
            return AssertionStatus.NONE;
        }

        String uri = requestKnob.getRequestUri();
        final Matcher urlMatcher = URL_PATTERN.matcher(uri);
        if (!urlMatcher.matches()) {
            context.getResponse().getHttpResponseKnob().setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return AssertionStatus.NONE;
        }

        String operation = urlMatcher.group(1);
        String subOids = urlMatcher.group(2);

        final boolean advance;
        if ("get".equalsIgnoreCase(operation)) {
            advance = false;
        } else if ("getnext".equalsIgnoreCase(operation)) {
            advance = true;
        } else {
            context.getResponse().getHttpResponseKnob().setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return AssertionStatus.NONE;
        }

        try {
            if (subOids.startsWith(BASE_PUBLISHED_SERVICES)) {
                String returnText = getServiceResponseText(responseKnob,
                        subOids.substring(BASE_PUBLISHED_SERVICES.length()),
                                                 advance);
                if(returnText == null){
                    //bad request.
                    context.getResponse().getHttpResponseKnob().setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return AssertionStatus.NONE;
                }
                //set context var with response text
                context.setVariable(ServerSnmpAgentAssertion.SNMP_RESPONSE_CONTEXT_VAR, returnText);
                return AssertionStatus.NONE;
            }

            // Unrecognized query
            context.getResponse().getHttpResponseKnob().setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return AssertionStatus.NONE;

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            logger.log( Level.INFO, "Internal Server Error", e );
            context.getResponse().getHttpResponseKnob().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return AssertionStatus.NONE;
        }
    }

    //returning null means bad request
    private String getServiceResponseText(HttpResponseKnob responseKnob,
                                                  String id,
                                                  boolean advance)
            throws IOException, FindException
    {
        final int maxField = 17;
        Goid service = null;
        int field = -1;

        ServiceUsage[] table = getCurrentServiceUsage();
        table = sortByServiceOid(table);

        Matcher subMatchWithGoid = SUB_URL_PATTERN_WITH_GOID.matcher(id);
        Matcher subMatchWithoutGoid = SUB_URL_PATTERN_WITHOUT_GOID.matcher(id);
        if (id == null || id.length() < 1) {
            if (advance) {
                //set service to first service.
                if(table!=null&&table.length>0)
                    service = table[0].getServiceid();
                else
                    service = null;
                field = 1;
                advance = false;
            } else {
                return getSendText(responseKnob, BASE_PUBLISHED_SERVICES, "string", "Service Usage");
            }
        } else if (subMatchWithGoid.matches()) {
            field = Integer.parseInt(subMatchWithGoid.group(1));
            service = convertIntsToGoid(subMatchWithGoid.group(2));
        } else if (subMatchWithoutGoid.matches()) {
            field = Integer.parseInt(subMatchWithoutGoid.group(1));
            if(table!=null&&table.length>0){
                    service = table[0].getServiceid();
                    advance = false;
            }
                else
                    service = null;
        }


        if (service == null || field < 1 || field > maxField) {
            return null;
        }

        if (advance) {
            //get the next service id here.
            service = getNextServiceID(service, table);
            if (service == null) {
                //set the service id back to the first one here
                service = table[0].getServiceid();
                field++;
            }
        }

        if (field > maxField) {
            //return null here if you are past the last service with no advance
            return null;
        }

        //get the su object for the service
        ServiceUsage su = getServiceUsage(service, table);//table[service - 1]; // (column is still 1-based, not 0-based)
        if(su == null){
            logger.log(Level.WARNING, "Requested SNMP statistic not available: The requested service "+service+" does not exist on the Layer 7 Gateway");
            return null;
        }

        //get bin data
        MetricsSummaryBin latestDailyBin = getBinData(service, MetricsBin.RES_DAILY);
        MetricsSummaryBin latestHourlyBin = getBinData(service, MetricsBin.RES_HOURLY);
        MetricsSummaryBin latestFineBin = getBinData(service, MetricsBin.RES_FINE);

        double averageBackEndResponseTime24hours = getBackEndResponseTime(latestDailyBin);
        double averageBackEndResponseTimeHour = getBackEndResponseTime(latestHourlyBin);
        double averageBackEndResponseTimeFine = getBackEndResponseTime(latestFineBin);

        double averageFrontEndResponseTime24hours = getFrontEndResponseTime(latestDailyBin);
        double averageFrontEndResponseTimeHour = getFrontEndResponseTime(latestHourlyBin);
        double averageFrontEndResponseTimeFine = getFrontEndResponseTime(latestFineBin);

        int policyViolations24Hours = getPolicyViloations(latestDailyBin);
        int policyViolationsHour = getPolicyViloations(latestHourlyBin);
        int policyViolationsFine = getPolicyViloations(latestFineBin);

        int dayFails = getFailedRequests(latestDailyBin);
        int hourFails = getFailedRequests(latestHourlyBin);
        int fineFails = getFailedRequests(latestFineBin);

        String addr = BASE_PUBLISHED_SERVICES + "." + field + "." + convertGoidToUints(su.getServiceid());
        String responseText;
        switch (field) {
            case 1:
                responseText = getSendText(responseKnob, addr, "string", su.getServiceid().toHexString());
                break;
            case 2:
                responseText = getSendText(responseKnob, addr, "string", su.getName());
                break;
            case 3:
                responseText = getSendText(responseKnob, addr, "counter", String.valueOf(su.getRequests()));
                break;
            case 4:
                responseText = getSendText(responseKnob, addr, "counter", String.valueOf(su.getAuthorized()));
                break;
            case 5:
                responseText = getSendText(responseKnob, addr, "counter", String.valueOf(su.getCompleted()));
                break;
            case 6:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf(dayFails));
                break;
            case 7:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf(hourFails));
                break;
            case 8:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf(fineFails));
                break;
            case 9:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf((int)averageBackEndResponseTime24hours));
                break;
            case 10:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf((int)averageBackEndResponseTimeHour));
                break;
            case 11:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf((int)averageBackEndResponseTimeFine));
                break;
            case 12:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf((int)averageFrontEndResponseTime24hours));
                break;
            case 13:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf((int)averageFrontEndResponseTimeHour));
                break;
            case 14:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf((int)averageFrontEndResponseTimeFine));
                break;
            case 15:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf(policyViolations24Hours));
                break;
            case 16:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf(policyViolationsHour));
                break;
            case 17:
                responseText = getSendText(responseKnob, addr, "integer", String.valueOf(policyViolationsFine));
                break;
            default:
                responseText = null;
                break;
        }

        return responseText;
    }

    private int getFailedRequests(MetricsSummaryBin bin){
        if(bin==null)
            return 0;
        int fails = bin.getNumRoutingFailure();
        if(fails<0)
            fails = 0;
        return fails;
    }

    private double getBackEndResponseTime(MetricsSummaryBin bin){
        if(bin==null)
            return 0.0d;
        double time = bin.getAverageBackendResponseTime();
        if(time<0)
               time = 0.0d;
        return time;
    }

    private double getFrontEndResponseTime(MetricsSummaryBin bin){
        if(bin==null)
            return 0.0d;
        double time = bin.getAverageFrontendResponseTime();
        if(time<0)
               time = 0.0d;
        return time;
    }

    private int getPolicyViloations(MetricsSummaryBin bin){
        if(bin==null)
            return 0;
        int violations = 0;
        violations = bin.getNumPolicyViolation();
        if(violations<0)
            violations = 0;
        return violations;
    }


    public Goid getNextServiceID(Goid service, ServiceUsage[] serviceUsages){
        Goid returnInt = null;
        for(int i=0; i<serviceUsages.length; i++){
            ServiceUsage s = serviceUsages[i];
            if(s.getServiceid().equals(service)){
                //check if this is the last one or not.
                //if it is the last one, return -1;
                //if it is not the last one, return the next service id.
                if(i == serviceUsages.length-1){
                    //is the last service
                    return null;
                }
                else{
                    return serviceUsages[i+1].getServiceid();
                }
            }
        }
        return returnInt;
    }

    public ServiceUsage getServiceUsage(Goid serviceId, ServiceUsage[] usage){
         for(int i=0; i<usage.length; i++){
              if(usage[i].getServiceid().equals(serviceId))
                  return usage[i];
         }
        return null;
    }


    public ServiceUsage[] sortByServiceOid(ServiceUsage[] table){
        List myList = Arrays.asList(table);
        Collections.sort(myList, new Comparator(){
            public int compare(Object o1, Object o2) {
                ServiceUsage p1 = (ServiceUsage) o1;
                ServiceUsage p2 = (ServiceUsage) o2;

                // SNMP getnext requires that we return in lexiographical order
                // since we convert the GOID to int32 unsigned value, it is easier to utilize the String comparison
                // on the Goid.toHexString() as the hex string length is consistent
                return p1.getServiceid().toHexString().compareTo(p2.getServiceid().toHexString());
            }
        });

        ServiceUsage[] sortedUsage = new ServiceUsage[myList.size()];
        myList.toArray(sortedUsage);
        return sortedUsage;
    }


    private synchronized ServiceUsage[] getCurrentServiceUsage() throws FindException {
        long now = System.currentTimeMillis();
        if (serviceTable != null && now - serviceTableTime > 3500)
            serviceTable = null;

        if (serviceTable != null) {
            serviceTableTime = now;
            return serviceTable;
        }

        ServiceUsageManager statsManager = (ServiceUsageManager)springContext.getBean("serviceUsageManager");
        Map<Pair<Goid, String>, ServiceUsage> statsByOid = new HashMap<Pair<Goid, String>, ServiceUsage>();
        Collection collection = statsManager.getAll();
        for (Iterator i = collection.iterator(); i.hasNext();) {
            ServiceUsage serviceUsage = (ServiceUsage)i.next();
            statsByOid.put(new Pair<>(serviceUsage.getServiceid(), serviceUsage.getNodeid()), serviceUsage);
        }

        ServiceManager serviceManager = (ServiceManager) springContext.getBean("serviceManager");
        EntityHeader[] headers = serviceManager.findAllHeaders().toArray(new EntityHeader[0]);
        ServiceUsage[] fullTable = new ServiceUsage[headers.length];
        for (int i = 0; i < headers.length; i++) {
            EntityHeader header = headers[i];
            ServiceUsage su = statsByOid.get(new Pair<>(header.getGoid(), clusterInfoManager.getSelfNodeInf().getId()));
            if (su != null) {
                fullTable[i] = su;
            } else {
                fullTable[i] = new ServiceUsage();
                fullTable[i].setServiceid(header.getGoid());
            }
            fullTable[i].setName(header.getName());
        }

        serviceTable = fullTable;
        serviceTableTime = System.currentTimeMillis();

        return serviceTable;
    }

    private MetricsSummaryBin getBinData(Goid serviceId, int bin){
        try{
            switch (bin){
                case MetricsBin.RES_FINE :
                    return metricsManager.summarizeLatest(null, new Goid[] {serviceId}, MetricsBin.RES_FINE, metricsServices.getFineInterval(), false);
                case MetricsBin.RES_HOURLY :
                    return metricsManager.summarizeLatest(null, new Goid[] {serviceId}, MetricsBin.RES_FINE, (60 * 60 * 1000), false);
                case MetricsBin.RES_DAILY :
                    return metricsManager.summarizeLatest(null, new Goid[] {serviceId}, MetricsBin.RES_HOURLY, (24 * 60 * 60 * 1000), false);
                default :
                    return null;
            }
        }catch(FindException fe){
            logger.log(Level.WARNING, "Find exception caught: ServerSnmpAgentAssertion could not find metrics for service "+serviceId);
            return null;
        }
    }


    private String getSendText(HttpResponseKnob responseKnob, String next, String type, String value) throws IOException {
        responseKnob.setStatus(HttpServletResponse.SC_OK);
        StringBuilder responseText = new StringBuilder();
        responseText.append(BASE);
        responseText.append(next != null ? next : "");
        responseText.append('\n');
        responseText.append(type != null && type.length() > 0 ? type : "string");
        responseText.append('\n');
        responseText.append(value);
        responseText.append('\n');
        return responseText.toString();
    }


    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerSnmpAgentAssertion is preparing itself to be unloaded");
    }

    /**
     * Converts from GOID (128-bit) to "uint32.uint32.uint32.uint32"
     *
     * This is done due to the data type limitations of OIDs in the SNMP protocols.
     */
    private static String convertGoidToUints(Goid goid) {

        byte[] bytes = goid.getBytes();

        // Splitting up into 4x32-bit ints and then converting to be unsigned ints (long)
        long long1 = 0x00000000ffffffffL & (long) ByteBuffer.wrap(Arrays.copyOfRange(bytes, 0, 4)).getInt();
        long long2 = 0x00000000ffffffffL & (long) ByteBuffer.wrap(Arrays.copyOfRange(bytes, 4, 8)).getInt();
        long long3 = 0x00000000ffffffffL & (long) ByteBuffer.wrap(Arrays.copyOfRange(bytes, 8, 12)).getInt();
        long long4 = 0x00000000ffffffffL & (long) ByteBuffer.wrap(Arrays.copyOfRange(bytes, 12, 16)).getInt();

        return String.valueOf(long1) + "." + String.valueOf(long2) + "." + String.valueOf(long3) + "." + String.valueOf(long4);
    }

    /**
     * Converts from "int32.int32.int32.int32" to GOID (128-bit)
     *
     * Note that even though the assertion returns unsigned longs for ints, SNMP clients
     * request objects using signed ints.
     */
    private static Goid convertIntsToGoid(String ints) {
        String[] goidAsInts = ints.split("\\.");

        ByteBuffer buf1 = ByteBuffer.allocate(4).putInt(Integer.valueOf(goidAsInts[0]));
        ByteBuffer buf2 = ByteBuffer.allocate(4).putInt(Integer.valueOf(goidAsInts[1]));
        ByteBuffer buf3 = ByteBuffer.allocate(4).putInt(Integer.valueOf(goidAsInts[2]));
        ByteBuffer buf4 = ByteBuffer.allocate(4).putInt(Integer.valueOf(goidAsInts[3]));

        byte[] bytes = ArrayUtils.concat(ArrayUtils.concat(buf1.array(), buf2.array()), ArrayUtils.concat(buf3.array(), buf4.array()));
        return new Goid(bytes);
    }
}

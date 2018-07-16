package com.l7tech.external.assertions.gatewaymetrics;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 3/18/13
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class GatewayMetricsExternalReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(GatewayMetricsExternalReference.class.getName());

    private static final String ELMT_NAME_REF = "GatewayMetricsReference";
    private static final String ELMT_TYPE = "Type";
    private static final String ELMT_ID = "ID"; // Cluster node id
    private static final String ELMT_OID = "OID"; // Published service oid
    private static final String ELMT_GOID = "GOID";
    private static final String ELMT_NAME = "Name";

    private String id; // Cluster node id
    private Goid goid;
    private String clusterNodeName;
    private String publishedServiceName;
    private ExternalReferenceType type;

    private String localId; // Cluster node id
    private Goid localGoid; // Published service oid
    private LocalizeAction localizeType;

    public enum ExternalReferenceType {
        CLUSTER_NODE {
            @Override
            public String toString() {
                return "ClusterNode";
            }
        },

        PUBLISHED_SERVICE {
            @Override
            public String toString() {
                return "PublishedService";
            }
        };

        public static ExternalReferenceType getType(String type) {
            if (type != null) {
                if (type.equals("ClusterNode")) {
                    return CLUSTER_NODE;
                } else if (type.equals("PublishedService")) {
                    return PUBLISHED_SERVICE;
                } else {
                    return null;
                }
            }
            return null;
        }
    }

    public static GatewayMetricsExternalReference parseFromElement(Object context, Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + ELMT_NAME_REF);
        }

        GatewayMetricsExternalReference output = new GatewayMetricsExternalReference((ExternalReferenceFinder) context);

        String value = getParamFromEl(el, ELMT_TYPE);
        ExternalReferenceType type = ExternalReferenceType.getType(value);
        output.type = type;
        switch (type) {
            case CLUSTER_NODE:
                output.id = getParamFromEl(el, ELMT_ID);
                output.clusterNodeName = getParamFromEl(el, ELMT_NAME);
                break;

            case PUBLISHED_SERVICE:
                String goid = getParamFromEl(el, ELMT_GOID);
                if (goid != null) {
                    output.goid = Goid.parseGoid(goid);
                } else {
                    value = getParamFromEl(el, ELMT_OID);
                    if (value != null) {
                        output.goid = GoidUpgradeMapper.mapOid(EntityType.SERVICE, Long.parseLong(value));
                    }
                }

                output.publishedServiceName = getParamFromEl(el, ELMT_NAME);
                break;

            default:
                logger.log(Level.WARNING, "Invalid gateway metrics external reference type. Must be either ClusterNode or PublishedService.");
        }

        return output;
    }

    public GatewayMetricsExternalReference(ExternalReferenceFinder finder) {
        super(finder);
    }

    public GatewayMetricsExternalReference(ExternalReferenceFinder finder, GatewayMetricsAssertion assertion) {
        super(finder);

        // Cluster node
        //
        id = assertion.getClusterNodeId();
        if (id == null) {
            // All cluster nodes. There is no external reference.
            //
        } else {
            try {
                ClusterNodeInfo[] clusterNodes = getClusterStatusAdmin().getClusterStatus();
                for (ClusterNodeInfo currentClusterNode : clusterNodes) {
                    if (currentClusterNode.getId().equals(id)) {
                        clusterNodeName = currentClusterNode.getName();
                        break;
                    }
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to retrieve cluster nodes.");
            }
        }

        // Published service
        //
        goid = assertion.getPublishedServiceGoid();
        if (goid.equals(PublishedService.DEFAULT_GOID)) {
            // All services. There is no external reference.
        } else {
            try {
                PublishedService service = getServiceAdmin().findServiceByID(String.valueOf(goid));

                if (service != null) {
                    publishedServiceName = service.getName();
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, "Unable to retrieve published policy.");
            }
        }
    }

    public String getClusterNodeId() {
        return id;
    }

    public String getClusterNodeName() {
        return clusterNodeName;
    }

    public Goid getPublishedServiceGoid() {
        return goid;
    }

    public String getPublishedServiceName() {
        return publishedServiceName;
    }

    public String getName() {
        switch (type) {
            case CLUSTER_NODE:
                return clusterNodeName;

            case PUBLISHED_SERVICE:
                return publishedServiceName;
            default:
                logger.log(Level.WARNING, "Invalid gateway metrics external reference type. Must be either ClusterNode or PublishedService.");
                return null;
        }
    }

    public ExternalReferenceType getType() {
        return type;
    }

    /**
     * Used in unit testing to easily set the external reference type
     * @param type
     */
    protected void setType(ExternalReferenceType type) {
        this.type = type;
    }

    @Override
    public boolean setLocalizeReplace(final String nodeId) {
        localizeType = LocalizeAction.REPLACE;
        localId = nodeId;
        return true;
    }

    @Override
    public boolean setLocalizeReplace(final Goid publishedServiceGoid) {
        localizeType = LocalizeAction.REPLACE;
        localGoid = publishedServiceGoid;
        return true;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        if (id == null) {
            // All cluster nodes. There is no external reference.
            //
        } else {
            Element referenceElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_REF);
            setTypeAttribute(referenceElement);
            referencesParentElement.appendChild(referenceElement);

            addParameterElement(ELMT_ID, id, referenceElement);
            addParameterElement(ELMT_NAME, clusterNodeName, referenceElement);
            addParameterElement(ELMT_TYPE, ExternalReferenceType.CLUSTER_NODE.toString(), referenceElement);
        }

        if (goid.equals(PublishedService.DEFAULT_GOID)) {
            // All services. There is no external reference.
            //
        } else {
            Element referenceElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_REF);
            setTypeAttribute(referenceElement);
            referencesParentElement.appendChild(referenceElement);

            addParameterElement(ELMT_GOID, goid.toHexString(), referenceElement);
            addParameterElement(ELMT_NAME, publishedServiceName, referenceElement);
            addParameterElement(ELMT_TYPE, ExternalReferenceType.PUBLISHED_SERVICE.toString(), referenceElement);
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        switch (type) {
            case CLUSTER_NODE:
                if (id == null) {
                    // All cluster nodes. There is no external reference.
                    //
                    return true;
                } else {
                    try {
                        ClusterStatusAdmin admin = getClusterStatusAdmin();
                        ClusterNodeInfo[] clusterNodes = getClusterStatusAdmin().getClusterStatus();
                        for (ClusterNodeInfo currentClusterNode : clusterNodes) {
                            if (isMatch(currentClusterNode.getId(),id) &&
                                isMatch(currentClusterNode.getName(), clusterNodeName) &&
                                permitMapping(id, currentClusterNode.getId())) {
                                // Perfect Match (ID and name are matched.)
                                logger.fine("The cluster node was resolved by id '" + id + "' and name '" + clusterNodeName + "'");
                                return true;
                            }
                        }

                        for (ClusterNodeInfo currentClusterNode : clusterNodes) {
                            if (isMatch(currentClusterNode.getName(), clusterNodeName) &&
                                permitMapping(id, currentClusterNode.getId())) {
                                // Cluster node name matched
                                logger.fine("The cluster node resolved from id '" + id + "' to '" + currentClusterNode.getId() + "'");
                                localId = currentClusterNode.getId();
                                localizeType = LocalizeAction.REPLACE;
                                return true;
                            }
                        }

                    } catch (FindException e) {
                        logger.warning("Cannot load cluster node from id, " + id);
                    }

                    return false;
                }

            case PUBLISHED_SERVICE:
                if (goid.equals(Goid.DEFAULT_GOID)) {
                    // All services. There is no external reference.
                    //
                    return true;
                } else {
                    try {
                        ServiceAdmin admin = getServiceAdmin();
                        PublishedService service = admin.findServiceByID(String.valueOf(goid));

                        if (service != null) {
                            if (isMatch(service.getName(), publishedServiceName) && permitMapping(goid, service.getGoid())) {
                                // Perfect Match (GOID and name are matched.)
                                logger.fine("The published service was resolved by goid '" + goid + "' and name '" + publishedServiceName + "'");
                                return true;
                            }
                        } else {
                            ServiceHeader[] services = admin.findAllPublishedServices();
                            for (ServiceHeader currentService : services) {
                                if (isMatch(currentService.getName(), publishedServiceName) && permitMapping(goid, currentService.getGoid())) {
                                    // Service name matched
                                    logger.fine("The published service resolved from goid '" + goid + "' to '" + currentService.getGoid() + "'");
                                    localGoid = currentService.getGoid();
                                    localizeType = LocalizeAction.REPLACE;
                                    return true;
                                }
                            }
                        }
                    } catch (FindException e) {
                        logger.warning("Cannot load published service from goid, " + goid);
                    }

                    return false;
                }

            default:
                logger.log(Level.WARNING, "Invalid gateway metrics external reference type. Must be either ClusterNode or PublishedService.");
                return false;
        }
    }

    @Override
    protected boolean localizeAssertion(@Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof GatewayMetricsAssertion) {
                final GatewayMetricsAssertion gatewayMetricsAssertion = (GatewayMetricsAssertion) assertionToLocalize;

                switch (type) {
                    case CLUSTER_NODE:
                        final String clusterNodeId = gatewayMetricsAssertion.getClusterNodeId();
                        if (clusterNodeId.equals(id)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                            if (localizeType == LocalizeAction.REPLACE) {
                                gatewayMetricsAssertion.setClusterNodeId(localId);
                            }  else if (localizeType == LocalizeAction.DELETE) {
                                logger.info("Deleted this assertion from the tree.");
                                return false;
                            }
                        }
                        break;

                    case PUBLISHED_SERVICE:
                        final Goid publishedServiceGoid = gatewayMetricsAssertion.getPublishedServiceGoid();
                        if (publishedServiceGoid.equals(goid)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                            if (localizeType == LocalizeAction.REPLACE) {
                                gatewayMetricsAssertion.setPublishedServiceGoid(localGoid);
                            }  else if (localizeType == LocalizeAction.DELETE) {
                                logger.info("Deleted this assertion from the tree.");
                                return false;
                            }
                        }
                        break;

                    default:
                        logger.log(Level.WARNING, "Invalid gateway metrics external reference type. Must be either ClusterNode or PublishedService.");
                }
            }
        }

        return true;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GatewayMetricsExternalReference that = (GatewayMetricsExternalReference) o;
        boolean result = true;
        if (type != null ? !type.equals(that.type) : that.type != null)
            result = false;
        if (clusterNodeName != null ? !clusterNodeName.equals(that.clusterNodeName) : that.clusterNodeName != null)
            result = false;
        if (publishedServiceName != null ? !publishedServiceName.equals(that.publishedServiceName) : that.publishedServiceName != null)
            result = false;

        return result;
    }

    @Override
    public int hashCode() {
        switch (type) {
            case CLUSTER_NODE:
                return clusterNodeName != null ? clusterNodeName.hashCode() : 0;

            case PUBLISHED_SERVICE:
                return publishedServiceName != null ? publishedServiceName.hashCode() : 0;

            default:
                return 0;
        }
    }

    private ClusterStatusAdmin getClusterStatusAdmin() {
        return Registry.getDefault().getClusterStatusAdmin();
    }

    private ServiceAdmin getServiceAdmin() {
        return Registry.getDefault().getServiceManager();
    }

    private void addParameterElement(final String name, final String value, final Element parent) {
        if (value != null) {
            final Element parameterElement = parent.getOwnerDocument().createElement(name);
            final Text txt = DomUtils.createTextNode(parent, value);
            parameterElement.appendChild(txt);
            parent.appendChild(parameterElement);
        }
    }

    private boolean isMissing( final String value ) {
        return value == null || value.isEmpty();
    }

    private boolean isMatch( final String leftValue,
                             final String rightValue) {
        return isMissing(leftValue) ? isMissing(rightValue) : leftValue.equals(rightValue);
    }
}
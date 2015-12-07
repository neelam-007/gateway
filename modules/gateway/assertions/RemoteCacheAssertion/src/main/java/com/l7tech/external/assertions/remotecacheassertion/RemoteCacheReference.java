package com.l7tech.external.assertions.remotecacheassertion;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.remotecacheassertion.server.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.util.InvalidDocumentFormatException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 04/05/12
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(RemoteCacheReference.class.getName());

    private static final String ELMT_NAME_REF = "RemoteCacheReference";
    private static final String ELMT_OID = "OID";
    private static final String ELMT_GOID = "GOID";
    private static final String ELMT_NAME = "Name";
    private static final String ELMT_NAME_CLASSNAME = "Classname";
    private static final String ELMT_TYPE = "Type";
    private static final String ELMT_ENABLED = "Enabled";
    private static final String ELMT_TIMEOUT = "Timeout";
    private static final String ELMT_PROPERTIES = "Properties";
    private static final String ELMT_PROPERTY = "Property";
    //There was a spelling mistake in the properties name element. Instead of name, we had mame. We need this for importing old policies.
    private static final String ELMT_PROPERTY_ATTR_NAME_OLD = "mame";
    private static final String ELMT_PROPERTY_ATTR_NAME = "name";
    private Goid goid;
    private String name;
    private String classname;
    private String type;
    private boolean enabled;
    private int timeout;
    private HashMap<String, String> properties = new HashMap<>();

    private Goid localGoid;
    private LocalizeAction localizeType;

    public RemoteCacheReference(ExternalReferenceFinder finder) {
        super(finder);
    }

    private static RemoteCacheEntityAdmin getEntityManager(ExternalReferenceFinder finder) throws FindException {
        if (finder.getClass().getName().contains("Console")) {
            return getEntityManager();
        } else {
            return new RemoteCacheEntityAdminImpl(finder.getGenericEntityManager(RemoteCacheEntity.class));
        }
    }

    private static RemoteCacheEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);
    }

    public RemoteCacheReference(ExternalReferenceFinder finder, RemoteCacheAssertion assertion) {
        super(finder);
        try {
            RemoteCacheEntity entity = getEntityManager(finder).find(assertion.getRemoteCacheGoid());

            if (entity != null) {
                goid = entity.getGoid();
                name = entity.getName();
                classname = entity.getEntityClassName();
                type = entity.getType();
                enabled = entity.isEnabled();
                timeout = entity.getTimeout();
                properties = entity.getProperties();
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to retrieve Entity from EntityManager");
        }
    }

    @Override
    public String getRefId() {
        String id = null;

        if (!goid.equals(GenericEntity.DEFAULT_GOID)) {
            id = goid.toString();
        }

        return id;
    }

    public Goid getOid() {
        return goid;
    }

    public String getName() {
        return name;
    }

    public String getClassname() {
        return classname;
    }

    public String getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTimeout() {
        return timeout;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    @Override
    public boolean setLocalizeReplace(final Goid connectionGoid) {
        localizeType = LocalizeAction.REPLACE;
        localGoid = connectionGoid;
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

    /**
     * Adds a child element to the passed references element that contains the xml
     * form of this reference object. Used by the policy exporter when serializing
     * references to xml format.
     *
     * @param referencesParentElement Element containing assertion
     */
    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element referenceElement = referencesParentElement.getOwnerDocument().createElement(ELMT_NAME_REF);
        setTypeAttribute(referenceElement);
        referencesParentElement.appendChild(referenceElement);

        addParameterElement(ELMT_GOID, goid.toString(), referenceElement);
        addParameterElement(ELMT_NAME, name, referenceElement);
        addParameterElement(ELMT_NAME_CLASSNAME, classname, referenceElement);
        addParameterElement(ELMT_TYPE, type, referenceElement);
        addParameterElement(ELMT_ENABLED, Boolean.toString(enabled), referenceElement);
        addParameterElement(ELMT_TIMEOUT, Integer.toString(timeout), referenceElement);

        Element propertiesElement = referencesParentElement.getOwnerDocument().createElement(ELMT_PROPERTIES);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Element propertyElement = referencesParentElement.getOwnerDocument().createElement(ELMT_PROPERTY);
            propertyElement.setAttribute(ELMT_PROPERTY_ATTR_NAME, entry.getKey());
            propertyElement.appendChild(referencesParentElement.getOwnerDocument().createTextNode(entry.getValue()));
            propertiesElement.appendChild(propertyElement);
        }

        referenceElement.appendChild(propertiesElement);
    }

    private void addParameterElement(final String name, final String value, final Element parent) {
        if (value != null) {
            final Element parameterElement = parent.getOwnerDocument().createElement(name);
            final Text txt = DomUtils.createTextNode(parent, value);
            parameterElement.appendChild(txt);
            parent.appendChild(parameterElement);
        }
    }

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        try {
            final RemoteCacheEntity activeRemoteCache = getEntityManager(getFinder()).find(goid);
            if (activeRemoteCache != null) {
                if (isMatch(activeRemoteCache.getName(), name) && permitMapping(goid, activeRemoteCache.getGoid())) {
                    // Perfect Match (OID and name are matched.)
                    logger.fine("The remote cache was resolved by goid '" + goid.toString() + "' and name '" + activeRemoteCache.getName() + "'");
                    return true;
                }
            } else {
                final Collection<RemoteCacheEntity> activeRemoteCaches = getEntityManager(getFinder()).findAll();
                for (RemoteCacheEntity remoteCache : activeRemoteCaches) {
                    if (isMatch(remoteCache.getName(), name) && permitMapping(goid, remoteCache.getGoid())) {
                        // Connector Name matched
                        logger.fine("The remote cache was resolved from goid '" + goid.toString() + "' to '" + remoteCache.getGoid() + "'");
                        localGoid = remoteCache.getGoid();
                        localizeType = LocalizeAction.REPLACE;
                        return true;
                    }
                }

                // Check if partial matched
                for (RemoteCacheEntity remoteCache : activeRemoteCaches) {
                    RemoteCacheTypes cacheType = RemoteCacheTypes.getEntityEnumType(remoteCache.getType());
                    //only continue with partial match if they are the same type.
                    if (!cacheType.equals(RemoteCacheTypes.getEntityEnumType(type))) {
                        break;
                    }
                    switch (cacheType) {
                        case Memcached:
                            if (Boolean.parseBoolean(remoteCache.getProperties().get(MemcachedRemoteCache.PROP_BUCKET_SPECIFIED))) {
                                if (StringUtils.defaultString(properties.get(MemcachedRemoteCache.PROP_BUCKETNAME)).equals(remoteCache.getProperties().get(MemcachedRemoteCache.PROP_BUCKETNAME))) {
                                    String[] serverList1 = StringUtils.defaultString(remoteCache.getProperties().get(MemcachedRemoteCache.PROP_SERVERPORTS)).split(",");
                                    String[] serverList2 = StringUtils.defaultString(properties.get(MemcachedRemoteCache.PROP_SERVERPORTS)).split(",");

                                    if (serverList1.length == serverList2.length) {
                                        Arrays.sort(serverList1);
                                        Arrays.sort(serverList2);

                                        if (Arrays.equals(serverList1, serverList2)) {
                                            // Partial matched
                                            logger.fine("The remote cache was resolved from goid '" + goid.toString() + "' to '" + remoteCache.getGoid() + "'");
                                            localGoid = remoteCache.getGoid();
                                            localizeType = LocalizeAction.REPLACE;
                                            return true;
                                        }
                                    }
                                }
                            } else {
                                String[] serverList1 = StringUtils.defaultString(remoteCache.getProperties().get(MemcachedRemoteCache.PROP_SERVERPORTS)).split(",");
                                String[] serverList2 = StringUtils.defaultString(properties.get(MemcachedRemoteCache.PROP_SERVERPORTS)).split(",");

                                if (serverList1.length == serverList2.length) {
                                    Arrays.sort(serverList1);
                                    Arrays.sort(serverList2);

                                    if (Arrays.equals(serverList1, serverList2)) {
                                        // Partial matched
                                        logger.fine("The remote cache was resolved from goid '" + goid.toString() + "' to '" + remoteCache.getGoid() + "'");
                                        localGoid = remoteCache.getGoid();
                                        localizeType = LocalizeAction.REPLACE;
                                        return true;
                                    }
                                }
                            }
                            break;
                        case Terracotta:
                            if (StringUtils.defaultString(properties.get(TerracottaRemoteCache.PROPERTY_CACHE_NAME)).equals(remoteCache.getProperties().get(TerracottaRemoteCache.PROPERTY_CACHE_NAME))) {
                                String[] serverList1 = StringUtils.defaultString(remoteCache.getProperties().get(TerracottaRemoteCache.PROPERTY_URLS)).split(",");
                                String[] serverList2 = StringUtils.defaultString(properties.get(TerracottaRemoteCache.PROPERTY_URLS)).split(",");

                                if (serverList1.length == serverList2.length) {
                                    Arrays.sort(serverList1);
                                    Arrays.sort(serverList2);

                                    if (Arrays.equals(serverList1, serverList2)) {
                                        // Partial matched
                                        logger.fine("The remote cache was resolved from goid '" + goid.toString() + "' to '" + remoteCache.getGoid() + "'");
                                        localGoid = remoteCache.getGoid();
                                        localizeType = LocalizeAction.REPLACE;
                                        return true;
                                    }
                                }
                            }
                            break;
                        case Coherence:
                            String[] serverList1 = StringUtils.defaultString(remoteCache.getProperties().get(CoherenceRemoteCache.PROP_SERVERS)).split(",");
                            String[] serverList2 = StringUtils.defaultString(properties.get(CoherenceRemoteCache.PROP_SERVERS)).split(",");

                            if (serverList1.length == serverList2.length) {
                                Arrays.sort(serverList1);
                                Arrays.sort(serverList2);

                                if (Arrays.equals(serverList1, serverList2)) {
                                    // Partial matched
                                    logger.fine("The remote cache was resolved from goid '" + goid.toString() + "' to '" + remoteCache.getGoid() + "'");
                                    localGoid = remoteCache.getGoid();
                                    localizeType = LocalizeAction.REPLACE;
                                    return true;
                                }
                            }
                            break;
                        case GemFire:
                            serverList1 = StringUtils.defaultString(remoteCache.getProperties().get(GemfireRemoteCache.PROPERTY_SERVERS)).split(",");
                            serverList2 = StringUtils.defaultString(properties.get(GemfireRemoteCache.PROPERTY_SERVERS)).split(",");

                            if (serverList1.length == serverList2.length) {
                                Arrays.sort(serverList1);
                                Arrays.sort(serverList2);

                                if (Arrays.equals(serverList1, serverList2)) {
                                    // Partial matched
                                    logger.fine("The remote cache was resolved from goid '" + goid.toString() + "' to '" + remoteCache.getGoid() + "'");
                                    localGoid = remoteCache.getGoid();
                                    localizeType = LocalizeAction.REPLACE;
                                    return true;
                                }
                            }
                            break;
                        case Redis:
                            String[] redisServerList = StringUtils.defaultString(remoteCache.getProperties().get(RedisRemoteCache.PROPERTY_SERVERS)).split(",");
                            String[] redisPropsServerList = StringUtils.defaultString(properties.get(RedisRemoteCache.PROPERTY_SERVERS)).split(",");

                            if (redisServerList.length == redisPropsServerList.length) {
                                Arrays.sort(redisServerList);
                                Arrays.sort(redisPropsServerList);

                                if (Arrays.equals(redisServerList, redisPropsServerList)) {
                                    // Partial matched
                                    logger.fine("The remote cache was resolved from goid '" + goid.toString() + "' to '" + remoteCache.getGoid() + "'");
                                    localGoid = remoteCache.getGoid();
                                    localizeType = LocalizeAction.REPLACE;
                                    return true;
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (FindException e) {
            logger.warning("Cannot load Active Connector from goid, " + goid.toString());
        }

        return false;
    }

    /**
     * Once an exported policy is loaded with it's references and the references are
     * verified, this method will apply the necessary changes to the assertion. If
     * the assertion type passed does not relate to the reference, it will be left
     * untouched.
     * Returns false if the assertion should be deleted from the tree.
     *
     * @param assertionToLocalize will be fixed once this method returns.
     */
    @Override
    protected boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof RemoteCacheLookupAssertion) {
                final RemoteCacheLookupAssertion remoteCacheLookupAssertion = (RemoteCacheLookupAssertion) assertionToLocalize;
                final Goid remoteCacheGoid = remoteCacheLookupAssertion.getRemoteCacheGoid();
                if (remoteCacheGoid.equals(goid)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        remoteCacheLookupAssertion.setRemoteCacheGoid(localGoid);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            } else if (assertionToLocalize instanceof RemoteCacheStoreAssertion) {
                final RemoteCacheStoreAssertion remoteCacheStoreAssertion = (RemoteCacheStoreAssertion) assertionToLocalize;
                final Goid remoteCacheGoid = remoteCacheStoreAssertion.getRemoteCacheGoid();
                if (remoteCacheGoid.equals(goid)) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        remoteCacheStoreAssertion.setRemoteCacheGoid(localGoid);
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RemoteCacheReference that = (RemoteCacheReference) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public static Object parseFromElement(Object context, Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of connectorName " + ELMT_NAME_REF);
        }

        RemoteCacheReference output = new RemoteCacheReference((ExternalReferenceFinder) context);

        String value = getParamFromEl(el, ELMT_GOID);
        if (value != null) {
            output.goid = Goid.parseGoid(value);
        } else {
            value = getParamFromEl(el, ELMT_OID);
            output.goid = GoidUpgradeMapper.mapOid(EntityType.GENERIC, Long.parseLong(value));
        }

        output.name = getParamFromEl(el, ELMT_NAME);
        output.classname = getParamFromEl(el, ELMT_NAME_CLASSNAME);
        output.type = getParamFromEl(el, ELMT_TYPE);
        //If there is no enabled element, enable the connection by default. This will ensure that the connection is available when importing connection that was exported prior to this enhancement.
        output.enabled = getParamFromEl(el, ELMT_ENABLED) == null ? Boolean.TRUE : Boolean.parseBoolean(getParamFromEl(el, ELMT_ENABLED));

        value = getParamFromEl(el, ELMT_TIMEOUT);
        if (value != null) {
            output.timeout = Integer.parseInt(value);
        }

        output.properties.clear();

        NodeList propertiesEls = el.getElementsByTagName(ELMT_PROPERTIES);
        if (propertiesEls.getLength() > 0) {
            Element propertiesEl = (Element) propertiesEls.item(0);

            NodeList propertyEls = propertiesEl.getElementsByTagName(ELMT_PROPERTY);
            for (int i = 0; i < propertyEls.getLength(); i++) {
                Element propertyEl = (Element) propertyEls.item(i);
                //Get the value for the attribute name. If it's an old export, the attribute might be mis-spelled to mame
                String propertyValue = StringUtils.isBlank(propertyEl.getAttribute(ELMT_PROPERTY_ATTR_NAME))? propertyEl.getAttribute(ELMT_PROPERTY_ATTR_NAME_OLD): propertyEl.getAttribute(ELMT_PROPERTY_ATTR_NAME);
                output.properties.put(propertyValue, propertyEl.getTextContent());
            }
        }

        return output;
    }

    private boolean isMissing(final String value) {
        return value == null || value.isEmpty();
    }

    private boolean isMatch(final String leftValue,
                            final String rightValue) {
        return isMissing(leftValue) ? isMissing(rightValue) : leftValue.equals(rightValue);
    }
}

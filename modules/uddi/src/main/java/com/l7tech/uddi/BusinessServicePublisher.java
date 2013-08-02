package com.l7tech.uddi;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import com.l7tech.wsdl.Wsdl;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_BINDING;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE;

/**
 * Responsible for publishing a set of BusinessServices or Gateway endpoints extracted from a WSDL to a UDDI Registry.
 *
 * @author darmstrong
 */
public class BusinessServicePublisher implements Closeable {
    private static final Logger logger = Logger.getLogger(BusinessServicePublisher.class.getName());
    public static final String UDDI_SYSTINET_COM_MANAGEMENT_TYPE = "uddi:systinet.com:management:type";
    public static final String UDDI_SYSTINET_COM_MANAGEMENT_PROXY_REFERENCE = "uddi:systinet.com:management:proxy-reference";
    public static final String UDDI_SYSTINET_COM_MANAGEMENT_URL = "uddi:systinet.com:management:url";
    public static final String FUNCTIONAL_ENDPOINT = "functionalEndpoint";
    public static final String MANAGED_ENDPOINT = "managedEndpoint";
    public static final String UDDI_SYSTINET_COM_MANAGEMENT_SYSTEM = "uddi:systinet.com:management:system";
    public static final String UDDI_SYSTINET_COM_MANAGEMENT_STATE = "uddi:systinet.com:management:state";
    public static final String SYSTINET_STATE_MANAGED = "managed";

    private Wsdl wsdl;
    private final UDDIClient uddiClient;
    private final JaxWsUDDIClient jaxWsUDDIClient;
    private String serviceId;

    /**
     * Only off interest when an overwritten service is being updated. This is the set of BindingTemplates added
     * to the overwritten service. Used when deleting the Overwritten service
     * This property will be empty when an entire wsdl is published, only avaialble when either a bindingTemplate
     * is being published or a service is being overwritten. 
     */
    private Set<String> publishedBindingTemplates = new HashSet<String>();

    /**
     * Create a new BusinessServicePublisher.
     *
     * @param wsdl WSDL to publish to UDDI
     * @param serviceId services GOID, which originates from the WSDL
     * @param uddiCfg UDDIClientConfig for the UDDI Registry to publish to
     */
    public BusinessServicePublisher(final Wsdl wsdl,
                                    final String serviceId,
                                    final UDDIClientConfig uddiCfg) {

        this(buildUDDIClient(uddiCfg), wsdl, serviceId);
    }

    public BusinessServicePublisher(final Wsdl wsdl,
                                    final String serviceId,
                                    final UDDIClient uddiClient) {

        this(uddiClient, wsdl, serviceId);
    }

    /**
     * Create a new BusinessServicePublisher.
     *
     * @param serviceId services GOID, which originates from the WSDL
     * @param uddiCfg UDDIClientConfig for the UDDI Registry to publish to
     */
    public BusinessServicePublisher(final String serviceId,
                                    final UDDIClientConfig uddiCfg) {

        this(buildUDDIClient(uddiCfg), serviceId);
    }

    public BusinessServicePublisher(final String serviceId,
                                    final UDDIClient uddiClient) {

        this(uddiClient, serviceId);
    }

    /**
     * Publish a set of bindingTemplates to a BusinessService in UDDI. Each bindingTemplate published will represent
     * the same published service. There is more than one bindingTemplate published if the cluster defines a https
     * endpoint in addition to a http endpoint. Note: that as many endpoints will be created as supplied in allEndpointPairs
     * <p/>
     * Provides best effort commit / rollback. If any UDDIException occurs during the published an attempt to roll back
     * any previously published tModels and bindingTemplates will be made.
     * <p/>
     * When a previously published BindingTemplate is no longer applicable, e.g. a listener was removed, then it will be
     * deleted from UDDI. publishedBindingKeys is the set of bindingTemplate keys which were published the last time
     * the gateway checked the UDDI Registry. It's possible that based on gateway configuration changes, that some of
     * the previously published bindingTemplates are no longer required in UDDI.
     * <p/>
     * What ever keyedReferences are included in configKeyedReferences will be attached to the categoryBag of each
     * bindingTemplate published. Any references which exist in runtimeKeyedReferences but do not exist in
     * configKeyedReferences will be removed from UDDI.
     * <p/>
     * See UDDIUtilities.validateAllEndpointPairs(allEndpointPairs) for validation of allEndpointPairs
     *
     * @param serviceKey               String serviceKey of the owning BusinessService
     * @param wsdlPortName             String wsdl:port local name of the wsdl:port to publish as a bindingTemplate to UDDI
     * @param wsdlPortBinding          String wsdl:binding local name of the wsdl:binding to publish as a bindingTemplate to UDDI.
     *                                 if no wsdl:port by the name of wsdlPortName is found in the gateway's WSDL, then the first implementing wsdl:port
     *                                 of this binding local name, and namespace if defined, will be published to UDDI.
     * @param wsdlPortBindingNamespace String namespace of the above wsdl:binding. Can be null. Cannot be the empty string
     * @param previousEndpointPairs    Collection&lt;EndpointPair&gt; The collection of endpoint pairs which were used to
     *                                 previously publish the endpoint for the current service to an original service in UDDI. Can be null or empty. Only
     *                                 the endpoint URL is used here, the WSDL URL is not required. Used to find the applicable set of bindingTemplates
     *                                 when reusing bindingKeys. This is only required to support the upgrade from Pandora to Albacore.
     *                                 This is used to find existing bindingTemplates and to delete them when this publish is performing a republish.
     * @param allEndpointPairs         Collection&lt;EndpointPair&gt; Required. All endpoints to publish bindingTemplates for
     * @param publishedBindingKeys     Set &lt;String&gt; Can be null. The set of previously published bindingKeys for this service.
     *                                 This is used to know when a bindingTemplate needs to be deleted e.g. after a listener was removed
     * @param removeOthers             boolean, if true all other endpoints will be removed
     * @param configKeyedReferences    Set of UDDIKeyedReferences that are required to be published. Can be null when none need to be published.
     * @param runtimeKeyedReferences   Set of UDDIKeyedReferences that were last published. Can be null when no references have been published.
     * @return Set&lt;String&gt; The set of BindingTemplate bindingKeys which were published for the supplied endpoints
     * @throws UDDIException any problems searching / updating / deleting from / to UDDI
     */
    public Set<String> publishBindingTemplate(
            final String serviceKey,
            final String wsdlPortName,
            final String wsdlPortBinding,
            final String wsdlPortBindingNamespace,
            final Collection<EndpointPair> previousEndpointPairs,
            final Collection<EndpointPair> allEndpointPairs,
            final Set<String> publishedBindingKeys,
            final boolean removeOthers,
            final Set<UDDIKeyedReference> configKeyedReferences,
            final Set<UDDIKeyedReference> runtimeKeyedReferences) throws UDDIException {
        if (serviceKey == null || serviceKey.trim().isEmpty())
            throw new IllegalArgumentException("serviceKey cannot be null or empty");
        if (wsdlPortName == null || wsdlPortName.trim().isEmpty())
            throw new IllegalArgumentException("wsdlPortName cannot be null or empty");
        if (wsdlPortBinding == null || wsdlPortBinding.trim().isEmpty())
            throw new IllegalArgumentException("wsdlPortBinding cannot be null or empty");
        if (wsdlPortBindingNamespace == null || wsdlPortBindingNamespace.trim().isEmpty())
            throw new IllegalArgumentException("wsdlPortBindingNamespace cannot be null or empty");

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);

        final Pair<List<BindingTemplate>, List<TModel>> publishedTemplateAndModels = publishEndPointToExistingService(serviceKey,
                wsdlPortName,
                wsdlPortBinding,
                wsdlPortBindingNamespace,
                allEndpointPairs,
                previousEndpointPairs,
                publishedBindingKeys,
                removeOthers,
                configKeyedReferences,
                runtimeKeyedReferences);

        final List<BindingTemplate> bindingTemplates = publishedTemplateAndModels.left;
        final Set<String> justPublishedBindingKeys = new HashSet<String>();
        for (BindingTemplate bindingTemplate : bindingTemplates) {
            justPublishedBindingKeys.add(bindingTemplate.getBindingKey());
        }

        //if an endpoint previous published by the gateway no longer applies (e.g. a listener was removed) then we need
        // to delete it from UDDI
        if (publishedBindingKeys != null) {
            Set<String> bindingTemplatesToDelete = new HashSet<String>();

            for (String publishedBindingKey : publishedBindingKeys) {
                if (!justPublishedBindingKeys.contains(publishedBindingKey)) {
                    bindingTemplatesToDelete.add(publishedBindingKey);
                }
            }

            if (!bindingTemplatesToDelete.isEmpty()) {
                deleteGatewayBindingTemplates(serviceKey, null, bindingTemplatesToDelete);
            }
        }

        return justPublishedBindingKeys;
    }

    /**
     * Publish a GIF endpoint to an existing service. This involves publishing a new bindingTemplate, giving it the
     * bindingKey of the bindingTemplate it is proxying, then adding the required meta data to the new bindingTemplate
     * (called the proxy endpoint) and to the original endpoint (called the functional endpoint).
     * <p/>
     * What ever keyedReferences are included in configKeyedReferences will be attached to the categoryBag of each
     * bindingTemplate published. Any references which exist in runtimeKeyedReferences but do not exist in
     * configKeyedReferences will be removed from UDDI.
     *
     * @param serviceKey               key of the BusinessService to update
     * @param wsdlPortName             wsdl:port to model as a bindingTEmplate
     * @param wsdlPortBinding          wsdl:binding binding the wsdl:port implements
     * @param wsdlPortBindingNamespace namespace of the wsdl:binding
     * @param endpointPair             the external gateway URL and WSDL URL for the GIF endpoint.
     * @param publishedBindingKey      may be null on first publish. The key of the 'proxy'. When not null this is the
     *                                 original bindingKey before the GIF publish was done.
     * @param functionalBindingKey     may be null on first publish. When not null this is the newly assigned bindingKey
     *                                 given to the original bindingTemplate after it was proxied.
     * @param mgmtSystemKeyValue       the name to use for GIF meta data C.2 - the name of the WSMS (Web Services Management System).
     *                                 Required.
     * @param configKeyedReferences    Set of UDDIKeyedReferences that are required to be published. Can be null when none need to be published.
     * @param runtimeKeyedReferences   Set of UDDIKeyedReferences that were last published. Can be null when no references have been published.
     * @return a pair, the lhs is the proxy bindingTemplates's bindingKey and the rhs is the functional
     *         bindingTemplate's bindingKey
     * @throws UDDIException any exceptions updating UDDI.
     */
    public Pair<String, String> publishBindingTemplateGif(
            final String serviceKey,
            final String wsdlPortName,
            final String wsdlPortBinding,
            final String wsdlPortBindingNamespace,
            final EndpointPair endpointPair,
            final String publishedBindingKey,
            final String functionalBindingKey,
            final String mgmtSystemKeyValue,
            final Set<UDDIKeyedReference> configKeyedReferences,
            final Set<UDDIKeyedReference> runtimeKeyedReferences) throws UDDIException {
        if (serviceKey == null || serviceKey.trim().isEmpty())
            throw new IllegalArgumentException("serviceKey cannot be null or empty");
        if (wsdlPortName == null || wsdlPortName.trim().isEmpty())
            throw new IllegalArgumentException("wsdlPortName cannot be null or empty");
        if (wsdlPortBinding == null || wsdlPortBinding.trim().isEmpty())
            throw new IllegalArgumentException("wsdlPortBinding cannot be null or empty");
        if (wsdlPortBindingNamespace == null || wsdlPortBindingNamespace.trim().isEmpty())
            throw new IllegalArgumentException("wsdlPortBindingNamespace cannot be null or empty");
        if (mgmtSystemKeyValue == null || mgmtSystemKeyValue.trim().isEmpty())
            throw new IllegalArgumentException("mgmtSystemKeyValue cannot be null or empty");

        UDDIUtilities.validateAllEndpointPairs(Arrays.asList(endpointPair));


        //Create the bindingTemplate structure
        final Pair<List<BindingTemplate>, Map<String, TModel>> bindingToModels;
        try {
            bindingToModels = UDDIUtilities.createBindingTemplateFromWsdl(wsdl, wsdlPortName, wsdlPortBinding, wsdlPortBindingNamespace, Arrays.asList(endpointPair));
        } catch (UDDIUtilities.PortNotFoundException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.NonSoapWsdlPortException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.NonHttpBindingException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        }

        if (bindingToModels.left.size() != 1)
            throw new IllegalStateException("Only a single binding template should be created.");

        //Get the service
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);

        final UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(jaxWsUDDIClient);
        final List<Pair<BusinessService, Map<String, TModel>>>
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModels(serviceKeys);

        if (uddiServicesToDependentModels.isEmpty())
            throw new UDDIException("Could not find BusinessService with serviceKey: " + serviceKey);

        final Pair<BusinessService, Map<String, TModel>> serviceMapPair = uddiServicesToDependentModels.iterator().next();
        final BusinessService businessService = serviceMapPair.left;
        final Map<String, TModel> publishedTModels = serviceMapPair.right;

        final List<BindingTemplate> bindingTemplates;
        if (businessService.getBindingTemplates() != null) {
            bindingTemplates = businessService.getBindingTemplates().getBindingTemplate();
        } else {
            bindingTemplates = Collections.emptyList();
        }

        //prepare our binding template with required meta data for GIF
        final Set<UDDIKeyedReference> uddiKeyedReferences = getProxyGifMetaData(endpointPair, mgmtSystemKeyValue);
        if (configKeyedReferences != null) {
            uddiKeyedReferences.addAll(configKeyedReferences);
        }

        final BindingTemplate toPublishTemplate = bindingToModels.left.get(0);
        final Map<String, TModel> toPublishTModels = bindingToModels.right;
        //we know these reference have not been added yet
        if (toPublishTemplate.getCategoryBag() == null) {
            final CategoryBag cBag = new CategoryBag();
            toPublishTemplate.setCategoryBag(cBag);
        }
        toPublishTemplate.getCategoryBag().getKeyedReference().addAll(convertFromBeans(uddiKeyedReferences));

        BindingTemplate functionalTemplate = null;
        final boolean updateFunctionalTemplate;
        final boolean firstPublish;
        //has this endpoint already been published?
        if (publishedBindingKey != null) {
            firstPublish = false;
            BindingTemplate prevPublished = null;

            //we must find the bindingTemplate
            for (BindingTemplate publishedTemplate : bindingTemplates) {
                if (publishedTemplate.getBindingKey().equals(publishedBindingKey)) {
                    prevPublished = publishedTemplate;
                } else if (publishedTemplate.getBindingKey().equals(functionalBindingKey)) {
                    functionalTemplate = publishedTemplate;
                }
            }

            if (prevPublished == null) {
                //cause publish to fail, will eventually fail out.
                throw new UDDIException("Cannot find bindingTemplate previously published to UDDI with bindingKey #(" + publishedBindingKey + ")");
            }

            if (functionalTemplate == null) {
                throw new UDDIException("Cannot find the functionalTemplate with bindingKey #(" + functionalBindingKey + ").");
            }

            updateTemplate(
                    toPublishTemplate,
                    Arrays.asList(prevPublished),
                    Arrays.asList(endpointPair),
                    toPublishTModels,
                    publishedTModels,
                    new HashSet<String>());

            //clean up any required UDDI references
            if (runtimeKeyedReferences != null) {
                manageKeyedReferences(configKeyedReferences, runtimeKeyedReferences, toPublishTemplate);
            }

            //confirm the keys were transferred
            if (toPublishTemplate.getBindingKey() == null) {
                throw new UDDIException("Previously published bindingTemplate with key #(" + publishedBindingKey + ")" +
                        " does not match the service's WSDL. Cannot update.");
            }

            //if this update was causes by a change that causes the external URL to change, then the URL keyedReference
            //will need to be updated. The synchronizing in updateTemplate above will have brought over the old
            //reference which has the old URL value, therefore we must remove it here
            final List<KeyedReference> toPublishReferences = toPublishTemplate.getCategoryBag().getKeyedReference();
            for (int i = toPublishReferences.size() - 1; i >= 0; i--) {//revers to protect against compaction
                KeyedReference toPublishReference = toPublishReferences.get(i);
                if (toPublishReference.getTModelKey().equals(UDDI_SYSTINET_COM_MANAGEMENT_URL)) {
                    if (!toPublishReference.getKeyValue().equals(endpointPair.getEndPointUrl())) {
                        toPublishReferences.remove(i);
                    }
                }
            }

            //manage D1 and D2 on functional endpoint
            final List<KeyedReference> functionalRefs = getFunctionalEndPointMetaData(publishedBindingKey);

            final List<UDDIKeyedReference> requiredRefs = convertToBeans(functionalRefs);
            final List<UDDIKeyedReference> existingRefs = convertToBeans(functionalTemplate.getCategoryBag().getKeyedReference());

            if (!existingRefs.containsAll(requiredRefs)) {
                //put refs D1 and D2 back on the functional endpoint if they have been removed
                final CategoryBag cBag = new CategoryBag();
                cBag.getKeyedReference().addAll(functionalRefs);
                synchronizeCategoryBags(functionalTemplate.getCategoryBag(), cBag);
                updateFunctionalTemplate = true;
            } else {
                updateFunctionalTemplate = false;
            }
        } else {
            firstPublish = true;
            //this is the first publish. The first publish there is no publishedBindingKey
            //find the bindingTemplate we are going to proxy
            //find the functional endpoint
            final Triple<BindingTemplate, TModel, TModel> tModelTriple = findMatchingEndpointAndModels(bindingTemplates,
                    toPublishTemplate, toPublishTModels, publishedTModels, false);
            if (tModelTriple == null)
                throw new UDDIException("The functional endpoint to proxy " +
                        "could not be found in UDDI on BusinessService with serviceKey #(" + serviceKey + ")");

            functionalTemplate = tModelTriple.left;

            //record the bindingkey of the original bindingTemplate
            final String originalBindingKey = functionalTemplate.getBindingKey();
            //swap the keys
            toPublishTemplate.setBindingKey(originalBindingKey);
            functionalTemplate.setBindingKey(null);

            if(functionalTemplate.getCategoryBag() == null){
                //functional should not be null, but protect against it. We know our to publish category bag is not null.
                final CategoryBag newCatBag = new CategoryBag();
                functionalTemplate.setCategoryBag(newCatBag);
            }

            //synchronize the functional template's keyed references with the proxy - to ensure it is returned from the same searches
            //do this before adding D.1 and D.2 below as we don't want them on the proxy.
            synchronizeCategoryBags(toPublishTemplate.getCategoryBag(), functionalTemplate.getCategoryBag());

            //add the required meta data to functional endpoint
            //manage D1 and D2 on functional endpoint
            final List<KeyedReference> functionalRefs = getFunctionalEndPointMetaData(originalBindingKey);
            functionalTemplate.getCategoryBag().getKeyedReference().addAll(functionalRefs);
            
            updateFunctionalTemplate = true;
        }

        try {
            //publish the wsdl:portType tModels first so we can update the binding tModel references to them
            for (Map.Entry<String, TModel> entry : toPublishTModels.entrySet()) {
                if (UDDIUtilities.getTModelType(entry.getValue(), true) != UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE)
                    continue;
                jaxWsUDDIClient.publishTModel(entry.getValue());
            }

            final List<TModel> allBindingTModels = new ArrayList<TModel>();
            for (final TModel tModel : bindingToModels.right.values()) {
                if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                allBindingTModels.add(tModel);
            }
            if (allBindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");
            UDDIUtilities.updateBindingTModelReferences(allBindingTModels, bindingToModels.right);

            //now publish the binding tmodels after they have been updated
            for (TModel bindingModel : allBindingTModels) {
                jaxWsUDDIClient.publishTModel(bindingModel);
            }

            //Save all bindingTemplates together
            final List<BindingTemplate> toSaveTemplates = new ArrayList<BindingTemplate>();
            //publish update to the functional endpoint
            if (updateFunctionalTemplate) {
                toSaveTemplates.add(functionalTemplate);
            }

            toPublishTemplate.setServiceKey(businessService.getServiceKey());
            UDDIUtilities.updateBindingTemplateTModelReferences(toPublishTemplate, bindingToModels.right);
            toSaveTemplates.add(toPublishTemplate);

            jaxWsUDDIClient.publishBindingTemplate(toSaveTemplates);
            //NO MORE UDDI OPERATIONS AFTER THIS POINT. ROLL BACK LOGIC ASSUMES BINDING TEMPLATES PUBLISH IS THE LAST INTERACTION
            return new Pair<String, String>(toPublishTemplate.getBindingKey(), functionalTemplate.getBindingKey());

        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Problem publishing to UDDI: " + ExceptionUtils.getMessage(e));
            if (firstPublish) {
                for (Map.Entry<String, TModel> entry : toPublishTModels.entrySet()) {
                    final String tModelKey = entry.getValue().getTModelKey();
                    if (tModelKey != null && !tModelKey.trim().isEmpty()) {
                        handleTModelRollback(tModelKey);
                    }
                }

                //do not roll back the bindingTemplate as it will delete the original bindingTemplate which 'we' do
                //not own. tModels are the only thing to clean up as the bindingTemplate is the last publish, so
                //either they were not attempted to be published or the publish failed.
            }
            //no else. If we have already published to UDDI, the data is possibly out of date but the publish will
            //be reattempted. We have reused tModel keys so there is no clean up to do.

            throw e;
        }
    }

    /**
     * Delete all gateway endpoints contained by a BusinessService
     * Delete bindingTemplates either with the Collection of previous end point pairs (to support Pandora->Albacore) or
     * preferably delete via the persisted bindingKeys in persistedBindingKeys
     * <p/>
     * If persistedBindingKeys is not supplied, then any bindingTemplate found in the BusinessService with the supplied
     * serviceKey, will be deleted if it's accessPoint has a useType of 'endPoint' and a URL value which either equals
     * an endPointUrl from any EndpointPair in previousEndpointPairs, or partially matches it ignoring the protocol and
     * port number (for Pandora support)
     * <p/>
     * In Pandora a gateway endpoint was defined as any bindingTemplate in UDDI which contains an accessPoint with an endPoint value
     * containing both the supplied publishedHostname and the serviceOid of a published service. This support is maintained
     * so that after an upgrade bindingTemplates are not orphaned in UDDI.
     *
     * @param serviceKey            String serviceKey of a BusinessService from which to delete gateway bindingTemplates. Required
     * @param previousEndpointPairs Collection&lt;EndpointPair&gt; The collection of endpoint pairs which were used to
     *                              previously publish the endpoint for the current service to an original service in UDDI.
     *                              Can be null / empty when persistedBindingKeys is not. Only the endpoint URL is used here, the WSDL URL is not required.
     *                              This is used to find existing bindingTemplates and to delete them when this publish is performing a republish.
     * @param persistedBindingKeys  Set&lt;String&gt; Can be null / empty when previousEndpointPairs is not
     * @throws UDDIException any problems searching / updating UDDI
     */
    public void deleteGatewayBindingTemplates(final String serviceKey,
                                              final Set<EndpointPair> previousEndpointPairs,
                                              final Set<String> persistedBindingKeys) throws UDDIException {
        if (serviceKey == null || serviceKey.trim().isEmpty())
            throw new IllegalArgumentException("serviceKey cannot be null or empty");

        boolean haveKeys = persistedBindingKeys != null && !persistedBindingKeys.isEmpty();
        boolean haveEndpoints = previousEndpointPairs != null && !previousEndpointPairs.isEmpty();

        if (!(haveKeys || haveEndpoints)) {
            throw new IllegalArgumentException("To delete BindingTemplates either published endpoints or the set of bindingKeys must be supplied");
        }

        final BusinessService businessService = jaxWsUDDIClient.getBusinessService(serviceKey);

        final BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        if (bindingTemplates != null && !bindingTemplates.getBindingTemplate().isEmpty()) {
            outer:
            for (BindingTemplate bt : bindingTemplates.getBindingTemplate()) {
                if (haveKeys) {
                    if (persistedBindingKeys.contains(bt.getBindingKey())) {
                        logger.log(Level.FINE, "bindingTemplate with persisted key '" + bt.getBindingKey() + "' will be deleted");
                        uddiClient.deleteBindingTemplate(bt.getBindingKey());
                    }
                } else {
                    final AccessPoint accessPoint = bt.getAccessPoint();
                    //SSG published this endpoint, therefore we are strict with this requirement
                    if (!accessPoint.getUseType().equalsIgnoreCase(WsdlToUDDIModelConverter.USE_TYPE_END_POINT))
                        continue;

                    final String endPoint = accessPoint.getValue();

                    //look for exact match
                    for (EndpointPair previousEndpointPair : previousEndpointPairs) {
                        if (endPoint.equalsIgnoreCase(previousEndpointPair.getEndPointUrl())) {
                            logger.log(Level.INFO, "bindingTemplate with key '" + bt.getBindingKey() + "' will be deleted");
                            uddiClient.deleteBindingTemplate(bt.getBindingKey());
                            continue outer;
                        }
                    }

//                    if (previousEndpointPairs == null) continue;
                    //Pandora -> Maytag support, in Pandora only a hostname was used to delete the bindingTemplate, keep in this support
                    for (EndpointPair previousEndpointPair : previousEndpointPairs) {
                        String hostName = previousEndpointPair.getEndPointUrl();
                        if (hostName == null) continue;

                        hostName = hostName.substring(hostName.indexOf("://") + 3, hostName.length());

                        if (endPoint.indexOf(hostName) != -1 && endPoint.indexOf(SecureSpanConstants.SERVICE_FILE + serviceId) != -1) {
                            logger.log(Level.INFO, "bindingTemplate with key '" + bt.getBindingKey() + "' will be deleted");
                            uddiClient.deleteBindingTemplate(bt.getBindingKey());
                            continue outer;
                        }
                    }
                    logger.log(Level.FINEST, "BindingTemplate with key '" + bt.getBindingKey() + "' and accessPoint '" + endPoint + "' was not deleted");
                }
            }
        }
    }

    /**
     * Best effort attempt to undo the GIF publish. The functional bindingTemplate is saved with the proxyBindingKey,
     * this reverts it back to having it's original key. The GIF references D.1 and D.2 are also removed from the
     * functional bindingTemplate before it is saved back to UDDI. After this save the proxy bindingTemplate has been
     * removed and there are now 2 functional binding templates attached to the service. The old functional binding
     * template is then deleted, followed by the tModels the proxy bindingTemplate used to reference.
     *
     * @param serviceKey BusinessService which contains the proxied GIF endpoint. Required.
     * @param proxyBindingKey key of the proxied bindingTemplate. Required.
     * @param functionalBindingKey key of the functional bindingTemplate. Required.
     * @throws UDDIException thrown if any problems updating UDDI.
     */
    public void deleteGatewayGifBindingTemplates(final String serviceKey,
                                                 final String proxyBindingKey,
                                                 final String functionalBindingKey)
            throws UDDIException {

        if (serviceKey == null || serviceKey.trim().isEmpty())
            throw new IllegalArgumentException("serviceKey cannot be null or empty");

        if(proxyBindingKey == null || proxyBindingKey.trim().isEmpty()){
            throw new IllegalArgumentException("proxyBindingKey cannot be null or empty");
        }

        if(functionalBindingKey == null || functionalBindingKey.trim().isEmpty()){
            throw new IllegalArgumentException("functionalBindingKey cannot be null or empty");            
        }

        final BusinessService businessService = jaxWsUDDIClient.getBusinessService(serviceKey);

        final BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        final Map<String, BindingTemplate> keyToTemplate = new HashMap<String, BindingTemplate>();
        for (BindingTemplate template : bindingTemplates.getBindingTemplate()) {
            keyToTemplate.put(template.getBindingKey(), template);
        }

        final BindingTemplate proxyTemplate = keyToTemplate.get(proxyBindingKey);
        final BindingTemplate functionalTemplate = keyToTemplate.get(functionalBindingKey);

        if (proxyTemplate == null) {
            final String msg = "Cannot find the proxy GIF endpoint to delete with bindingKey #(" + proxyBindingKey + ").";
            logger.log(Level.WARNING, msg);
            throw new UDDIUnpublishException(msg);
        }

        if (functionalTemplate == null) {
            final String msg = "Cannot find the functional template with bindingKey #(" + functionalBindingKey + ").";
            logger.log(Level.WARNING, msg);
            throw new UDDIUnpublishException(msg);
        }

        //fix the functional template - back to it's state before it was proxied
        functionalTemplate.setBindingKey(proxyTemplate.getBindingKey());
        final CategoryBag funcCatBag = functionalTemplate.getCategoryBag();
        if (funcCatBag != null) {
            final List<KeyedReference> refs = funcCatBag.getKeyedReference();
            for (int i = refs.size() - 1; i >= 0; i--) { //protect against compaction
                KeyedReference ref = refs.get(i);
                final String refTModelKey = ref.getTModelKey();
                final String keyValue = ref.getKeyValue();

                if (refTModelKey.equals(UDDI_SYSTINET_COM_MANAGEMENT_TYPE) && keyValue.equals(FUNCTIONAL_ENDPOINT)) {
                    refs.remove(i);
                } else if (refTModelKey.equals(UDDI_SYSTINET_COM_MANAGEMENT_PROXY_REFERENCE)) {
                    refs.remove(i);
                }
            }
        }

        //Record the set of tModels referenced from the proxy bindingTemplate as once we have saved the functional
        //template with a new key the proxy reference is lost
        final List<TModelInstanceInfo> tModelInstanceInfos = proxyTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
        final Set<String> tModelsToDelete = new HashSet<String>();
        for (TModelInstanceInfo tModelInstanceInfo : tModelInstanceInfos) {
            //blindly add them all - we own this proxy bindingTemplate and it's about to be deleted so we can remove
            //all tModels referenced
            tModelsToDelete.add(tModelInstanceInfo.getTModelKey());
        }

        //save functional endpoint - this automatically removes the proxy as it's saved with the same bindingKey
        jaxWsUDDIClient.publishBindingTemplate(functionalTemplate);
        uddiClient.deleteTModel(tModelsToDelete);

        //delete the old functional endpoint
        uddiClient.deleteBindingTemplateOnly(functionalBindingKey);
    }

    /**
     * Overwrite all soap + http endpoints of an original Business Service in UDDI with soap + http endpoints from a
     * published service's WSDL.
     * <p/>
     * Should only be called once for an original business service in UDDI.
     * <p/>
     * This will overwrite a single BusinessService in UDDI with the contents of a single wsdl:service from the
     * gateways WSDL.
     *
     * @param serviceKey       String serviceKey of a BusinessService to overwrite. Required.
     * @param businessKey      Stirng businessKey which owns the BusinessService identified by serviceKey. Required
     * @param allEndpointPairs Collection&lt;EndpointPair&gt; Required. All endpoints to publish bindingTemplates for
     * @return Set&lt;String&gt; The set of bindingKeys published for the overwritten service
     * @throws UDDIException any problems searching / updating UDDI
     */
    public Set<String> overwriteServiceInUDDI(
            final String serviceKey,
            final String businessKey,
            final Collection<EndpointPair> allEndpointPairs) throws UDDIException {
        if (serviceKey == null || serviceKey.trim().isEmpty())
            throw new IllegalArgumentException("serviceKey cannot be null or empty");
        if (businessKey == null || businessKey.trim().isEmpty())
            throw new IllegalArgumentException("businessKey cannot be null or empty");

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);

        //fail early if problem with the WSDL
        final WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        try {
            modelConverter.convertWsdlToUDDIModel(allEndpointPairs, null, null);
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceId + ") into UDDI object model.", e);
        }

        final UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(jaxWsUDDIClient);
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);
        final List<Pair<BusinessService, Map<String, TModel>>> uddiServicesToDependentTModelsPairs = serviceDownloader.getBusinessServiceModels(serviceKeys);

        if (uddiServicesToDependentTModelsPairs.isEmpty())
            throw new UDDIException("No BusinessService found for serviceKey: " + serviceKey);
        final Pair<BusinessService, Map<String, TModel>> serviceToDependentTModels = uddiServicesToDependentTModelsPairs.iterator().next();

        BusinessService overwriteService = serviceToDependentTModels.left;//not final as we will update it's reference
        //fail early if problem with the business service.
        //extract the service's local name from it's keyed references - it must exist to be valid
        if (overwriteService.getCategoryBag() == null) {
            throw new UDDIException("Invalid BusinessService. It does not contain a CategoryBag. serviceKey: " + overwriteService.getServiceKey());
        }
        //we must find both the service name and the correct namespace in the Gateway's WSDL, otherwise we are mis configured
        final Pair<String, String> wsdlServiceNameAndNameSpace = getServiceNameAndNameSpace(overwriteService);

        final BindingTemplates templates = overwriteService.getBindingTemplates();
        if (templates == null) {
            //we can work with this, we don't care as were going to be uploading our own bindingTemplates
            //this happens if all bindings are removed between when we got our record of the service and us
            //initiating this overwrite action
            BindingTemplates newTemplates = new BindingTemplates();
            overwriteService.setBindingTemplates(newTemplates);
        }

        final Map<String, TModel> allDependentTModels = serviceToDependentTModels.right;

        final Pair<Set<String>, Set<String>> deleteAndKeep = getBindingsToDeleteAndKeep(overwriteService.getBindingTemplates().getBindingTemplate(), allDependentTModels);
        final Set<String> deleteSet = deleteAndKeep.left;

        final List<Pair<BusinessService, Map<String, TModel>>> wsdlBusinessServicesToDependentTModels = modelConverter.getServicesAndDependentTModels();
        //fail early before updating UDDI
        final List<Pair<BusinessService, Map<String, TModel>>> pairToUseList =
                extractSingleService(wsdlServiceNameAndNameSpace.left, wsdlServiceNameAndNameSpace.right, wsdlBusinessServicesToDependentTModels);

        //Delete existing bindingTemplates from UDDI. Do this before trying to publish to ensure user has permissions
        //do not want to successfully publish and then find that we cannot remove the bindingTemplates. If that is the
        //case then the normal publish can be done.
        for (String bindingKey : deleteSet) {
            uddiClient.deleteBindingTemplate(bindingKey);
        }

        //Wsdl name to service name is a 1 to M relationship. A service in a WSDL has a single value, but a business
        //service in UDDI can have multiple names. This map allows the various service names to look up the wsdl name.
        final Map<String, String> serviceNameToWsdlNameMap = new HashMap<String, String>();
        for (Name name : overwriteService.getName()) {
            serviceNameToWsdlNameMap.put(name.getValue(), wsdlServiceNameAndNameSpace.left);
        }

        //now we are ready to work with this service. We will publish all soap bindings found and leave any others on
        //the original service in UDDI intact - this will likely be a configurable option in the future

        //true here means that we will keep any existing bindings found on the BusinessService
        publishToUDDI(serviceKeys,
                pairToUseList,
                serviceNameToWsdlNameMap,
                true,
                null,
                allEndpointPairs);

        return getPublishedBindingTemplates();
    }

    /**
     * Get the set of services which should be deleted and the set of services which were published as a result
     * of publishing the WSDL
     * <p/>
     * This handles both an initial publish and a subsequent publish. For updates, existing serviceKeys are reused
     * where they represent the same wsdl:service from the wsdl, this is determined by the wsdl:service localnames matching
     * <p/>
     * If isOverWriteUpdate is true, then publishedServiceKeys must contain at least 1 key.
     *
     * @param businessKey              String businessKey of the BusinessEntity in UDDI which owns all services published
     * @param publishedServiceKeys     Set String of serviceKeys for BusinessServices which were already published at an earlier time for this WSDL. Can be empty, but not null
     * @param isOverwriteUpdate        boolean if true, then we are updating an overwritten service. In this case we need
     *                                 to make sure we only publish a single service from the WSDL. The first serviceKey found in publishedServiceKeys is then
     *                                 assumed to be the serviceKey of the overwritten service.
     * @param registrySpecificMetaData if not null, the registry specific meta data will be added appropriately to each applicable UDDI entity published
     * @param allEndpointPairs         Collection&lt;EndpointPair&gt; all endpoints which should be published to UDDI
     *                                 as distinct endpoints for each service published. This means that a single wsdl:port may have more than one
     *                                 bindingTemplate in UDDI. The endPointURL of each EndpointPair is String URL which will become the value of the 'endPoint'
     *                                 in the accessPoint element of a bindingTemplate. The wsdlURL of each EndpointPair is the String WSDL URL,
     *                                 this will be included in every tModel published to UDDI as an overviewURL.
     * @return Pair Left side: Set of serviceKeys which should be deleted. Right side: set of BusinessServices published
     *         as a result of this publish / update operation
     * @throws UDDIException any problems searching / updating UDDI or with data model
     */
    public Pair<Set<String>, Set<UDDIBusinessService>> publishServicesToUDDIRegistry(
            final String businessKey,
            final Set<String> publishedServiceKeys,
            final boolean isOverwriteUpdate,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData,
            final Collection<EndpointPair> allEndpointPairs) throws UDDIException {

        if (businessKey == null || businessKey.trim().isEmpty())
            throw new IllegalArgumentException("businessKey cannot be null or empty");
        if (publishedServiceKeys == null) throw new NullPointerException("publishedServiceKeys cannot be null");

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);

        final WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        try {
            final String prependServiceName = ConfigFactory.getProperty( "com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "Layer7" );
            final String appendServiceName = ConfigFactory.getProperty( "com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", serviceId );
            if (!isOverwriteUpdate) {
                modelConverter.convertWsdlToUDDIModel(allEndpointPairs, prependServiceName, appendServiceName);
            } else {
                modelConverter.convertWsdlToUDDIModel(allEndpointPairs, null, null);
            }

        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceId + ") into UDDI object model.", e);
        }

        //not final as we may modify the reference
        List<Pair<BusinessService, Map<String, TModel>>> wsdlBusinessServicesToDependentTModels = modelConverter.getServicesAndDependentTModels();

        if (isOverwriteUpdate) {
            final BusinessService overwriteService = jaxWsUDDIClient.getBusinessService(publishedServiceKeys.iterator().next());
            final Pair<String, String> serviceNameAndNameSpace = getServiceNameAndNameSpace(overwriteService);
            wsdlBusinessServicesToDependentTModels =
                    extractSingleService(serviceNameAndNameSpace.left, serviceNameAndNameSpace.right, wsdlBusinessServicesToDependentTModels);
        }

        final Map<String, String> serviceToWsdlServiceName = modelConverter.getServiceNameToWsdlServiceNameMap();

        return publishToUDDI(publishedServiceKeys, wsdlBusinessServicesToDependentTModels, serviceToWsdlServiceName, isOverwriteUpdate, registrySpecificMetaData, allEndpointPairs);
    }

    @Override
    public void close() throws IOException {
        uddiClient.close();
    }

    public Set<String> getPublishedBindingTemplates() {
        return publishedBindingTemplates;
    }

    //- PROTECTED

    /**
     * Create a new BusinessServicePublisher.
     *
     * @param uddiClient UDDIClient for the UDDI Registry to publish to
     * @param wsdl WSDL to publish to UDDI
     * @param serviceId services GOID, which originates from the WSDL
     */
    protected BusinessServicePublisher(final UDDIClient uddiClient,
                                       final Wsdl wsdl,
                                       final String serviceId
    ) {
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");
        if(uddiClient == null) throw new NullPointerException("uddiClient cannot be null");

        this.wsdl = wsdl;
        this.uddiClient = uddiClient;
        this.serviceId = serviceId;
        if( uddiClient instanceof JaxWsUDDIClient ){
            jaxWsUDDIClient = (JaxWsUDDIClient) uddiClient;
        }else{
            throw new IllegalStateException( "JaxWsUDDIClient is required." );
        }
    }

    /**
     * Create a new BusinessServicePublisher which does not require a WSDL. Many operations will not be available
     * on a BusinessServicePublisher constructed like this.
     *
     * @param uddiClient UDDIClient for the UDDI Registry to publish to
     * @param serviceId services GOID, which originates from the WSDL
     */
    protected BusinessServicePublisher(final UDDIClient uddiClient,
                                       final String serviceId
    ) {
        if(uddiClient == null) throw new NullPointerException("uddiClient cannot be null");

        this.wsdl = null;
        this.uddiClient = uddiClient;
        this.serviceId = serviceId;
        if( uddiClient instanceof JaxWsUDDIClient ){
            jaxWsUDDIClient = (JaxWsUDDIClient) uddiClient;
        }else{
            throw new IllegalStateException( "JaxWsUDDIClient is required." );
        }
    }

    /**
     *
     * @return A pair of List &lt;BindingTemplate&gt; and List &lt;TModel&gt;. The left side is the list of BindingTemplates
     * which were published to UDDI and the right side is their published dependent tModels. Not all may have just been
     * published for the first time, as if an update was carried out for an individual bindingTemplate, then it's
     * bindingKey was reused. When this happens then the dependent tModels's tModelKeys are also reused.
     * @throws UDDIException
     */
    Pair<List<BindingTemplate>, List<TModel>> publishEndPointToExistingService(
            final String serviceKey,
            final String wsdlPortName,
            final String wsdlPortBinding,
            final String wsdlPortBindingNamespace,
            final Collection<EndpointPair> allEndpointPairs,
            final Collection<EndpointPair> previousEndpointPairs,
            Set<String> publishedBindingKeys,
            final boolean removeOthers,
            final Set<UDDIKeyedReference> configKeyedReferences,
            final Set<UDDIKeyedReference> runtimeKeyedReferences)
            throws UDDIException {

        if(publishedBindingKeys == null) publishedBindingKeys = new HashSet<String>();

        final Pair<List<BindingTemplate>, Map<String, TModel>> bindingToModels;
        try {
            bindingToModels = UDDIUtilities.createBindingTemplateFromWsdl(wsdl, wsdlPortName, wsdlPortBinding, wsdlPortBindingNamespace, allEndpointPairs);
        } catch (UDDIUtilities.PortNotFoundException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.NonSoapWsdlPortException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.NonHttpBindingException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        }

        //Get the service
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);

        final UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(jaxWsUDDIClient);
        final List<Pair<BusinessService, Map<String, TModel>>>
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModels(serviceKeys);

        if (uddiServicesToDependentModels.isEmpty())
            throw new UDDIException("Could not find BusinessService with serviceKey: " + serviceKey);
        final Pair<BusinessService, Map<String, TModel>> serviceMapPair = uddiServicesToDependentModels.iterator().next();
        final BusinessService businessService = serviceMapPair.left;
        final Map<String, TModel> publishedTModels = serviceMapPair.right;

        final Set<String> allOtherBindingKeys;//all binding keys currently on the service , some may have been previously published by the gateway
        final List<BindingTemplate> bindingTemplates;//same as above except the actual BindingTemplate objects
        if (businessService.getBindingTemplates() != null) {
            bindingTemplates = businessService.getBindingTemplates().getBindingTemplate();
            allOtherBindingKeys = new HashSet<String>();
            for (BindingTemplate template : bindingTemplates) {
                allOtherBindingKeys.add(template.getBindingKey());
            }
        } else {
            allOtherBindingKeys = Collections.emptySet();
            bindingTemplates = Collections.emptyList();
        }

        //applicableTemplates is the list of bindingTemplates which the Gateway has published. With this list we
        //can ensure that the same template being published again can reuse an existing key
        List<BindingTemplate> applicableTemplates = new ArrayList<BindingTemplate>();
        //has this endpoint already been published?
        if(!publishedBindingKeys.isEmpty()){
            //with persisted keys, we just want to see if any of our keys have been removed
            //if one of our bindingKeys has been removed, we don't need to do anything
            for (BindingTemplate publishedTemplate : bindingTemplates) {
                if(publishedBindingKeys.contains(publishedTemplate.getBindingKey())){
                    applicableTemplates.add(publishedTemplate);
                }
            }
        }else{
//            fall back on Pandora method - try to match based on endpoint / hostname
//            deleteGatewayBindingTemplates(businessService, previousEndpointPairs);
            applicableTemplates.addAll(getApplicableBindingTemplates(businessService, previousEndpointPairs));
        }

        final List<BindingTemplate> toPublishTemplates = bindingToModels.left;
        final Map<String, TModel> toPublishTModels = bindingToModels.right;
        final Set<String> updateKeysSoFar = new HashSet<String>();
        for (BindingTemplate toPublishTemplate : toPublishTemplates) {
            if(configKeyedReferences != null && !configKeyedReferences.isEmpty()){
                if(toPublishTemplate.getCategoryBag() == null){
                    final CategoryBag catBag = new CategoryBag();
                    toPublishTemplate.setCategoryBag(catBag);
                }
                toPublishTemplate.getCategoryBag().getKeyedReference().addAll(convertFromBeans(configKeyedReferences));
            }
            
            updateTemplate(toPublishTemplate,
                    applicableTemplates,
                    allEndpointPairs,
                    toPublishTModels,
                    publishedTModels,
                    updateKeysSoFar);

            //clean up any required UDDI references
            if (runtimeKeyedReferences != null && !runtimeKeyedReferences.isEmpty()) {
                manageKeyedReferences(configKeyedReferences, runtimeKeyedReferences, toPublishTemplate);
            }
        }

        final Map<String, TModel> tModelsToPublish = bindingToModels.right;
        try {
            //publish the wsdl:portType tmodels first so we can update the binding tmodel references to them
            for (Map.Entry<String, TModel> entry : tModelsToPublish.entrySet()) {
                if (UDDIUtilities.getTModelType(entry.getValue(), true) != UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE)
                    continue;
                jaxWsUDDIClient.publishTModel(entry.getValue());
            }

            final List<TModel> allBindingTModels = new ArrayList<TModel>();
            for (final TModel tModel : bindingToModels.right.values()) {
                if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                allBindingTModels.add(tModel);
            }
            if (allBindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");
            UDDIUtilities.updateBindingTModelReferences(allBindingTModels, bindingToModels.right);

            //now publish the binding tmodels after they have been updated
            for (TModel bindingModel : allBindingTModels) {
                jaxWsUDDIClient.publishTModel(bindingModel);
            }

            for (BindingTemplate bt : bindingToModels.left) {
                bt.setServiceKey(businessService.getServiceKey());

                UDDIUtilities.updateBindingTemplateTModelReferences(bt, bindingToModels.right);
                jaxWsUDDIClient.publishBindingTemplate(bt);
            }

            //remove other bindings?
            if (removeOthers) {
                logger.log(Level.FINE, "Deleting other bindingTemplates");
                for(BindingTemplate bt: bindingToModels.left){
                    //remove any bindings the gateway has published
                    allOtherBindingKeys.remove(bt.getBindingKey());
                }
                uddiClient.deleteBindingTemplateFromSingleService(allOtherBindingKeys);
            }

            List<TModel> models = new ArrayList<TModel>();
            models.addAll(bindingToModels.right.values());
            return new Pair<List<BindingTemplate>, List<TModel>>(Collections.unmodifiableList(bindingToModels.left), Collections.unmodifiableList(models));

        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Problem publishing to UDDI: " + ExceptionUtils.getMessage(e));
            //this may delete a bindingTemplate and tModels which existed from a previous publish. However if we are
            //publishing then the are out of date so this is ok.
            for (Map.Entry<String, TModel> entry : tModelsToPublish.entrySet()) {
                final String tModelKey = entry.getValue().getTModelKey();
                if (tModelKey != null && !tModelKey.trim().isEmpty()) {
                    handleTModelRollback(tModelKey);
                }
            }
            for (BindingTemplate bt : bindingToModels.left) {
                final String bindingKey = bt.getBindingKey();
                if (bindingKey != null && !bindingKey.trim().isEmpty()) {
                    handleBindingTemplateRollback(bindingKey);
                }
            }
            throw e;
        }
    }

    /**
     * Publish all TModels contained in the dependentTModels map, which are not already published. Only the type
     * of TModel represented by tmodelType will be published. This is to allow for TModels to be published in their
     * dependency order
     * <p/>
     * Any exception occurs and an attempt is made to delete any tModels which were successfully published.
     *
     * @param dependentTModels          tModels to publish
     * @param tmodelType                TMODEL_TYPE the type of TModel we should publish.
     * @param allPublishedServicesSoFar Set of all published business services published so far. Any errors will cause an
     *                                  attempt to rollback any previously published business serviecs.
     * @return List of TModels which were successfully published
     * @throws UDDIException any exception publishing the tmodels
     */
    void publishDependentTModels(final Map<String, TModel> dependentTModels,
                                         final UDDIUtilities.TMODEL_TYPE tmodelType,
                                         final Set<Pair<String, BusinessService>> allPublishedServicesSoFar) throws UDDIException {
        try {
            for (Map.Entry<String, TModel> entrySet : dependentTModels.entrySet()) {
                final TModel tModel = entrySet.getValue();
                //only publish the type were currently interested in
                if (UDDIUtilities.getTModelType(tModel, true) != tmodelType) continue;
                jaxWsUDDIClient.publishTModel(tModel);
            }
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing tModels: " + e.getMessage());
            handleUDDIRollback(dependentTModels.values(), allPublishedServicesSoFar);
            throw e;
        }
    }

    //- PRIVATE

    /**
     * Based on the previously published endpoints, find the list of applicable BindingTemplates which were published
     * by a gateway
     *
     */
    private List<BindingTemplate> getApplicableBindingTemplates(final BusinessService businessService,
                                                                final Collection<EndpointPair> previousEndpointPairs) throws UDDIException {
        final List<BindingTemplate> applicableTemplates = new ArrayList<BindingTemplate>();

        if (previousEndpointPairs == null || previousEndpointPairs.isEmpty()) {
            return applicableTemplates;
        }

        final BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        if (bindingTemplates != null && !bindingTemplates.getBindingTemplate().isEmpty()) {
            outer:
            for (BindingTemplate bt : bindingTemplates.getBindingTemplate()) {
                final AccessPoint accessPoint = bt.getAccessPoint();
                if (!accessPoint.getUseType().equalsIgnoreCase(WsdlToUDDIModelConverter.USE_TYPE_END_POINT)) continue;

                final String endPoint = accessPoint.getValue();

                //look for exact match
                for (EndpointPair previousEndpointPair : previousEndpointPairs) {
                    if (endPoint.equalsIgnoreCase(previousEndpointPair.getEndPointUrl())) {
                        applicableTemplates.add(bt);
                        continue outer;
                    }
                }

                if (previousEndpointPairs == null) continue;
                //Pandora -> Albacore support, in Pandora only a hostname was used to delete the bindingTemplate, keep in this support
                for (EndpointPair previousEndpointPair : previousEndpointPairs) {
                    String hostName = previousEndpointPair.getEndPointUrl();
                    if (hostName == null) continue;

                    hostName = hostName.substring(hostName.indexOf("://") + 3, hostName.length());

                    if (endPoint.indexOf(hostName) != -1 && endPoint.indexOf(SecureSpanConstants.SERVICE_FILE + serviceId) != -1) {
                        applicableTemplates.add(bt);
                        continue outer;
                    }
                }
            }
        }
        return applicableTemplates;
    }

    /**
     * Extract out the real wsdl:service name and namespace if applicable from the supplied BusinessService.
     * @param overwriteService BusinessService to get the information from. Cannot be null
     * @return Pair of strings. The left is the service name, the right is the namespace. The namespace can be null.
     * @throws UDDIException If the service name is not found, or the namepace is empty. No UDDI interactions are done here.
     */
    private Pair<String, String> getServiceNameAndNameSpace(final BusinessService overwriteService) throws UDDIException {
        if(overwriteService.getCategoryBag() == null){
            throw new UDDIException("Invalid BusinessService. It does not contain a CategoryBag. serviceKey: " + overwriteService.getServiceKey());
        }

        String foundServiceName = null;
        String foundNameSpace = null;
        for(KeyedReference kr: overwriteService.getCategoryBag().getKeyedReference()){
            if(kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_LOCALNAME)){
                foundServiceName = kr.getKeyValue();
            } else if(kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE)){
                foundNameSpace = kr.getKeyValue();
            }
        }

        if(foundServiceName == null || foundServiceName.trim().isEmpty()){
            throw new UDDIException("BusinessService does not contain a local name KeyedReference with the name of the service. serviceKey: " + overwriteService.getServiceKey());
        }
        if(foundNameSpace != null){
            //ok if namespace is null, but if it's supplied it can't be an empty string
            if(foundNameSpace.trim().isEmpty()){
                throw new UDDIException("BusinessService does not contain a non empty namespace KeyedReference. serviceKey: " + overwriteService.getServiceKey());
            }
        }
        return new Pair<String, String>(foundServiceName, foundNameSpace);
    }

    /**
     * From a Collection of BusinessServices, extract a single BusinessService who's name matches the requiredServiceName
     * parameter and if requiredNameSpace is not null, who's namespace matches this parameter.
     *
     * @param requiredServiceName String service name. Required.
     * @param requiredNameSpace   String Can be null. If not null it cannot be the empty string after a trim
     * @param wsdlBusinessServicesToDependentTModels
     *                            Collection of Pairs of BusinessServices to dependent tModels. This should
     *                            represent a gateway WSDL converted into the UDDI data model.
     * @return the required BusinessService and dependent tModels. For convenience for callers, it's returned as a list.
     *         The list will only every contain a single element.
     * @throws UDDIException if the required BusinessService is not found in the wsdlBusinessServicesToDependentTModels Collection
     */
    private List<Pair<BusinessService, Map<String, TModel>>> extractSingleService(
            final String requiredServiceName,
            final String requiredNameSpace,
            final Collection<Pair<BusinessService, Map<String, TModel>>> wsdlBusinessServicesToDependentTModels) throws UDDIException {
        if (requiredNameSpace != null && requiredNameSpace.trim().isEmpty())
            throw new IllegalArgumentException("If requiredNameSpace is supplied, it cannot be the empty string");

        Pair<BusinessService, Map<String, TModel>> foundPair = null;
        for (Pair<BusinessService, Map<String, TModel>> pair : wsdlBusinessServicesToDependentTModels) {
            final BusinessService checkIfItsTheOne = pair.left;

            String uddiServiceLocalName = null;
            String uddiNameSpace = null;
            for (KeyedReference kr : checkIfItsTheOne.getCategoryBag().getKeyedReference()) {
                if (kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_LOCALNAME)) {
                    uddiServiceLocalName = kr.getKeyValue();
                } else if (kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE)) {
                    uddiNameSpace = kr.getKeyValue();
                }
            }

            final boolean localNameOk = uddiServiceLocalName != null && !uddiServiceLocalName.trim().isEmpty();
            final boolean nameSpaceOk = requiredNameSpace != null && (uddiNameSpace != null && !uddiNameSpace.trim().isEmpty());

            if (!localNameOk) {
                continue;//this service is not valid, cannot use
            }

            if (uddiServiceLocalName.equalsIgnoreCase(requiredServiceName) &&
                    (!nameSpaceOk || uddiNameSpace.equalsIgnoreCase(requiredNameSpace))) {
                foundPair = pair;
                break;
            }
        }

        //if foundPair is null, then the UDDI does not model the WSDL, we cannot work with this
        if (foundPair == null)
            throw new UDDIException("The wsdl:service '" + requiredServiceName + "' is not contained in the Gateway's WSDL. Cannot overwrite original service");

        return Arrays.asList(foundPair);
    }

    /**
     * Publish a Collection of BusinessServices to UDDI. This method provides a best effort attempt to successfully
     * publish all BusinessServices and their tModels in an atomic fashion. This cannot be guaranteed. If any publish
     * fails, then an individual attempt is made to delete any previously published data. This is also best effort. If
     * errors happen then it is very probable that information will be orphaned in UDDI.
     *
     * @param publishedServiceKeys     Set String of all serviceKeys for each BusinessService which has previously been
     *                                 published for the same WSDL. Required. Cannot be null. Can be empty
     * @param wsdlServiceNameToDependentTModels
     *                                 List of Pairs containing a BusinessService and all of it's unique dependent tModels.
     *                                 Each tModel is new and should have a null tModelKey property. Required. Cannot be null or empty.
     * @param serviceToWsdlServiceName Map of String BusinessService name to the wsdl:service localname which generated the business service name.
     *                                 Each BusinessService in wsdlServiceNameToDependentTModels must contain an entry here. It is ok if two share the same entry. Required cannot be null or empty
     * @param isOverwriteUpdate        if true, if a previously published BusinessService is found, along with
     *                                 it's serviceKey, any bindings it has will also be preserved
     * @param registrySpecificMetaData if not null, the registry specific meta data will be added appropriately to each uddi piece of infomation published
     * @param allEndpointPairs Collection&lt;EndpointPair&gt; Required. All endpoints to publish bindingTemplates for
     * @return Pair of two String sets. The left side is a Set of serviceKeys which represent the previously published BusinessService which are no longer required and have
     *         been deleted from the UDDI Registry. The right side is a Set of UDDIBusinessServices representing every published BusinessService for the WSDL.
     * @throws UDDIException any problems querying / updating the UDDI Registry
     */
    private Pair<Set<String>, Set<UDDIBusinessService>> publishToUDDI(
            final Set<String> publishedServiceKeys,
            final List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels,
            final Map<String, String> serviceToWsdlServiceName,
            final boolean isOverwriteUpdate,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData,
            final Collection<EndpointPair> allEndpointPairs) throws UDDIException {
        if (publishedServiceKeys == null) throw new NullPointerException("publishedServiceKeys cannot be null");
        if (wsdlServiceNameToDependentTModels == null)
            throw new NullPointerException("wsdlServiceNameToDependentTModels cannot be null");
        if (wsdlServiceNameToDependentTModels.isEmpty())
            throw new IllegalArgumentException("wsdlServiceNameToDependentTModels cannot be empty. At least one BusinessService is required to be published");
        if (serviceToWsdlServiceName == null) throw new NullPointerException("serviceToWsdlServiceName cannot be null");
        for (Pair<BusinessService, Map<String, TModel>> aBsPair : wsdlServiceNameToDependentTModels) {
            if (!serviceToWsdlServiceName.containsKey(aBsPair.left.getName().get(0).getValue()))
                throw new IllegalArgumentException("Each BusinessService's 0 index Name value contained in wsdlServiceNameToDependentTModels must exist as a key in serviceToWsdlServiceName");
        }

        final UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(jaxWsUDDIClient);
        //Get the info on all published business services from UDDI
        //Now we know every single tModel reference each existing Business Service contains
        final List<Pair<BusinessService, Map<String, TModel>>>
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModels(publishedServiceKeys);

        //map to look up existing services
        final Map<String, BusinessService> uddiPreviouslyPublishedServiceMap = new HashMap<String, BusinessService>();
        final Map<String, Map<String, TModel>> uddiPreviouslyTModelsForService = new HashMap<String, Map<String, TModel>>();

        for (Pair<BusinessService, Map<String, TModel>> aServiceToItsModels : uddiServicesToDependentModels) {
            final String uniqueKey = getUniqueKeyForService(aServiceToItsModels.left);
            uddiPreviouslyPublishedServiceMap.put(uniqueKey, aServiceToItsModels.left);
            uddiPreviouslyTModelsForService.put(uniqueKey, aServiceToItsModels.right);
        }

        //create a map to look up the services from the wsdl to publish
        final Map<String, BusinessService> wsdlServicesToPublish = new HashMap<String, BusinessService>();
        final Map<String, Map<String, TModel>> tModelsForServiceToPublish = new HashMap<String, Map<String, TModel>>();
        for (Pair<BusinessService, Map<String, TModel>> serviceToModels : wsdlServiceNameToDependentTModels) {
            BusinessService bs = serviceToModels.left;
            final String uniqueKey = getUniqueKeyForService(bs);
            wsdlServicesToPublish.put(uniqueKey, bs);
            tModelsForServiceToPublish.put(uniqueKey, serviceToModels.right);
            addRegistrySpecifcMetaToBusinessService(registrySpecificMetaData, serviceToModels.left);
        }

        //find which services already exist, and reuse their serviceKeys if found
        //also need to know which BusinessServices to delete

        final Set<String> servicesToDelete = new HashSet<String>();

        //for overwrite - we keep non soap/http bindings. for these bindings, do not delete any tmodels they reference
        //for an update which simply updated URLs, do not delete these tModels either
        final Set<String> tModelsNotToDelete = new HashSet<String>();

        for (Map.Entry<String, BusinessService> entry : uddiPreviouslyPublishedServiceMap.entrySet()) {
            final BusinessService uddiPublishedService = entry.getValue();
            final String uniquePublishedSvcName = entry.getKey();
            //has this service already been published?
            //a BusinessService represents a wsdl:service, if the name is the same reuse the service key
            //a wsdl:service from a wsdl will always produce the same name, see WsdlToUDDIModelConverter
            if (wsdlServicesToPublish.containsKey(uniquePublishedSvcName)) {
                final BusinessService businessService = wsdlServicesToPublish.get(uniquePublishedSvcName);

                final Set<String> tModelsToKeep = synchronizeServices(businessService,
                        uddiPublishedService,
                        allEndpointPairs,
                        tModelsForServiceToPublish.get(uniquePublishedSvcName),
                        uddiPreviouslyTModelsForService.get(uniquePublishedSvcName));

                tModelsNotToDelete.addAll(tModelsToKeep);
                
                if (isOverwriteUpdate) {
                    if (businessService.getBindingTemplates() == null)
                        businessService.setBindingTemplates(new BindingTemplates());
                    if (uddiPublishedService.getBindingTemplates() == null)
                        uddiPublishedService.setBindingTemplates(new BindingTemplates());

                    final Pair<Set<String>, Set<String>> deleteAndKeep =
                            getBindingsToDeleteAndKeep(uddiPublishedService.getBindingTemplates().getBindingTemplate(),
                                    uddiServicesToDependentModels.iterator().next().right);

                    //only keep the bindings which are not soap / http - add them onto the BusinessService created from the WSDL
                    for (BindingTemplate bt : uddiPublishedService.getBindingTemplates().getBindingTemplate()) {
                        if (!deleteAndKeep.right.contains(bt.getBindingKey())) continue;
                        businessService.getBindingTemplates().getBindingTemplate().add(bt);
                        for (TModelInstanceInfo infos : bt.getTModelInstanceDetails().getTModelInstanceInfo()) {
                            tModelsNotToDelete.add(infos.getTModelKey());
                        }
                    }
                }
            } else {
                servicesToDelete.add(uddiPublishedService.getServiceKey());
            }
        }

        final Set<Pair<String, BusinessService>> newlyPublishedServices =
                publishServicesToUDDI(Collections.unmodifiableCollection(wsdlServiceNameToDependentTModels));

        if (isOverwriteUpdate) {
            //obtain the set of bindingTemplates published
            //todo this redownloading could be avoided, we have this information locally
            final List<Pair<BusinessService, Map<String, TModel>>>
                    overwrittenService = serviceDownloader.getBusinessServiceModels(publishedServiceKeys);
            final Pair<BusinessService, Map<String, TModel>> serviceMapPair = overwrittenService.iterator().next();

            final Map<String, Set<String>> bindingAndTModelKeys = UDDIUtilities.getAllBindingAndTModelKeys(serviceMapPair.left);
            for (Map.Entry<String, Set<String>> entry : bindingAndTModelKeys.entrySet()) {
                for (String tModelKey : entry.getValue()) {
                    final TModel model = serviceMapPair.right.get(tModelKey);
                    final UDDIUtilities.TMODEL_TYPE tModelType = UDDIUtilities.getTModelType(model, false);
                    if(tModelType != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                    //we removed all non soap + http bindings already. So any found were added by the gateway
                    if(isSoapAndHttpBinding(model)){
                        publishedBindingTemplates.add(entry.getKey());
                    }
                }
            }
        }

        //NOTE - No UDDI interaction below here should cause a UDDIException to be thrown
        //The publish has been successful. Any delete failures below should just be logged

        //find out what tModels need to be deleted
        //do this after initial update, so we have valid references
        if (!servicesToDelete.isEmpty()) {
            logger.log(Level.FINE, "Attemping to delete BusinessServices no longer referenced by Gateway's WSDL");
            for (String deleteKey : servicesToDelete) {
                try {
                    uddiClient.deleteBusinessServiceByKey(deleteKey);
                } catch (UDDIException e) {
                    logger.log(Level.WARNING, "Problem deleting BusinessServices: " + ExceptionUtils.getMessage(e));
                }
            }
        }

        //Now find what tModels are no longer referenced by any remaining service which already existed in UDDI
        for (Pair<BusinessService, Map<String, TModel>> aServiceToItsModel : uddiServicesToDependentModels) {
            final BusinessService bs = aServiceToItsModel.left;
            //if a service was deleted, so were its tmodels
            if (servicesToDelete.contains(bs.getServiceKey())) continue;

            //we will delete every tModel previously published which may no longer be applicable
            //if we get here it's because the service is still in UDDI
            final Set<String> oldTModels = new HashSet<String>();
            for (TModel tModel : aServiceToItsModel.right.values()) {
                //don't delete any tmodels we left after taking over a service or any tModels whose keys were reused
                if (tModelsNotToDelete.contains(tModel.getTModelKey())) continue;
                oldTModels.add(tModel.getTModelKey());
            }
            if(!oldTModels.isEmpty()){
                try {
                    logger.log(Level.FINE, "Deleting old tmodels no longer referenced by business service with serviceKey: " + bs.getServiceKey());
                    uddiClient.deleteTModel(oldTModels);
                } catch (UDDIException e) {
                    logger.log(Level.WARNING, "Could not delete old tModels from UDDI: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }

        final Set<UDDIBusinessService> newlyCreatedSet = new HashSet<UDDIBusinessService>();
        for (Pair<String, BusinessService> serviceKeyToObject : newlyPublishedServices) {
            final BusinessService bs = serviceKeyToObject.right;
            final String uddiServiceName = bs.getName().get(0).getValue();
            final UDDIBusinessService uddiBs = new UDDIBusinessService(
                    bs.getName().get(0).getValue(),
                    bs.getServiceKey(),
                    serviceToWsdlServiceName.get(uddiServiceName),
                    UDDIUtilities.extractNamespace(bs),
                    UDDIUtilities.getAllBindingAndTModelKeys(bs));

            newlyCreatedSet.add(uddiBs);
        }

        //were any service keys asked for not found? - could happen if something else deletes from UDDI instead of us
        final Set<String> keysDeletedViaUDDI = new HashSet<String>();
        final Set<String> receivedFromUDDISet = new HashSet<String>();
        for (Pair<BusinessService, Map<String, TModel>> aServiceAndModel : uddiServicesToDependentModels) {
            receivedFromUDDISet.add(aServiceAndModel.left.getServiceKey());
        }

        //these are the keys we asked for
        for (String s : publishedServiceKeys) {
            //and these are they keys we got - uddiServicesToDependentModels
            if (!receivedFromUDDISet.contains(s)) {
                logger.log(Level.INFO, "Gateway published BusinessService with serviceKey #("+s+") " +
                        "was deleted from UDDI outside of Gateway's control. Service was republished if applicable.");
                keysDeletedViaUDDI.add(s);
            }
        }
        servicesToDelete.addAll(keysDeletedViaUDDI);

        return new Pair<Set<String>, Set<UDDIBusinessService>>(Collections.unmodifiableSet(servicesToDelete), Collections.unmodifiableSet(newlyCreatedSet));
    }

    /**
     * Get a unique key for a BusinessService. This will return the concatenation of the wsdl:service's local name and
     * namespace, if it is not null. A BusinessService itself may have multiple names, but it should only have a single
     * wsdl localname in it's categoryBag.
     * @param bs BusinessService to generate unique value for
     * @return unique String representing this BusinessService.
     */
    private String getUniqueKeyForService(BusinessService bs) {
        final String nameSpace = UDDIUtilities.extractNamespace(bs);
        final String serviceName = UDDIUtilities.extractWsdlLocalName(bs);
        return (nameSpace == null) ? serviceName : serviceName + "_" + nameSpace;
    }

    /**
     * Synchronize a BusinessService and it's dependent tModels with the same BusinessService and tModels which were
     * previously published.
     * This involves transferring the serviceKeys and tModelKeys from the published BusinessServices and tModels as well
     * as well as categoryBag information. categoryBag information is only synchronized on the BusinessService and is
     * not done for tModels. This could be implemented if required in the future.
     *
     * Following this operation, toPublish BusinessService's categoryBag will contain a union of it's categoryBag and
     * the categoryBag from the same service 'published' from UDDI, with the caveat that any keyNames which differ
     * between keyedReferences will favor the name from UDDI (when the keyedReference is not a general keywords reference).
     *
     * @param toPublish BusinessService being prepared for publishing
     * @param published The same BusinessService, previously published, in it's current state from UDDI
     * @param toPublishEndpoints all endpoints to publish for the service
     * @param toPublishTModels all tModels the service being published depends on
     * @param publishedTModels the tModels already published for this service
     * @return Set&lt;String&gt; each String is a tModelKey, which belongs to a bindingTemplate, so should not be deleted
     * when remove other bindings is configured.
     */
    private Set<String> synchronizeServices(
            final BusinessService toPublish,
            final BusinessService published,
            final Collection<EndpointPair> toPublishEndpoints,
            final Map<String, TModel> toPublishTModels,
            final Map<String, TModel> publishedTModels) {

        toPublish.setServiceKey(published.getServiceKey());
        toPublish.getName().clear();
        toPublish.getName().addAll(published.getName());
        toPublish.getDescription().clear();
        toPublish.getDescription().addAll(published.getDescription());
        
        //maintain any meta data added by 3rd parties
        final CategoryBag publishedCategoryBag = published.getCategoryBag();
        if(publishedCategoryBag != null){//it should never be null as it's always required
            //build set of existing references -
            final CategoryBag toPublishCategoryBag = toPublish.getCategoryBag();//we always add this, never null.
            synchronizeCategoryBags(toPublishCategoryBag, publishedCategoryBag);
        }

        final Set<String> tModelsToKeep = new HashSet<String>();

        final BindingTemplates publishedTemplates = published.getBindingTemplates();
        if (publishedTemplates == null) return tModelsToKeep;
        if (publishedTemplates.getBindingTemplate() == null) return tModelsToKeep;
        if (publishedTemplates.getBindingTemplate().isEmpty()) return tModelsToKeep;

        List<BindingTemplate> toPublishTemplates = toPublish.getBindingTemplates().getBindingTemplate();

        final Set<String> updateKeysSoFar = new HashSet<String>();
        for (BindingTemplate toPublishTemplate : toPublishTemplates) {
            tModelsToKeep.addAll(updateTemplate( toPublishTemplate,
                    publishedTemplates.getBindingTemplate(),
                    toPublishEndpoints,
                    toPublishTModels,
                    publishedTModels,
                    updateKeysSoFar));
        }

        return Collections.unmodifiableSet(tModelsToKeep);
    }

    /**
     * Find the BindingTemplate and it's dependent tModels from the Map applicableTemplates that matches the
     * BindingTemplate toPublishTemplate which is about to be published.
     * <p/>
     *
     * @param applicableTemplates The list of BindingTemplates to search through. It is up to the caller to make sure
     * that these templates contain no duplicates, as the first match wins.
     * @param toPublishTemplate
     * @param toPublishTModels
     * @param publishedTModels
     * @param validateEndpoint if true, a match will only be found if the useType of the AccessPoint is 'endPoint' and
     * if the current URL value has the same scheme (HTTP or HTTPS) as the bindingTemplate being published. This should
     * always be true when looking for an endpoint the gateway published.
     * @return
     */
    private Triple<BindingTemplate, TModel, TModel> findMatchingEndpointAndModels(
            final List<BindingTemplate> applicableTemplates,
            final BindingTemplate toPublishTemplate,
            final Map<String, TModel> toPublishTModels,
            final Map<String, TModel> publishedTModels,
            final boolean validateEndpoint) {
        final TModel toPublishWsdlPortType = getImplementedWsdlPortType(toPublishTemplate, toPublishTModels);
        final Pair<TModel, String> toPublishBindingAndWsdlPort = getImplementedWsdlPortAndBinding(toPublishTemplate, toPublishTModels);

        final String toPublishWsdlPort = toPublishBindingAndWsdlPort.right;
        final String bindingNamespace = UDDIUtilities.extractNamespace(toPublishBindingAndWsdlPort.left);
        final String bindingName = toPublishBindingAndWsdlPort.left.getName().getValue();
        for (BindingTemplate publishedTemplate : applicableTemplates) {
            final Pair<TModel, String> publishedBindingTModelAndWsdlPort = getImplementedWsdlPortAndBinding(publishedTemplate, publishedTModels);
            if (publishedBindingTModelAndWsdlPort == null) continue;

            //ignore non soap + http bindings - important for overwritten services
            if (!isSoapAndHttpBinding(publishedBindingTModelAndWsdlPort.left)) continue;

            final String publishedWsdlPort = publishedBindingTModelAndWsdlPort.right;
            if (publishedWsdlPort == null) continue;

            //check the wsdl:port s are the same
            if (!publishedWsdlPort.equals(toPublishWsdlPort)) continue;

            final String publishedBindingNamespace = UDDIUtilities.extractNamespace(publishedBindingTModelAndWsdlPort.left);
            final String publishedBindingName = publishedBindingTModelAndWsdlPort.left.getName().getValue();

            //check the binding implements the same wsdl:binding - name and namespace
            if (!new Pair<String, String>(bindingNamespace, bindingName).equals(
                    new Pair<String, String>(publishedBindingNamespace, publishedBindingName))) continue;

            //check that the to publish and published bindings implement the same wsdl:portType
            final TModel publishedWsdlPortTModel = getImplementedWsdlPortType(publishedTemplate, publishedTModels);
            if (!toPublishWsdlPortType.getName().getValue().equals(publishedWsdlPortTModel.getName().getValue()))
                continue;

            if(validateEndpoint){
                final String publishedUseType = publishedTemplate.getAccessPoint().getUseType();
                if (publishedUseType == null || !publishedUseType.equals("endPoint")) continue;

                final String publishEndpoint = toPublishTemplate.getAccessPoint().getValue();
                final String publishedEndpoint = publishedTemplate.getAccessPoint().getValue();

                if (EndpointPair.getScheme(publishEndpoint) != EndpointPair.getScheme(publishedEndpoint)) continue;
            }

            //found the bindingTemplate
            return new Triple<BindingTemplate, TModel, TModel>(publishedTemplate,
                    publishedBindingTModelAndWsdlPort.left,
                    publishedWsdlPortTModel);
        }

        return null;
    }

    /**
     * See if a BindingTemplate being published is already published, in which case it's key can be reused.
     * A template matches if it implements the same wsdl:port
     *
     * @param updateKeysSoFar Set&lt;String&gt; all keys used so far. Helps avoid the case where two endpoints in
     * toPublishEndpoints have the same scheme, in which case we could accidentally give two bindingTemplates the same
     * key. First in wins. Right now there is only http + https, but in theory there could be multiple of both
     * @return Set&lt;String&gt; each String is a tModelKey, which was updated, so should not be deleted by the caller
     */
    private Set<String> updateTemplate(final BindingTemplate toPublishTemplate,
                                       final List<BindingTemplate> allPublishedTemplates,
                                       final Collection<EndpointPair> toPublishEndpoints,
                                       final Map<String, TModel> toPublishTModels,
                                       final Map<String, TModel> publishedTModels,
                                       final Set<String> updateKeysSoFar) {

        final TModel toPublishWsdlPortType = getImplementedWsdlPortType(toPublishTemplate, toPublishTModels);

        final Pair<TModel, String> toPublishBindingAndWsdlPort = getImplementedWsdlPortAndBinding(toPublishTemplate, toPublishTModels);
        //Any tModels which get updated will be returned in this set so they do not get deleted
        if (toPublishBindingAndWsdlPort == null) return Collections.emptySet();

        final Triple<BindingTemplate, TModel, TModel> modelTriple =
                findMatchingEndpointAndModels(allPublishedTemplates, toPublishTemplate, toPublishTModels, publishedTModels, true);
        if(modelTriple == null) return Collections.emptySet();

        final BindingTemplate publishedTemplate = modelTriple.left;
        final TModel publishedBindingTModel = modelTriple.middle;
        final TModel publishedPortTypeTModel = modelTriple.right;
        if(publishedTemplate == null) return Collections.emptySet();

        //we have a match, copy keys
        toPublishTemplate.setBindingKey(publishedTemplate.getBindingKey());
        final CategoryBag publishedCategoryBag = publishedTemplate.getCategoryBag();
        if(publishedCategoryBag != null){
            CategoryBag toPublishCategoryBag = toPublishTemplate.getCategoryBag();
            if(toPublishCategoryBag == null){
                toPublishCategoryBag = new CategoryBag();
                toPublishTemplate.setCategoryBag(toPublishCategoryBag);
            }

            synchronizeCategoryBags(toPublishCategoryBag, publishedCategoryBag);
        }
        updateKeysSoFar.add(publishedTemplate.getBindingKey());
        //the toPublishTemplate already has the correct endpoint value

        final String publishEndpoint = toPublishTemplate.getAccessPoint().getValue();

        final Set<String> tModelsToKeep = new HashSet<String>();
        //update the tModels
        for (EndpointPair endpoint : toPublishEndpoints) {
            if(endpoint.getEndPointUrl().equals(publishEndpoint)){
                updateWsdlUrl(toPublishBindingAndWsdlPort.left, endpoint.getWsdlUrl());
                toPublishBindingAndWsdlPort.left.setTModelKey(publishedBindingTModel.getTModelKey());
                tModelsToKeep.add(publishedBindingTModel.getTModelKey());
                updateWsdlUrl(toPublishWsdlPortType, endpoint.getWsdlUrl());
                toPublishWsdlPortType.setTModelKey(publishedPortTypeTModel.getTModelKey());
                tModelsToKeep.add(publishedPortTypeTModel.getTModelKey());
                break;
            }
        }
        return Collections.unmodifiableSet(tModelsToKeep);
    }

    private void updateWsdlUrl(TModel tModel, String wsdlUrl){

        final List<OverviewDoc> overviewDoc = tModel.getOverviewDoc();
        if(overviewDoc == null || overviewDoc.isEmpty()) return; //should never be, but can in theory be null

        for (OverviewDoc doc : overviewDoc) {
            final OverviewURL overviewUrl = doc.getOverviewURL();
            if(overviewUrl.getUseType() == null || !overviewUrl.getUseType().equals("wsdlInterface")) continue;
            overviewUrl.setValue(wsdlUrl);
            break;
        }
    }

    /**
     * Return the implementing wsdl:portType of this bindingTemplate, if found. Null otherwise
     */
    private TModel getImplementedWsdlPortType(BindingTemplate bindingTemplate, Map<String, TModel> applicableTModels){
        final List<TModelInstanceInfo> tModelInstanceInfo = bindingTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
        for (TModelInstanceInfo instanceInfo : tModelInstanceInfo) {
            //this must be found
            final TModel tModel = applicableTModels.get(instanceInfo.getTModelKey());
            final UDDIUtilities.TMODEL_TYPE tModelType = UDDIUtilities.getTModelType(tModel, false);
            if (tModelType != null && tModelType == UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE) {
                return tModel;
            }
        }
        return null;
    }

    /**
     * Return the TModel for the implemented wsdl:binding and the name of the implemented wsdl:port
     */
    private Pair<TModel, String> getImplementedWsdlPortAndBinding(BindingTemplate bindingTemplate, Map<String, TModel> applicableTModels){
        final List<TModelInstanceInfo> tModelInstanceInfo = bindingTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
        for (TModelInstanceInfo instanceInfo : tModelInstanceInfo) {
            //this must be found
            final TModel tModel = applicableTModels.get(instanceInfo.getTModelKey());
            final UDDIUtilities.TMODEL_TYPE tModelType = UDDIUtilities.getTModelType(tModel, false);
            if (tModelType != null && tModelType == UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) {
                return new Pair<TModel, String> (tModel, instanceInfo.getInstanceDetails().getInstanceParms());
            }
        }
        return null;
    }

    /**
     * Publish a collection of BusinessServices to UDDI.
     * <p/>
     * A best effort attempt is made to publish all BusinessServices and their dependent tModels successfully or not at
     * all. If any errors happen individual deletes of any previously published data will be attempted, but it cannot
     * be guaranteed. Data may be orphaned in the UDDI Registry.
     * <p/>
     * Some of the BusinessServices may already exist in UDDI. In this case the BusinessService is being updated. The only
     * property which will remain of the BusinessService in UDDI is it's serviceKey. Everything else will have been
     * recreated.
     *
     * @param serviceNameToDependentTModels Collection of Pairs. Each Pair contains a BusinessService on it's left which
     *                                      will be published to UDDI. The right side is every tModel the BusinessService depends on. tModels are always
     *                                      published first. Each tModel must contain a non null tModelKey property, if it doesn't it will be ignored.
     * @return Set of Pairs, where the left side is the serviceKey of a newly published BusinessService and the right side
     *         is the newly published BusinessService. This set is never null, but can be empty (i.e. only updates were done).
     *         Any BusinessServices which already existsed in UDDI will not be included in this set. Callers can use this returned
     *         value to know which BusinessServices were published to UDDI for the first time.
     * @throws UDDIException any problems updating / querying UDDI
     */
    private Set<Pair<String, BusinessService>> publishServicesToUDDI(
            final Collection<Pair<BusinessService, Map<String, TModel>>> serviceNameToDependentTModels)
            throws UDDIException {

        final Set<Pair<String, BusinessService>> serviceKeysToNewlyPublishedServices = new HashSet<Pair<String, BusinessService>>();

        for (Pair<BusinessService, Map<String, TModel>> serviceAndModels : serviceNameToDependentTModels) {
            Map<String, TModel> dependentTModels = serviceAndModels.right;
            //first publish TModels which represent wsdl:portType, as they have no keyedReference dependencies
            publishDependentTModels(dependentTModels, WSDL_PORT_TYPE, Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices));
            final List<TModel> bindingTModels = new ArrayList<TModel>();
            for (final TModel tModel : dependentTModels.values()) {
                if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                bindingTModels.add(tModel);
            }
            if (bindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");

            UDDIUtilities.updateBindingTModelReferences(bindingTModels, Collections.unmodifiableMap(dependentTModels));
            //next publish TModels which represent wsdl:binding, as they are dependent on wsdl:portType tModels
            publishDependentTModels(dependentTModels, WSDL_BINDING, Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices));
            UDDIUtilities.updateBusinessServiceReferences(serviceAndModels.left, Collections.unmodifiableMap(dependentTModels));

            final Pair<String, BusinessService> servicePair =
                    publishBusinessService(
                            serviceAndModels.left,
                            Collections.unmodifiableCollection(dependentTModels.values()),
                            Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices));
            if (servicePair != null) {
                serviceKeysToNewlyPublishedServices.add(servicePair);
            }
        }

        return Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices);
    }

    /**
     * This will blindly add any meta data found in registrySpecificMetaData to the BusinessService. Make sure any
     * required synchronization of categoryBag data is done before this method is called.
     *
     * @param registrySpecificMetaData
     * @param businessService
     */
    private void addRegistrySpecifcMetaToBusinessService(final UDDIRegistrySpecificMetaData registrySpecificMetaData,
                                                         final BusinessService businessService) {
        if(registrySpecificMetaData == null) return;

        final CategoryBag categoryBag = businessService.getCategoryBag();
        //this 100% can never be null, as we should have correctly added keyed references to it previously
        if(categoryBag == null) throw new IllegalStateException("Attempt to publish a BusinessService with no categoryBag");

        final Collection<UDDIKeyedReference> referenceCollection = registrySpecificMetaData.getBusinessServiceKeyedReferences();
        if(referenceCollection != null){
            for(UDDIKeyedReference kr: referenceCollection){
                final KeyedReference newRef = new KeyedReference();
                newRef.setTModelKey(kr.getTModelKey());
                newRef.setKeyName(kr.getKeyName());
                newRef.setKeyValue(kr.getKeyValue());
                categoryBag.getKeyedReference().add(newRef);
            }
        }

        final Collection<UDDIClient.UDDIKeyedReferenceGroup> keyedReferenceGroups = registrySpecificMetaData.getBusinessServiceKeyedReferenceGroups();
        if(keyedReferenceGroups != null){
            for(UDDIClient.UDDIKeyedReferenceGroup krg: keyedReferenceGroups){
                final KeyedReferenceGroup newGroup = new KeyedReferenceGroup();
                newGroup.setTModelKey(krg.getTModelKey());
                for(UDDIKeyedReference kr: krg.getKeyedReferences()){
                    final KeyedReference newRef = new KeyedReference();
                    newRef.setTModelKey(kr.getTModelKey());
                    newRef.setKeyName(kr.getKeyName());
                    newRef.setKeyValue(kr.getKeyValue());
                    newGroup.getKeyedReference().add(newRef);
                }
                categoryBag.getKeyedReferenceGroup().add(newGroup);
            }
        }
    }

    /**
     * Publish a BusinessService to UDDI. The BusinessService may already have been published to UDDI.
     *
     * @param businessService BusinessService to publish to UDDI. Required.
     * @param rollbackTModelsToDelete Collection of all published tModels for the current publish operation
     * @param allPublishedServicesSoFar Collection of all published BusinessServices so far. The left side of each Pair
     * is not used.
     * @return Pair of String newly created serviceKey to the newly published BusinessService. If this value is null, then
     *         the BusinessService was though to already exist in UDDI due to it already having a non null serviceKey, which was used
     *         as the value when saving the BusinessService.
     * @throws UDDIException any problems updating UDDI
     */
    private Pair<String, BusinessService> publishBusinessService(
            final BusinessService businessService,
            final Collection<TModel> rollbackTModelsToDelete,
            final Set<Pair<String, BusinessService>> allPublishedServicesSoFar)
            throws UDDIException {
        try {
            final boolean newlyPublished = jaxWsUDDIClient.publishBusinessService(businessService);
            if (newlyPublished) {
                return new Pair<String, BusinessService>(businessService.getServiceKey(), businessService);
            }
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing BusinesService: " + e.getMessage());
            handleUDDIRollback(rollbackTModelsToDelete, allPublishedServicesSoFar);
            throw e;
        }

        return null;
    }

    /**
     * Provides best effort to delete any published tmodels and business services from UDDI. Each item is tried
     * individually. It is possible then that some will fail / succeed. It is only a best effort to clean up UDDI.
     *
     * @param rollbackTModelsToDelete Collection of tModels to delete
     * @param allPublishedServicesSoFar Collection of BusinessServices to delete. The left side of each Pair is not used
     */
    private void handleUDDIRollback(
            final Collection<TModel> rollbackTModelsToDelete,
            final Set<Pair<String, BusinessService>> allPublishedServicesSoFar ) {
        logger.log(Level.WARNING, "handleUDDIRollback called. Num services: " + allPublishedServicesSoFar.size()+" num tmodels: " + rollbackTModelsToDelete.size());
        //Deleting a published business service will delete all it's referenced tModels
        if (!allPublishedServicesSoFar.isEmpty()){
            logger.log(Level.WARNING, "Attempting to rollback published BusinessServices");
            for(Pair<String, BusinessService> keyAndObject: allPublishedServicesSoFar){
                if(keyAndObject.right.getServiceKey() != null){
                    try {
                        uddiClient.deleteBusinessServiceByKey(keyAndObject.right.getServiceKey());
                    } catch (UDDIException e) {
                        logger.log(Level.WARNING, "Problems rolling back published Business Services with serviceKey: "
                                + keyAndObject.right.getServiceKey()+", due to: " + ExceptionUtils.getMessage(e));
                    }
                }
            }
        }

        //When allPublishedServicesSoFar is not empty, the tModel collection are only the tmodels for the service
        //which was attempted to be published. If the service failed to publish, then we need to manually delete
        //it's dependent tmodels here
        //otherwise a publish fail happened when publishing tmodels, in which case we need to delete them all if possible
        if (!rollbackTModelsToDelete.isEmpty()) {
            logger.log(Level.WARNING, "Attempting to rollback published tModels");
            for (TModel tModel : rollbackTModelsToDelete) {
                if(tModel.getTModelKey() != null){
                    try {
                        uddiClient.deleteTModel(tModel.getTModelKey());
                    } catch (UDDIException e) {
                        logger.log(Level.WARNING, "Could not rollback published tModel with tModelKey: " + tModel.getTModelKey()+", due to: " + ExceptionUtils.getMessage(e));
                    }
                }
            }
        }
    }

    private static UDDIClient buildUDDIClient( final UDDIClientConfig uddiCfg ) {
        if(uddiCfg == null) throw new NullPointerException("uddiCfg cannot be null");

        UDDIClient uddiClient = UDDIClientFactory.getInstance().newUDDIClient( uddiCfg );
        if(!(uddiClient instanceof JaxWsUDDIClient)){
            uddiClient = new GenericUDDIClient(uddiCfg.getInquiryUrl(), uddiCfg.getPublishUrl(), uddiCfg.getSubscriptionUrl(),
                    uddiCfg.getSecurityUrl(), uddiCfg.getLogin(), uddiCfg.getPassword(),
                    UDDIClientFactory.getDefaultPolicyAttachmentVersion(), uddiCfg.getTlsConfig(), uddiCfg.isCloseSession());
        }

        return uddiClient;
    }

    /**
     * Get all the bindingTemplates which should be kept and removed. Any template which implements a wsdl:binding
     * with soap / http should be returned in the delete set.
     *
     * @param templates           List of BindingTemplates from which to determine which should be removed
     * @param allDependentTModels Every single tmodel keyed by it's tModelKey which the owning BusinessService is
     *                            known to depend on. It is only needed for it's binding tModels, if it only contained that, it would suffice
     * @return Pair of Sets String of bindingKeys to delete and keep. Pair will never be null. Sets can be empty,
     * never null. Left is the set to keep, right is the set to delete
     */
    private Pair<Set<String>, Set<String>> getBindingsToDeleteAndKeep(List<BindingTemplate> templates, Map<String, TModel> allDependentTModels) {

        final Set<String> bindingKeysToDelete = new HashSet<String>();
        for (final BindingTemplate bindingTemplate : templates) {
            //remove all soap/http bindings
            final TModelInstanceDetails modelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
            if (modelInstanceDetails == null) {
                bindingKeysToDelete.add(bindingTemplate.getBindingKey());
                continue;
            }

            for (final TModelInstanceInfo tii : modelInstanceDetails.getTModelInstanceInfo()) {
                final String tModelRefKey = tii.getTModelKey();
                final TModel tModel = allDependentTModels.get(tModelRefKey);
                if (tModel == null)
                    throw new IllegalStateException("Should have all dependent tModels. Missing tModel: " + tModelRefKey);

                final UDDIUtilities.TMODEL_TYPE type = UDDIUtilities.getTModelType(tModel, false);
                switch (type) {
                    case WSDL_BINDING:
                        break;
                    default:
                        //don't know what this tModel is, it's not what we require so move onto next tModelInstanceInfo
                        continue;
                }

                final CategoryBag categoryBag = tModel.getCategoryBag();
                if (categoryBag == null) {
                    //this is a messed up tModel which from the above switch is a wsdl:binding
                    //which means we can't tell what the implementing protocol / transport is - leave it alone
                    //nothing to do, this is for the comment only
                    break;
                }

                boolean protocolIsSoap = UDDIUtilities.isSoapBinding(tModel);
                boolean transportIsHttp = UDDIUtilities.isHttpBinding(tModel);

                if (protocolIsSoap && transportIsHttp) {
                    bindingKeysToDelete.add(bindingTemplate.getBindingKey());
                }
            }
        }

        final Set<String> bindingKeysToKeep = new HashSet<String>();
        for (final BindingTemplate bindingTemplate : templates) {
            if (!bindingKeysToDelete.contains(bindingTemplate.getBindingKey())) {
                bindingKeysToKeep.add(bindingTemplate.getBindingKey());
            }
        }

        if (bindingKeysToKeep.size() + bindingKeysToDelete.size() != templates.size()) {
            throw new IllegalStateException("Invalid calculation of which bindings to keep and which to delete");
        }

        return new Pair<Set<String>, Set<String>>(bindingKeysToDelete, bindingKeysToKeep);
    }

    //Only call with a TModel which is known to represent a wsdl:binding
    private boolean isSoapAndHttpBinding(TModel bindingTModel){
        boolean protocolIsSoap = UDDIUtilities.isSoapBinding(bindingTModel);
        boolean transportIsHttp = UDDIUtilities.isHttpBinding(bindingTModel);

        return protocolIsSoap && transportIsHttp;
    }
    /**
     * Try to delete a tModel published to UDDI. Will not throw a UDDIException
     * @param tModelKey String tModelKey to delete
     */
    private void handleTModelRollback(final String tModelKey){
        try {
            logger.log(Level.FINE, "Attemping to delete tModel published to UDDI with tModelKey: " + tModelKey);
            uddiClient.deleteTModel(tModelKey);
            logger.log(Level.FINE, "Succesfully deleted tModel with tModelKey: " + tModelKey);
        } catch (UDDIException e1) {
            logger.log(Level.WARNING, "Could not rollback published tModel with key " + tModelKey + "to UDDI: " + ExceptionUtils.getMessage(e1));
        }
    }

    private void handleBindingTemplateRollback(final String bindingKey){
        try {
            logger.log(Level.FINE, "Attemping to delete bindingTemplate published to UDDI with bindingKey: " + bindingKey);
            uddiClient.deleteBindingTemplate(bindingKey);
            logger.log(Level.FINE, "Succesfully deleted bindingTemplate with bindingKey: " + bindingKey);
        } catch (UDDIException e1) {
            logger.log(Level.WARNING, "Could not rollback published bindingTemplate with key " + bindingKey + "to UDDI: " + ExceptionUtils.getMessage(e1));
        }
    }

    /**
     * Synchronize toPublishCategoryBag with publishedCategoryBag. Following this operation, toPublishCategoryBag will
     * contain all references it contained before this operation was called (with the note that some keyNames may have
     * been changed), as well as any references which were contained on the publishedCategoryBag which were not already
     * present in toPublishCategoryBag.
     *
     * @param toPublishCategoryBag
     * @param publishedCategoryBag
     */
    private void synchronizeCategoryBags(
            final CategoryBag toPublishCategoryBag,
            final CategoryBag publishedCategoryBag){
        final List<KeyedReference> toPublishReferences = toPublishCategoryBag.getKeyedReference();
        final List<KeyedReference> publishedReferences = publishedCategoryBag.getKeyedReference();

        //synchronize keyNames used in keyReferences
        synchronizeKeyNames(toPublishReferences, publishedReferences);

        final Collection<KeyedReference> keyedRefDiff = getKeyedReferenceDiff(toPublishReferences, publishedReferences);
        toPublishReferences.addAll(keyedRefDiff);

        final List<KeyedReferenceGroup> publishedReferenceGroupList = publishedCategoryBag.getKeyedReferenceGroup();
        if(!publishedReferenceGroupList.isEmpty()){//if it's empty there is nothing to synchronize
            final List<KeyedReferenceGroup> toPublishReferenceGroupList = toPublishCategoryBag.getKeyedReferenceGroup();
            synchronizeKeyedReferenceGroups(toPublishReferenceGroupList, publishedReferenceGroupList);
        }
    }

    private void synchronizeKeyNames(
            final Collection<KeyedReference> toPublishReferences,
            final Collection<KeyedReference> publishedReferences){
        synchronizeKeyNamesBeans(toPublishReferences, convertToBeans(publishedReferences));
    }

    private void synchronizeKeyNamesBeans(
            final Collection<KeyedReference> toPublishReferences,
            final Collection<UDDIKeyedReference> refsToCompareTo){
        final Map<UDDIKeyedReference, KeyedReference> beanToKeyedRef =
                new HashMap<UDDIKeyedReference, KeyedReference>();
        for (KeyedReference toPublishReference : toPublishReferences) {
            final UDDIKeyedReference uddiRef = new UDDIKeyedReference(
                    toPublishReference.getTModelKey(),
                    toPublishReference.getKeyName(),
                    toPublishReference.getKeyValue());
            beanToKeyedRef.put(uddiRef, toPublishReference);
        }

        final Map<UDDIKeyedReference, UDDIKeyedReference> pubRefToPubRef =
                new HashMap<UDDIKeyedReference, UDDIKeyedReference>();
        for (UDDIKeyedReference pubRef : refsToCompareTo) {
            //allow the published reference to be look up with a bean with the same equality as it's self.
            pubRefToPubRef.put(pubRef, pubRef);
        }

        for (UDDIKeyedReference toPubRef : beanToKeyedRef.keySet()) {
            if(!toPubRef.getTModelKey().equals(UDDIKeyedReference.GENERAL_KEYWORDS) && refsToCompareTo.contains(toPubRef)){
                //match, are the names the same?
                final UDDIKeyedReference publishedRef = pubRefToPubRef.get(toPubRef);//the equality is the same as the name is not part of equals
                final String publishedRefKeyName = publishedRef.getKeyName();
                final KeyedReference keyedReference = beanToKeyedRef.get(toPubRef);
                if(publishedRefKeyName != null){
                    if(!(publishedRefKeyName.equals(toPubRef.getKeyName()))){
                        keyedReference.setKeyName(publishedRef.getKeyName());
                    }
                } else {
                    //if it's null in UDDI, then keep it null
                    keyedReference.setKeyName(null);
                }
            }
        }
    }

    private List<KeyedReference> convertFromBeans(Collection<UDDIKeyedReference> references){
        final List<KeyedReference> returnList = new ArrayList<KeyedReference>();

        for (UDDIKeyedReference reference : references) {
            final KeyedReference newRef = new KeyedReference();
            newRef.setTModelKey(reference.getTModelKey());
            newRef.setKeyValue(reference.getKeyValue());
            newRef.setKeyName(reference.getKeyName());
            returnList.add(newRef);
        }

        return returnList;
    }

    private List<UDDIKeyedReference> convertToBeans(Collection<KeyedReference> references){
        final List<UDDIKeyedReference> refBeans = new ArrayList<UDDIKeyedReference>();
        for (KeyedReference toPublishReference : references) {
            refBeans.add(new UDDIKeyedReference(
                    toPublishReference.getTModelKey(),
                    toPublishReference.getKeyName(),
                    toPublishReference.getKeyValue()));
        }

        return refBeans;
    }

    private void synchronizeKeyedReferenceGroups(
            final Collection<KeyedReferenceGroup> toPublishGroups,
            final Collection<KeyedReferenceGroup> publishedGroups) {

        final Map<String, KeyedReferenceGroup> toPublishKeyToGroup = new HashMap<String, KeyedReferenceGroup>();
        for (KeyedReferenceGroup toPublishGroup : toPublishGroups) {
            toPublishKeyToGroup.put(toPublishGroup.getTModelKey(), toPublishGroup);
        }

        for (KeyedReferenceGroup publishedGroup : publishedGroups) {
            if(!toPublishKeyToGroup.containsKey(publishedGroup.getTModelKey())){
                final KeyedReferenceGroup newGroup = new KeyedReferenceGroup();
                newGroup.setTModelKey(publishedGroup.getTModelKey());
                final List<KeyedReference> keyedRefs = new ArrayList<KeyedReference>();
                for (KeyedReference keyedReference : publishedGroup.getKeyedReference()) {
                    final KeyedReference newRef = new KeyedReference();
                    newRef.setTModelKey(keyedReference.getTModelKey());
                    newRef.setKeyValue(keyedReference.getKeyValue());
                    newRef.setKeyName(keyedReference.getKeyName());
                    keyedRefs.add(newRef);
                }
                newGroup.getKeyedReference().addAll(keyedRefs);
                toPublishGroups.add(newGroup);
            } else {
                //we need to sync the keyed references
                final KeyedReferenceGroup toPublishGroup = toPublishKeyToGroup.get(publishedGroup.getTModelKey());
                final Collection<KeyedReference> refDiff =
                        getKeyedReferenceDiff(toPublishGroup.getKeyedReference(), publishedGroup.getKeyedReference());
                toPublishGroup.getKeyedReference().addAll(refDiff);
            }
        }
    }

    /**
     * Get the list of KeyedReferences from one List which are not contained in the other.
     * <p/>
     * From the List of published KeyedReferences (added by the SSG and possibly 3rd parties), get the list of
     * references which should be published. The returned list has the following characteristics:
     * <ul>
     * <li>Returned list is the List of keyedReferences which exist in publishedReferences but not in toPublishReferences</li>
     * <li>keyNames are not part of any comparison, unless the tModelKey is 'uddi:uddi.org:categorization:general_keywords'</li>
     * <li>keyNames, when applicable, are compared in a case sensitive manner</li>
     * <li>keyValues are compared in a case sensitive manner</li>
     * </ul>
     *
     * @param toPublishReferences List of references the gateway will publish.
     * @param publishedReferences List of references from UDDI. Find the references which are here and not in
     *                            toPublishReferences
     * @return the list of KeyedReferences to publish.
     */
    private Collection<KeyedReference> getKeyedReferenceDiff(
            final List<KeyedReference> toPublishReferences,
            final List<KeyedReference> publishedReferences) {

        final List<UDDIKeyedReference> awaitingPublication = convertToBeans(toPublishReferences);

        final List<UDDIKeyedReference> published = convertToBeans(publishedReferences);

        //add missing references from the gateway
        final List<UDDIKeyedReference> diffRefs = getKeyedReferenceDiff(awaitingPublication, published);
        final List<KeyedReference> diffKeyedRefs = new ArrayList<KeyedReference>();
        for (UDDIKeyedReference diffRef : diffRefs) {
            final KeyedReference keyedReference = new KeyedReference();
            keyedReference.setTModelKey(diffRef.getTModelKey());
            keyedReference.setKeyValue(diffRef.getKeyValue());
            keyedReference.setKeyName(diffRef.getKeyName());
            diffKeyedRefs.add(keyedReference);
        }

        return diffKeyedRefs;
    }

    /**
     * Get the collection of UDDI.UDDIKeyedReferences which are missing from toPublish
     * @param toPublish Collection of references to publish, this collection represents what the ssg knows about and
     * wants to publish.
     * @param published
     * @return a collection of UDDI.UDDIKeyedReference which contains all references from toPublish and any references
     * found in publishedKeys which do not exist in toPublish.
     */
    private List<UDDIKeyedReference> getKeyedReferenceDiff(
            Collection<UDDIKeyedReference> toPublish,
            Collection<UDDIKeyedReference> published){

        final List<UDDIKeyedReference> diffList = new ArrayList<UDDIKeyedReference>();

        for (UDDIKeyedReference publishedRef : published) {
            if(!toPublish.contains(publishedRef)){
                diffList.add(publishedRef);
            }
        }

        return diffList;
    }

    private Set<UDDIKeyedReference> getProxyGifMetaData(EndpointPair endpointPair, String mgmtSystemKeyValue) {
        final Set<UDDIKeyedReference> keyedReferences = new HashSet<UDDIKeyedReference>();
        final UDDIKeyedReference managedEndpoint = new UDDIKeyedReference();
        managedEndpoint.setTModelKey(UDDI_SYSTINET_COM_MANAGEMENT_TYPE);
        managedEndpoint.setKeyName("Management entity type");
        managedEndpoint.setKeyValue(MANAGED_ENDPOINT);
        keyedReferences.add(managedEndpoint);

        final UDDIKeyedReference managementSystem = new UDDIKeyedReference();
        managementSystem.setTModelKey(UDDI_SYSTINET_COM_MANAGEMENT_SYSTEM);
        managementSystem.setKeyName("Management System");
        managementSystem.setKeyValue(mgmtSystemKeyValue);
        keyedReferences.add(managementSystem);

        final UDDIKeyedReference managementState = new UDDIKeyedReference();
        managementState.setTModelKey(UDDI_SYSTINET_COM_MANAGEMENT_STATE);
        managementState.setKeyName("Governance state");
        managementState.setKeyValue(SYSTINET_STATE_MANAGED);
        keyedReferences.add(managementState);

        final UDDIKeyedReference endPoint = new UDDIKeyedReference();
        endPoint.setTModelKey(UDDI_SYSTINET_COM_MANAGEMENT_URL);
        endPoint.setKeyName("URL from AccessPoint");
        endPoint.setKeyValue(endpointPair.getEndPointUrl());
        keyedReferences.add(endPoint);
        return keyedReferences;
    }

    private List<KeyedReference> getFunctionalEndPointMetaData(final String publishedBindingKey){
        KeyedReference funcEndpoint = new KeyedReference();
        funcEndpoint.setTModelKey(UDDI_SYSTINET_COM_MANAGEMENT_TYPE);
        funcEndpoint.setKeyName("Management entity type");
        funcEndpoint.setKeyValue(FUNCTIONAL_ENDPOINT);

        KeyedReference proxyRef = new KeyedReference();
        proxyRef.setTModelKey(UDDI_SYSTINET_COM_MANAGEMENT_PROXY_REFERENCE);
        proxyRef.setKeyName("Proxy reference");
        proxyRef.setKeyValue(publishedBindingKey);

        return Arrays.asList(funcEndpoint, proxyRef);
    }

    /**
     * Manage the keyedReferences published by the Gateway. After this operation any references on toPublishTemplate
     * which were previously published by the gateway but were removed via user configuration, will be removed.
     * In addition, if any keyName's were changed in UDDI, then those key names will be maintained. If any keyNames
     * were changed by user configuration, then those keyName changes will be persisted in UDDI (unless UDDI changed
     * the same keyName, which takes precedence).
     * 
     * @param configKeyedReferences user configuration of KeyedReferences to publish
     * @param runtimeKeyedReferences previously published KeyedReferences
     * @param toPublishTemplate BindingTemplate to publish
     */
    private void manageKeyedReferences(final Set<UDDIKeyedReference> configKeyedReferences,
                                       final Set<UDDIKeyedReference> runtimeKeyedReferences,
                                       final BindingTemplate toPublishTemplate) {
        final CategoryBag categoryBag = toPublishTemplate.getCategoryBag();
        if(categoryBag == null) return;

        final List<KeyedReference> toPubRefs = categoryBag.getKeyedReference();
        final List<UDDIKeyedReference> refBeans = convertToBeans(toPubRefs);
        final Map<UDDIKeyedReference, UDDIKeyedReference> refBeansToRefBeans =
                new HashMap<UDDIKeyedReference, UDDIKeyedReference>();
        for (UDDIKeyedReference refBean : refBeans) {
            refBeansToRefBeans.put(refBean, refBean);
        }
        final Map<UDDIKeyedReference, UDDIKeyedReference> configToConfigRefs =
                new HashMap<UDDIKeyedReference, UDDIKeyedReference>();
        if(configKeyedReferences != null){
            for (UDDIKeyedReference configRef : configKeyedReferences) {
                configToConfigRefs.put(configRef, configRef);
            }
        }
        for (UDDIKeyedReference runtimeRef : runtimeKeyedReferences) {
            if (configKeyedReferences == null || !configKeyedReferences.contains(runtimeRef)) {
                refBeans.remove(runtimeRef);
            } else {
                //Has the keyName of one of our references changed?
                final UDDIKeyedReference configRef = configToConfigRefs.get(runtimeRef);
                final boolean namesDifferent = UDDIUtilities.areNamesDifferent(configRef.getKeyName(), runtimeRef.getKeyName());
                if(namesDifferent){
                    //are the names different because of a user modification or a UDDI modification? UDDI takes precedence over user modifications.
                    final UDDIKeyedReference toPubRef = refBeansToRefBeans.get(runtimeRef);
                    //if the topublish is the same as the runtime ref, then UDDI holds the same value as previously published
                    final boolean different = UDDIUtilities.areNamesDifferent(toPubRef.getKeyName(), runtimeRef.getKeyName());
                    if(!different){
                        //user modification, not UDDI, so ensure it's kept
                        toPubRef.setKeyName(configRef.getKeyName());
                    }
                }

            }
        }
        toPubRefs.clear();
        toPubRefs.addAll(convertFromBeans(refBeans));
    }
}

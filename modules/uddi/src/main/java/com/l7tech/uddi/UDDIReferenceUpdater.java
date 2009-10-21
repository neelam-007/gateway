package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.uddi.WsdlToUDDIModelConverter;

import java.util.List;
import java.util.Map;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * <p/>
 * Update a BusinessService to contain references to valid tModels which represent the wsdl:binding and wsdl:portType
 *
 * @author darmstrong
 */
public class UDDIReferenceUpdater {

    public enum TMODEL_TYPE {
        WSDL_PORT_TYPE, WSDL_BINDING
    }

    /**
     * Update all placeholder tModelKey's instead of keyedReferences in the list of BusinessServices. This applies
     * to the bindingTemplates they contain, and not any category bag of the Business Services themselves
     *
     * @param businessServices the list of BusinessService s to update
     * @param dependentTModels the list of dependent TModel s which the BusinessServices depend on. All have valid
     *                         tModelKeys from the correct UDDIRegistry
     */
    public static void updateBusinessServiceReferences(final List<BusinessService> businessServices,
                                                       final Map<String, TModel> dependentTModels) {

        for (BusinessService businessService : businessServices) {
            //the business service itself has no references to update
            BindingTemplates bindingTemplates = businessService.getBindingTemplates();
            List<BindingTemplate> allTemplates = bindingTemplates.getBindingTemplate();
            for (BindingTemplate bindingTemplate : allTemplates) {
                TModelInstanceDetails tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
                List<TModelInstanceInfo> tModelInstanceInfos = tModelInstanceDetails.getTModelInstanceInfo();
                for (TModelInstanceInfo tModelInstanceInfo : tModelInstanceInfos) {
                    final TModel tModel = dependentTModels.get(tModelInstanceInfo.getTModelKey());
                    tModelInstanceInfo.setTModelKey(tModel.getTModelKey());
                }
            }
        }
    }

    /**
     * TModels which represent wsdl:binding elements, have a dependency on the TModel which represent
     * wsdl:portType.
     * <p/>
     * Update all binding TModels with the correct tModelKey of their dependent portType tModel.
     *
     * @param bindingTModels   List of TModels to udpate. Each tModel must represent a binding tModel
     * @param dependentTModels The Map of all TModels for WSDL being published. This contains the wsdl:portType which
     *                         must have valid keys by the time this method is called
     */
    public static void updateBindingTModelReferences(final List<TModel> bindingTModels,
                                                     final Map<String, TModel> dependentTModels) {
        for (TModel tModel : bindingTModels) {
            if (getTModelType(tModel) != TMODEL_TYPE.WSDL_BINDING) continue;

            final CategoryBag categoryBag = tModel.getCategoryBag();
            List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
            //find the portType keyedReference
            for (KeyedReference keyedReference : keyedReferences) {
                if (!keyedReference.getTModelKey().equals(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE)) continue;
                final TModel publishedTModel = dependentTModels.get(keyedReference.getKeyValue());
                keyedReference.setKeyValue(publishedTModel.getTModelKey());
            }
        }
    }

    /**
     * Work out what wsdl element the tModel represents. Either wsdl:portType or wsdl:binding
     *
     * @param tModel the TModel to get the type for
     * @return TMODEL_TYPE the type of TModel
     */
    public static TMODEL_TYPE getTModelType(final TModel tModel) {
        final CategoryBag categoryBag = tModel.getCategoryBag();
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
        for (KeyedReference keyedReference : keyedReferences) {
            if (!keyedReference.getTModelKey().equals(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES)) continue;
            final String keyValue = keyedReference.getKeyValue();
            if (keyValue.equals("portType")) return TMODEL_TYPE.WSDL_PORT_TYPE;
            if (keyValue.equals("binding")) return TMODEL_TYPE.WSDL_BINDING;
            throw new IllegalStateException("Type of TModel does not follow UDDI Technical Note: '" + keyValue + "'");
        }
        throw new IllegalStateException("TModel did not contain a KeyedReference of type " + WsdlToUDDIModelConverter.UDDI_WSDL_TYPES);
    }

}


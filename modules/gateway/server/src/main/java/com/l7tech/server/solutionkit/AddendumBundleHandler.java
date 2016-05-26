package com.l7tech.server.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitsConfig;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.l7tech.gateway.common.solutionkit.SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY;
import static com.l7tech.gateway.common.solutionkit.SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE;
import static java.lang.System.lineSeparator;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.status;

/**
 * Addendum Bundle: this is an undocumented "dark feature" that's not QA'd.
 *
 * Prerequisites:
 *      - Requires that only one Solution Kit is in scope.  If using a collection of Solution Kits, select only one child Solution Kit for the request (e.g. specify a GUID in form-field name solutionKitSelect).
 *      - If Solution Kit supports upgrade, a separate upgrade addendum bundle is required (e.g. mapping action="NewOrUpdate"instead of "AlwaysCreateNew")
 *      - The .skar file's bundle requires the <l7:Mappings> element (note the ending "s").
 *      - <l7:AllowAddendum> must be true in .skar file metadata (i.e. SolutionKit.xml)
 *      - The .skar file mapping(s) to change must set "SK_AllowMappingOverride" property to true
 *
 * Algorithm:
 *      Loop through addendum bundle mappings
 *          if skar bundle has mapping AND has override flag true
 *              replace bundle mapping with addendum bundle mapping
 *          else
 *          throw error (400 Bad Request or 403 Forbidden)
 *
 *      if addendum bundle has reference item matching it's mapping srdId
 *          if skar bundle has reference item with same id
 *              replace skar bundle's reference item with addendum bundle reference item
 *          else
 *              add addendum bundle reference item
 */
public class AddendumBundleHandler {
    private final FormDataMultiPart formDataMultiPart;
    private final SolutionKitsConfig solutionKitsConfig;
    private final String bundleFormFieldName;

    public AddendumBundleHandler(@NotNull final FormDataMultiPart formDataMultiPart, @NotNull final SolutionKitsConfig solutionKitsConfig, @NotNull final String bundleFormFieldName) {
        this.formDataMultiPart = formDataMultiPart;
        this.solutionKitsConfig = solutionKitsConfig;
        this.bundleFormFieldName = bundleFormFieldName;
    }

    public void apply() throws IOException, SAXException, AddendumBundleException {
        final Set<SolutionKit> selectedSolutionKits = solutionKitsConfig.getSelectedSolutionKits();
        final FormDataBodyPart addendumPart = formDataMultiPart.getField(bundleFormFieldName);
        if (addendumPart != null) {
            if (selectedSolutionKits.size() > 1) {
                throw new AddendumBundleException(status(BAD_REQUEST).entity("Can't have more than one Solution Kit in scope when using form field named '" +
                        bundleFormFieldName + "'. If using a collection of Solution Kits, select only one child Solution Kit for the request." + lineSeparator()).build());
            }

            // future: handle collection of skars using field name "bundle" + <solution_kit_guid>
            SolutionKit solutionKit = selectedSolutionKits.iterator().next();

            // check if metadata allows Add flag to solution kit meta to allow author to control support for this dynamic bundle (e.g. allow or not allow dynamic bundle)
            final Boolean allowAddendum = Boolean.valueOf(solutionKit.getProperty(SK_PROP_ALLOW_ADDENDUM_KEY));
            if (!allowAddendum) {
                throw new AddendumBundleException(status(FORBIDDEN).entity("The selected .skar file does not allow addendum bundle.  Form field named '" + bundleFormFieldName +
                        "' not allow unless the .skar file author has metadata element '" + SK_PROP_ALLOW_ADDENDUM_KEY + "' to true." + lineSeparator()).build());
            }

            final InputStream addendumInputStream = addendumPart.getValueAs(InputStream.class);
            final DOMSource addendumBundleSource = new DOMSource();
            final Document addendumBundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(addendumInputStream)));
            final Element addendumBundleEle = addendumBundleDoc.getDocumentElement();
            addendumBundleSource.setNode(addendumBundleEle);
            final Bundle addendumBundle = MarshallingUtils.unmarshal(Bundle.class, addendumBundleSource, true);

            if (addendumBundle == null || addendumBundle.getMappings() == null) {
                throw new AddendumBundleException(status(BAD_REQUEST).entity(
                        "The addendum bundle specified using form field named '" + bundleFormFieldName + "' can't be null (nor have null mappings)." + lineSeparator()).build());
            }

            // get the skar bundle
            final Bundle bundle = solutionKitsConfig.getBundle(solutionKit);
            if (bundle == null || bundle.getReferences() == null || bundle.getMappings() == null) {
                // skar bundle null shouldn't happen; just in case
                throw new AddendumBundleException(status(BAD_REQUEST).entity("The .skar file bundle can't be null (nor have null references, nor null mappings) " +
                        "when addendum bundle has been specified (i.e. form field named '" + bundleFormFieldName + "' was provided)." + lineSeparator()).build());
            }

            // use skar bundle to build a map of srcId->mapping and map of id->reference
            final List<Item> referenceItems = bundle.getReferences();
            final Map<String, Item> idReferenceItemMap = new HashMap<>(referenceItems.size());
            for (Item referenceItem : referenceItems) {
                idReferenceItemMap.put(referenceItem.getId(), referenceItem);
            }
            final List<Mapping> mappings = bundle.getMappings();
            final Map<String, Mapping> srcIdMappingMap = new HashMap<>(mappings.size());
            for (Mapping mapping : mappings) {
                srcIdMappingMap.put(mapping.getSrcId(), mapping);
            }

            // use addendum bundle to build a map of id->reference
            final Map<String, Item> addendumIdReferenceMap = new HashMap<>(addendumBundle.getReferences().size());
            for (Item referenceItem : addendumBundle.getReferences()) {
                addendumIdReferenceMap.put(referenceItem.getId(), referenceItem);
            }

            // make decisions on addendum bundle mappings
            for (Mapping addendumMapping : addendumBundle.getMappings()) {

                // check if skar bundle has mapping AND has override flag true
                Mapping mapping = srcIdMappingMap.get(addendumMapping.getSrcId());
                if (mapping != null) {
                    if (!SolutionKitsConfig.allowOverride(mapping)) {
                        throw new AddendumBundleException(status(FORBIDDEN).entity("Unable to process addendum bundle for mapping with scrId=" + addendumMapping.getSrcId() +
                                ".  This requires the .skar file author to set mapping property '" + MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' to true." + lineSeparator()).build());
                    }

                    // replace skar bundle mapping with addendum bundle mapping
                    mappings.set(mappings.indexOf(mapping), addendumMapping);

                    // set addendum mapping targetId for any previously resolved ids (e.g. instance modified, user configured)
                    Pair<SolutionKit, Map<String, String>> previouslyResolvedIds = solutionKitsConfig.getPreviouslyResolvedEntityIds().get(solutionKit.getSolutionKitGuid());
                    if (previouslyResolvedIds != null) {
                        String resolvedId = previouslyResolvedIds.right.get(addendumMapping.getSrcId());
                        if (resolvedId != null) {
                            addendumMapping.setTargetId(resolvedId);
                        }
                    }

                    // check if addendum bundle has reference item with srdId
                    Item addendumReferenceItem = addendumIdReferenceMap.get(addendumMapping.getSrcId());
                    if (addendumReferenceItem != null) {
                        // check if skar bundle has reference item with same id
                        Item referenceItem = idReferenceItemMap.get(addendumReferenceItem.getId());
                        if (referenceItem != null) {
                            // replace skar bundle's reference item with addendum bundle reference item
                            referenceItems.set(referenceItems.indexOf(referenceItem), addendumReferenceItem);
                        } else {
                            // add addendum bundle reference item
                            referenceItems.add(addendumReferenceItem);
                        }
                    }
                }
            }
        }
    }

    public class AddendumBundleException extends Exception {
        @NotNull
        private Response response;

        public AddendumBundleException(@NotNull final Response response) {
            super();
            this.response = response;
        }

        @NotNull
        public Response getResponse() {
            return response;
        }
    }
}

package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.bundle.BundleUtils;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.PolicyBundleInstallerContext;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.FOLDER;
import static com.l7tech.server.policy.bundle.BundleResolver.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getEntityName;
import static com.l7tech.server.policy.bundle.ssgman.wsman.WsmanInvoker.CREATE_ENTITY_XML;
import static com.l7tech.server.policy.bundle.ssgman.wsman.WsmanInvoker.GATEWAY_MGMT_ENUMERATE_FILTER;

/**
 * Install folder.
 */
public class FolderInstaller extends WsmanInstaller {
    public static final String FOLDER_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/folders";

    public FolderInstaller(@NotNull final PolicyBundleInstallerContext context,
                           @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                           @NotNull final GatewayManagementInvoker gatewayManagementInvoker) {
        super(context, cancelledCallback, gatewayManagementInvoker);
    }

    public Map<String, Goid> install() throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, PolicyBundleInstaller.InstallationException, GatewayManagementDocumentUtilities.UnexpectedManagementResponse, AccessDeniedManagementResponse {
        checkInterrupted();

        final Document folderBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), FOLDER, true);
        if (folderBundle == null) {
            return Collections.emptyMap();
        } else {
            final Goid folderToInstallInto = context.getFolderGoid();
            return installFolders(folderToInstallInto, folderBundle);
        }
    }

    /**
     * Folders will be created if needed. If folders already exist in the parent folder then they will not be modified.
     * Incoming XML will contain ids for folders, however these are ignored by the management api and new ids will be
     * assigned.
     *
     * @param parentFolderGoid       oid of the parent folder to install into.
     * @param folderMgmtEnumeration Gateway management enumeration document of folders to install
     * @return map of all folders ids to ids in the folderMgmtEnumeration. The key is always the folders canned id, the id in the
     *         folderMgmtEnumeration, the value will either be a new id if the folder was created or it will be the id of the existing
     *         folder on the target gateway.
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse if the admin user does not contain the permission to create any required folder
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse   if the gateway mgmt assertion returns an unexpected error status
     * @throws InterruptedException           if the installation is cancelled
     */
    private Map<String, Goid> installFolders(final Goid parentFolderGoid, @NotNull final Document folderMgmtEnumeration)
            throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse,
            InterruptedException,
            GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {

        final List<Element> folderElms = GatewayManagementDocumentUtilities.getEntityElements(folderMgmtEnumeration.getDocumentElement(), "Folder");

        // find the root node
        Element rootFolder = null;
        for (Element folderElm : folderElms) {
            final String idAttr = folderElm.getAttribute("id");
            if (idAttr.equals("-5002") || idAttr.equals(new Goid(0,-5002L).toHexString())) {
                rootFolder = folderElm;
            }
        }

        final Stack<Element> toProcess = new Stack<>();
        toProcess.push(rootFolder);

        final Map<String, Goid> oldToNewIds = new HashMap<>();
        // parent folder goid may already be -5002 when installing to the root node
        oldToNewIds.put("-5002", parentFolderGoid);

        while (!toProcess.isEmpty()) {

            checkInterrupted();

            final Element currentElm = toProcess.pop();
            final String id = currentElm.getAttribute("id");
            final String folderId = currentElm.getAttribute("folderId");

            // add all children which have currentElm as their parent
            for (Element folderElm : folderElms) {
                final String parentId = folderElm.getAttribute("folderId");
                if (parentId.equals(id)) {
                    toProcess.push(folderElm);
                }
            }

            if (id.equals("-5002") || id.equals(new Goid(0,-5002L).toHexString())) {
                // the root node will always already exist
                continue;
            }

            final Goid newParentId = oldToNewIds.get(folderId);
            if (newParentId == null) {
                throw new RuntimeException("Parent folder " + folderId + " for folder " + id + " not found. Input Folder XML must be corrupt.");
            }

            final Document document = XmlUtil.createEmptyDocument("Folder", "l7", BundleUtils.L7_NS_GW_MGMT);
            final Element folder = document.getDocumentElement();
            folder.setAttribute("folderId", String.valueOf(newParentId));
            folder.setAttribute("id", id); //this gets overwritten and ignored by mgmt assertion.

            final Element name = DomUtils.createAndAppendElementNS(folder, "Name", BundleUtils.L7_NS_GW_MGMT, "l7");
            String folderName = getEntityName(currentElm);

            // suffix allow folders from specific installers to display together.
            // e.g. otk1, otk2, sfdc1, sfdc2, simple1, simple2 vs 1otk, 1sfdc, 1simple, 2otk, 2sfdc, 2simple
            String folderSuffix = context.getInstallationPrefix();
            if (isValidVersionModifier(folderSuffix)) {
                folderName = getSuffixedFolderName(folderSuffix, folderName);
            }

            final Text nameText = document.createTextNode(folderName);
            name.appendChild(nameText);

            final String folderXmlTemplate;
            try {
                folderXmlTemplate = XmlUtil.nodeToString(document.getDocumentElement());
            } catch (IOException e) {
                throw new RuntimeException("Unexpected exception serializing template Folder XML", e);
            }
            final String createFolderXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), FOLDER_MGMT_NS, folderXmlTemplate);

            final Pair<AssertionStatus, Document> pair = callManagementCheckInterrupted(createFolderXml);
            final Goid newId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
            final Goid idToRecord;
            if (newId == null) {
                if (GatewayManagementDocumentUtilities.resourceAlreadyExists(pair.right)) {
                    idToRecord = getExistingFolderId(newParentId, folderName);
                } else {
                    idToRecord = null;
                }
            } else {
                idToRecord = newId;
            }

            if (idToRecord == null) {
                throw new RuntimeException("Could not create or find id for xml folder with bundle id: " + id);
            }

            oldToNewIds.put(id, idToRecord);
        }

        return oldToNewIds;
    }

    @Nullable
    private Goid getExistingFolderId(Goid parentId, String folderName)
            throws GatewayManagementDocumentUtilities.UnexpectedManagementResponse, InterruptedException, GatewayManagementDocumentUtilities.AccessDeniedManagementResponse {
        final String folderFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(),
                FOLDER_MGMT_NS, 10, "/l7:Folder[@folderId='" + parentId + "']/l7:Name[text()='" + folderName + "']");

        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(folderFilter);
        return GatewayManagementDocumentUtilities.getCreatedId(documentPair.right);
    }
}

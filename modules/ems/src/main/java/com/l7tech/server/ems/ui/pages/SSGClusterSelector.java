package com.l7tech.server.ems.ui.pages;

import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.FailoverException;
import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.server.ems.user.UserPropertyManager;
import org.apache.wicket.spring.injection.annot.SpringBean;

import javax.xml.ws.soap.SOAPFaultException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
@RequiredPermissionSet()
public class SSGClusterSelector extends EsmBaseWebPage {
    private static final Logger logger = Logger.getLogger(SSGClusterSelector.class.getName());

    @SpringBean
    private EnterpriseFolderManager enterpriseFolderManager;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private EsmSecurityManager securityManager;

    @SpringBean
    private UserPropertyManager userPropertyManager;

    public SSGClusterSelector() {
        Map<String,String> up = Collections.emptyMap();
        try {
            up = userPropertyManager.getUserProperties( getUser() );
        } catch ( FindException fe ){
            logger.log( Level.WARNING, "Error loading user properties", fe );
        }
        final Map<String,String> userProperties = up;

        JsonInteraction interaction = new JsonInteraction("actiondiv", "ssgClusterJsonUrl", new JsonDataProvider(){
            @Override
            public Object getData() {
                try {
                    final List<Object> entities = new ArrayList<Object>();
                    final EnterpriseFolder rootFolder = enterpriseFolderManager.findRootFolder();
                    entities.add(new JSONSupport( rootFolder ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_ENTERPRISE_FOLDER, rootFolder)) );
                        }
                    } );
                    addChildren(entities, rootFolder);
                    return entities;
                } catch (SOAPFaultException e) {
                    if ( GatewayContext.isNetworkException(e) ) {
                        return new JSONException("Gateway not available.");
                    } else if ( GatewayContext.isConfigurationException(e) ) {
                        return new JSONException(e.getMessage());
                    } else {
                        logger.warning(e.toString());
                        return new JSONException(e);
                    }
                } catch (FailoverException fo) {
                    return new JSONException(fo);
                } catch (FindException fe) {
                    logger.log( Level.WARNING, "Error loading enterprise folders.", fe );
                    return new JSONException(new Exception("Error loading folders.", fe));
                }
            }

            @Override
            public void setData(Object jsonData) {
                throw new UnsupportedOperationException("setData not required in JsonInteraction");
            }

            private void addChildren(final List<Object> nodes, final EnterpriseFolder folder) throws FindException {
                // Display order is alphabetical on name, with folders before clusters.
                for ( final EnterpriseFolder childFolder : enterpriseFolderManager.findChildFolders(folder)) {
                    nodes.add( new JSONSupport( childFolder ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_ENTERPRISE_FOLDER, childFolder)) );
                        }
                    }  );
                    addChildren(nodes, childFolder);
                }

                for ( final SsgCluster childCluster : ssgClusterManager.findChildSsgClusters(folder) ) {
                    nodes.add( new JSONSupport( childCluster ){
                        @Override
                        protected void writeJson() {
                            super.writeJson();
                            add(JSONConstants.RBAC_CUD, securityManager.hasPermission( new AttemptedDeleteSpecific(EntityType.ESM_SSG_CLUSTER, childCluster)) );
                            add(JSONConstants.ACCESS_STATUS, userProperties.containsKey("cluster." +  childCluster.getGuid() + ".trusteduser"));
                        }
                    });
                }
            }
        });

        add(interaction);
    }
}

package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.ems.enterprise.*;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.soap.SOAPFaultException;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Nov 26, 2008
 */
@RequiredPermissionSet()
public class SSGClusterSelector extends WebPage {
    private static final Logger logger = Logger.getLogger(SSGClusterSelector.class.getName());

    @SpringBean
    private EnterpriseFolderManager enterpriseFolderManager;

    @SpringBean
    private SsgClusterManager ssgClusterManager;

    @SpringBean
    private EmsSecurityManager securityManager;

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

        JsonInteraction interaction = new JsonInteraction("actiondiv", "ssgClusterJasonUrl", new JsonDataProvider(){
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
                        return new JSONException( new Exception("Gateway not available.") );
                    } else {
                        logger.warning(e.toString());
                        return new JSONException(e);
                    }
                } catch (FindException e) {
                    logger.warning(e.toString());
                    return new JSONException(e);
                }
            }

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

    private EmsSecurityManager.LoginInfo getLoginInfo() {
        EmsSecurityManager.LoginInfo info;

        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        info = securityManager.getLoginInfo( request.getSession(true) );

        return info;
    }


    private User getUser() {
        User user = null;

        EmsSecurityManager.LoginInfo info = getLoginInfo();
        if ( info != null ) {
            user = info.getUser();
        }

        return user;
    }
}

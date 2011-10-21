package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.external.assertions.ssh.console.SftpPollingListenerDialogSettings;
import com.l7tech.external.assertions.ssh.console.SftpPollingListenerPropertiesDialog;
import com.l7tech.external.assertions.ssh.console.SftpPollingListenerXmlUtilities;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SFTP polling resource manager.
 */
public class SftpPollingListenerResourceManager {
    private static final Logger logger = Logger.getLogger(SftpPollingListenerResourceManager.class.getName());

    protected static SftpPollingListenerResourceManager instance;
    protected ArrayList<SftpPollingListenerResource> listenerResources;
    private ApplicationContext context;

    private final ClusterPropertyManager cpManager;

    private Long sftpPollingConfigPropertyOid;

    public static SftpPollingListenerResourceManager getInstance(final ApplicationContext appCtx) {
        if (instance == null) {
            instance = new SftpPollingListenerResourceManager(appCtx);
        }
        return instance;
    }

    protected SftpPollingListenerResourceManager(final ApplicationContext appCtx)
    {
        cpManager = appCtx.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        listenerResources = new ArrayList<SftpPollingListenerResource>();
        this.context = appCtx;
    }

    public void init() {
        // should only be called once
        updateFromClusterProperty(listenerResources);
    }

    public SftpPollingListenerResource[] getListenerConfigurations()
    {
        return listenerResources.toArray(new SftpPollingListenerResource[listenerResources.size()]);
    }

    public long getConfigPropertyOid() {

        if (sftpPollingConfigPropertyOid == null) {
            long theOid = -1L;
            try {
                ClusterProperty key = cpManager.findByUniqueName(SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY);
                if (key != null)
                    theOid = key.getOid();

            } catch (FindException fe) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(fe), ExceptionUtils.getDebugException(fe));

            } finally {
                if (theOid == -1L)
                    logger.log(Level.WARNING, "required property [{0}] not found in ClusterPropertyManager, " +
                            "this may cause problems in the SFTP polling listener sub-system",
                            SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY);
            }
            sftpPollingConfigPropertyOid = theOid;
        }

        return sftpPollingConfigPropertyOid;
    }

    public SftpPollingListenerResource getResourceByResId(long id) {
        SftpPollingListenerResource foundConfig = null;
        foundConfig = getResourceFromList(id, listenerResources);
        return foundConfig;
    }

    private SftpPollingListenerResource getResourceFromList(long id, ArrayList<SftpPollingListenerResource> configs){
        for(SftpPollingListenerResource config : configs)
        {
            if(config.getResId()==id)
                return config;
        }
        return null;
    }

    private void updateFromClusterProperty( ArrayList<SftpPollingListenerResource>  newConfigurations)
    {
        newConfigurations.clear();

        ArrayList<SftpPollingListenerDialogSettings> listenerConfigurations = null;
        String configProp = null;
        try{
            configProp = cpManager.getProperty(SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY);
        }catch(FindException fe){
            logger.warning("Could not find any cluster properties for property >"+SftpPollingListenerConstants.SFTP_POLLING_CONFIGURATION_UI_PROPERTY+"<");
        }

        if( configProp != null && configProp.length() > 0 ) {
            //load the listener configurations from the property string!
            final ClassLoader currentContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(SftpPollingListenerPropertiesDialog.class.getClassLoader());
                SftpPollingListenerXmlUtilities xmlUtil = new SftpPollingListenerXmlUtilities();
                listenerConfigurations = xmlUtil.unmarshallFromXMLString(configProp);
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextClassLoader);
            }
        }

        if(listenerConfigurations != null){
            // sort out in/out queues
            for(SftpPollingListenerDialogSettings configuration : listenerConfigurations)
            {
                SftpPollingListenerResource res = new SftpPollingListenerResource(configuration);
                if(configuration.getPasswordOid() != null) {
                    String decrypted = getDecryptedPassword(configuration.getPasswordOid());
                    res.setPassword(decrypted);
                }
                if (configuration.getPrivateKeyOid() != null) {
                    String decrypted = getDecryptedPassword(configuration.getPrivateKeyOid());
                    res.setPrivateKey(decrypted);
                }

                newConfigurations.add(res);
            }
        }
    }

    private String getDecryptedPassword(long passwordOid) {
        SecurePasswordManager securePasswordManager = (SecurePasswordManager)context.getBean("securePasswordManager");
        String decrypted = null;
        try{
            String encrypted = securePasswordManager.findByPrimaryKey(passwordOid).getEncodedPassword();
            char[] pwd = securePasswordManager.decryptPassword(encrypted);
            decrypted = new String(pwd);
        } catch(ParseException pe) {
            decrypted = null;
            logger.warning("The password could not be parsed, the stored password is corrupted.");
        } catch(FindException fe) {
            decrypted = null;
            logger.warning("The password could not be found in the password manager storage.  The password should be fixed or set in the password manager.");
        } catch(NullPointerException npe) {
            decrypted = null;
            logger.warning("The password could not be found in the password manager storage.  The password should be fixed or set in the password manager.");
        }
        return decrypted;
    }

    /**
     *
     * @return list of updated resources id's
     */
    public UpdateStatus[] onUpdate()
    {
        ArrayList<SftpPollingListenerResource> newConfigurations = new ArrayList<SftpPollingListenerResource>();
        updateFromClusterProperty(newConfigurations);

        List<UpdateStatus> diffListenerConfigs = new ArrayList<UpdateStatus>();
        // only update the listeners array if the listener configurations did change
        UpdateStatus[] updated = diffListenerConfigurations(listenerResources, newConfigurations);
        if (updated.length > 0) {
            diffListenerConfigs.addAll(Arrays.asList(updated));
            listenerResources = newConfigurations;
        }

        // save updated listeners
        return diffListenerConfigs.toArray(new UpdateStatus[diffListenerConfigs.size()]);
    }

    /**
     *
     * @param oldConfigurations
     * @param newConfigurations
     * @return list of diff queue's res id's
     */
    private UpdateStatus[] diffListenerConfigurations(ArrayList<SftpPollingListenerResource> oldConfigurations, ArrayList<SftpPollingListenerResource> newConfigurations) {
        List<SftpPollingListenerResource>oldCopy =  new ArrayList<SftpPollingListenerResource>(oldConfigurations);
        List<SftpPollingListenerResource>newCopy =  new ArrayList<SftpPollingListenerResource>(newConfigurations);

        oldCopy.removeAll(newConfigurations);
        newCopy.removeAll(oldConfigurations);


        List<UpdateStatus> queueResIds = new ArrayList<UpdateStatus>();
        for(SftpPollingListenerResource queue : oldCopy)
        {
            queueResIds.add(new UpdateStatus(queue.getResId(), UpdateStatus.DELETE));
        }
        for(SftpPollingListenerResource queue : newCopy)
        {
            if(queueResIds.contains(new UpdateStatus(queue.getResId(), UpdateStatus.DELETE))){
                int index =  queueResIds.indexOf(new UpdateStatus(queue.getResId(), UpdateStatus.DELETE));
                queueResIds.get(index).status = UpdateStatus.UPDATE;
            }
            else
                queueResIds.add(new UpdateStatus(queue.getResId(), UpdateStatus.ADD));
        }

        return queueResIds.toArray(new UpdateStatus[queueResIds.size()]);
    }

    public class UpdateStatus {
        public static final char DELETE = 'D';
        public static final char UPDATE = 'U';
        public static final char ADD = 'A';

        public long resId;
        public char status;

        public UpdateStatus(long resId, char status) {
            this.resId = resId;
            this.status = status;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof UpdateStatus){
                UpdateStatus status = (UpdateStatus) obj;
                return this.resId == status.resId &&
                       this.status == status.status;
            }

            return super.equals(obj);
        }
    }
}

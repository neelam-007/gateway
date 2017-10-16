package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.objectmodel.*;

import com.l7tech.server.solutionkit.SolutionKitManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an UpgradeTask that gets executed on Gateway startup when the proper cluster_properties record is inserted.
 * This program will update the data in the Solution Kit table so that there will be one Solution Kit record for each
 * parent Solution Kit (a Solution Kit which IsSollection==true) having an unique instance modifier. The child Solution Kits
 * will be updated to point to the correct parent Solution Kit.
 * <p>
 * Created by chaja24 on 10/4/2017.
 */
public class Upgrade9200To9300SolutionKits implements UpgradeTask {

    private ApplicationContext applicationContext;
    private PlatformTransactionManager transactionManager;
    private static final Logger logger = Logger.getLogger(Upgrade9200To9300SolutionKits.class.getName());
    private int numNewParentSK = 0;
    private int numDeleteParentSK = 0;

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {

        this.applicationContext = applicationContext;

        final SolutionKitManager solutionKitManager = getBean("solutionKitManager", SolutionKitManager.class);
        transactionManager = getBean("transactionManager", PlatformTransactionManager.class);

        logger.log(Level.INFO, "Executing Solution Kit table data upgrade.");

        upgradeSolutionKitDatabaseTable(solutionKitManager);
        //throw new NonfatalUpgradeException();

        logger.log(Level.INFO, "Solution Kit table data upgrade completed. {0} new Parent Solution Kit records created.  {1} deleted.",
                new Object[]{numNewParentSK, numDeleteParentSK});
    }

    private void upgradeSolutionKitDatabaseTable(SolutionKitManager solutionKitManager) throws NonfatalUpgradeException {

        final List<Goid> parentSolutionKitHasNoInstanceModList = new ArrayList<>();
        final Collection<SolutionKit> solutionKitCollection;

        try {
            // retrieve all the Solution Kits.
            solutionKitCollection = solutionKitManager.findAll();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error retrieving Solution Kits");
            throw new NonfatalUpgradeException("Error retrieving Solution Kits");
        }

        if (solutionKitCollection.isEmpty()) {
            // exit upgrade if there are no Solution Kit entries.
            return;
        }

        // For each Solution Kit, get the instance modifier, find the parent with matching instance modifier.
        // If parent does not exist, create it.  Then update child Solution Kit to point to the new parent.
        for (final SolutionKit solutionKit : solutionKitCollection) {

            final Goid parentGoid = solutionKit.getParentGoid();
            final String solutionKitInstanceModifer = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);

            if (parentGoid == null) {
                // This must be a parent (skar of skar) or a regular Solution Kit without a parent.
                if ((solutionKitInstanceModifer == null) && (Boolean.valueOf(solutionKit.getProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY)))) {
                    // Add the Solution Kit which does not have an instance modifier and is a parent Solution Kit to
                    // the parentSolutionKitHasNoInstanceModList
                    parentSolutionKitHasNoInstanceModList.add(solutionKit.getGoid());
                }
                continue;
            }

            try {
                // Process the child/regular Solution Kit
                final SolutionKit parentSolutionKit = solutionKitManager.findByPrimaryKey(parentGoid);
                if (parentSolutionKit == null) {
                    logger.log(Level.WARNING, "Could not find parent Solution Kit for child Solution Kit:{0} with instance:{1}",
                            new Object[]{solutionKit.getName(), solutionKitInstanceModifer});
                    continue;
                }

                // check if the parent Solution Kit already exist for this instance.
                final SolutionKit parentSolutionKitWithInstanceModifier =
                        solutionKitManager.findBySolutionKitGuidAndIM(parentSolutionKit.getSolutionKitGuid(), solutionKitInstanceModifer);

                if (parentSolutionKitWithInstanceModifier == null) {
                    // parent Solution Kit with matching instance modifier was not found.  Create new parent Solution Kit of for the instance.
                    final Goid newParentGoid = createParentSolutionKit(transactionManager,
                            solutionKitManager,
                            parentSolutionKit,
                            solutionKitInstanceModifer);
                    // Update the child instance to point to the parent.
                    solutionKit.setParentGoid(newParentGoid);
                    updateSolutionKit(transactionManager, solutionKitManager, solutionKit);

                } else {
                    // parent Solution Kit with matching instance modifier was found.  Update the child to point to the
                    // existing parent Solution Kit with matching instance modifier.
                    if (!Goid.equals(parentGoid, parentSolutionKitWithInstanceModifier.getGoid())) {
                        solutionKit.setParentGoid(parentSolutionKitWithInstanceModifier.getGoid());
                        updateSolutionKit(transactionManager, solutionKitManager, solutionKit);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception caught while processing SolutionKit:{0} Instance: {1} Message:{2}",
                        new Object[]{solutionKit.getName(), solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), e.toString()});
                throw new NonfatalUpgradeException();
            }
        }

        removeParentSolutionKitsWithNoInstance(solutionKitManager, parentSolutionKitHasNoInstanceModList);
    }


    private void removeParentSolutionKitsWithNoInstance(SolutionKitManager solutionKitManager,
                                                        List<Goid> parentSolutionKitHasNoInstanceModList) throws NonfatalUpgradeException {

        // remove parent Solution Kits that have no children.
        for (Goid parentSKGoid : parentSolutionKitHasNoInstanceModList) {
            try {
                final Collection colChildSK = solutionKitManager.findAllChildrenByParentGoid(parentSKGoid);

                if (colChildSK.size() == 0) {
                    solutionKitManager.delete(parentSKGoid);
                    numDeleteParentSK++;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception caught while trying to delete parent Solution Kit with Goid:" + parentSKGoid.toString());
                throw new NonfatalUpgradeException();
            }
        }
    }

    /**
     * Get a bean safely.
     *
     * @param name      the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException if there is no application context or the requested bean was not found
     */
    @SuppressWarnings({"unchecked"})
    private <T> T getBean(final String name,
                          final Class<T> beanClass) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            return applicationContext.getBean(name, beanClass);
        } catch (BeansException be) {
            throw new FatalUpgradeException("Error accessing  bean '" + name + "' from ApplicationContext.");
        }
    }


    private SolutionKit cloneSolutionKit(SolutionKit sourceSolutionKit) {

        SolutionKit sk = new SolutionKit();
        sk.setSolutionKitGuid(sourceSolutionKit.getSolutionKitGuid());
        sk.setName(sourceSolutionKit.getName());
        sk.setMappings(sourceSolutionKit.getMappings());
        sk.setInstallationXmlProperties(sourceSolutionKit.getInstallationXmlProperties());
        sk.setSolutionKitVersion(sourceSolutionKit.getSolutionKitVersion());
        sk.setUninstallBundle(sourceSolutionKit.getUninstallBundle());
        sk.setXmlProperties(sourceSolutionKit.getXmlProperties());
        sk.setSolutionKitVersion(sourceSolutionKit.getSolutionKitVersion());

        return sk;
    }

    // Need to flush the transaction after save otherwise queries following this code within this utility
    // will not see the changes.
    private Goid createParentSolutionKit(PlatformTransactionManager transactionManager,
                                         SolutionKitManager solutionKitManager,
                                         SolutionKit existingParentSolutionKit,
                                         String instanceModifer) throws SaveException, NonfatalUpgradeException {


        if (existingParentSolutionKit == null) {
            logger.log(Level.WARNING, "The existing Solution Kit should not be null.");
            throw new NonfatalUpgradeException();
        }

        final SolutionKit newParentSK = cloneSolutionKit(existingParentSolutionKit);
        newParentSK.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, instanceModifer);
        // Should generate a new Solution Kit instance.

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        final Goid goid = (Goid) tt.execute((TransactionCallback) transactionStatus -> {
            try {
                final Goid newGoid = solutionKitManager.save(newParentSK);
                transactionStatus.flush();
                numNewParentSK++;
                return newGoid;
            } catch (SaveException e) {
                logger.log(Level.WARNING, "Exception caught while trying save new parent Solution Kit:{0} for instance:{1}",
                        new Object[]{newParentSK.getName(), instanceModifer});
                return null;
            }
        });

        if (goid == null) {
            throw new NonfatalUpgradeException();
        }
        return goid;
    }

    // Need to flush the transaction after update otherwise queries following this code within this utility
    // will not see the changes.
    private void updateSolutionKit(PlatformTransactionManager transactionManager,
                                   SolutionKitManager solutionKitManager,
                                   SolutionKit solutionKit) throws NonfatalUpgradeException {

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        final boolean success = (boolean) tt.execute((TransactionCallback) transactionStatus -> {
            try {
                solutionKitManager.update(solutionKit);
                transactionStatus.flush();
            } catch (UpdateException ioe) {
                logger.log(Level.WARNING, "Exception caught while trying to update Solution Kit:{0} for instance:{1}",
                        new Object[]{solutionKit.getName(), solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY)});
                return false;
            }
            return true;
        });

        if (!success) {
            throw new NonfatalUpgradeException();
        }

    }
}

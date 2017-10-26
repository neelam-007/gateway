package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.objectmodel.*;

import com.l7tech.server.solutionkit.SolutionKitManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an UpgradeTask that gets executed on Gateway startup when the proper cluster_properties record is inserted.
 * This program will update the data in the Solution Kit table so that there will be one Solution Kit record for each
 * parent Solution Kit (a Solution Kit which IsCollection==true) that has an unique instance modifier. The child Solution Kits
 * will be updated to point to the corresponding parent Solution Kit with matching Instance Modifier. If the Upgrade Task
 * encounters an error, the NonfatalUpgradeException is thrown and the upgrade will rollback. The gateway will continue with the startup.
 *
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

        logger.log(Level.INFO, "Solution Kit table data upgrade completed. {0} new Parent Solution Kit records created.  {1} deleted.",
                new Object[]{numNewParentSK, numDeleteParentSK});
    }

    /**
     * This is the main routine that updates the Solution Kit table records.
     * If the upgrade fails, the NonfatalUpgradeException is thrown and the entire Solution Kit table record modifications
     * are reverted.
     *
     * @param solutionKitManager - the reference to the SolutionKitManager for querying & updating the Solution Kit database table.
     * @throws NonfatalUpgradeException - occurs during any exception resulting from reading/writing from/to the SK database table.
     */
    private void upgradeSolutionKitDatabaseTable(final SolutionKitManager solutionKitManager) throws NonfatalUpgradeException {

        final Set<Goid> parentSKWithNoInstanceModSet = new HashSet<>();
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
            final String solutionKitInstanceModifier = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);

            if (parentGoid == null) {
                // This must be a parent (skar of skar) or a regular Solution Kit without a parent.
                if ((StringUtils.isBlank(solutionKitInstanceModifier)) && (Boolean.valueOf(solutionKit.getProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY)))) {
                    // Add the Solution Kit which does not have an instance modifier and is a parent Solution Kit to
                    // the parentSKWithNoInstanceModSet
                    parentSKWithNoInstanceModSet.add(solutionKit.getGoid());
                }
                continue;
            }

            try {
                // Process the child Solution Kit
                final SolutionKit parentSolutionKit = solutionKitManager.findByPrimaryKey(parentGoid);
                if (parentSolutionKit == null) {
                    logger.log(Level.WARNING, "Could not find parent Solution Kit for child Solution Kit: {0} with instance: {1}." +
                            "The parent Goid will be set to null and this Solution Kit will become a standalone.",
                            new Object[]{solutionKit.getName(), solutionKitInstanceModifier});
                    solutionKit.setParentGoid(Goid.DEFAULT_GOID);
                    updateSolutionKit(transactionManager, solutionKitManager, solutionKit);
                    continue;
                }

                // check if the parent Solution Kit already exist for this instance.
                final SolutionKit parentSolutionKitWithInstanceModifier =
                        solutionKitManager.findBySolutionKitGuidAndIM(parentSolutionKit.getSolutionKitGuid(), solutionKitInstanceModifier);

                if (parentSolutionKitWithInstanceModifier == null) {
                    // parent Solution Kit with matching instance modifier was not found.  Create new parent Solution Kit of for the instance.

                    final SolutionKit newParentSK = cloneSolutionKit(parentSolutionKit);
                    newParentSK.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, solutionKitInstanceModifier);

                    final Goid newParentGoid = createParentSolutionKit(transactionManager,
                            solutionKitManager,
                            newParentSK);
                    // Update the child instance to point to the parent.
                    solutionKit.setParentGoid(newParentGoid);
                    updateSolutionKit(transactionManager, solutionKitManager, solutionKit);

                } else if (!Goid.equals(parentGoid, parentSolutionKitWithInstanceModifier.getGoid())) {
                        // parent Solution Kit with matching instance modifier was found. If the Solution Kit parent Goid
                        // needs updating, update the child to point to the existing parent Solution Kit with matching instance modifier.
                        solutionKit.setParentGoid(parentSolutionKitWithInstanceModifier.getGoid());
                        updateSolutionKit(transactionManager, solutionKitManager, solutionKit);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception caught while processing SolutionKit: {0} Instance: {1} Message: {2}",
                        new Object[]{solutionKit.getName(), solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY), e.toString()});
                throw new NonfatalUpgradeException();
            }
        }

        removeParentSolutionKitsWithNoInstance(solutionKitManager, parentSKWithNoInstanceModSet);
    }

    /**
     *  remove any parent Solution Kits with no instance modifiers if the parent has no children.
     *
     * @param solutionKitManager - the reference to the SolutionKitManager for querying & updating the Solution Kit database table.
     * @param parentSolutionKitHasNoInstanceModSet - a set of parent Solution Kits that have no instance modifiers.
     * @throws NonfatalUpgradeException - occurs during any exception resulting from reading/writing from/to the SK database table.
     */
    private void removeParentSolutionKitsWithNoInstance(final SolutionKitManager solutionKitManager,
                                                        final Set<Goid> parentSolutionKitHasNoInstanceModSet) throws NonfatalUpgradeException {

        // remove parent Solution Kits that have no children.
        for (Goid parentSKGoid : parentSolutionKitHasNoInstanceModSet) {
            try {
                final Collection colChildSK = solutionKitManager.findAllChildrenByParentGoid(parentSKGoid);

                if (colChildSK.isEmpty()) {
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


    private SolutionKit cloneSolutionKit(final SolutionKit sourceSolutionKit) {

        SolutionKit sk = new SolutionKit();
        sk.setSolutionKitGuid(sourceSolutionKit.getSolutionKitGuid());
        sk.setName(sourceSolutionKit.getName());
        sk.setMappings(sourceSolutionKit.getMappings());
        sk.setInstallationXmlProperties(sourceSolutionKit.getInstallationXmlProperties());
        sk.setSolutionKitVersion(sourceSolutionKit.getSolutionKitVersion());
        sk.setUninstallBundle(sourceSolutionKit.getUninstallBundle());
        sk.setXmlProperties(sourceSolutionKit.getXmlProperties());

        return sk;
    }


    /**
     * Create a parent Solution Kit record. Provide a Solution Kit object. When the method is invoked,
     * a Solution Kit record will be created in the database.  The Goid of the new record is returned.
     * The method is performed in a transaction and flushed so that other queries performed in this module
     * will see the new records in the database.
     * @param transactionManager - PlatformTransactionManager to enable the Solution Kit table record to be created and flushed
     *                           so that other queries will see the new record.
     * @param solutionKitManager - the reference to the SolutionKitManager for updating the Solution Kit database table.
     * @return Goid - the Goid of the new Solution Kit.
     * @throws SaveException
     * @throws NonfatalUpgradeException - occurs during any exception resulting from writing to the SK database table.
     */
    private Goid createParentSolutionKit(final PlatformTransactionManager transactionManager,
                                         final SolutionKitManager solutionKitManager,
                                         @NotNull final SolutionKit newParentSK) throws SaveException, NonfatalUpgradeException {

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        final Goid goid = tt.execute( transactionStatus -> {
            try {
                final Goid newGoid = solutionKitManager.save(newParentSK);
                transactionStatus.flush();
                numNewParentSK++;
                return newGoid;
            } catch (SaveException e) {
                logger.log(Level.WARNING, "Exception caught while trying save new parent Solution Kit: {0} for instance: {1}",
                        new Object[]{newParentSK.getName(), newParentSK.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY)});
                return null;
            }
        });

        if (goid == null) {
            throw new NonfatalUpgradeException();
        }
        return goid;
    }


    /**
     * updateSolutionKit - updates the Solution Kit record in the database. Provide a Solution Kit object that already
     * exists in the Solution Kit table. The Solution Kit provided to this method may have newer attribute values than what is stored
     * in the table.  When the method is invoked, the previous values in the database will be over written with the newer ones.
     * The update is done in a transaction which is immediately flushed so that other queries in this module will see the update.
     * @param transactionManager - PlatformTransactionManager to enable the Solution Kit table record to be created and flushed
     *                           so that other queries will see the new record.
     * @param solutionKitManager - the reference to the SolutionKitManager for updating the Solution Kit database table.
     * @param solutionKit - the Solution Kit to record to the Solution Kit database table.
     * @throws NonfatalUpgradeException - occurs during any exception resulting from writing to the SK database table.
     */
    private void updateSolutionKit(final PlatformTransactionManager transactionManager,
                                   final SolutionKitManager solutionKitManager,
                                   final SolutionKit solutionKit) throws NonfatalUpgradeException {

        final TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.setReadOnly(false);
        final boolean success = tt.execute(transactionStatus -> {
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

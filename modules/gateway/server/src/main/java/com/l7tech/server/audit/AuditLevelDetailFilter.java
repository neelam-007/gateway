/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.util.Config;
import com.l7tech.util.Pair;
import org.springframework.beans.factory.InitializingBean;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allow audit message levels to be modified based on audit.setDetailLevel.SEVERE down to FINEST cluster property values.
 * Changes to these properties will be picked up at runtime.
 */
public class AuditLevelDetailFilter implements MessagesUtil.AuditDetailLevelFilter, InitializingBean, PropertyChangeListener {

    public AuditLevelDetailFilter(Config config) {
        this.config = config;
    }

    @Override
    public Level filterLevelForAuditDetailMessage(final int id, final Level defaultLevel) {

        //check never set first
        final Set<String> neverSet = copyOnWriteNeverAuditSet.get();
        if(neverSet.contains(String.valueOf(id))){
            return null;
        }

        final List<Pair<Level, Set<String>>> levelList = copyOnWritePairLevelList.get();

        for (Pair<Level, Set<String>> levelSetPair : levelList) {
            final Set<String> idsForLevel = levelSetPair.right;
            if (idsForLevel.contains(String.valueOf(id))) {
                return levelSetPair.left;
            }
        }

        return defaultLevel;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        MessagesUtil.registerMessageLevelFilter(this);
        buildCache();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if(event.getPropertyName().startsWith(clusterPropPrefix) ||
                event.getPropertyName().equals(clusterPropNever)){
            buildCache();
        }
    }

    // - PRIVATE

    final private Config config;
    final private static String clusterPropPrefix= "audit.setDetailLevel.";
    final private static String clusterPropNever= "audit.auditDetailExcludeList";
    //Order is important. These levels are ordered from highest value to lowest. Do not modify without updating usages.
    final private static List<Level> allLevels = Arrays.asList(Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST);
    final private static Logger logger = Logger.getLogger(AuditLevelDetailFilter.class.getName());

    private AtomicReference<CopyOnWriteArrayList<Pair<Level, Set<String>>>> copyOnWritePairLevelList =
            new AtomicReference<CopyOnWriteArrayList<Pair<Level, Set<String>>>>(new CopyOnWriteArrayList<Pair<Level, Set<String>>>());

    private AtomicReference<Set<String>> copyOnWriteNeverAuditSet = new AtomicReference<Set<String>>(new HashSet<String>());

    private void buildCache(){
        //order is important.
        List<Pair<Level, Set<String>>> pairList = new ArrayList<Pair<Level, Set<String>>>();
        for (Level level : allLevels) {
            final String idsForLevel = config.getProperty(clusterPropPrefix + level, null);
            if(idsForLevel != null){
                final Set<String> idsForSet = new HashSet<String>(Arrays.asList(idsForLevel.trim().split("\\s+")));
                Pair<Level, Set<String>> levelToIds = new Pair<Level, Set<String>>(level, idsForSet);
                pairList.add(levelToIds);
            }
        }

        copyOnWritePairLevelList.set(new CopyOnWriteArrayList<Pair<Level, Set<String>>>(pairList));

        //Get the never list
        final String neverIds = config.getProperty(clusterPropNever, null);
        final Set<String> neverSet = new HashSet<String>();
        if(neverIds != null){
            neverSet.addAll(Arrays.asList(neverIds.split("\\s+|,")));
        }

        //reset in case property was deleted
        copyOnWriteNeverAuditSet.set(new HashSet<String>(neverSet));
    }
}

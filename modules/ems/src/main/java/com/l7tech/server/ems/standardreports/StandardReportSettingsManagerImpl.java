package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Jan 20, 2009
 * @since Enterprise Manager 1.0
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class StandardReportSettingsManagerImpl extends HibernateEntityManager<StandardReportSettings, EntityHeader> implements StandardReportSettingsManager {

    @Override
    public Class<? extends Entity> getImpClass() {
        return StandardReportSettings.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return StandardReportSettings.class;
    }

    @Override
    public String getTableName() {
        return "standard_report_settings";
    }

    @Override
    public Collection<StandardReportSettings> findByUser( final User user ) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("userId", user.getId());
        map.put("provider", user.getProviderId());

        return findMatching(Arrays.asList(map));
    }

    @Override
    public StandardReportSettings findByPrimaryKeyForUser( final User user, final Goid goid ) throws FindException {
        StandardReportSettings settingsForUser = null;

        final StandardReportSettings settings = findByPrimaryKey( goid );
        if ( settings.getProvider().equals(user.getProviderId()) &&
             settings.getUserId() != null &&
             settings.getUserId().equals(user.getId()) ) {
            settingsForUser = settings;           
        }

        return settingsForUser;
    }

    @Override
    public StandardReportSettings findByNameAndUser( final User user, final String name ) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", name);
        map.put("userId", user.getId());
        map.put("provider", user.getProviderId());

        Collection<StandardReportSettings> matched = findMatching(Arrays.asList(map));
        if ( matched.isEmpty() ) {
            return null;
        } else if ( matched.size()==1 ) {
            return matched.iterator().next();
        } else {
            throw new FindException("Error finding unique settings for user with name '"+name+"'.");
        }
    }

    @Override
    public void delete( final Goid goid ) throws DeleteException, FindException {
        findAndDelete( goid );
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final StandardReportSettings standardReportSettings ) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("name", standardReportSettings.getName());
        map.put("userId", standardReportSettings.getUserId());
        map.put("provider", standardReportSettings.getProvider());
        return Arrays.asList(map);
    }

}

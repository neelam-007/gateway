package com.l7tech.server.ems.standardreports;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.identity.User;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

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
    public StandardReportSettings findByPrimaryKeyForUser( final User user, final long oid ) throws FindException {
        StandardReportSettings settingsForUser = null;

        final StandardReportSettings settings = findByPrimaryKey( oid );
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
    public void delete( final long oid ) throws DeleteException, FindException {
        findAndDelete( oid );
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

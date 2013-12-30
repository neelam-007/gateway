package com.l7tech.server;

import org.hibernate.*;
import org.hibernate.classic.Session;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/**
 * This was created: 11/1/13 as 2:15 PM
 *
 * @author Victor Kazakov
 */
public class SessionFactoryStub implements SessionFactory {
    @Override
    public Session openSession() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session openSession(Interceptor interceptor) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session openSession(Connection connection) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session openSession(Connection connection, Interceptor interceptor) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StatelessSession openStatelessSession() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StatelessSession openStatelessSession(Connection connection) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClassMetadata getClassMetadata(Class aClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClassMetadata getClassMetadata(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CollectionMetadata getCollectionMetadata(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, ClassMetadata> getAllClassMetadata() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map getAllCollectionMetadata() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Statistics getStatistics() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isClosed() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Cache getCache() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evict(Class aClass) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evict(Class aClass, Serializable serializable) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictEntity(String s) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictEntity(String s, Serializable serializable) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictCollection(String s) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictCollection(String s, Serializable serializable) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictQueries(String s) throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictQueries() throws HibernateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set getDefinedFilterNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FilterDefinition getFilterDefinition(String s) throws HibernateException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean containsFetchProfileDefinition(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TypeHelper getTypeHelper() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Reference getReference() throws NamingException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

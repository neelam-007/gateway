package com.l7tech.external.assertions.snmpagent.server;

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
 * User: rseminoff
 * Date: 5/15/12
 */
public class MockSessionFactory implements SessionFactory {
    @Override
    public Session openSession() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: openSession()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session openSession(Interceptor interceptor) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: openSession(Interceptor)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session openSession(Connection connection) {
        System.out.println("*** CALL *** MockSessionFactory: openSession(Connection)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session openSession(Connection connection, Interceptor interceptor) {
        System.out.println("*** CALL *** MockSessionFactory: openSession(Connection, Interceptor)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: getCurrentSession()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StatelessSession openStatelessSession() {
        System.out.println("*** CALL *** MockSessionFactory: openStatelessSession()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StatelessSession openStatelessSession(Connection connection) {
        System.out.println("*** CALL *** MockSessionFactory: openStatelessSession(Connection)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClassMetadata getClassMetadata(Class aClass) {
        System.out.println("*** CALL *** MockSessionFactory: getClassMetadata(Class)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClassMetadata getClassMetadata(String s) {
        System.out.println("*** CALL *** MockSessionFactory: getClassMetadata(String)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CollectionMetadata getCollectionMetadata(String s) {
        System.out.println("*** CALL *** MockSessionFactory: getCollectionMetadata()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, ClassMetadata> getAllClassMetadata() {
        System.out.println("*** CALL *** MockSessionFactory: getAllClassMetadata()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map getAllCollectionMetadata() {
        System.out.println("*** CALL *** MockSessionFactory: getAllCollectionMetadata()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Statistics getStatistics() {
        System.out.println("*** CALL *** MockSessionFactory: getStatistics()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: close()");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isClosed() {
        System.out.println("*** CALL *** MockSessionFactory: isClosed()");
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Cache getCache() {
        System.out.println("*** CALL *** MockSessionFactory: getCache)");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evict(Class aClass) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evict(Class)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evict(Class aClass, Serializable serializable) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evict(Class, Serializable)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictEntity(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictEntity(String)");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictEntity(String s, Serializable serializable) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictEntity(String, Serializable)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictCollection(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictCollection(String)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictCollection(String s, Serializable serializable) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictCollection(String, Serializable)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictQueries(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictQueries(String)");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void evictQueries() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictQueries()");
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set getDefinedFilterNames() {
        System.out.println("*** CALL *** MockSessionFactory: getDefinedFilterNames()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FilterDefinition getFilterDefinition(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: getFilterDefinition()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean containsFetchProfileDefinition(String s) {
        System.out.println("*** CALL *** MockSessionFactory: containsFetchProfileDefinition()");
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TypeHelper getTypeHelper() {
        System.out.println("*** CALL *** MockSessionFactory: getTypeHelper()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Reference getReference() throws NamingException {
        System.out.println("*** CALL *** MockSessionFactory: getReference()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

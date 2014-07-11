package com.l7tech.external.assertions.xmppassertion.server;

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
 * Date: 24/05/12
 *
 * Copied from com.l7tech.external.assertions.snmpagent.server.MockSessionFactory
 */
public class MockSessionFactory implements SessionFactory {
    @Override
    public Session openSession() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: openSession()");
        return null;
    }

    @Override
    public Session openSession(Interceptor interceptor) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: openSession(Interceptor)");
        return null;
    }

    @Override
    public Session openSession(Connection connection) {
        System.out.println("*** CALL *** MockSessionFactory: openSession(Connection)");
        return null;
    }

    @Override
    public Session openSession(Connection connection, Interceptor interceptor) {
        System.out.println("*** CALL *** MockSessionFactory: openSession(Connection, Interceptor)");
        return null;
    }

    @Override
    public Session getCurrentSession() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: getCurrentSession()");
        return null;
    }

    @Override
    public StatelessSession openStatelessSession() {
        System.out.println("*** CALL *** MockSessionFactory: openStatelessSession()");
        return null;
    }

    @Override
    public StatelessSession openStatelessSession(Connection connection) {
        System.out.println("*** CALL *** MockSessionFactory: openStatelessSession(Connection)");
        return null;
    }

    @Override
    public ClassMetadata getClassMetadata(Class aClass) {
        System.out.println("*** CALL *** MockSessionFactory: getClassMetadata(Class)");
        return null;
    }

    @Override
    public ClassMetadata getClassMetadata(String s) {
        System.out.println("*** CALL *** MockSessionFactory: getClassMetadata(String)");
        return null;
    }

    @Override
    public CollectionMetadata getCollectionMetadata(String s) {
        System.out.println("*** CALL *** MockSessionFactory: getCollectionMetadata()");
        return null;
    }

    @Override
    public Map<String, ClassMetadata> getAllClassMetadata() {
        System.out.println("*** CALL *** MockSessionFactory: getAllClassMetadata()");
        return null;
    }

    @Override
    public Map getAllCollectionMetadata() {
        System.out.println("*** CALL *** MockSessionFactory: getAllCollectionMetadata()");
        return null;
    }

    @Override
    public Statistics getStatistics() {
        System.out.println("*** CALL *** MockSessionFactory: getStatistics()");
        return null;
    }

    @Override
    public void close() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: close()");
    }

    @Override
    public boolean isClosed() {
        System.out.println("*** CALL *** MockSessionFactory: isClosed()");
        return false;
    }

    @Override
    public Cache getCache() {
        System.out.println("*** CALL *** MockSessionFactory: getCache)");
        return null;
    }

    @Override
    public void evict(Class aClass) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evict(Class)");
    }

    @Override
    public void evict(Class aClass, Serializable serializable) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evict(Class, Serializable)");
    }

    @Override
    public void evictEntity(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictEntity(String)");
    }

    @Override
    public void evictEntity(String s, Serializable serializable) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictEntity(String, Serializable)");
    }

    @Override
    public void evictCollection(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictCollection(String)");
    }

    @Override
    public void evictCollection(String s, Serializable serializable) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictCollection(String, Serializable)");
    }

    @Override
    public void evictQueries(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictQueries(String)");
    }

    @Override
    public void evictQueries() throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: evictQueries()");
    }

    @Override
    public Set getDefinedFilterNames() {
        System.out.println("*** CALL *** MockSessionFactory: getDefinedFilterNames()");
        return null;
    }

    @Override
    public FilterDefinition getFilterDefinition(String s) throws HibernateException {
        System.out.println("*** CALL *** MockSessionFactory: getFilterDefinition()");
        return null;
    }

    @Override
    public boolean containsFetchProfileDefinition(String s) {
        System.out.println("*** CALL *** MockSessionFactory: containsFetchProfileDefinition()");
        return false;
    }

    @Override
    public TypeHelper getTypeHelper() {
        System.out.println("*** CALL *** MockSessionFactory: getTypeHelper()");
        return null;
    }

    @Override
    public Reference getReference() throws NamingException {
        System.out.println("*** CALL *** MockSessionFactory: getReference()");
        return null;
    }
}

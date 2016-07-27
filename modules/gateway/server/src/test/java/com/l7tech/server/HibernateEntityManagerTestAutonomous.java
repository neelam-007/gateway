package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.PersistentEntityImp;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.test.BugId;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.SimpleExpression;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.Table;
import java.util.*;

import static com.l7tech.server.HibernateEntityManagerTestAutonomous.MOCK_TABLE;
import static com.l7tech.server.HibernateEntityManagerTestAutonomous.randomGoid;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class HibernateEntityManagerTestAutonomous {

    private static final Random RANDOM = new Random();
    private static final Goid ENTITY_GOID = randomGoid();
    private static final int MAX_AGE = 100;

    static final String MOCK_TABLE = "mock_table";

    /**
     * Generates a Goid out of {@link GoidRange#RESERVED_RANGE}
     * @return a new Goid out of {@link GoidRange#RESERVED_RANGE}
     */
    @NotNull
    static Goid randomGoid() {
        return new Goid(Math.abs(RANDOM.nextInt(Integer.MAX_VALUE - 65535)) + 65535, RANDOM.nextInt());
    }

    /**
     * Verify that changing an entity's name does not prevent it from being properly cached by name.
     * @throws FindException
     * @throws SaveException
     */
    @BugId("SSG-13576")
    @Test
    public void shouldCacheByNameAfterNameWasChanged() throws FindException, SaveException {
        // we need to keep count of the number of times the entity was fetched by name from the database
        final IncrementTracker dbHitsByNameCounter = new IncrementTracker();

        // our entity manager
        MockHibernateEntityManager entityManager = new MockHibernateEntityManager.Builder()
                .withTransactionManager(new TransactionManagerBuilder().build())
                .withHibernateTemplate(new HibernateTemplateBuilder()
                        .withGetByNameCallCounter(dbHitsByNameCounter)
                        .build())
                .build();

        // let's create the entity
        entityManager.save(ENTITY_GOID,
                new MockPersistentEntity.Builder()
                        .withId("mockEntityId")
                        .withName("mockEntityName")
                        .build());

        // let's fetch it - should fetch from the database once
        MockPersistentEntity entity = entityManager.getCachedEntityByName("mockEntityName", MAX_AGE);

        // let's change the entity name and save it
        entity.setName("changedMockEntityName");
        entityManager.save(entity);

        // retrieve it once more - should hit the database
        entityManager.getCachedEntityByName("changedMockEntityName", MAX_AGE);

        // retrieve it a bunch more times - should NOT hit the database
        entityManager.getCachedEntityByName("changedMockEntityName", MAX_AGE);
        entityManager.getCachedEntityByName("changedMockEntityName", MAX_AGE);
        entityManager.getCachedEntityByName("changedMockEntityName", MAX_AGE);
        entityManager.getCachedEntityByName("changedMockEntityName", MAX_AGE);

        // check that the entity was only fetched twice - once after creation, once after renaming
        assertEquals("Unexpected number of GetByName invocations", 2, dbHitsByNameCounter.get());
    }

    @Test
    public void shouldNotKeepRenamedEntitiesWithOldNamesInCache() throws SaveException, FindException {
        // our entity manager
        MockHibernateEntityManager entityManager = new MockHibernateEntityManager.Builder()
                .withTransactionManager(new TransactionManagerBuilder().build())
                .withHibernateTemplate(new HibernateTemplateBuilder().build())
                .build();

        // let's create the entity
        entityManager.save(ENTITY_GOID,
                new MockPersistentEntity.Builder()
                        .withId("mockEntityId")
                        .withName("mockEntityName")
                        .build());

        // let's fetch it - should fetch from the database once
        MockPersistentEntity entity = entityManager.getCachedEntityByName("mockEntityName", MAX_AGE);

        // let's rename it
        entity.setName("changedMockEntityName");
        entityManager.save(entity);

        // now let's try to fetch it by its old name - should no longer be there
        entity = entityManager.getCachedEntityByName("mockEntityName", MAX_AGE);

        assertNull("Renamed entity retrieved by its old name", entity);
    }

    @Test
    public void shouldFindRenamedEntitiesByGoid() throws SaveException, FindException {
        // our entity manager
        MockHibernateEntityManager entityManager = new MockHibernateEntityManager.Builder()
                .withTransactionManager(new TransactionManagerBuilder().build())
                .withHibernateTemplate(new HibernateTemplateBuilder().build())
                .build();

        // let's create the entity
        entityManager.save(ENTITY_GOID,
                new MockPersistentEntity.Builder()
                        .withId("mockEntityId")
                        .withName("mockEntityName")
                        .build());

        // let's fetch it - should fetch from the database once
        MockPersistentEntity entity = entityManager.getCachedEntity(ENTITY_GOID, MAX_AGE);

        // let's rename it
        entity.setName("changedMockEntityName");
        entityManager.save(entity);

        // is it still there with the same Goid?
        entity = entityManager.getCachedEntity(ENTITY_GOID, MAX_AGE);
        assertNotNull("Renamed entity not found by Goid");

        // does it have the new name?
        assertEquals("Name change did not work", "changedMockEntityName", entity.getName());
    }

    @Test
    public void shouldNotFailWhenAskedForNonexistentName() throws FindException {
        // our entity manager
        MockHibernateEntityManager entityManager = new MockHibernateEntityManager.Builder()
                .withTransactionManager(new TransactionManagerBuilder().build())
                .withHibernateTemplate(new HibernateTemplateBuilder().build())
                .build();

        PersistentEntity bepo = entityManager.getCachedEntityByName("OhBotherNeverSaved! :(", MAX_AGE);

        // succeeds by not throwing
    }

    @Test
    public void shouldNotFailWhenAskedForNonexistentGoid() throws FindException {
        // our entity manager
        MockHibernateEntityManager entityManager = new MockHibernateEntityManager.Builder()
                .withTransactionManager(new TransactionManagerBuilder().build())
                .withHibernateTemplate(new HibernateTemplateBuilder().build())
                .build();

        entityManager.getCachedEntity(randomGoid(), MAX_AGE);

        // succeeds by not throwing
    }

    @Test
    public void shouldPretendNotToKnowEntityCachedByNameIfOtherNodeInClusterHasChangedIt()
            throws SaveException, FindException, InterruptedException {

        // these two will keep the state of our 'database'
        PlatformTransactionManager transactionManager = new TransactionManagerBuilder().build();
        HibernateTemplate hibernateTemplate = new HibernateTemplateBuilder().build();

        // the entity manager for Gateway 1 in our cluster
        MockHibernateEntityManager gatewayOne = new MockHibernateEntityManager.Builder()
                .withTransactionManager(transactionManager)
                .withHibernateTemplate(hibernateTemplate)
                .build();

        // the entity manager for Gateway 2 in our cluster
        MockHibernateEntityManager gatewayTwo = new MockHibernateEntityManager.Builder()
                .withTransactionManager(transactionManager)
                .withHibernateTemplate(hibernateTemplate)
                .build();

        Goid goid = randomGoid();

        // save entity named "mockEntityName" in node 1
        gatewayOne.save(goid, new MockPersistentEntity.Builder()
                .withId("mockEntityId")
                .withName("mockEntityName")
                .build());

        // retrieve it once so it gets cached
        gatewayOne.getCachedEntityByName("mockEntityName", MAX_AGE);

        // retrieve it from the other node
        MockPersistentEntity retrievedFromTwo = gatewayTwo.getCachedEntity(goid, MAX_AGE);

        // rename it
        retrievedFromTwo.setName("changedMockEntityName");
        gatewayTwo.save(retrievedFromTwo);

        // wait for cache to expire...
        Thread.sleep(MAX_AGE);

        // now gateway 1 should report it doesn't know 'mockEntityName' even though it still has it in its cache
        MockPersistentEntity retrievedFromOneAgain = gatewayOne.getCachedEntityByName("mockEntityName", MAX_AGE);

        // should not exist
        assertNull("Entity was found in cache even though its name had changed in the database", retrievedFromOneAgain);
    }
}


/**
 * A trivial implementation of {@link HibernateEntityManager} which only provides mock objects
 * for the type parameters.
 */
class MockHibernateEntityManager extends HibernateEntityManager<MockPersistentEntity, MockEntityHeader> {

    @Override
    public Class<? extends Entity> getImpClass() {
        return MockPersistentEntity.class;
    }

    static class Builder {

        private final MockHibernateEntityManager entityManager;

        Builder() {
            this.entityManager = new MockHibernateEntityManager();
        }

        Builder withTransactionManager(PlatformTransactionManager transactionManager) {
            this.entityManager.setTransactionManager(transactionManager);
            return this;
        }

        Builder withHibernateTemplate(HibernateTemplate hibernateTemplate) {
            this.entityManager.setHibernateTemplate(hibernateTemplate);
            return this;
        }

        MockHibernateEntityManager build() {
            return entityManager;
        }

    }

}


/**
 * A fake persistent entity which we will be "saving to" and "retrieving from" the "database".
 */
@Table(name = MOCK_TABLE)
class MockPersistentEntity extends PersistentEntityImp implements NamedEntity {

    private Goid goid;
    private boolean unsaved;
    private int version;
    private String id;
    private String name;

    MockPersistentEntity() {
    }

    @Override
    public Goid getGoid() {
        return goid;
    }

    @Override
    public void setGoid(Goid goid) {
        this.goid = goid;
    }

    @Override
    public boolean isUnsaved() {
        return unsaved;
    }

    public void setUnsaved(boolean unsaved) {
        this.unsaved = unsaved;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * The cache objects where these will be kept are {@link Map}s, so we have to provide these.
     * They are based only on the {@link #goid}.
     * @param o object to compare <code>this</code> to
     * @return true if <code>this</code> is equal to <code>o</code>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        MockPersistentEntity that = (MockPersistentEntity) o;

        return goid.equals(that.goid);
    }

    /**
     * The cache objects where these will be kept are {@link Map}s, so we have to provide {@link #equals(Object)}
     * and {@link #hashCode()}.
     * They are based only on the {@link #goid}.
     * @return the calculated hashcode for this instance
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + goid.hashCode();
        return result;
    }

    static class Builder {
        private final MockPersistentEntity entity;

        Builder() {
            entity = new MockPersistentEntity();
            entity.setVersion(1);
        }

        Builder withGoid(Goid goid) {
            entity.setGoid(goid);
            return this;
        }

        Builder withId(String id) {
            entity.setId(id);
            return this;
        }

        Builder withName(String name) {
            entity.setName(name);
            return this;
        }

        Builder withVersion(int version) {
            entity.setVersion(version);
            return this;
        }

        MockPersistentEntity build() {
            return entity;
        }

        /**
         * When things are 'saved to' and 'retrieved from' the database, we need to make brand new copies,
         * since putting references to the same actual objects would have a different behaviour
         * from what Hibernate does with an actual DB, where it instantiates a brand new object each time.
         * @param toCopyFrom
         * @return
         */
        MockPersistentEntity build(MockPersistentEntity toCopyFrom) {
            if (toCopyFrom == null) {
                return null;
            }
            return withGoid(toCopyFrom.getGoid()).withId(toCopyFrom.id).withName(toCopyFrom.name)
                    .withVersion(toCopyFrom.getVersion()).build();
        }
    }
}


/**
 * A trivial {@link EntityHeader} implementation which only serves the purpose of
 */
class MockEntityHeader extends EntityHeader {
}


/**
 * A builder for a {@link javax.transaction.TransactionManager TransactionManager}.
 * This is needed only because it's part of the plumbing of instantiating a {@link HibernateEntityManager}.
 */
class TransactionManagerBuilder {
    private final PlatformTransactionManager transactionManager;

    TransactionManagerBuilder() {
        transactionManager = mock(PlatformTransactionManager.class);
    }

    PlatformTransactionManager build() {
        return transactionManager;
    }

}


/**
 * This class does the bulk of the mockout work
 */
class HibernateTemplateBuilder {
    private HibernateTemplate hibernateTemplate;
    private final MultiKeyedMap table;
    private IncrementTracker dbHitsByNameCounter;

    HibernateTemplateBuilder() {
        table = new MultiKeyedMap();
    }

    HibernateTemplateBuilder withGetByNameCallCounter(IncrementTracker getByNameCallCounter) {
        this.dbHitsByNameCounter = getByNameCallCounter;
        return this;
    }

    HibernateTemplate build() {
        final Session session = new MockSessionBuilder(table, dbHitsByNameCounter).build();

        hibernateTemplate = mock(HibernateTemplate.class);

        // calls to execute() may have any of a diversity of criteria that are further analyzed in mockSession()
        when(hibernateTemplate.execute(any(ReadOnlyHibernateCallback.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                @SuppressWarnings("unchecked")
                ReadOnlyHibernateCallback<Object> callback =
                        (ReadOnlyHibernateCallback) invocationOnMock.getArguments()[0];
                return callback.doInHibernate(session);
            }
        });

        // calls to save() put the saved object in this.byGoid so it can be returned by findAllCriteria(),
        // generate a new goid, and return it
        when(hibernateTemplate.save(any(ReadOnlyHibernateCallback.class))).thenAnswer(new Answer<Goid>() {
            @Override
            public Goid answer(InvocationOnMock invocationOnMock) throws Throwable {
                MockPersistentEntity entity = (MockPersistentEntity) invocationOnMock.getArguments()[0];
                Goid goid = entity.getGoid() != null ? entity.getGoid() : randomGoid();
                entity.setGoid(goid);

                MockPersistentEntity entityToSave = new MockPersistentEntity.Builder().build(entity);

                if (!table.findByGoid(goid)) {
                    entityToSave.setVersion(1);
                } else {
                    entityToSave.setVersion(entity.getVersion() + 1);
                }

                table.put(entityToSave);

                return goid;
            }
        });

        // there you go
        return hibernateTemplate;
    }

}

/**
 * This class produces a {@link Session} ready to serve mock results of the HQL queries that are expected
 * in the scenarios that this test suite exercises.
 */
class MockSessionBuilder {

    /**
     * This is a reference to the "table" that's held in {@link HibernateTemplateBuilder}.
     * We add things here and it retrieves them from its own state, but they point to the same data structure.
     */
    private final MultiKeyedMap table;


    /** To keep track of database hits looking for the entity by name (indicating it was not found in the cache) */
    private IncrementTracker dbHitsByNameCounter;

    /**
     * Constructor
     * @param table a reference to the map where entities are held by
     * @param dbHitsByNameCounter a counter to keep track of how many times the cache was missed in a lookup by name
     */
    MockSessionBuilder(MultiKeyedMap table,
                       IncrementTracker dbHitsByNameCounter) {
        this.table = table;
        this.dbHitsByNameCounter = dbHitsByNameCounter;
    }

    Session build() {
        // program response to all Criteria (as opposed to HQL) queries
        Criteria mockCriteria = new MockCriteriaBuilder(table, dbHitsByNameCounter).build();
        Query mockVersionQuery = mockVersionQuery();
        Query mockByGoidQuery = mockByGoidQuery();

        Session session = mock(Session.class);
        when(session.createCriteria(MockPersistentEntity.class)).thenReturn(mockCriteria);

        // program response to version HQL query (for when there's a cache hit and only the version needs to be verified)
        when(session.createQuery(startsWith("SELECT " + MOCK_TABLE + ".version FROM"))).thenReturn(mockVersionQuery);

        // program response to find by Goid HQL query
        when(session.createQuery(startsWith("FROM " + MOCK_TABLE + " IN CLASS com.l7tech.server.MockPersistentEntity")))
                .thenReturn(mockByGoidQuery);

        return session;
    }

    private Query mockVersionQuery() {
        final Query query = mock(Query.class);
        final ReferenceHolder<Goid> goidParameter = new ReferenceHolder<>();

        // when setParameter() is called to get the Goid of the object to be looked up, we store its value in a ReferenceHolder
        when(query.setParameter(eq(0), any(Goid.class))).thenAnswer(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocationOnMock) throws Throwable {
                // argument 0 of setParameter is the parameter name/position, argument 1 is the parameter value (i.e. the Goid)
                Goid goid = (Goid) invocationOnMock.getArguments()[1];
                goidParameter.set(goid);

                return query;
            }
        });

        // when uniqueResult() is called, we grab the Goid from the ReferenceHolder and use it to index our 'table'
        when(query.uniqueResult()).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                MockPersistentEntity entity = table.get(goidParameter.get());
                // clear that ReferenceHolder for the next query
                goidParameter.clear();
                return entity.getVersion();
            }
        });

        return query;
    }

    private Query mockByGoidQuery() {
        final Query query = mock(Query.class);
        final ReferenceHolder<Goid> goidParameter = new ReferenceHolder<>();

        when(query.setParameter(eq(0), any(Goid.class))).thenAnswer(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocationOnMock) throws Throwable {
                Goid goid = (Goid) invocationOnMock.getArguments()[1];
                goidParameter.set(goid);
                return query;
            }
        });

        when(query.uniqueResult()).thenAnswer(new Answer<MockPersistentEntity>() {
            @Override
            public MockPersistentEntity answer(InvocationOnMock invocationOnMock) throws Throwable {
                MockPersistentEntity result = table.get(goidParameter.get());
                goidParameter.clear();
                return result;
            }
        });
        return query;
    }

}


class MockCriteriaBuilder {

    /** This is the object we're building and that will be returned by {@link #build()} */
    private final Criteria criteria;

    /** This list will hold all the Criterion objects that are passed in right before a call to list() or uniqueResult() */
    private final List<Criterion> criterionList;

    /**
     * This is a reference to the "table" that's held in {@link HibernateTemplateBuilder}.
     * We add things here and it retrieves them from its own state, but they point to the same data structure.
     */
    private final MultiKeyedMap table;

    /** To keep track of database hits looking for the entity by name (indicating it was not found in the cache) */
    private final IncrementTracker dbHitsByNameCounter;

    /**
     * Constructor
     * @param table a reference to the map where entities are held
     * @param dbHitsByNameCounter a counter to keep track of how many times the cache was missed in a lookup by name
     */
    MockCriteriaBuilder(MultiKeyedMap table, IncrementTracker dbHitsByNameCounter) {
        criteria = mock(Criteria.class);
        criterionList = new ArrayList<>();
        this.table = table;
        this.dbHitsByNameCounter = dbHitsByNameCounter;
    }

    Criteria build() {
        programAddCriterion();
        programListMethod();
        programUniqueResult();
        return criteria;
    }

    /**
     * Set up mock behaviour so that when {@link Criteria#add(Criterion)} is called, the {@link Criterion} objects
     * that are passed in are accumulated in an internal structure that can later be consulted when one of
     * {@link Criteria#list()} or {@link Criteria#uniqueResult()} are called.
     */
    private void programAddCriterion() {
        when(criteria.add(any(Criterion.class))).thenAnswer(new Answer<Criteria>() {
            @Override
            public Criteria answer(InvocationOnMock invocationOnMock) throws Throwable {
                Criterion criterion = (Criterion) invocationOnMock.getArguments()[0];
                criterionList.add(criterion);
                return criteria;
            }
        });
    }

    /**
     * <p>
     *     Programming the {@link Criteria#list()} method is tricky.
     *     Because Hibernate knows what to list based on Criterion objects that are added before the list() call,
     *     we have to accumulate said "Criterions" in {@link #criterionList}
     *     and parse them from here to figure out what to return exactly.
     * </p>
     * <p>
     *     We only understand the specific Criterion combinations that are produced by the {@link HibernateEntityManager}
     *     in the cases that we exercise in this test suite - this is by no means a complete implementation of a
     *     Criterion parser.
     * </p>
     */
    private void programListMethod() {
        when(criteria.list()).thenAnswer(new Answer<List>() {
            @Override
            public List answer(InvocationOnMock invocationOnMock) throws Throwable {
                // if the criterion list is empty, that means list everything
                if (criterionList.isEmpty()) {
                    return table.all(); // return everything
                }

                // if it's not empty, we need to analyze its contents...
                SimpleExpression expression = parseCriterion();
                String propertyName = findPropertyName(expression);
                verifyOp(expression);
                Object value = Whitebox.getInternalState(expression, "value");

                // get the entity from the cache corresponding to the column it's being looked up by
                MockPersistentEntity entity = propertyName.equals("name")
                        ? new MockPersistentEntity.Builder().build(table.get((String) value))
                        : new MockPersistentEntity.Builder().build(table.get((Goid) value));

                // return an empty list if not found (not a list with a single null element)
                if (entity == null) {
                    return new ArrayList();
                }

                // otherwise return a list containing the single element that matches the search criteria
                return new ListBuilder().add(entity).build();
            }
        });
    }

    /**
     * <p>
     *     Programming {@link Criteria#uniqueResult()} involves looking at the Criterion objects
     *     in {@link #criterionList} and finding out whether the desired retrieval is by Goid or by Name,
     *     and which Goid or Name is being looked up.
     *  </p>
     *  <p>
     *     Additionally, in order to verify correctness of caching functionality, we need to count the number of times
     *     the uniqueResult() method is getting called (as it does not get called when the object is found in the cache).
     *     Because the call is buried so deep in objects that are created on the fly as local variables
     *     in HibernateEntityManager methods, we can't use Mockito's verify() functionality and we have to roll out
     *     our own call counting mechanism.
     *  </p>
     */
    private void programUniqueResult() {
        when(criteria.uniqueResult()).thenAnswer(new Answer<Object>() {
            @Override
            public MockPersistentEntity answer(InvocationOnMock invocationOnMock) throws Throwable {
                SimpleExpression expression = parseCriterion();
                String propertyName = findPropertyName(expression);
                verifyOp(expression);

                if (propertyName.equals("name")) {
                    String name = (String) Whitebox.getInternalState(expression, "value");
                    if (dbHitsByNameCounter != null) {
                        dbHitsByNameCounter.increment();
                    }
                    return new MockPersistentEntity.Builder().build(table.get(name));
                }

                // else...
                Goid goid = (Goid) Whitebox.getInternalState(expression, "value");
                return new MockPersistentEntity.Builder().build(table.get(goid));

            }
        });
    }

    /**
     * "Parse" the {@link Criterion} objects in the {@link #criterionList}.
     * Not really a parser, rather we look for the very specific values that we know are created by the code paths
     * that we exercise in this test suite.
     * @return
     */
    private SimpleExpression parseCriterion() {
        if (criterionList.size() != 1) {
            throw new RuntimeException("I don't understand your formula");
        }

        Criterion criterion = criterionList.get(0);
        criterionList.clear();

        if (criterion instanceof SimpleExpression) {
            return (SimpleExpression) criterion;
        }

        if (!(criterion instanceof Disjunction)) {
            throw new RuntimeException("I don't understand your formula");
        }
        Disjunction disjunction = (Disjunction) criterion;

        List criteria = (List) Whitebox.getInternalState(disjunction, "criteria");
        if (criteria.size() != 1) {
            throw new RuntimeException("I don't understand your formula");
        }

        Conjunction conjunction = (Conjunction) criteria.get(0);
        criteria = (List) Whitebox.getInternalState(conjunction, "criteria");

        return (SimpleExpression) criteria.get(0);
    }

    /**
     * Throw a RuntimeException if the operator is anything other than equals.
     * @param expression the Criterion expression you're trying to check for equals.
     * @return the string "=" - or throw
     * @throws RuntimeException if the op is anything other than equals
     */
    private String verifyOp(SimpleExpression expression) {
        String op = (String) Whitebox.getInternalState(expression, "op");
        if ((!"=".equals(op))) {
            throw new RuntimeException("I don't understand your formula");
        }
        return op;
    }

    /**
     * Returns the name of the property that's being looked for by this expression.
     * Only understands "name" and "goid"
     * @param expression the equals expression you want to find the property name for
     * @return "name" or "goid", whichever is the property name being looked up by
     * @throws RuntimeException if the property name is neither "name" nor "goid"
     */
    @NotNull
    private String findPropertyName(SimpleExpression expression) {
        String propertyName = (String) Whitebox.getInternalState(expression, "propertyName");
        if ((!"name".equals(propertyName) && !"giod".equals(propertyName))) {
            throw new RuntimeException("I don't understand your formula");
        }
        return propertyName;
    }

}


/**
 * A simple list builder to allow chained creation of lists.
 * TODO: this class is repeated, it need to be promoted to some globally-visible utils class somewhere
 * @param <T>
 */
class ListBuilder<T> {

    private List<T> list;

    @SuppressWarnings("unchecked")
    ListBuilder() {
        list = new ArrayList<>();
    }

    ListBuilder<T> add(final T element) {
        list.add(element);
        return this;
    }

    List<T> build() {
        return list;
    }
}


/**
 * Just a mutable container for an integer because the call that we have to keep track of
 * is so deep in the guts of {@link HibernateEntityManager} that we can't use Mockito's call verification mechanism.
 */
class IncrementTracker {
    private int number;

    IncrementTracker() {
        this.number = 0;
    }

    int get() {
        return number;
    }

    void increment() {
        number++;
    }
}

/**
 * Trivial container object to make variables decalred final modifiable.
 * These are necessary because method-local variables referenced from within an anonymous class through a closure
 * must be declared final.
 * @param <T> the type of whatever you want to hold in this container
 */
class ReferenceHolder<T> {
    private T reference;

    ReferenceHolder() {
    }

    public T get() {
        if (reference == null) {
            throw new IllegalStateException("Not set");
        }
        return reference;
    }

    public void set(T reference) {
        this.reference = reference;
    }

    public boolean isSet() {
        return reference != null;
    }

    public void clear() {
        reference = null;
    }

}

class MultiKeyedMap {
    private final Map<Goid, MockPersistentEntity> goidMap;
    private final Map<String, MockPersistentEntity> nameMap;

    MultiKeyedMap() {
        this.goidMap = new HashMap<>();
        this.nameMap = new HashMap<>();
    }

    MockPersistentEntity get(Goid goid) {
        return goidMap.get(goid);
    }

    MockPersistentEntity get(String name) {
        return nameMap.get(name);
    }

    void put(MockPersistentEntity entity) {
        MockPersistentEntity oldEntity = goidMap.get(entity.getGoid());
        if (oldEntity != null && nameMap.containsKey(oldEntity.getName())) {
            nameMap.remove(oldEntity.getName());
        }
        goidMap.put(entity.getGoid(), entity);
        nameMap.put(entity.getName(), entity);
    }

    List<MockPersistentEntity> all() {
        return new ArrayList<>(goidMap.values());
    }

    public boolean findByGoid(Goid goid) {
        return goidMap.containsKey(goid);
    }

    public boolean findByName(String name) {
        return nameMap.containsKey(name);
    }
}

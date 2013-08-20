package com.l7tech.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test for PolicyUtil.
 */
@RunWith(MockitoJUnitRunner.class)
public class PolicyUtilTest {
    @Test
    public void testVisitLeaf() throws Exception {
        PolicyUtil.visitDescendantsAndSelf(leafAssertion, visitor, translator);
        assertEquals(1, visitOrder.size());
        assertEquals(leafAssertion, visitOrder.get(0));
        assertEquals("Leaf assertion shall have been visited exactly once", 1, (long)visitCounts.get(leafAssertion));
    }

    @BugNumber(9111)
    @Test
    public void testVisitDisabledLeaf() throws Exception {
        leafAssertion.setEnabled(false);
        PolicyUtil.visitDescendantsAndSelf(leafAssertion, visitor, translator);
        assertEquals("Disabled assertions shall not be visited", 0, visitOrder.size());
    }

    @Test
    public void testVisitSubtree() throws Exception {
        PolicyUtil.visitDescendantsAndSelf(simplePolicy, visitor, translator);
        assertEquals(5, visitOrder.size());
        assertEquals(simplePolicy, visitOrder.get(0));
        assertEquals("Leaf assertion shall have been visited exactly once", 1, (long)visitCounts.get(leafAssertion));
    }

    @Test
    public void testVisitInclude() throws Exception {
        PolicyUtil.visitDescendantsAndSelf(includePolicy, visitor, translator);
        assertEquals(17, visitOrder.size());
        assertEquals(includePolicy, visitOrder.get(0));
        assertEquals("Leaf assertion shall have been visited twice since its fragment was included twice", 2, (long)visitCounts.get(leafAssertion));
    }

    @Test
    public void testVisitIncludeWithDisabledTarget() throws Exception {
        simplePolicy.setEnabled(false);
        PolicyUtil.visitDescendantsAndSelf(includePolicy, visitor, translator);
        assertNull("Children of disabled include target shall not have been visited", visitCounts.get(leafAssertion));
        assertEquals(7, visitOrder.size());
    }

    @Mock
    private UsesEntitiesAtDesignTime entityUser;

    @Mock
    private HeaderBasedEntityFinder entityProvider;

    @Mock
    private Functions.BinaryVoid<EntityHeader, FindException> errorHandler;

    private final EntityHeader encassHeader = new EntityHeader(new Goid(22,10), EntityType.ENCAPSULATED_ASSERTION, "Foo Encass", "foo qwer");
    private final EncapsulatedAssertionConfig encassEntity = new EncapsulatedAssertionConfig();
    private final EntityHeader genericHeader = new GenericEntityHeader("20", "Foo Generic", "generic thing", 33, "com.l7tech.blah.Blah");
    private final GenericEntity genericEntity = new GenericEntity();

    @Test
    public void testProvideNeededEntities() throws Exception {
        when(entityUser.needsProvideEntity(Matchers.<EntityHeader>any())).thenReturn(true);
        when(entityUser.getEntitiesUsedAtDesignTime()).thenReturn(new EntityHeader[] { encassHeader, genericHeader });
        when(entityProvider.find(encassHeader)).thenReturn(encassEntity);
        when(entityProvider.find(genericHeader)).thenReturn(genericEntity);

        PolicyUtil.provideNeededEntities(entityUser, entityProvider, errorHandler);

        verify(entityUser).provideEntity(encassHeader, encassEntity);
        verify(entityUser).provideEntity(genericHeader, genericEntity);
        verify(errorHandler, never()).call(any(EntityHeader.class), any(FindException.class));
    }

    @Test
    public void testProvideNeededEntitiesOneEntityNotFound() throws Exception {
        when(entityUser.needsProvideEntity(Matchers.<EntityHeader>any())).thenReturn(true);
        when(entityUser.getEntitiesUsedAtDesignTime()).thenReturn(new EntityHeader[] {encassHeader, genericHeader});
        when(entityProvider.find(encassHeader)).thenReturn(encassEntity);
        when(entityProvider.find(genericHeader)).thenReturn(null);

        PolicyUtil.provideNeededEntities(entityUser, entityProvider, errorHandler);

        verify(entityUser).provideEntity(encassHeader, encassEntity);
        verify(entityUser, never()).provideEntity(genericHeader, genericEntity);
        verify(errorHandler, atLeast(1)).call(eq(genericHeader), any(FindException.class));
        verify(errorHandler, never()).call(eq(encassHeader), any(FindException.class));
    }

    @Test
    public void testProvideNeededEntitiesFindException() throws Exception {
        when(entityUser.needsProvideEntity(Matchers.<EntityHeader>any())).thenReturn(true);
        when(entityUser.getEntitiesUsedAtDesignTime()).thenReturn(new EntityHeader[] {encassHeader, genericHeader});
        when(entityProvider.find(encassHeader)).thenReturn(encassEntity);
        FindException findException = new FindException();
        when(entityProvider.find(genericHeader)).thenThrow(findException);

        PolicyUtil.provideNeededEntities(entityUser, entityProvider, errorHandler);

        verify(entityUser).provideEntity(encassHeader, encassEntity);
        verify(entityUser, never()).provideEntity(genericHeader, genericEntity);
        verify(errorHandler, atLeast(1)).call(genericHeader, findException);
        verify(errorHandler, never()).call(eq(encassHeader), any(FindException.class));
    }

    @Test
    public void testProvideNeededEntitiesNoErrorHandler() throws Exception {
        when(entityUser.needsProvideEntity(Matchers.<EntityHeader>any())).thenReturn(true);
        when(entityUser.getEntitiesUsedAtDesignTime()).thenReturn(new EntityHeader[] {encassHeader, genericHeader});
        when(entityProvider.find(encassHeader)).thenReturn(encassEntity);
        FindException findException = new FindException();
        when(entityProvider.find(genericHeader)).thenThrow(findException);

        try {
            PolicyUtil.provideNeededEntities(entityUser, entityProvider, null);
            fail("expected exception not thrown");
        } catch (FindException e) {
            assertTrue(e == findException);
        }

        verify(entityUser).provideEntity(encassHeader, encassEntity);
        verify(entityUser, never()).provideEntity(genericHeader, genericEntity);
    }

    private static class MyEntityUsingAssertion extends Assertion implements UsesEntitiesAtDesignTime {
        public EntityHeader[] getEntitiesUsedAtDesignTime() { return new EntityHeader[0]; }
        public boolean needsProvideEntity(@NotNull EntityHeader header) { return false; }
        public void provideEntity(@NotNull EntityHeader header, @NotNull Entity entity) {}
        public EntityHeader[] getEntitiesUsed() { return new EntityHeader[0]; }
        public void replaceEntity(EntityHeader oldEntityHeader, EntityHeader newEntityHeader) { }
    }

    @Spy
    MyEntityUsingAssertion entityUsingAssertion;

    @Test
    public void testTestProvideNeededEntitiesToAssertion() throws Exception {
        when(entityUsingAssertion.getEntitiesUsedAtDesignTime()).thenReturn(new EntityHeader[] { encassHeader, genericHeader });
        when(entityUsingAssertion.needsProvideEntity(encassHeader)).thenReturn(true);
        when(entityUsingAssertion.needsProvideEntity(genericHeader)).thenReturn(true);
        when(entityProvider.find(encassHeader)).thenReturn(encassEntity);
        when(entityProvider.find(genericHeader)).thenReturn(genericEntity);

        PolicyUtil.provideNeededEntities((Assertion)entityUsingAssertion, entityProvider, errorHandler);

        verify(entityUsingAssertion).provideEntity(encassHeader, encassEntity);
        verify(entityUsingAssertion).provideEntity(genericHeader, genericEntity);
        verify(errorHandler, never()).call(any(EntityHeader.class), any(FindException.class));
    }

    @Test
    public void testTestProvideNeededEntitiesToAssertionWithErrorHandler() throws Exception {
        when(entityUsingAssertion.getEntitiesUsedAtDesignTime()).thenReturn(new EntityHeader[] { encassHeader, genericHeader });
        when(entityUsingAssertion.needsProvideEntity(encassHeader)).thenReturn(true);
        when(entityUsingAssertion.needsProvideEntity(genericHeader)).thenReturn(true);
        when(entityProvider.find(encassHeader)).thenReturn(encassEntity);
        when(entityProvider.find(genericHeader)).thenReturn(null);

        PolicyUtil.provideNeededEntities((Assertion)entityUsingAssertion, entityProvider, errorHandler);

        verify(entityUsingAssertion).provideEntity(encassHeader, encassEntity);
        verify(entityUsingAssertion, never()).provideEntity(genericHeader, genericEntity);
        verify(errorHandler, atLeast(1)).call(eq(genericHeader), any(FindException.class));
        verify(errorHandler, never()).call(eq(encassHeader), any(FindException.class));
    }

    List<Assertion> visitOrder = new ArrayList<Assertion>();
    Map<Assertion,Integer> visitCounts = new HashMap<Assertion,Integer>();

    Assertion simplePolicy = new AllAssertion(Arrays.<Assertion>asList(
            new FalseAssertion(),
            new OneOrMoreAssertion(Arrays.asList(
                    leafAssertion = new TrueAssertion(),
                    new TrueAssertion()
            ))
    ));
    Assertion leafAssertion;

    // Include the same fragment twice, just to keep things interesting
    Assertion includePolicy = new AllAssertion(Arrays.<Assertion>asList(
            new TrueAssertion(),
            new OneOrMoreAssertion(Arrays.asList(
                    new TrueAssertion(),
                    includeAssertion = new Include("asdf"),
                    new FalseAssertion()
            )),
            new Include("asdf")
    ));
    Assertion includeAssertion;

    Map<Assertion,Integer> translateCount = new HashMap<Assertion, Integer>();
    Map<Assertion,Integer> translationFinishedCount = new HashMap<Assertion, Integer>();
    AssertionTranslator translator = new AssertionTranslator() {
        @Override
        public Assertion translate(@Nullable Assertion sourceAssertion) throws PolicyAssertionException {
            count(sourceAssertion, translateCount);
            if (sourceAssertion instanceof Include) {
                Include include = (Include) sourceAssertion;
                if ("asdf".equals(include.getPolicyGuid()))
                    return simplePolicy;
            }
            return sourceAssertion;
        }

        @Override
        public void translationFinished(@Nullable Assertion sourceAssertion) {
            count(sourceAssertion, translationFinishedCount);
        }
    };

    Functions.UnaryVoid<Assertion> visitor = new Functions.UnaryVoid<com.l7tech.policy.assertion.Assertion>() {
        @Override
        public void call(Assertion assertion) {
            visitOrder.add(assertion);
            count(assertion, visitCounts);
        }
    };

    static void count(Assertion assertion, Map<Assertion, Integer> counter) {
        if (!counter.containsKey(assertion))
            counter.put(assertion, 1);
        else
            counter.put(assertion, counter.get(assertion) + 1);
    }
}

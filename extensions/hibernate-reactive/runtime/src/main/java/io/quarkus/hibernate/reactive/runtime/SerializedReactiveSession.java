package io.quarkus.hibernate.reactive.runtime;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Attribute;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.UnknownProfileException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.DeleteContext;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.event.spi.PersistContext;
import org.hibernate.event.spi.RefreshContext;
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.reactive.common.AffectedEntities;
import org.hibernate.reactive.common.ResultSetMapping;
import org.hibernate.reactive.engine.ReactiveActionQueue;
import org.hibernate.reactive.pool.ReactiveConnection;
import org.hibernate.reactive.query.ReactiveMutationQuery;
import org.hibernate.reactive.query.ReactiveNativeQuery;
import org.hibernate.reactive.query.ReactiveQuery;
import org.hibernate.reactive.query.ReactiveQueryImplementor;
import org.hibernate.reactive.query.ReactiveSelectionQuery;
import org.hibernate.reactive.session.ReactiveSession;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * This class is not thread safe, it serialises atomic operations from the same thread,
 * so make sure only a single operation is in progress on the session at any given time
 */
public class SerializedReactiveSession implements ReactiveSession {

    private CompletionStage<?> currentCompletionStage;
    final Deque<Supplier<CompletionStage<?>>> workQueue = new ArrayDeque<>();
    final ReactiveSession delegate;

    final BiConsumer completionTask = new BiConsumer() {
        @Override
        public void accept(Object o, Object o2) {
            var task = workQueue.pollFirst();
            if (task == null) {
                currentCompletionStage = null;
            } else {
                currentCompletionStage = task.get();
            }
        }
    };

    public SerializedReactiveSession(ReactiveSession delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> CompletionStage<List<T>> reactiveFind(Class<T> entityClass, Object... ids) {
        if (currentCompletionStage == null) {
            //nothing in progress, just run the operation directly
            CompletionStage<List<T>> ret = delegate.reactiveFind(entityClass, ids);
            currentCompletionStage = ret;
            currentCompletionStage.whenComplete(completionTask);
            return ret;
        }
        //we need to queue the operation
        CompletableFuture<List<T>> ret = new CompletableFuture<List<T>>();
        workQueue.push(() -> {
            var realResult = delegate.reactiveFind(entityClass, ids);
            realResult.whenComplete(new BiConsumer<List<T>, Throwable>() {
                @Override
                public void accept(List<T> ts, Throwable throwable) {
                    if (throwable != null) {
                        ret.completeExceptionally(throwable);
                    } else {
                        ret.complete(ts);
                    }
                }
            });
            return realResult;
        });

        return ret;
    }


    @Override
    public ReactiveActionQueue getReactiveActionQueue() {
        return null;
    }

    @Override
    public SessionFactoryImplementor getFactory() {
        return null;
    }

    @Override
    public SessionImplementor getSharedContract() {
        return null;
    }

    @Override
    public Dialect getDialect() {
        return null;
    }

    @Override
    public <T> CompletionStage<T> reactiveFetch(T association, boolean unproxy) {
        return null;
    }

    @Override
    public CompletionStage<Object> reactiveInternalLoad(String entityName, Object id, boolean eager, boolean nullable) {
        return null;
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> entity) {
        return null;
    }

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> entity, String name) {
        return null;
    }

    @Override
    public <T> EntityGraph<T> getEntityGraph(Class<T> entity, String name) {
        return null;
    }

    @Override
    public <R> ReactiveQuery<R> createReactiveQuery(String queryString) {
        return null;
    }

    @Override
    public <R> ReactiveQuery<R> createReactiveQuery(CriteriaQuery<R> criteriaQuery) {
        return null;
    }

    @Override
    public <R> ReactiveQuery<R> createReactiveQuery(String queryString, Class<R> resultType) {
        return null;
    }

    @Override
    public <R> ReactiveQueryImplementor<R> createReactiveNamedQuery(String queryString, Class<R> resultType) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String sqlString) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String sqlString, Class<R> resultClass) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String sqlString, String resultSetMappingName) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
        return null;
    }

    @Override
    public <R> ReactiveSelectionQuery<R> createReactiveSelectionQuery(String hqlString) {
        return null;
    }

    @Override
    public <R> ReactiveSelectionQuery<R> createReactiveSelectionQuery(String hqlString, Class<R> resultType) {
        return null;
    }

    @Override
    public <R> ReactiveSelectionQuery<R> createReactiveSelectionQuery(CriteriaQuery<R> criteria) {
        return null;
    }

    @Override
    public <R> ReactiveMutationQuery<R> createReactiveMutationQuery(String hqlString) {
        return null;
    }

    @Override
    public <R> ReactiveMutationQuery<R> createReactiveMutationQuery(CriteriaUpdate<R> updateQuery) {
        return null;
    }

    @Override
    public <R> ReactiveMutationQuery<R> createReactiveMutationQuery(CriteriaDelete<R> deleteQuery) {
        return null;
    }

    @Override
    public <R> ReactiveMutationQuery<R> createReactiveMutationQuery(JpaCriteriaInsertSelect<R> insertSelect) {
        return null;
    }

    @Override
    public <R> ReactiveMutationQuery<R> createNativeReactiveMutationQuery(String sqlString) {
        return null;
    }

    @Override
    public <R> ReactiveSelectionQuery<R> createNamedReactiveSelectionQuery(String name) {
        return null;
    }

    @Override
    public <R> ReactiveSelectionQuery<R> createNamedReactiveSelectionQuery(String name, Class<R> resultType) {
        return null;
    }

    @Override
    public <R> ReactiveMutationQuery<R> createNamedReactiveMutationQuery(String name) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String queryString, AffectedEntities affectedEntities) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String queryString, Class<R> resultType, AffectedEntities affectedEntities) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String queryString, ResultSetMapping<R> resultSetMapping) {
        return null;
    }

    @Override
    public <R> ReactiveNativeQuery<R> createReactiveNativeQuery(String queryString, ResultSetMapping<R> resultSetMapping, AffectedEntities affectedEntities) {
        return null;
    }

    @Override
    public <T> ResultSetMapping<T> getResultSetMapping(Class<T> resultType, String mappingName) {
        return null;
    }

    @Override
    public <E, T> CompletionStage<T> reactiveFetch(E entity, Attribute<E, T> field) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactivePersist(Object entity) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactivePersist(Object object, PersistContext copiedAlready) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactivePersistOnFlush(Object entity, PersistContext copiedAlready) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveRemove(Object entity) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveRemove(String entityName, boolean isCascadeDeleteEnabled, DeleteContext transientObjects) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveRemove(String entityName, Object child, boolean isCascadeDeleteEnabled, DeleteContext transientEntities) {
        return null;
    }

    @Override
    public <T> CompletionStage<T> reactiveMerge(T object) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveMerge(Object object, MergeContext copiedAlready) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveFlush() {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveAutoflush() {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveForceFlush(EntityEntry entry) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveRefresh(Object entity, LockOptions lockMode) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveRefresh(Object child, RefreshContext refreshedAlready) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveLock(Object entity, LockOptions lockMode) {
        return null;
    }

    @Override
    public <T> CompletionStage<T> reactiveGet(Class<T> entityClass, Object id) {
        return null;
    }

    @Override
    public <T> CompletionStage<T> reactiveFind(Class<T> entityClass, Object id, LockOptions lockOptions, EntityGraph<T> fetchGraph) {
        return null;
    }


    @Override
    public <T> CompletionStage<T> reactiveFind(Class<T> entityClass, Map<String, Object> naturalIds) {
        return null;
    }

    @Override
    public CompletionStage<Object> reactiveImmediateLoad(String entityName, Object id) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveInitializeCollection(PersistentCollection<?> collection, boolean writing) {
        return null;
    }

    @Override
    public CompletionStage<Void> reactiveRemoveOrphanBeforeUpdates(String entityName, Object child) {
        return null;
    }

    @Override
    public void setHibernateFlushMode(FlushMode flushMode) {

    }

    @Override
    public FlushMode getHibernateFlushMode() {
        return null;
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {

    }

    @Override
    public CacheMode getCacheMode() {
        return null;
    }

    @Override
    public Integer getBatchSize() {
        return null;
    }

    @Override
    public void setBatchSize(Integer batchSize) {

    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object id) {
        return null;
    }

    @Override
    public void detach(Object entity) {

    }

    @Override
    public boolean isDefaultReadOnly() {
        return false;
    }

    @Override
    public void setDefaultReadOnly(boolean readOnly) {

    }

    @Override
    public void setReadOnly(Object entityOrProxy, boolean readOnly) {

    }

    @Override
    public boolean isReadOnly(Object entityOrProxy) {
        return false;
    }

    @Override
    public String getEntityName(Object entity) {
        return null;
    }

    @Override
    public Object getIdentifier(Object entity) {
        return null;
    }

    @Override
    public boolean contains(Object entity) {
        return false;
    }

    @Override
    public <T> Class<? extends T> getEntityClass(T entity) {
        return null;
    }

    @Override
    public Object getEntityId(Object entity) {
        return null;
    }

    @Override
    public LockMode getCurrentLockMode(Object entity) {
        return null;
    }

    @Override
    public Filter enableFilter(String filterName) {
        return null;
    }

    @Override
    public void disableFilter(String filterName) {

    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        return null;
    }

    @Override
    public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
        return false;
    }

    @Override
    public void enableFetchProfile(String name) throws UnknownProfileException {

    }

    @Override
    public void disableFetchProfile(String name) throws UnknownProfileException {

    }

    @Override
    public void clear() {

    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public CompletionStage<Void> reactiveClose() {
        return null;
    }

    @Override
    public PersistenceContext getPersistenceContext() {
        return null;
    }

    @Override
    public ReactiveConnection getReactiveConnection() {
        return null;
    }
}

package io.quarkus.rest.server.runtime.jaxrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class QuarkusRestAsyncResponse implements AsyncResponse, Handler<Long> {

    private final QuarkusRestRequestContext context;
    private volatile boolean suspended;
    private volatile boolean cancelled;
    private volatile TimeoutHandler timeoutHandler;
    // only used with lock, no need for volatile
    private long timerId = -1;

    public QuarkusRestAsyncResponse(QuarkusRestRequestContext context) {
        this.context = context;
        suspended = true;
    }

    @Override
    public synchronized boolean resume(Object response) {
        if (!suspended) {
            return false;
        } else {
            suspended = false;
        }
        cancelTimer();
        context.setResult(response);
        context.resume();
        return true;
    }

    @Override
    public synchronized boolean resume(Throwable response) {
        if (!suspended) {
            return false;
        } else {
            suspended = false;
        }
        cancelTimer();
        context.handleException(response);
        context.resume();
        return true;
    }

    @Override
    public boolean cancel() {
        return internalCancel(null);
    }

    @Override
    public boolean cancel(int retryAfter) {
        return internalCancel(retryAfter);
    }

    private synchronized boolean internalCancel(Object retryAfter) {
        if (cancelled) {
            return true;
        }
        if (!suspended) {
            return false;
        }
        cancelTimer();
        suspended = false;
        cancelled = true;
        ResponseBuilder response = Response.status(503);
        if (retryAfter != null)
            response.header(HttpHeaders.RETRY_AFTER, retryAfter);
        // It's not clear if we should go via the exception handlers here, but our TCK setup makes us
        // go through it, while RESTEasy doesn't because it does resume like this, so we do too
        context.setResult(response.build());
        context.resume();
        return true;
    }

    @Override
    public boolean cancel(Date retryAfter) {
        return internalCancel(retryAfter);
    }

    // CALL WITH LOCK
    private void cancelTimer() {
        if (timerId != -1) {
            context.getContext().vertx().cancelTimer(timerId);
            timerId = -1;
        }
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        // we start suspended and stop being suspended on resume/cancel/timeout(which resumes) so
        // this flag is enough to know if we're done
        return !suspended;
    }

    @Override
    public synchronized boolean setTimeout(long time, TimeUnit unit) {
        if (!suspended)
            return false;
        Vertx vertx = context.getContext().vertx();
        if (timerId != -1)
            vertx.cancelTimer(timerId);
        timerId = vertx.setTimer(TimeUnit.MILLISECONDS.convert(time, unit), this);
        return true;
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {
        timeoutHandler = handler;
    }

    @Override
    public Collection<Class<?>> register(Class<?> callback) {
        Objects.requireNonNull(callback);
        // FIXME: does this mean we should use CDI to look it up?
        try {
            return register(callback.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks) {
        Objects.requireNonNull(callback);
        Objects.requireNonNull(callbacks);
        Map<Class<?>, Collection<Class<?>>> ret = new HashMap<>();
        ret.put(callback.getClass(), register(callback));
        for (Class<?> cb : callbacks) {
            ret.put(cb, register(cb));
        }
        return ret;
    }

    @Override
    public Collection<Class<?>> register(Object callback) {
        Objects.requireNonNull(callback);
        List<Class<?>> ret = new ArrayList<>(2);
        if (callback instanceof ConnectionCallback) {
            context.registerConnectionCallback((ConnectionCallback) callback);
            ret.add(ConnectionCallback.class);
        }
        if (callback instanceof CompletionCallback) {
            context.registerCompletionCallback((CompletionCallback) callback);
            ret.add(CompletionCallback.class);
        }
        return ret;
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks) {
        Objects.requireNonNull(callback);
        Objects.requireNonNull(callbacks);
        Map<Class<?>, Collection<Class<?>>> ret = new HashMap<>();
        ret.put(callback.getClass(), register(callback));
        for (Object cb : callbacks) {
            ret.put(cb.getClass(), register(cb));
        }
        return ret;
    }

    @Override
    public synchronized void handle(Long event) {
        // perhaps it's possible that we updated a timer and we're getting notified with the
        // previous timer we registered, in which case let's wait for the latest timer registered
        if (event.longValue() != timerId)
            return;
        // make sure we're not marked as waiting for a timer anymore
        timerId = -1;
        // if we're not suspended anymore, or we were cancelled, drop it
        if (!suspended || cancelled)
            return;
        if (timeoutHandler != null) {
            timeoutHandler.handleTimeout(this);
            // Spec says:
            // In case the time-out handler does not take any of the actions mentioned above [resume/new timeout], 
            // a default time-out strategy is executed by the runtime.
            // Stef picked to do this if the handler did not resume or set a new timeout:
            if (suspended && timerId == -1)
                resume(new ServiceUnavailableException());
        } else {
            resume(new ServiceUnavailableException());
        }
    }
}

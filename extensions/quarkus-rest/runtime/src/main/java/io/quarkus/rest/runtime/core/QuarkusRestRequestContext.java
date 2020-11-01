package io.quarkus.rest.runtime.core;

import java.io.Closeable;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.rest.runtime.core.serialization.EntityWriter;
import io.quarkus.rest.runtime.handlers.RestHandler;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestAsyncResponse;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestContainerRequestContext;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestHttpHeaders;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestProviders;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestRequest;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestSseEventSink;
import io.quarkus.rest.runtime.jaxrs.QuarkusRestUriInfo;
import io.quarkus.rest.runtime.mapping.RuntimeResource;
import io.quarkus.rest.runtime.mapping.URITemplate;
import io.quarkus.rest.runtime.util.EmptyInputStream;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

public class QuarkusRestRequestContext implements Runnable, Closeable {
    private static final Logger log = Logger.getLogger(QuarkusRestRequestContext.class);
    public static final Object[] EMPTY_ARRAY = new Object[0];
    private final QuarkusRestDeployment deployment;
    private final QuarkusRestProviders providers;
    private final RoutingContext context;
    private final ManagedContext requestContext;
    private final CurrentVertxRequest currentVertxRequest;
    private InjectableContext.ContextState currentRequestScope;
    /**
     * The parameters array, populated by handlers
     */
    private Object[] parameters;
    private RuntimeResource target;
    private RestHandler[] handlers;

    /**
     * The parameter values extracted from the path.
     *
     * This is not a map, for two reasons. One is raw performance, as an array causes
     * less allocations and is generally faster. The other is that it is possible
     * that you can have equivalent templates with different names. This allows the
     * mapper to ignore the names, as everything is resolved in terms of indexes.
     * 
     * If there is only a single path param then it is stored directly into the field,
     * while multiple params this will be an array. This optimisation allows us to avoid
     * allocating anything in the common case that there is zero or one path param.
     */
    private Object pathParamValues;

    private UriInfo uriInfo;
    /**
     * The endpoint to invoke
     */
    private Object endpointInstance;
    /**
     * The result of the invocation
     */
    private Object result;
    private boolean suspended = false;
    private volatile boolean resetRequestContext = false;
    private volatile boolean running = false;
    private volatile Executor executor;
    private int position;
    private Throwable throwable;
    private QuarkusRestHttpHeaders httpHeaders;
    private Object requestEntity;
    private Map<String, Object> properties;
    private Request request;
    private EntityWriter entityWriter;
    private QuarkusRestContainerRequestContext containerRequestContext;
    private String method;
    private String path;
    private String remaining;
    private MediaType producesMediaType;
    private MediaType consumesMediaType;

    private Annotation[] annotations;
    private Type genericReturnType;

    /**
     * The input stream, if an entity is present.
     */
    private InputStream inputStream = EmptyInputStream.INSTANCE;

    /**
     * used for {@link UriInfo#getMatchedURIs()}
     */
    private List<UriMatch> matchedURIs;

    private QuarkusRestAsyncResponse asyncResponse;
    private QuarkusRestSseEventSink sseEventSink;

    public QuarkusRestRequestContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers, RoutingContext context,
            ManagedContext requestContext,
            CurrentVertxRequest currentVertxRequest, RestHandler... handlerChain) {
        this.deployment = deployment;
        this.providers = providers;
        this.context = context;
        this.requestContext = requestContext;
        this.currentVertxRequest = currentVertxRequest;
        this.handlers = handlerChain;
        this.parameters = EMPTY_ARRAY;
    }

    public QuarkusRestDeployment getDeployment() {
        return deployment;
    }

    public QuarkusRestProviders getProviders() {
        return providers;
    }

    public void suspend() {
        suspended = true;
    }

    public void resume() {
        resume(null, null);
    }

    public synchronized void resume(Executor executor) {
        resume(executor, null);
    }

    public synchronized void resume(Throwable throwable) {
        resume(null, throwable);
    }

    public synchronized void resume(Executor executor, Throwable throwable) {
        if (throwable != null) {
            this.throwable = throwable;
        }
        if (running) {
            this.executor = executor;
            if (executor == null) {
                suspended = false;
            }
        } else {
            suspended = false;
            resetRequestContext = true;
            if (executor == null) {
                ((ConnectionBase) context.request().connection()).getContext().nettyEventLoop().execute(this);
            } else {
                executor.execute(this);
            }
        }
    }

    @Override
    public void run() {
        running = true;
        //if this is a blocking target we don't activate for the initial non-blocking part
        //unless there are pre-mapping filters as these may require CDI
        boolean activationRequired = target == null || (target.isBlocking() && executor == null) || resetRequestContext;
        if (activationRequired) {
            if (currentRequestScope == null) {
                requestContext.activate();
                currentVertxRequest.setCurrent(context, this);
            } else {
                requestContext.activate(currentRequestScope);
            }
            resetRequestContext = false;
        }
        try {
            while (position < handlers.length) {
                int pos = position;
                position++; //increment before, as reset may reset it to zero
                try {
                    handlers[pos].handle(this);
                    if (suspended) {
                        Executor exec = null;
                        synchronized (this) {
                            if (this.executor != null) {
                                //resume happened in the meantime
                                suspended = false;
                                exec = this.executor;
                            } else if (suspended) {
                                running = false;
                                return;
                            }
                        }
                        if (exec != null) {
                            //outside sync block
                            exec.execute(this);
                            return;
                        }
                    }
                } catch (Throwable t) {
                    if (handlers == deployment.getAbortHandlerChain()) {
                        handleException(t);
                        return;
                    } else {
                        invokeExceptionMapper(t);
                        restart(deployment.getAbortHandlerChain());
                    }
                }
            }
        } catch (Throwable t) {
            handleException(t);
            close();
        } finally {
            running = false;
            if (activationRequired) {
                if (position == handlers.length) {
                    requestContext.terminate();
                    close();
                } else {
                    currentRequestScope = requestContext.getState();
                    requestContext.deactivate();
                }
            }
        }
    }

    /**
     * Restarts handler chain processing on a chain that does not target a specific resource
     *
     * Generally used to abort processing.
     *
     * @param newHandlerChain The new handler chain
     */
    public void restart(RestHandler[] newHandlerChain) {
        this.handlers = newHandlerChain;
        position = 0;
        parameters = new Object[0];
        target = null;
    }

    /**
     * Restarts handler chain processing with a new chain targeting a new resource.
     *
     * @param target The resource target
     */
    public void restart(RuntimeResource target) {
        this.handlers = target.getHandlerChain();
        position = 0;
        parameters = new Object[target.getParameterTypes().length];
        this.target = target;
    }

    /**
     * Resets the build time serialization assumptions. Called if a filter
     * modifies the response
     */
    public void resetBuildTimeSerialization() {
        entityWriter = deployment.getDynamicEntityWriter();
    }

    public UriInfo getUriInfo() {
        if (uriInfo == null) {
            uriInfo = new QuarkusRestUriInfo(this);
        }
        return uriInfo;
    }

    public QuarkusRestHttpHeaders getHttpHeaders() {
        if (httpHeaders == null) {
            httpHeaders = new QuarkusRestHttpHeaders(context.request().headers());
        }
        return httpHeaders;
    }

    public RoutingContext getContext() {
        return context;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setMaxPathParams(int maxPathParams) {
        if (maxPathParams > 1) {
            pathParamValues = new String[maxPathParams];
        } else {
            pathParamValues = null;
        }
    }

    public String getPathParam(int index) {
        if (pathParamValues instanceof String[]) {
            return ((String[]) pathParamValues)[index];
        }
        if (index > 1) {
            throw new IndexOutOfBoundsException();
        }
        return (String) pathParamValues;
    }

    public QuarkusRestRequestContext setPathParamValue(int index, String value) {
        if (pathParamValues instanceof String[]) {
            ((String[]) pathParamValues)[index] = value;
        } else {
            if (index > 1) {
                throw new IndexOutOfBoundsException();
            }
            pathParamValues = value;
        }
        return this;
    }

    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public Object getRequestEntity() {
        return requestEntity;
    }

    public QuarkusRestRequestContext setRequestEntity(Object requestEntity) {
        this.requestEntity = requestEntity;
        return this;
    }

    public EntityWriter getEntityWriter() {
        return entityWriter;
    }

    public QuarkusRestRequestContext setEntityWriter(EntityWriter entityWriter) {
        this.entityWriter = entityWriter;
        return this;
    }

    public Object getEndpointInstance() {
        return endpointInstance;
    }

    public QuarkusRestRequestContext setEndpointInstance(Object endpointInstance) {
        this.endpointInstance = endpointInstance;
        return this;
    }

    public Object getResult() {
        return result;
    }

    public QuarkusRestRequestContext setResult(Object result) {
        this.result = result;
        return this;
    }

    public RuntimeResource getTarget() {
        return target;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public QuarkusRestRequestContext setSuspended(boolean suspended) {
        this.suspended = suspended;
        return this;
    }

    public boolean isRunning() {
        return running;
    }

    public QuarkusRestRequestContext setRunning(boolean running) {
        this.running = running;
        return this;
    }

    public Executor getExecutor() {
        return executor;
    }

    public QuarkusRestRequestContext setExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public int getPosition() {
        return position;
    }

    public QuarkusRestRequestContext setPosition(int position) {
        this.position = position;
        return this;
    }

    public RestHandler[] getHandlers() {
        return handlers;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * ATM this can only be called by the InvocationHandler
     */
    public QuarkusRestRequestContext setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    private void invokeExceptionMapper(Throwable throwable) {
        this.producesMediaType = null;
        this.result = deployment.getExceptionMapping().mapException(throwable);
    }

    private void handleException(Throwable throwable) {
        log.error("Request failed", throwable);
        context.response().setStatusCode(500).end();
    }

    @Override
    public void close() {
        //TODO: do we even have any resources to close?
        // FIXME: probably move request context termination here, otherwise we can only terminate it from run()
    }

    public Response getResponse() {
        return (Response) result;
    }

    public Object getProperty(String name) {
        if (properties == null) {
            return null;
        }
        return properties.get(name);
    }

    public Collection<String> getPropertyNames() {
        if (properties == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableSet(properties.keySet());
    }

    public void setProperty(String name, Object object) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(name, object);
    }

    public void removeProperty(String name) {
        if (properties == null) {
            return;
        }
        properties.remove(name);
    }

    public Request getRequest() {
        if (request == null) {
            request = new QuarkusRestRequest(this);
        }
        return request;
    }

    public QuarkusRestContainerRequestContext getContainerRequestContext() {
        if (containerRequestContext == null) {
            containerRequestContext = new QuarkusRestContainerRequestContext(this);
        }
        return containerRequestContext;
    }

    public String getMethod() {
        if (method == null) {
            return context.request().rawMethod();
        }
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRemaining(String remaining) {
        this.remaining = remaining;
    }

    public String getRemaining() {
        return remaining;
    }

    public String getPath() {
        if (path == null) {
            return context.normalisedPath();
        }
        return path;
    }

    public QuarkusRestRequestContext setPath(String path) {
        this.path = path;
        return this;
    }

    public MediaType getProducesMediaType() {
        return producesMediaType;
    }

    public QuarkusRestRequestContext setProducesMediaType(MediaType producesMediaType) {
        this.producesMediaType = producesMediaType;
        return this;
    }

    public MediaType getConsumesMediaType() {
        return consumesMediaType;
    }

    public QuarkusRestRequestContext setConsumesMediaType(MediaType consumesMediaType) {
        this.consumesMediaType = consumesMediaType;
        return this;
    }

    public Annotation[] getAnnotations() {
        if (annotations == null) {
            if (target == null) {
                return null;
            }
            return target.getLazyMethod().getAnnotations();
        }
        return annotations;
    }

    public QuarkusRestRequestContext setAnnotations(Annotation[] annotations) {
        this.annotations = annotations;
        return this;
    }

    public Type getGenericReturnType() {
        if (genericReturnType == null) {
            if (target == null) {
                return null;
            }
            return target.getLazyMethod().getGenericReturnType();
        }
        return genericReturnType;
    }

    public QuarkusRestRequestContext setGenericReturnType(Type genericReturnType) {
        this.genericReturnType = genericReturnType;
        return this;
    }

    public QuarkusRestAsyncResponse getAsyncResponse() {
        return asyncResponse;
    }

    public QuarkusRestRequestContext setAsyncResponse(QuarkusRestAsyncResponse asyncResponse) {
        if (this.asyncResponse != null) {
            throw new RuntimeException("Async can only be started once");
        }
        this.asyncResponse = asyncResponse;
        return this;
    }

    public void saveUriMatchState() {
        if (matchedURIs == null) {
            matchedURIs = new LinkedList<>();
        } else if (matchedURIs.get(0).resource == target) {
            //already saved
            return;
        }
        URITemplate classPath = target.getClassPath();
        if (classPath != null) {
            //this is not great, but the alternative is to do path based matching on every request
            //given that this method is likely to be called very infrequently it is better to have a small
            //cost here than a cost applied to every request
            int pos = classPath.stem.length();
            String path = context.request().path();
            //we already know that this template matches, we just need to find the matched bit
            for (int i = 1; i < classPath.components.length; ++i) {
                URITemplate.TemplateComponent segment = classPath.components[i];
                if (segment.type == URITemplate.Type.LITERAL) {
                    pos += segment.literalText.length();
                } else if (segment.type == URITemplate.Type.DEFAULT_REGEX) {
                    for (; pos < path.length(); ++pos) {
                        if (path.charAt(pos) == '/') {
                            --pos;
                            break;
                        }
                    }
                } else {
                    Matcher matcher = segment.pattern.matcher(path);
                    if (matcher.find(pos) && matcher.start() == pos) {
                        pos = matcher.end();
                    }
                }
            }
            matchedURIs.add(new UriMatch(path.substring(1, pos), null, null));
        }
        String path = context.request().path();
        matchedURIs.add(0, new UriMatch(path.substring(1, path.length() - (remaining == null ? 0 : remaining.length())),
                target, endpointInstance));
    }

    public List<UriMatch> getMatchedURIs() {
        saveUriMatchState();
        return matchedURIs;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public QuarkusRestRequestContext setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        return this;
    }

    public QuarkusRestSseEventSink getSseEventSink() {
        return sseEventSink;
    }

    public void setSseEventSink(QuarkusRestSseEventSink sseEventSink) {
        this.sseEventSink = sseEventSink;
    }
}

package io.quarkus.hibernate.orm.runtime.proxies;

import static org.objectweb.asm.Opcodes.ACC_FINAL;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.proxy.pojo.ProxyFactoryHelper;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;
import org.jboss.logging.Logger;

/**
 * Runtime proxies are used by Hibernate ORM to handle a number of corner cases;
 * in particular Enhanced Proxies need special consideration in Quarkus as
 * they aren't generated by the enhancers during the build.
 * Since we can't generate new class definitions at runtime, this value holder
 * class is meant to be created at build time and hold onto those class definitions.
 *
 * Implementors of a custom {@link org.hibernate.bytecode.spi.ProxyFactoryFactory} are
 * then able to lookup such class definitions at runtime to create new instances of the
 * required enhanced proxies.
 *
 * Failure to generate such a proxy is not critical, but it implies that Hibernate ORM
 * will not be able to use the enhanced proxy mechanism, possibly having to generate
 * an additional round trip to the database in some circumstances.
 * Most notably we'll fail to generate such a proxy when the entity has a "final" modifier;
 * we'll also need a default constructor.
 * Default constructors are required beyond proxy generation, so a lack of such a constructor
 * will have us abort the bootstrap process with a critical error.
 * On the other hand, having the entities marked as "final" is handled gracefully, as we
 * can simply fallback to not use the enhanced proxy for the specific entity, and because
 * it's a common case when writing entities in Kotlin.
 */
public final class ProxyDefinitions {

    private final Map<Class<?>, ProxyClassDetailsHolder> proxyDefinitionMap;
    private static final Logger LOGGER = Logger.getLogger(ProxyDefinitions.class.getName());

    private ProxyDefinitions(Map<Class<?>, ProxyClassDetailsHolder> proxyDefinitionMap) {
        this.proxyDefinitionMap = proxyDefinitionMap;
    }

    public static ProxyDefinitions createFromMetadata(Metadata storeableMetadata) {

        //Check upfront for any need across all metadata: would be nice to avoid initializing the Bytecode provider.
        if (needAnyProxyDefinitions(storeableMetadata)) {
            final HashMap<Class<?>, ProxyClassDetailsHolder> proxyDefinitionMap = new HashMap<>();
            final BytecodeProviderImpl bytecodeProvider = new BytecodeProviderImpl();
            try {
                final ByteBuddyProxyHelper byteBuddyProxyHelper = bytecodeProvider.getByteBuddyProxyHelper();
                for (PersistentClass persistentClass : storeableMetadata.getEntityBindings()) {
                    if (needsProxyGeneration(persistentClass)) {
                        final Class mappedClass = persistentClass.getMappedClass();
                        final Class proxyClassDefinition = generateProxyClass(persistentClass, byteBuddyProxyHelper);
                        if (proxyClassDefinition == null) {
                            continue;
                        }
                        final boolean overridesEquals = ReflectHelper.overridesEquals(mappedClass);
                        try {
                            proxyDefinitionMap.put(mappedClass,
                                    new ProxyClassDetailsHolder(overridesEquals, proxyClassDefinition.getConstructor()));
                        } catch (NoSuchMethodException e) {
                            throw new HibernateException(
                                    "Failed to generate Enhanced Proxy: default constructor is missing for entity '"
                                            + mappedClass.getName() + "'. Please add a default constructor explicitly.");
                        }
                    }
                }
            } finally {
                bytecodeProvider.resetCaches();
            }
            return new ProxyDefinitions(proxyDefinitionMap);
        } else {
            return new ProxyDefinitions(Collections.emptyMap());
        }
    }

    private static boolean needAnyProxyDefinitions(Metadata storeableMetadata) {
        for (PersistentClass persistentClass : storeableMetadata.getEntityBindings()) {
            if (needsProxyGeneration(persistentClass))
                return true;
        }
        return false;
    }

    private static boolean needsProxyGeneration(PersistentClass persistentClass) {
        //Only lazy entities need a proxy, and only class-mapped classed can be proxies (Envers!)
        return persistentClass.isLazy() && (persistentClass.getMappedClass() != null);
    }

    private static Class generateProxyClass(PersistentClass persistentClass, ByteBuddyProxyHelper byteBuddyProxyHelper) {
        final String entityName = persistentClass.getEntityName();
        final Class mappedClass = persistentClass.getMappedClass();
        if ((mappedClass.getModifiers() & ACC_FINAL) == ACC_FINAL) {
            LOGGER.warn("Could not generate an enhanced proxy for entity '" + entityName + "' (class='"
                    + mappedClass.getCanonicalName()
                    + "') as it's final. Your application might perform better if we're allowed to extend it.");
            return null;
        }
        final Set<Class> proxyInterfaces = ProxyFactoryHelper.extractProxyInterfaces(persistentClass, entityName);
        Class proxyDef = byteBuddyProxyHelper.buildProxy(mappedClass, toArray(proxyInterfaces));
        return proxyDef;
    }

    private static Class[] toArray(final Set<Class> interfaces) {
        if (interfaces == null) {
            return ArrayHelper.EMPTY_CLASS_ARRAY;
        }
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    public ProxyClassDetailsHolder getProxyForClass(Class persistentClass) {
        return proxyDefinitionMap.get(persistentClass);
    }

    public static class ProxyClassDetailsHolder {

        private final boolean overridesEquals;
        private final Constructor constructor;

        private ProxyClassDetailsHolder(boolean overridesEquals, Constructor constructor) {
            this.overridesEquals = overridesEquals;
            this.constructor = constructor;
        }

        public boolean isOverridesEquals() {
            return overridesEquals;
        }

        public Constructor getConstructor() {
            return constructor;
        }
    }

}

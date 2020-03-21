package io.quarkus.deltaspike.partialbean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.deltaspike.partialbean.api.PartialBeanBinding;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public class PartialBeanProcessor {

    private static final DotName PARTIAL_BEAN = DotName.createSimple(PartialBeanBinding.class.getName());

    @BuildStep(loadsApplicationClasses = true)
    void processPartialBeans(CombinedIndexBuildItem index, BuildProducer<GeneratedBeanBuildItem> geneClass,
            BuildProducer<BeanDefiningAnnotationBuildItem> bda) throws Exception {

        for (AnnotationInstance binding : index.getIndex().getAnnotations(PARTIAL_BEAN)) {
            String invocationHandler = null;
            Set<Class<?>> beans = new HashSet<>();

            DotName bindingName = binding.target().asClass().name();
            bda.produce(new BeanDefiningAnnotationBuildItem(bindingName));
            for (AnnotationInstance bean : index.getIndex().getAnnotations(bindingName)) {
                if (bean.target().kind() != AnnotationTarget.Kind.CLASS) {
                    throw new RuntimeException("Cannot place @PartialBeanBinding " + binding
                            + " on something that is not a class " + bean.target());
                }
                Class<?> clazz = Class.forName(bean.target().asClass().name().toString(), false,
                        Thread.currentThread().getContextClassLoader());
                if (InvocationHandler.class.isAssignableFrom(clazz)) {
                    if (invocationHandler != null) {
                        throw new RuntimeException("Cannot have multiple invocation handlers for " + binding + " found "
                                + invocationHandler + " and " + clazz.getName());
                    }
                    invocationHandler = clazz.getName();
                } else {
                    beans.add(clazz);
                }
            }
            if (beans.isEmpty()) {
                continue;
            }
            if (invocationHandler == null) {
                throw new RuntimeException("No invocation handler for " + binding);
            }
            int fieldCount = 0;
            ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(geneClass);
            for (Class<?> bean : beans) {
                ClassCreator.Builder builder = ClassCreator.builder()
                        .className(bean.getName() + "$$partialBean");
                if (bean.isInterface()) {
                    builder.interfaces(bean);
                } else {
                    builder.superClass(bean);
                }
                ClassCreator c = builder.build();
                c.addAnnotation(Dependent.class);
                MethodCreator staticInit = c.getMethodCreator("<clinit>", void.class);
                staticInit.setModifiers(Modifier.STATIC);

                //inject the invocation handler 
                //TODO: qualifiers
                FieldDescriptor delegate = FieldDescriptor.of(c.getClassName(), "delegate", invocationHandler);
                c.getFieldCreator(delegate).addAnnotation(Inject.class);

                for (Method method : bean.getMethods()) {
                    if (Modifier.isAbstract(method.getModifiers())) {
                        //create a field to store the Method object
                        FieldDescriptor fd = FieldDescriptor.of(c.getClassName(), "method" + (fieldCount++), Method.class);
                        c.getFieldCreator(fd).setModifiers(Modifier.STATIC | Modifier.FINAL);
                        //init in static init

                        ResultHandle ptypes = staticInit.newArray(Class.class, method.getParameterCount());
                        for (int i = 0; i < method.getParameterCount(); ++i) {
                            staticInit.writeArrayValue(ptypes, i,
                                    staticInit.loadClass(method.getParameterTypes()[i].getName()));
                        }
                        ResultHandle mh = staticInit.invokeVirtualMethod(MethodDescriptor.ofMethod(Class.class, "getMethod",
                                Method.class, String.class, Class[].class),
                                staticInit.loadClass(method.getDeclaringClass().getName()), staticInit.load(method.getName()),
                                ptypes);
                        staticInit.writeStaticField(fd, mh);

                        //implement the method
                        MethodCreator mc = c.getMethodCreator(MethodDescriptor.ofMethod(method));
                        mc.setModifiers(Modifier.PUBLIC);
                        ResultHandle dInst = mc.readInstanceField(delegate, mc.getThis());
                        ResultHandle mInst = mc.readStaticField(fd);
                        ResultHandle args = mc.newArray(Object.class, method.getParameterCount());
                        for (int i = 0; i < method.getParameterCount(); ++i) {
                            mc.writeArrayValue(args, i, mc.getMethodParam(i));
                        }
                        ResultHandle res = mc.invokeInterfaceMethod(
                                MethodDescriptor.ofMethod(InvocationHandler.class, "invoke", Object.class,
                                        Object.class, Method.class, Object[].class),
                                dInst, mc.getThis(), mInst, args);
                        mc.returnValue(res);
                    }

                }
                staticInit.returnValue(null);
                c.writeTo(classOutput);
            }

        }

    }
}

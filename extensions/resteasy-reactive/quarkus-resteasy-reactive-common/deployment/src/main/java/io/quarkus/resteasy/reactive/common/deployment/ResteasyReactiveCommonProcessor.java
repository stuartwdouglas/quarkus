package io.quarkus.resteasy.reactive.common.deployment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.ws.rs.RuntimeType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.model.InterceptorContainer;
import org.jboss.resteasy.reactive.common.model.PreMatchInterceptorContainer;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptor;
import org.jboss.resteasy.reactive.common.model.ResourceInterceptors;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult.KeepProviderResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveInterceptorScanner;
import org.jboss.resteasy.reactive.common.processor.scanning.ResteasyReactiveScanner;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.resteasy.reactive.spi.AbstractInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerRequestFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.ContainerResponseFilterBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.resteasy.reactive.spi.ReaderInterceptorBuildItem;
import io.quarkus.resteasy.reactive.spi.WriterInterceptorBuildItem;

public class ResteasyReactiveCommonProcessor {

    @BuildStep
    ApplicationResultBuildItem handleApplication(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        ApplicationScanningResult result = ResteasyReactiveScanner
                .scanForApplicationClass(combinedIndexBuildItem.getComputingIndex());
        if (result.getSelectedAppClass() != null) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, result.getSelectedAppClass().name().toString()));
        }
        return new ApplicationResultBuildItem(result);
    }

    @BuildStep
    public ResourceInterceptorsContributorBuildItem scanForIOInterceptors(CombinedIndexBuildItem combinedIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem) {
        return new ResourceInterceptorsContributorBuildItem(new Consumer<ResourceInterceptors>() {
            @Override
            public void accept(ResourceInterceptors interceptors) {
                ResteasyReactiveInterceptorScanner.scanForIOInterceptors(interceptors,
                        combinedIndexBuildItem.getComputingIndex(),
                        applicationResultBuildItem.getResult());
            }
        });
    }

    @BuildStep
    public ResourceInterceptorsBuildItem buildResourceInterceptors(List<ResourceInterceptorsContributorBuildItem> scanningTasks,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer,
            List<WriterInterceptorBuildItem> writerInterceptors,
            List<ReaderInterceptorBuildItem> readerInterceptors,
            List<ContainerRequestFilterBuildItem> requestFilters,
            List<ContainerResponseFilterBuildItem> responseFilters) {
        ResourceInterceptors resourceInterceptors = new ResourceInterceptors();
        for (ResourceInterceptorsContributorBuildItem i : scanningTasks) {
            i.getBuildTask().accept(resourceInterceptors);
        }
        AdditionalBeanBuildItem.Builder beanBuilder = AdditionalBeanBuildItem.builder();
        registerContainerBeans(beanBuilder, resourceInterceptors.getContainerResponseFilters());
        registerContainerBeans(beanBuilder, resourceInterceptors.getContainerRequestFilters());
        registerContainerBeans(beanBuilder, resourceInterceptors.getReaderInterceptors());
        registerContainerBeans(beanBuilder, resourceInterceptors.getWriterInterceptors());
        Set<String> globalNameBindings = applicationResultBuildItem.getResult().getGlobalNameBindings();
        for (WriterInterceptorBuildItem i : writerInterceptors) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getWriterInterceptors(), i, beanBuilder);
        }
        for (ReaderInterceptorBuildItem i : readerInterceptors) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getReaderInterceptors(), i, beanBuilder);
        }
        for (ContainerRequestFilterBuildItem i : requestFilters) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getContainerRequestFilters(), i, beanBuilder);
        }
        for (ContainerResponseFilterBuildItem i : responseFilters) {
            registerInterceptors(globalNameBindings, resourceInterceptors.getContainerResponseFilters(), i, beanBuilder);
        }
        additionalBeanBuildItemBuildProducer.produce(beanBuilder.setUnremovable().build());
        return new ResourceInterceptorsBuildItem(resourceInterceptors);
    }

    protected <T, B extends AbstractInterceptorBuildItem> void registerInterceptors(Set<String> globalNameBindings,
            InterceptorContainer<T> interceptors, B filterItem, AdditionalBeanBuildItem.Builder beanBuilder) {
        if (filterItem.isRegisterAsBean()) {
            beanBuilder.addBeanClass(filterItem.getClassName());
        }
        ResourceInterceptor<T> interceptor = interceptors.create();
        interceptor.setClassName(filterItem.getClassName());
        Integer priority = filterItem.getPriority();
        if (priority != null) {
            interceptor.setPriority(priority);
        }
        if (interceptors instanceof PreMatchInterceptorContainer
                && ((ContainerRequestFilterBuildItem) filterItem).isPreMatching()) {
            ((PreMatchInterceptorContainer<T>) interceptors).addPreMatchInterceptor(interceptor);

        } else {
            Set<String> nameBindingNames = filterItem.getNameBindingNames();
            if (nameBindingNames.isEmpty() || namePresent(nameBindingNames, globalNameBindings)) {
                interceptors.addGlobalRequestInterceptor(interceptor);
            } else {
                interceptor.setNameBindingNames(nameBindingNames);
                interceptors.addNameRequestInterceptor(interceptor);
            }
        }
    }

    private void registerContainerBeans(AdditionalBeanBuildItem.Builder additionalProviders,
            InterceptorContainer<?> container) {
        for (ResourceInterceptor<?> i : container.getGlobalResourceInterceptors()) {
            additionalProviders.addBeanClass(i.getClassName());
        }
        for (ResourceInterceptor<?> i : container.getNameResourceInterceptors()) {
            additionalProviders.addBeanClass(i.getClassName());
        }
        if (container instanceof PreMatchInterceptorContainer) {
            for (ResourceInterceptor<?> i : ((PreMatchInterceptorContainer<?>) container).getPreMatchInterceptors()) {
                additionalProviders.addBeanClass(i.getClassName());
            }
        }
    }

    private boolean namePresent(Set<String> nameBindingNames, Set<String> globalNameBindings) {
        for (String i : globalNameBindings) {
            if (nameBindingNames.contains(i)) {
                return true;
            }
        }
        return false;
    }

    @BuildStep
    void scanResources(
            // TODO: We need to use this index instead of BeanArchiveIndexBuildItem to avoid build cycles. It it OK?
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerBuildItemBuildProducer,
            BuildProducer<ResourceScanningResultBuildItem> resourceScanningResultBuildItemBuildProducer) {

        ResourceScanningResult res = ResteasyReactiveScanner.scanResources(combinedIndexBuildItem.getComputingIndex());
        if (res == null) {
            return;
        }
        if (!res.getResourcesThatNeedCustomProducer().isEmpty()) {
            annotationsTransformerBuildItemBuildProducer
                    .produce(new AnnotationsTransformerBuildItem(
                            new VetoingAnnotationTransformer(res.getResourcesThatNeedCustomProducer().keySet())));
        }
        resourceScanningResultBuildItemBuildProducer.produce(new ResourceScanningResultBuildItem(res));
    }

    @BuildStep
    public void setupEndpoints(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            Optional<ResourceScanningResultBuildItem> resourceScanningResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<MessageBodyWriterBuildItem> messageBodyWriterBuildItemBuildProducer,
            BuildProducer<MessageBodyReaderBuildItem> messageBodyReaderBuildItemBuildProducer) throws NoSuchMethodException {

        if (!resourceScanningResultBuildItem.isPresent()) {
            // no detected @Path, bail out
            return;
        }

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<ClassInfo> writers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.MESSAGE_BODY_WRITER);
        Collection<ClassInfo> readers = index
                .getAllKnownImplementors(ResteasyReactiveDotNames.MESSAGE_BODY_READER);

        for (ClassInfo writerClass : writers) {
            KeepProviderResult keepProviderResult = applicationResultBuildItem.getResult().keepProvider(writerClass);
            if (keepProviderResult != KeepProviderResult.DISCARD) {
                RuntimeType runtimeType = null;
                if (keepProviderResult == KeepProviderResult.SERVER_ONLY) {
                    runtimeType = RuntimeType.SERVER;
                }
                List<String> mediaTypeStrings = Collections.emptyList();
                AnnotationInstance producesAnnotation = writerClass.classAnnotation(ResteasyReactiveDotNames.PRODUCES);
                if (producesAnnotation != null) {
                    mediaTypeStrings = Arrays.asList(producesAnnotation.value().asStringArray());
                }
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(writerClass.name(),
                        ResteasyReactiveDotNames.MESSAGE_BODY_WRITER,
                        index);
                String writerClassName = writerClass.name().toString();
                AnnotationInstance constrainedToInstance = writerClass.classAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                if (constrainedToInstance != null) {
                    runtimeType = RuntimeType.valueOf(constrainedToInstance.value().asEnum());
                }
                messageBodyWriterBuildItemBuildProducer.produce(new MessageBodyWriterBuildItem(writerClassName,
                        typeParameters.get(0).name().toString(), mediaTypeStrings, runtimeType, false));
            }
        }

        for (ClassInfo readerClass : readers) {
            KeepProviderResult keepProviderResult = applicationResultBuildItem.getResult().keepProvider(readerClass);
            if (keepProviderResult != KeepProviderResult.DISCARD) {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(readerClass.name(),
                        ResteasyReactiveDotNames.MESSAGE_BODY_READER,
                        index);
                RuntimeType runtimeType = null;
                if (keepProviderResult == KeepProviderResult.SERVER_ONLY) {
                    runtimeType = RuntimeType.SERVER;
                }
                List<String> mediaTypeStrings = Collections.emptyList();
                String readerClassName = readerClass.name().toString();
                AnnotationInstance consumesAnnotation = readerClass.classAnnotation(ResteasyReactiveDotNames.CONSUMES);
                if (consumesAnnotation != null) {
                    mediaTypeStrings = Arrays.asList(consumesAnnotation.value().asStringArray());
                }
                AnnotationInstance constrainedToInstance = readerClass.classAnnotation(ResteasyReactiveDotNames.CONSTRAINED_TO);
                if (constrainedToInstance != null) {
                    runtimeType = RuntimeType.valueOf(constrainedToInstance.value().asEnum());
                }
                messageBodyReaderBuildItemBuildProducer.produce(new MessageBodyReaderBuildItem(readerClassName,
                        typeParameters.get(0).name().toString(), mediaTypeStrings, runtimeType, false));
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, false, readerClassName));
            }
        }
    }

}

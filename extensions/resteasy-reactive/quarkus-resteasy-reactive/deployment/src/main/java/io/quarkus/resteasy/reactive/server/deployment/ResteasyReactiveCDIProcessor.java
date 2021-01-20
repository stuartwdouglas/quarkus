package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;
import org.jboss.resteasy.reactive.server.injection.ContextProducers;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AutoInjectAnnotationBuildItem;
import io.quarkus.arc.deployment.BeanDefiningAnnotationBuildItem;
import io.quarkus.arc.deployment.SynthesisFinishedBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.resteasy.reactive.server.runtime.QuarkusContextProducers;
import io.quarkus.resteasy.reactive.server.runtime.cdi.ArcParamExtractor;
import io.quarkus.resteasy.reactive.spi.DynamicFeatureBuildItem;
import io.quarkus.resteasy.reactive.spi.JaxrsFeatureBuildItem;

public class ResteasyReactiveCDIProcessor {

    @BuildStep
    AutoInjectAnnotationBuildItem contextInjection(
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItemBuildProducer) {
        additionalBeanBuildItemBuildProducer
                .produce(AdditionalBeanBuildItem.builder().addBeanClasses(ContextProducers.class, QuarkusContextProducers.class)
                        .build());
        return new AutoInjectAnnotationBuildItem(ResteasyReactiveServerDotNames.CONTEXT,
                DotName.createSimple(BeanParam.class.getName()));
    }

    @BuildStep
    void beanDefiningAnnotations(BuildProducer<BeanDefiningAnnotationBuildItem> beanDefiningAnnotations) {
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.PATH, BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.APPLICATION_PATH,
                        BuiltinScope.SINGLETON.getName()));
        beanDefiningAnnotations
                .produce(new BeanDefiningAnnotationBuildItem(ResteasyReactiveDotNames.PROVIDER,
                        BuiltinScope.SINGLETON.getName()));
    }

    @BuildStep
    void additionalBeans(List<DynamicFeatureBuildItem> additionalDynamicFeatures,
            List<JaxrsFeatureBuildItem> featureBuildItems,
            BuildProducer<AdditionalBeanBuildItem> additionalBean) {

        AdditionalBeanBuildItem.Builder additionalProviders = AdditionalBeanBuildItem.builder();
        for (DynamicFeatureBuildItem dynamicFeature : additionalDynamicFeatures) {
            if (dynamicFeature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(dynamicFeature.getClassName());
            }
        }
        for (JaxrsFeatureBuildItem dynamicFeature : featureBuildItems) {
            if (dynamicFeature.isRegisterAsBean()) {
                additionalProviders.addBeanClass(dynamicFeature.getClassName());
            }
        }
        additionalBean.produce(additionalProviders.setUnremovable().setDefaultScope(DotNames.SINGLETON).build());
    }

    @BuildStep
    CdiBeanMethodParametersBuildItem genericInjection(BuildProducer<MethodScannerBuildItem> mb) {
        CdiBeanMethodParametersBuildItem ret = new CdiBeanMethodParametersBuildItem();
        mb.produce(new MethodScannerBuildItem(new MyMethodScanner(ret.capturedParamInfos)));
        return ret;
    }

    @BuildStep
    @Consume(ResteasyReactiveDeploymentInfoBuildItem.class) //needed to make sure the param list is populated
    void markParamsUnremovable(BuildProducer<UnremovableBeanBuildItem> urb,
            CdiBeanMethodParametersBuildItem cdiBeanMethodParametersBuildItem) {
        for (CapturedParamInfo i : cdiBeanMethodParametersBuildItem.capturedParamInfos) {
            urb.produce(UnremovableBeanBuildItem.beanClassNames(i.paramType.name().toString()));
        }
    }

    @BuildStep
    @Consume(ResteasyReactiveDeploymentInfoBuildItem.class) //needed to make sure the param list is populated
    @Produce(MainBytecodeRecorderBuildItem.class) //we need this to be run before the bytecode recorders generate their output
    void resolveBeans(SynthesisFinishedBuildItem brf, CdiBeanMethodParametersBuildItem params) {
        Set<DotName> qualifiers = brf.getQualifiers().stream().map(ClassInfo::name).collect(Collectors.toSet());
        for (CapturedParamInfo i : params.capturedParamInfos) {
            Set<BeanInfo> info = brf.getBeanResolver().resolveBeans(i.paramType, i.annotations.values().stream()
                    .filter(s -> qualifiers.contains(s.name())).toArray(AnnotationInstance[]::new));
            if (info.isEmpty()) {
                throw new RuntimeException(
                        "Unable to resolve CDI bean for parameter for REST endpoint method: " + i.errorLocation);
            } else if (info.size() != 1) {
                throw new RuntimeException("Abiguous injection point detected for REST endpoint " + i.errorLocation
                        + " possible beans are " + info);
            } else {
                i.extractor.beanIdentifier = info.iterator().next().getIdentifier();
            }
        }
    }

    static final class CdiBeanMethodParametersBuildItem extends SimpleBuildItem {
        /**
         * IMPORTANT: This build item is mutable, items are added to this list outside the build step that creates it.
         *
         * This works in this case because this info is generated in the creation of ResteasyReactiveDeploymentInfoBuildItem,
         * and we also inject the info which acts a a guard to make sure that the gathering of data is complete.
         */
        final Deque<CapturedParamInfo> capturedParamInfos = new ConcurrentLinkedDeque<>();
    }

    public static class MyMethodScanner implements MethodScanner {

        final Deque<CapturedParamInfo> capturedParamInfos;

        public MyMethodScanner(Deque<CapturedParamInfo> capturedParamInfos) {
            this.capturedParamInfos = capturedParamInfos;
        }

        @Override
        public ParameterExtractor handleCustomParameter(Type paramType, Map<DotName, AnnotationInstance> annotations,
                boolean field, Map<String, Object> methodContext, String errorLocation) {
            ArcParamExtractor extractor = new ArcParamExtractor();
            capturedParamInfos.add(new CapturedParamInfo(paramType, annotations, extractor, errorLocation));
            return extractor;
        }

        @Override
        public int priority() {
            //CDI is lowest priority, only runs if nothing else has claimed the parameter
            return Integer.MIN_VALUE;
        }
    }

    static class CapturedParamInfo {
        final Type paramType;
        final Map<DotName, AnnotationInstance> annotations;
        final ArcParamExtractor extractor;
        final String errorLocation;

        CapturedParamInfo(Type paramType, Map<DotName, AnnotationInstance> annotations, ArcParamExtractor extractor,
                String errorLocation) {
            this.paramType = paramType;
            this.annotations = annotations;
            this.extractor = extractor;
            this.errorLocation = errorLocation;
        }
    }
}

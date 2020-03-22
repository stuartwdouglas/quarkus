package io.quarkus.deltaspike.data.deployment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import org.apache.deltaspike.data.api.Repository;
import org.apache.deltaspike.data.impl.RepositoryExtension;
import org.apache.deltaspike.jpa.impl.entitymanager.EntityManagerRefLookup;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deltaspike.data.runtime.QuarkusActiveEntityManagerHolder;
import io.quarkus.deltaspike.data.runtime.QuarkusTransactionStrategy;
import io.quarkus.deltaspike.data.runtime.RepositoryRecorder;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class DeltaspikeDataProcessor {

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem marker() {
        return new AdditionalApplicationArchiveMarkerBuildItem("org/apache/deltaspike/data/api/Repository.class");
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    SyntheticBeanBuildItem repoExtension(CombinedIndexBuildItem indexBuildItem, RepositoryRecorder repositoryRecorder) {
        List<String> repos = new ArrayList<>();
        for (AnnotationInstance i : indexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(Repository.class.getName()))) {
            String repo = i.target().asClass().name().toString();
            if (!repo.startsWith("org.apache.deltaspike.")) {
                repos.add(repo);
            }
        }

        return SyntheticBeanBuildItem.configure(org.apache.deltaspike.data.impl.RepositoryExtension.class)
                .types(RepositoryExtension.class)
                .unremovable()
                .scope(Singleton.class)
                .supplier(repositoryRecorder.repoExtension(repos))
                .done();
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(QuarkusTransactionStrategy.class)
                .addBeanClasses(QuarkusActiveEntityManagerHolder.class)
                .addBeanClasses(EntityManagerRefLookup.class)
                .build();
    }
}

package io.quarkus.deployment.mutability;

import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.model.PersistentAppModel;

public class MutabilityEntryPoint {

    public static void main(Path appRoot, String... args) throws Exception {

        try (ObjectInputStream in = new ObjectInputStream(
                Files.newInputStream(appRoot.resolve("deployment-quarkus/appmodel.dat")))) {
            PersistentAppModel appModel = (PersistentAppModel) in.readObject();

            AppModel existingModel = appModel.getAppModel(appRoot);
            System.setProperty("quarkus-rebuild-hack", "true"); //todo:fixme
            System.setProperty("quarkus.package.type", "fast-jar");
            CuratedApplication bootstrap = QuarkusBootstrap.builder()
                    .setAppArtifact(existingModel.getAppArtifact())
                    .setExistingModel(existingModel)
                    .setBaseName(appModel.getBaseName())
                    .setApplicationRoot(existingModel.getAppArtifact().getPath())
                    .setTargetDirectory(appRoot.getParent())
                    .setBaseClassLoader(MutabilityEntryPoint.class.getClassLoader())
                    .build().bootstrap();
            bootstrap.createAugmentor().createProductionApplication();

        }
    }
}

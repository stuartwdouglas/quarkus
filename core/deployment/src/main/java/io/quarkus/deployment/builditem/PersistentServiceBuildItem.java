/*
 * NOHEADER
 */

package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A symbolic build item that indicates than an extension creates persistent services, and as a result the
 * application should not immediately exit.
 *
 * If none of these are produced then the main application thread will not block, and the application will
 * exit as soon as all threads have finished as per the normal JVM shutdown rules
 */
public final class PersistentServiceBuildItem extends MultiBuildItem {
}

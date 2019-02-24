package io.quarkus.smallrye.opentracing.runtime.graal;

import java.lang.reflect.Type;

import com.google.gson.internal.ObjectConstructor;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.google.gson.internal.UnsafeAllocator")
@Delete
final class Target_com_google_gson_internal_UnsafeAllocator {

}

@TargetClass(className = "com.google.gson.internal.UnsafeAllocator$1")
@Delete
final class Target_com_google_gson_internal_UnsafeAllocator1 {

}

@TargetClass(className = "com.google.gson.internal.ConstructorConstructor")
final class Target_com_google_gson_internal_ConstructorConstructor {

    @Substitute
    private <T> ObjectConstructor<T> newUnsafeAllocator(
            final Type type, final Class<? super T> rawType) {
        throw new RuntimeException("unsafe allocator not supported on substrate");
    }
}

class OpenTracingSubstitutions {
}

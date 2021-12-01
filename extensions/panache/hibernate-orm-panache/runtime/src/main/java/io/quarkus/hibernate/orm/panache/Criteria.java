package io.quarkus.hibernate.orm.panache;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Criteria<T> {

    public static<T> Criteria<T> gt(Consumer<T> consumer) {

    }
    public static<T> Criteria<T> eq(Consumer<T> consumer) {

    }
    public static<T,V> Criteria<T> in(BiConsumer<T, V> consumer, V... params) {

    }

    public static <T> Criteria<T> and(Criteria<T> ... crits) {

    }

    public static <T, S> Criteria<T> join(Function<T, S> join, Criteria<S> criteria) {

    }

}
package io.quarkus.deltaspike.partialbean.test;

import java.util.List;

import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DataRepository {

    @Query("select e from MyEntity e")
    List<MyEntity> selectAll();

    @Query("select e from MyEntity e where e.id=:id")
    MyEntity find(@QueryParam("id") long id);
}

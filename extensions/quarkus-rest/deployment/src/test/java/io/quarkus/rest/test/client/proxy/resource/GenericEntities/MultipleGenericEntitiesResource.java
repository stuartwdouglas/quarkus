package io.quarkus.rest.test.client.proxy.resource.GenericEntities;

import java.util.HashMap;

import static io.quarkus.rest.test.client.proxy.resource.GenericEntities.GenericEntityExtendingBaseEntityResource.FIRST_NAME;
import static io.quarkus.rest.test.client.proxy.resource.GenericEntities.GenericEntityExtendingBaseEntityResource.LAST_NAME;

public class MultipleGenericEntitiesResource implements MultipleGenericEntities<String, EntityExtendingBaseEntity> {

    @Override
    public HashMap<String, EntityExtendingBaseEntity> findHashMap() {
        HashMap<String, EntityExtendingBaseEntity> res =  new HashMap<>();
        res.put(FIRST_NAME, new EntityExtendingBaseEntity(FIRST_NAME, LAST_NAME));
        return res;
    }
}

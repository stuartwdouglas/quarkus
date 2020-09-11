package io.quarkus.rest.test.cdi.basic.resource;

import javax.ejb.Remote;

@Remote
public interface EJBRemoteResource extends EJBResourceParent {
}

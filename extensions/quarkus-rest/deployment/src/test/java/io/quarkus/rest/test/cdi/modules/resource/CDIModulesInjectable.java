package io.quarkus.rest.test.cdi.modules.resource;

import javax.ejb.Stateless;

@Stateless
@CDIModulesInjectableBinder
public class CDIModulesInjectable implements CDIModulesInjectableIntf {
}

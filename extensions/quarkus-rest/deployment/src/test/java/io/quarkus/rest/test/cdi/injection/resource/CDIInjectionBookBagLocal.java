package io.quarkus.rest.test.cdi.injection.resource;

import java.util.Collection;

import javax.ejb.Local;

@Local
public interface CDIInjectionBookBagLocal {
    void addBook(CDIInjectionBook book);

    Collection<CDIInjectionBook> getContents();
}

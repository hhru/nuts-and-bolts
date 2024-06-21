package ru.hh.nab.hibernate.service;

import java.util.function.Supplier;
import org.hibernate.service.Service;

public interface ServiceSupplier<T extends Service> extends Supplier<T> {
  Class<T> getClazz();
}

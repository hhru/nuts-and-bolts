package ru.hh.nab;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import java.util.concurrent.ExecutionException;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.PreLoadEventListener;

@SuppressWarnings({"unchecked"})
public final class GuicyHibernateLoader implements PreLoadEventListener {
  private final LoadingCache<Class, MembersInjector> memberInjectors;

  public GuicyHibernateLoader(final Injector injector) {
    memberInjectors = CacheBuilder.newBuilder().softValues().build(
            new CacheLoader<Class, MembersInjector>() {
              @Override
              public MembersInjector load(Class key) throws Exception {
                return injector.getMembersInjector(key);
              }
            });
  }

  @Override
  public void onPreLoad(PreLoadEvent event) {
    try {
      memberInjectors.get(event.getEntity().getClass()).injectMembers(event.getEntity());
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }
}

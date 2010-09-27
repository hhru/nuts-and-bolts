package ru.hh.nab;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.inject.Injector;
import com.google.inject.MembersInjector;
import java.util.Map;
import javax.annotation.Nullable;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.PreLoadEventListener;

@SuppressWarnings({"unchecked"})
public final class GuicyHibernateLoader implements PreLoadEventListener {
  private final Map<Class, MembersInjector> memberInjectors;

  public GuicyHibernateLoader(final Injector injector) {
    memberInjectors = new MapMaker().softValues().makeComputingMap(
            new Function<Class, MembersInjector>() {
              @Override
              public MembersInjector apply(@Nullable Class from) {
                return injector.getMembersInjector(from);
              }
            });
  }

  @Override
  public void onPreLoad(PreLoadEvent event) {
    memberInjectors.get(event.getEntity().getClass()).injectMembers(event.getEntity());
  }
}

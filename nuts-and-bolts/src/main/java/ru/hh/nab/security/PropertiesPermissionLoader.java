package ru.hh.nab.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class PropertiesPermissionLoader implements PermissionLoader {
  private static final Pattern PERMISSIONS_SPLITTER = Pattern.compile("[^*\\w]+");
  private static final String ANONYMOUS_PERMISSIONS_KEY = "anonymous";
  private final Map<String, Permissions> permissions;

  public PropertiesPermissionLoader(Properties props) {
    Map<String, Permissions> b = Maps.newHashMap();
    for (String key : props.stringPropertyNames())
      b.put(key, permissionsFromString(props.getProperty(key)));
    if (!b.containsKey(ANONYMOUS_PERMISSIONS_KEY))
      b.put(ANONYMOUS_PERMISSIONS_KEY, new Permissions(Lists.<String>newArrayList()));
    this.permissions = ImmutableMap.copyOf(b);
  }

  private static Permissions permissionsFromString(String p) {
    return new Permissions(Lists.newArrayList(PERMISSIONS_SPLITTER.split(p)));
  }

  @Override
  public Permissions forKey(String key) {
    return permissions.get(key);
  }

  @Override
  public Permissions anonymous() {
    return permissions.get(ANONYMOUS_PERMISSIONS_KEY);
  }
}

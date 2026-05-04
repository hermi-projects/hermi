package org.hermi.logging.support;

import java.util.LinkedHashSet;
import java.util.Set;
import org.hermi.logging.annotations.EnableHermiLogging;

/** 负责 {@code @EnableHermiLogging} 的根包注册与判断。 */
public class RootPackageRegistry {

  private RootPackageRegistry() {}

  private static class Holder {
    static final RootPackageRegistry INSTANCE = new RootPackageRegistry();
  }

  public static RootPackageRegistry instance() {
    return Holder.INSTANCE;
  }

  private final Set<String> rootPackages = new LinkedHashSet<>();
  private volatile boolean rootInitialized;

  public void initIfNeeded(Class<?> clazz) {
    if (rootInitialized) return;

    EnableHermiLogging enable = clazz.getAnnotation(EnableHermiLogging.class);
    if (enable == null) return;

    synchronized (this) {
      if (rootInitialized) return;

      if (enable.value().isEmpty()) {
        rootPackages.add(clazz.getPackageName());
      } else {
        for (String pkg : enable.value().split(";")) {
          String trimmed = pkg.trim();
          if (!trimmed.isEmpty()) {
            rootPackages.add(trimmed);
          }
        }
      }
      rootInitialized = true;
    }
  }

  public boolean shouldTrace(Class<?> clazz) {
    String pkg = clazz.getPackageName();
    for (String root : rootPackages) {
      if (pkg.startsWith(root)) {
        return true;
      }
    }
    return false;
  }
}

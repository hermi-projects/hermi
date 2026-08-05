package org.hermi.test.jupiter;

import org.hermi.test.Boundary;
import org.hermi.test.BoundaryRegistry;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class HermiExecutorArgumentsProvider implements ArgumentsProvider {

  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
    Class<?>[] methodParams = context.getRequiredTestMethod().getParameterTypes();
    Class<?> domainType = methodParams[0];

    List<Arguments> argumentList = new ArrayList<>();

    try {
      Constructor<?> primaryConstructor = Arrays.stream(domainType.getDeclaredConstructors())
        .max(java.util.Comparator.comparingInt(Constructor::getParameterCount))
        .orElseThrow(() -> new IllegalStateException("No suitable constructor found for " + domainType.getName()));

      primaryConstructor.setAccessible(true);
      Class<?>[] ctorParamTypes = primaryConstructor.getParameterTypes();
      Field[] allFields = domainType.getDeclaredFields();

      Object[] baselineCtorArgs = new Object[ctorParamTypes.length];
      for (int i = 0; i < ctorParamTypes.length; i++) {
        Boundary<?> boundary = BoundaryRegistry.boundaryFor(ctorParamTypes[i]);
        baselineCtorArgs[i] = boundary.validValue();
      }

      // Parameter-wise generation for constructor arguments
      for (int i = 0; i < ctorParamTypes.length; i++) {
        Boundary<?> boundary = BoundaryRegistry.boundaryFor(ctorParamTypes[i]);
        for (Object invalidValue : boundary.invalidValues()) {
          Object[] currentCtorArgs = baselineCtorArgs.clone();
          currentCtorArgs[i] = invalidValue;

          Object domainObject = primaryConstructor.newInstance(currentCtorArgs);
          initializeUnsetFields(domainObject, allFields);

          argumentList.add(Arguments.of(domainObject));
        }
      }

      // Fallback for fields outside the primary constructor
      for (Field field : allFields) {
        Boundary<?> boundary = BoundaryRegistry.boundaryFor(field.getType());
        field.setAccessible(true);

        for (Object invalidValue : boundary.invalidValues()) {
          Object domainObject = primaryConstructor.newInstance(baselineCtorArgs);
          initializeUnsetFields(domainObject, allFields);

          field.set(domainObject, invalidValue);
          argumentList.add(Arguments.of(domainObject));
        }
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to generate boundary arguments via HermiExecutorArgumentsProvider", e);
    }

    return argumentList.stream();
  }

  private void initializeUnsetFields(Object target, Field[] fields) throws IllegalAccessException {
    for (Field field : fields) {
      field.setAccessible(true);
      if (field.get(target) == null) {
        Boundary<?> boundary = BoundaryRegistry.boundaryFor(field.getType());
        field.set(target, boundary.validValue());
      }
    }
  }
}

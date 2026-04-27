package br.edu.ifrs.poa.infra;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
public class CaseInsensitiveEnumConverterProvider implements ParamConverterProvider {

  @Override
  public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
    if (!rawType.isEnum()) {
      return null;
    }

    return new ParamConverter<T>() {

      @Override
      public T fromString(String value) {
        if (value == null || value.isBlank() || value.isEmpty() || value.equalsIgnoreCase("todas"))
          return null;

        for (T constant : rawType.getEnumConstants()) {
          if (constant.toString().equalsIgnoreCase(value)) {
            return constant;
          }
        }

        throw new IllegalArgumentException("Invalid enum value: " + value);
      }

      @Override
      public String toString(T value) {
        return value.toString();
      }
    };
  }
}

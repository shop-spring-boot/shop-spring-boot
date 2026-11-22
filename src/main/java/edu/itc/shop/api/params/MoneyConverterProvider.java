package edu.itc.shop.api.params;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import edu.itc.shop.domain.Money;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/** Lets Money be used as @QueryParam type: ?minPrice=5%20EUR. */
@Provider
public class MoneyConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (!rawType.equals(Money.class)) {
            return null;
        }
        return new ParamConverter<T>() {
            @Override
            public T fromString(String value) {
                if (value == null) {
                    return null;
                }
                try {
                    return rawType.cast(Money.parse(value));
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Cannot parse Money from '" + value + "'");
                }
            }

            @Override
            public String toString(T value) {
                return value == null ? null : value.toString();
            }
        };
    }
}

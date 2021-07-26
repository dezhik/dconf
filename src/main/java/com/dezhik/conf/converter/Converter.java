package com.dezhik.conf.converter;

/**
 * @author ilya.dezhin
 */
public interface Converter<T> {
    T convert(String value);
}

package com.dezhik.conf.converter;

/**
 * @author ilya.dezhin
 */
public class StringConverter implements Converter<String> {
    public static final Converter<String> INSTANCE = new StringConverter();

    @Override
    public String convert(String value) {
        return value;
    }
}

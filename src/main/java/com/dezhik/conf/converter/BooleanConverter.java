package com.dezhik.conf.converter;

/**
 * @author ilya.dezhin
 */
public class BooleanConverter implements Converter<Boolean> {
    public static final Converter<Boolean> INSTANCE = new BooleanConverter();

    @Override
    public Boolean convert(String value) {
        return value != null && value.trim().equalsIgnoreCase("true");
    }
}

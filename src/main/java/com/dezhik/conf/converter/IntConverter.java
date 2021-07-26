package com.dezhik.conf.converter;

/**
 * @author ilya.dezhin
 */
public class IntConverter implements Converter<Integer> {
    public static final IntConverter INSTANCE = new IntConverter();

    @Override
    public Integer convert(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) { }

        return 0;
    }
}

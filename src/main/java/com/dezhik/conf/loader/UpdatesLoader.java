package com.dezhik.conf.loader;

import com.dezhik.conf.client.ConfValues;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author ilya.dezhin
 */
public interface UpdatesLoader {

    /**
     * @param modules list of modules
     * @param version
     * @return map module's name -> ConfValues for requested version
     */
    Map<String, ConfValues> getUpdates(List<String> modules, final long version) throws IOException;
}

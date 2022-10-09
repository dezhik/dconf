package com.dezhik.conf.storage;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public interface Storage {

    // search by exact name
    List<Property> getByFilter(PropertyFilter filter);

    /**
     *
     * @param module
     * @param host
     * @param propertyName
     * @return existing non-deleted property
     *         or null if property is marked as deleted
     */
    default Property getExactProperty(String module, String host, String propertyName) {
        List<Property> list = getByFilter(
                new PropertyFilter.Builder(module)
                    .hosts(Collections.singletonList(host))
                    .propertyNames(Collections.singletonList(propertyName))
                    .build()
        );
        return list.size() > 0 ? list.get(0) : null;
    }


    /**
     *
     * @param module
     * @param host
     * @param name
     * @param value
     * @return true if creation succeeded and false if property defined by {module, host, name} already exists
     */
    boolean create(String module, String host, String name, String value);
    boolean update(String module, String host, String name, String value, long lastVersion);
    boolean delete(String module, String host, String name, long lastVersion);

    boolean createModule(String module);
    List<PropertyModule> getModules();

    void registerClient(String host, long lastVersion, String modulesList);
    List<ClientModel> getClients();

    Date getLatestUpdate(String module);
    long getCurrentVersion(String module);
}

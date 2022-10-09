package com.dezhik.conf.storage;

import java.util.Date;
import java.util.List;

public class MockStorage implements Storage {


    @Override
    public List<Property> getByFilter(PropertyFilter filter) {

        return null;
    }

    @Override
    public boolean create(String module, String host, String name, String value) {
        return false;
    }

    @Override
    public boolean update(String module, String host, String name, String value, long lastVersion) {
        return false;
    }

    @Override
    public boolean delete(String module, String host, String name, long lastVersion) {
        return false;
    }

    @Override
    public boolean createModule(String module) {
        return false;
    }

    @Override
    public List<PropertyModule> getModules() {
        return null;
    }

    @Override
    public void registerClient(String host, long lastVersion, String modulesList) {
    }

    @Override
    public List<ClientModel> getClients() {
        return null;
    }

    @Override
    public Date getLatestUpdate(String module) {
        return null;
    }

    @Override
    public long getCurrentVersion(String module) {
        return 0;
    }
}

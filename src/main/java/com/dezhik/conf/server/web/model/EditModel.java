package com.dezhik.conf.server.web.model;

import com.dezhik.conf.server.web.NavigationModel;
import com.dezhik.conf.storage.Property;
import com.dezhik.conf.storage.PropertyModule;

import java.util.List;

public class EditModel extends NavigationModel {
    private boolean edit;
    private String error;
    private String module;
    private String host;
    private String name;
    private String value;
    private Long version;
    private List<Property> properties;
    private List<PropertyModule> modules;

    public boolean isEdit() {
        return edit;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public List<PropertyModule> getModules() {
        return modules;
    }

    public void setModules(List<PropertyModule> modules) {
        this.modules = modules;
    }
}

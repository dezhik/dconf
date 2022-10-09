package com.dezhik.conf.storage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PropertyFilter {
    public static final PropertyFilter EMPTY = new PropertyFilter(new Builder(null));

    private final String module;
    private final List<String> hosts;
    private final Collection<String> propertyNames;
    private final String searchQueryName;
    private final Long version;

    private PropertyFilter(Builder builder) {
        this.module = builder.module;
        this.hosts = builder.hosts;
        this.propertyNames = builder.propertyNames;
        this.searchQueryName = builder.searchQueryName;
        this.version = builder.version;
    }

    public String getModule() {
        return module;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public Collection<String> getPropertyNames() {
        return propertyNames;
    }

    public String getSearchQueryName() {
        return searchQueryName;
    }

    public Long getVersion() {
        return version;
    }

    public boolean isClientDiffRequest() {
        return getVersion() != null;
    }

    @Override
    public String toString() {
        return "PropertyFilter{" +
                "module='" + module + '\'' +
                ", hosts=" + hosts +
                ", propertyNames=" + propertyNames +
                ", searchQueryName='" + searchQueryName + '\'' +
                ", version=" + version +
                '}';
    }

    public static class Builder {
        private String module;
        private List<String> hosts = Collections.emptyList();
        private Collection<String> propertyNames;
        private String searchQueryName;
        private Long version;

        public Builder(String module) {
            this.module = module;
        }

        public PropertyFilter build() {
            return new PropertyFilter(this);
        }

        public Builder hosts(List<String> hosts) {
            this.hosts = hosts;
            return this;
        }

        public Builder propertyNames(Collection<String> propertyName) {
            this.propertyNames = propertyName;
            return this;
        }

        public Builder searchQueryName(String searchQueryName) {
            this.searchQueryName = searchQueryName;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }
    }
}

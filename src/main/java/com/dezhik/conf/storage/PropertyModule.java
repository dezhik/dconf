package com.dezhik.conf.storage;

import java.util.Date;
import java.util.Objects;

public class PropertyModule {
    private String name;
    private Date lastUpdate;
    private Date lastRead;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastUpdate() {

        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Date getLastRead() {
        return lastRead;
    }

    public void setLastRead(Date lastRead) {
        this.lastRead = lastRead;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyModule that = (PropertyModule) o;
        return name.equals(that.name) && lastUpdate.equals(that.lastUpdate) && Objects.equals(lastRead, that.lastRead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, lastUpdate, lastRead);
    }
}

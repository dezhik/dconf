package com.dezhik.conf.storage;

import java.util.Date;

public class ClientModel {
    private String host;
    private long version;
    private Date lastRead;
    private String modulesList;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Date getLastRead() {
        return lastRead;
    }

    public void setLastRead(Date lastRead) {
        this.lastRead = lastRead;
    }

    public String getModulesList() {
        return modulesList;
    }

    public void setModulesList(String modulesList) {
        this.modulesList = modulesList;
    }
}

package com.dezhik.conf.server.web;

import com.dezhik.conf.client.ConfService;
import com.dezhik.conf.server.ConfServer;
import com.dezhik.conf.storage.PropertyModule;

import java.util.Date;
import java.util.List;

public class NavigationModel {
    private final String currentPage;
    private String selectedModule;
    private List<PropertyModule> modules;

    public NavigationModel() {
        this(null);
    }

    public NavigationModel(String selectedMenuItem) {
        this.currentPage = selectedMenuItem;
    }

    public String getCurrentPage() {
        return currentPage;
    }

    public List<PropertyModule> getModules() {
        return modules;
    }

    public void setModules(List<PropertyModule> modules) {
        this.modules = modules;
    }

    public String getSelectedModule() {
        return selectedModule;
    }

    public void setSelectedModule(String selectedModule) {
        this.selectedModule = selectedModule;
    }

    public String getAppVersion() {
        return ConfService.VERSION;
    }

    public Date getAppStartTime() {
        return ConfServer.START_TIME;
    }
}

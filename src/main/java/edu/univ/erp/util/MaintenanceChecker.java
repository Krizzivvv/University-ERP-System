package edu.univ.erp.util;

public class MaintenanceChecker {
    private final SettingsDao settingsDao;

    public MaintenanceChecker() {
        this.settingsDao = new SettingsDao();
    }

    public boolean isMaintenanceOn() {
        return settingsDao.getBoolean("maintenance_on", false);
    }
}

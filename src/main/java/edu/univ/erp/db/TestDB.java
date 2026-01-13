package edu.univ.erp.db;

import java.sql.Connection;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Starting DB initialization (creates DBs/tables if missing)...");
        DBManager.initDatabases();

        System.out.println("\nTesting AUTH DB...");
        try (Connection c = DBManager.getAuthConnection()) {
            System.out.println("Connected to: " + c.getCatalog());
        } catch (Exception e) {
            System.out.println("AUTH DB FAILED");
            e.printStackTrace();
        }

        System.out.println("\nTesting ERP DB...");
        try (Connection c = DBManager.getErpConnection()) {
            System.out.println("Connected to: " + c.getCatalog());
        } catch (Exception e) {
            System.out.println("ERP DB FAILED");
            e.printStackTrace();
        }
    }
}

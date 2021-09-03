package net.cg360.spookums.server.db;

import net.cg360.spookums.server.Server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public class DatabaseManager {

    private static DatabaseManager primaryInstance;

    public static final String SQLITE_DATABASE_PATH = "db/";
    public static final String CONNECTION_PREFIX = "jdbc:sqlite:";

    public DatabaseManager() { }

    public boolean setAsPrimaryInstance() {
        if(primaryInstance == null) {
            primaryInstance = this;
            return true;
        }
        return false;
    }



    public Connection access(String name) {
        File file = new File(Server.get().getDataPath(), SQLITE_DATABASE_PATH + name + ".db");
        String path = CONNECTION_PREFIX +  file.getAbsolutePath();

        try {
            Connection conn = DriverManager.getConnection(path);
            return conn;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static DatabaseManager get() {
        return primaryInstance;
    }
}

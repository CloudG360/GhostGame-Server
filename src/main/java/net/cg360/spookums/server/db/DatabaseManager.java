package net.cg360.spookums.server.db;

import net.cg360.spookums.server.Server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        boolean result = new File(Server.get().getDataPath(), SQLITE_DATABASE_PATH).mkdirs();

        File file = new File(Server.get().getDataPath(), SQLITE_DATABASE_PATH + name + ".db");
        String path = CONNECTION_PREFIX +  file.getAbsolutePath();

        try {
            return DriverManager.getConnection(path);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static DatabaseManager get() {
        return primaryInstance;
    }
}

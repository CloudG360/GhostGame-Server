package net.cg360.spookums.server.db;

import net.cg360.spookums.server.Server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

public class DatabaseManager {

    public static final String SQLITE_DATABASE_PATH = "db/";
    public static final String CONNEECTION_PREFIX = "jdbc:sqlite:";

    public DatabaseManager() { }

    public Connection access(String name) {
        File file = new File(Server.get().getDataPath(), SQLITE_DATABASE_PATH + name + ".db");
        String path = CONNEECTION_PREFIX +  file.getAbsolutePath();

        try {
            Connection conn = DriverManager.getConnection(path);
            return conn;

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}

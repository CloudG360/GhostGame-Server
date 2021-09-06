package net.cg360.spookums.server.util;

import java.sql.Connection;
import java.sql.Statement;

public class ErrorUtil {

    public static void quietlyClose(Connection connection) {
        try {
            connection.close();
        } catch (Exception ignored) {}
    }

    public static void quietlyClose(Statement statement) {
        try {
            statement.close();
        } catch (Exception ignored) {}
    }

    public static void tryLog(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception err) {
            err.printStackTrace();
        }

    }

}

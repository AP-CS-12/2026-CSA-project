import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL =
            "jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private static final String USER = "postgres.esmodnfwtcrhsldqysxf";
    private static final String PASSWORD = "APCSA2026?!";

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected!");
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
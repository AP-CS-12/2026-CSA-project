import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String URL =
            "jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private static final String un = System.getenv("USERNAME");
    private static final String pw = System.getenv("PASSWORD");

    public static void getConnection() {
        try (Connection conn = DriverManager.getConnection(URL, un, pw)) {
            System.out.println("Connected successfully!");
        } catch (SQLException e) {
            System.out.println("Connection failed:");
            e.printStackTrace();
        }
    }
}
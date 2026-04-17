import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class Database {

    private static final String URL =
            "jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require";

    private static final String un = System.getenv("USERNAME");
    private static final String pw = System.getenv("PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, un, pw);
    }

    // 🔹 Insert into questions table
    public static void insertQuestion(String topic, long difficulty, String jsonContent) {
        String sql = """
                INSERT INTO questions (id, topic, difficulty, content)
                VALUES (?, ?, ?, ?::jsonb)
                """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, UUID.randomUUID()); // id
            stmt.setString(2, topic);
            stmt.setLong(3, difficulty);
            stmt.setString(4, jsonContent);

            stmt.executeUpdate();
            System.out.println("Question inserted!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 🔹 Insert into sessions table
    public static void insertSession(String studentId, String jsonData) {
        String sql = """
                INSERT INTO sessions (id, student_id, data)
                VALUES (?, ?, ?::jsonb)
                """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, UUID.randomUUID()); // id
            stmt.setString(2, studentId);
            stmt.setString(3, jsonData);

            stmt.executeUpdate();
            System.out.println("Session inserted!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
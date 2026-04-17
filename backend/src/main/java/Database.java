import java.sql.*;

public class Database {

    private static final String URL =
            "jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require";

    // FIX: use consistent env var names for cloud deployment
    private static final String USER = System.getenv("DB_USER");
    private static final String PASS = System.getenv("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // ─────────────────────────────────────────────
    // QUESTIONS
    // ─────────────────────────────────────────────
    public static void insertQuestion(String topic, long difficulty, String jsonContent) {

        String sql = """
                INSERT INTO questions (topic, difficulty, content)
                VALUES (?, ?, ?::jsonb)
                """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, topic);
            stmt.setLong(2, difficulty);
            stmt.setString(3, jsonContent);

            stmt.executeUpdate();
            System.out.println("Question inserted!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────
    // STUDENTS (unchanged logic but stable)
    // ─────────────────────────────────────────────
    public static long getOrCreateStudent(String name) {

        try (Connection conn = getConnection()) {

            PreparedStatement check = conn.prepareStatement(
                    "SELECT id FROM students WHERE name = ?"
            );
            check.setString(1, name);

            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                return rs.getLong("id");
            }

            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO students (name) VALUES (?) RETURNING id"
            );
            insert.setString(1, name);

            ResultSet inserted = insert.executeQuery();
            inserted.next();

            return inserted.getLong("id");

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // ─────────────────────────────────────────────
    // SESSIONS (FIXED TO MATCH YOUR TABLE)
    // ─────────────────────────────────────────────
    public static long createSession(long studentId) {

        String sql = """
        INSERT INTO sessions (student_id, current_difficulty, status)
        VALUES (?, 'easy', 'active')
        RETURNING id
        """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, studentId);

            ResultSet rs = stmt.executeQuery();
            rs.next();

            return rs.getLong("id");

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
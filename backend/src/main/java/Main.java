import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        System.out.println("Server starting...");
        Database.getConnection();

        //Demo "add question"
        Database.insertQuestion(
                "math",
                3,
                """
                {
                  "question": "What is 2+2?",
                  "answers": [3, 4, 5],
                  "correct": 4
                }
                """
        );
    }
}
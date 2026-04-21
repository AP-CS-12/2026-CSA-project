public class Teacher {
    private String username;
    private String password;

    public Teacher(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean login(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }

    public void createClass() {
        System.out.println(username + " created a class.");
    }

    public void addStudent() {
        System.out.println("Student added to " + username + "'s class.");
    }

    public void viewStudentProfile() {
        System.out.println("Viewing student profile.");
    }
}
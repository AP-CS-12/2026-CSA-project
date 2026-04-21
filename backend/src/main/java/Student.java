public class Student {
    //Data
    private String username;
    private String password;
    
    //Getter methods
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    
    //Constructor
    public Student(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    //Methods(connect to buttons)
    public boolean login(String username, String password) { 
    	return this.username.equals(username) && this.password.equals(password);
    }
    public void viewScore() { 
    	//view the student's past scores
    }

}

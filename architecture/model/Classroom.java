package architecture.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Classroom {
    private final String id;
    private final String name;
    private final TeacherProfile teacher;
    private final List<StudentProfile> students;

    public Classroom(String id, String name, TeacherProfile teacher) {
        this.id = id;
        this.name = name;
        this.teacher = teacher;
        this.students = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TeacherProfile getTeacher() {
        return teacher;
    }

    public void addStudent(StudentProfile student) {
        students.add(student);
    }

    public List<StudentProfile> getStudents() {
        return Collections.unmodifiableList(students);
    }
}

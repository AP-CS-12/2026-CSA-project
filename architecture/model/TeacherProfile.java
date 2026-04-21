package architecture.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TeacherProfile {
    private final String id;
    private final String name;
    private final List<Classroom> classrooms;

    public TeacherProfile(String id, String name) {
        this.id = id;
        this.name = name;
        this.classrooms = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Classroom createClassroom(String classroomId, String classroomName) {
        Classroom classroom = new Classroom(classroomId, classroomName, this);
        classrooms.add(classroom);
        return classroom;
    }

    public void addStudent(Classroom classroom, StudentProfile student) {
        classroom.addStudent(student);
    }

    public List<Result> viewStudentResults(StudentProfile student) {
        Objects.requireNonNull(student, "student");
        return student.getResults();
    }

    public List<Classroom> getClassrooms() {
        return Collections.unmodifiableList(classrooms);
    }
}

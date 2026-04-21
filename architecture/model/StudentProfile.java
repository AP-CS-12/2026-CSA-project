package architecture.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StudentProfile {
    private final String id;
    private final String name;
    private int adaptiveLevel;
    private final List<Result> results;

    public StudentProfile(String id, String name, int adaptiveLevel) {
        this.id = id;
        this.name = name;
        this.adaptiveLevel = adaptiveLevel;
        this.results = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAdaptiveLevel() {
        return adaptiveLevel;
    }

    public void setAdaptiveLevel(int adaptiveLevel) {
        this.adaptiveLevel = adaptiveLevel;
    }

    public void addResult(Result result) {
        results.add(result);
    }

    public List<Result> getResults() {
        return Collections.unmodifiableList(results);
    }
}

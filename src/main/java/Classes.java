import java.util.ArrayList;
import java.util.List;

/**
 * Data structure for classes
 */
public class Classes {
    // Static variables to store the courses, labs, notCompatible, and unwantedTimes lists.
    private static final List<List<String>> courses = Scheduler.getCourses();
    private static final List<List<String>> labs = Scheduler.getLabs();
    private static final List<ArrayList<List<String>>> notCompatible = Scheduler.getNotCompatible();
    private static final List<ArrayList<List<String>>> unwantedTimes = Scheduler.getUnwanted();

    // Static variables to store the priority score for evening classes.
    private static final int EVENING = 50;

    // Properties to store various attributes and constraints of the class.
    // The name or identifier for the course or lab.
    private final List<String> name;

    // Flag indicating whether the class is an evening class.
    private final boolean isEvening;

    // Times that are not preferred for the class.
    private final ArrayList<ArrayList<List<String>>> unwanted = new ArrayList<>();

    // Other classes that cannot be scheduled at the same time.
    private final ArrayList<List<String>> incompatible = new ArrayList<>();

    // Associated lab sections if any.
    private final ArrayList<List<String>> labList = new ArrayList<>();

    // Index of the class.
    private int index;

    // Priority score of the class.
    private int priorityScore = 0;

    /**
     * Constructor initializes a new class with constraints based on its type and index.
     *
     * @param type  Type of the class, either "course" or "lab".
     * @param index Position of the class in the courses or labs list.
     */
    public Classes(String type, int index) {
        // Set name and index:
        try {
            this.index = type.equals("course")
                ? index
                : index + Classes.courses.size();
            this.name = type.equals("course")
                ? Classes.courses.get(index)
                : Classes.labs.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Invalid index for class " + index + " at Classes.java");
        }
        // Determines if the class is an evening class by checking the third element of the name list.
        // e.g. [CPSC, 433, LEC, 01]
        this.isEvening = this.name.size() > 3 && this.name.get(3).startsWith("9");
        // Populate incompatible classes based on the notCompatible list.
        // e.g. [[CPSC, 433, LEC, 01, TUT, 01], [CPSC, 433, LEC, 02, LAB, 02]]
        for (List<List<String>> pair : Classes.notCompatible) {
            // e.g. [CPSC, 433, LEC, 01] is incompatible with [CPSC, 433, LEC, 02]
            if (pair.get(0).equals(this.name)) {
                this.incompatible.add(pair.get(1));
            } else if (pair.get(1).equals(this.name)) {
                this.incompatible.add(pair.get(0));
            }
        }
        // Populate unwanted times for this class.
        // e.g. [[CPSC, 433, LEC, 01], [MO], [8:00]]
        for (ArrayList<List<String>> unwantedTime : Classes.unwantedTimes) {
            // e.g. [CPSC, 433, LEC, 01] is unwanted at [MO] [8:00]
            if (unwantedTime.get(0).equals(this.name)) {
                this.unwanted.add(unwantedTime);
            }
        }
        // Populate the list of associated labs if this is a course.
        for (List<String> labName : Classes.labs) {
            // e.g. [CPSC, 433, LEC, 01, TUT, 01] contains [CPSC, 433, LEC, 01]
            if (labName.containsAll(this.name)) {
                this.labList.add(labName);
            } else if (!labName.contains("LEC")) {
                // handle edge cases where lab is open to all sections
                // e.g. [CPSC, 567, TUT, 01]
                if (labName.get(0).equals(this.name.get(0))
                    && labName.get(1).equals(this.name.get(1))) {
                    this.labList.add(labName);
                }
            }
        }
        // Calculate the priority score of the class.
        this.priorityScore = this.unwanted.size()
            + this.incompatible.size()
            + this.labList.size()
            + (this.isEvening ? Classes.EVENING : 0);
    }

    /**
     * Method returns the index of the class.
     *
     * @return The index of the class.
     */
    public int getIndex() {
        return this.index;
    }

    /**
     * Method returns the name of the class.
     *
     * @return The name of the class.
     */
    public List<String> getName() {
        return this.name;
    }

    /**
     * Method returns the list of labs associated with the class.
     *
     * @return The list of labs associated with the class.
     */
    public ArrayList<List<String>> getLabList() {
        return this.labList;
    }

    /**
     * Method returns whether the class is an evening class.
     *
     * @return Whether the class is an evening class.
     */
    public boolean isEvening() {
        return this.isEvening;
    }

    /**
     * Method returns the list of unwanted times for the class.
     *
     * @return The list of unwanted times for the class.
     */
    public ArrayList<ArrayList<List<String>>> getUnwanted() {
        return this.unwanted;
    }

    /**
     * Method returns the list of incompatible classes for the class.
     *
     * @return The list of incompatible classes for the class.
     */
    public ArrayList<List<String>> getIncompatible() {
        return this.incompatible;
    }

    /**
     * Method returns the priority score of the class.
     * The priority score is calculated based on the number of constraints the class has.
     *
     * @return The priority score of the class.
     */
    public int priorityScore() {
        return this.priorityScore;
    }
}

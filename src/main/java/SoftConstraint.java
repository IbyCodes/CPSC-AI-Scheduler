import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates the soft constraints for a given schedule.
 */
public class SoftConstraint {
    // Maps to keep track of the number of times a slot has been assigned.
    private final HashMap<List<String>, Integer> courseSlotsMap = new HashMap<>();
    private final HashMap<List<String>, Integer> labSlotsMap = new HashMap<>();

    // General soft constraints
    private int penCourseMin;
    private int penLabMin;
    private int penNotPaired;

    // Department soft constraint
    private int penSection;

    private int wMinFilled;
    private int wPref;
    private int wPair;
    private int wSecDiff;

    // Data from the config file
    private final List<List<String>> courses;
    private final List<List<String>> labs;
    private final List<List<String>> courseSlots;
    private final List<List<String>> labSlots;
    private final List<ArrayList<List<String>>> preferences;
    private final List<ArrayList<List<String>>> pairs;

    private final int numCourses;
    private final int numCourseSlots;
    private final int numLabSlots;

    // For testing purposes, set to true to print the data from the config file
    private final boolean printData = false;

    /**
     * Constructor for the class. Takes in a config file that contains
     * all the parameters and weights.
     *
     * @param configFile Config file
     */
    public SoftConstraint(String configFile) throws Exception {
        // Read the config file
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile))) {
            // Read the file line by line
            String content;
            // Parse the file line by line
            while ((content = bufferedReader.readLine()) != null) {
                // Split the line into key and value
                String[] setting = content.trim().split("=");
                // Check if the line is valid
                if (setting.length >= 2) {
                    // Trim the key and value
                    String key = setting[0].trim();
                    String val = setting[1].trim();
                    try {
                        switch (key) {
                        case "wMinFilled":
                            this.wMinFilled = Integer.parseInt(val);
                            break;
                        case "wPref":
                            this.wPref = Integer.parseInt(val);
                            break;
                        case "wPair":
                            this.wPair = Integer.parseInt(val);
                            break;
                        case "wSecDiff":
                            this.wSecDiff = Integer.parseInt(val);
                            break;
                        case "penCourseMin":
                            this.penCourseMin = Integer.parseInt(val);
                            break;
                        case "penLabMin":
                            this.penLabMin = Integer.parseInt(val);
                            break;
                        case "penNotPaired":
                            this.penNotPaired = Integer.parseInt(val);
                            break;
                        case "penSection":
                            this.penSection = Integer.parseInt(val);
                            break;
                        case "printPr":
                            Scheduler.setPrintPR(Boolean.parseBoolean(val));
                            break;
                        case "printData":
                            Scheduler.setPrintData(Boolean.parseBoolean(val));
                            break;
                        case "initialPop":
                            Scheduler.setInitialPop(Integer.parseInt(val));
                            break;
                        case "maxPop":
                            Scheduler.setMaxPop(Integer.parseInt(val));
                            break;
                        case "maxGeneration":
                            Scheduler.setMaxGenerations(Integer.parseInt(val));
                            break;
                        case "numRemove":
                            Scheduler.setNumRemove(Integer.parseInt(val));
                            break;
                        case "stableThreshold":
                            Scheduler.setStableThreshold(Integer.parseInt(val));
                            break;
                        case "maxStableGeneration":
                            Scheduler.setMaxStableGenerations(Integer.parseInt(val));
                            break;
                        default:
                            System.out.println("Unknown parameter.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid format at SoftConstraint.java");
                        throw(e);
                    }
                }
            }
            // Print the data for testing purposes
            if (this.printData) {
                printDataForTesting();
            }
        } finally {
            // Fetch data once and store it
            this.courses = Scheduler.getCourses();
            this.labs = Scheduler.getLabs();
            this.courseSlots = Scheduler.getCourseSlots();
            this.labSlots = Scheduler.getLabSlots();

            this.numCourses = this.courses.size();
            this.numCourseSlots = this.courseSlots.size();
            this.numLabSlots = this.labSlots.size();

            this.preferences = Scheduler.getPreferences();
            this.pairs = Scheduler.getPairs();
            populateHashMaps();
        }
    }

    /**
     * Prints the data from the config file for testing purposes.
     */
    private void printDataForTesting() {
        System.out.println("wMinFilled: " + this.wMinFilled);
        System.out.println("wPref: " + this.wPref);
        System.out.println("wPair: " + this.wPair);
        System.out.println("wSecDiff: " + this.wSecDiff);
        System.out.println("penCourseMin: " + this.penCourseMin);
        System.out.println("penLabMin: " + this.penLabMin);
        System.out.println("penNotPaired: " + this.penNotPaired);
        System.out.println("penSection: " + this.penSection);
    }

    // Interested in only the day and time of the slot.
    // eg. MO, 8:00
    private void populateHashMaps() {
        // Populate the course and lab slots maps.
        try {
            this.courseSlotsMap.clear();
            this.labSlotsMap.clear();
            for (List<String> slot : this.courseSlots) {
                if (slot.size() >= 2) {
                    // Add the slot to the map with an initial count of 0.
                    // eg. [MO, 8:00] -> 0
                    List<String> key = Arrays.asList(slot.get(0), slot.get(1));
                    this.courseSlotsMap.put(key, 0);
                }
            }
            for (List<String> slot : this.labSlots) {
                if (slot.size() >= 2) {
                    // Add the slot to the map with an initial count of 0.
                    // eg. [MO, 8:00] -> 0
                    List<String> key = Arrays.asList(slot.get(0), slot.get(1));
                    this.labSlotsMap.put(key, 0);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Invalid format at SoftConstraint.java");
            System.out.println("Please check the input file.");
        }
    }

    /**
     * Returns the penalty incurred for either courses, or labs if number of slots
     * assigned below min.
     *
     * Course slots:
     * Day, Start time, coursemax, coursemin
     * e.g. MO, 8:00, 3, 2
     *
     * Lab slots:
     * Day, Start time, labmax, labmin
     * e.g. MO, 8:00, 3, 2
     *
     * @param assignmentCounter int array containing the number of times a slot has
     *                          been assigned
     * @param slots             list of course/lab slots given in input file
     * @param penaltyPerSlot    penalty values for course/lab
     * @return total penalty incurred for courses or labs
     */
    private int calculateSlotPenalties(int[] assignmentCounter, List<List<String>> slots, int penaltyPerSlot) {
        try {
            int slotPenalty = 0;
            for (int i = 0; i < slots.size(); i++) {
                // Follow format (Day, start, labmax, labmin)
                // Check if the slot has a min value
                List<String> slotInfo = slots.get(i);
                if (slotInfo.size() > 3) {
                    try {
                        // Check if the number of assignments is less than the min value
                        int minValue = Integer.parseInt(slotInfo.get(3));
                        if (assignmentCounter[i] < minValue) {
                            if (this.printData) {
                                System.out.println("Penalty for slot "
                                        + slotInfo.get(0) + " " + slotInfo.get(1) + " " + penaltyPerSlot);
                            }
                            // Add the penalty for each slot that has less than the min value.
                            slotPenalty += penaltyPerSlot;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid minValue format at SoftConstraint.java");
                    }
                }
            }
            // Return the total penalty incurred for courses or labs.
            return slotPenalty;
        } catch (Exception e) {
            System.out.println("Error occured at method calculateSlotPenalties in SoftConstraint.java");
            System.out.println(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Since there are usually time slots that are less liked than others,
     * there is a certain pressure to also put courses and labs into the more
     * unwanted slots.
     * To facilitate this pressure, we have for each slot s minimal numbers
     * coursemin(s) and labmin(s)
     * that indicate how many courses, resp. labs, should at least be scheduled into
     * the slot s.
     * Your system should be able to accept as input penalty values pen_coursemin
     * and pen_labsmin
     * (as system parameters) and for each course below coursemin we will get
     * pen_coursemin and
     * for each lab pen_labsmin added to the Eval-value of an assignment.
     *
     * @param courseAssignment slots assigned to courses
     * @param labAssignment    slots assigned to labs
     * @return penalty value
     */
    private int evalMinFilled(List<List<String>> courseAssignment, List<List<String>> labAssignment) {
        try {
            int penalty = 0;
            // Create arrays to keep track of the number of assignments for each slot.
            int[] courseAssignCount = new int[this.numCourseSlots];
            int[] labAssignCount = new int[this.numLabSlots];
            // Count the total number of assignments for each course slot
            for (int i = 0; i < courseAssignment.size(); i++) {
                // Retrieve the assigned slot for the course.
                List<String> assignedSlot = courseAssignment.get(i);
                // Skip unassigned slots
                if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                } else {
                    // Increment the count for the assigned slot.
                    courseAssignCount[this.courseSlots.indexOf(assignedSlot)]++;
                }
            }
            // Count the total number of assignments for each lab slot
            for (int i = 0; i < labAssignment.size(); i++) {
                // Retrieve the assigned slot for the lab.
                List<String> assignedSlot = labAssignment.get(i);
                // Skip unassigned slots
                if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                } else {
                    // Increment the count for the assigned slot.
                    labAssignCount[this.labSlots.indexOf(assignedSlot)]++;
                }
            }
            // Calculate penalties for courses
            penalty += calculateSlotPenalties(courseAssignCount, this.courseSlots, this.penCourseMin);
            // Calculate penalties for labs
            penalty += calculateSlotPenalties(labAssignCount, this.labSlots, this.penLabMin);
            // Return the total penalty incurred for courses and labs.
            return penalty;
        } catch (Exception e) {
            System.out.println("Error occured at method evalMinFilled in SoftConstraint.java");
            System.out.println(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Calculates the penalty for an individual preference based on the assigned
     * slot.
     *
     * @param assignments     List of slots assigned to class.
     * @param classIdentifier Identifier of the class.
     * @param preferredDay    Preferred day for the class.
     * @param preferredTime   Preferred time for the class.
     * @param preferenceValue The penalty value if the preference is not satisfied.
     * @param classList       List of all classes.
     * @param slotList        List of all time slots.
     * @param slotsMap        Map of class slots to their indices for quick lookup.
     * @return The penalty score for this preference.
     */
    private int calculateIndividualPreferencePenalty(List<List<String>> assignments, List<String> classIdentifier,
            String preferredDay, String preferredTime, int preferenceValue,
            List<List<String>> classList, List<List<String>> slotList,
            Map<List<String>, Integer> slotsMap) {
        try {
            // Combine the preferred day and time into a single slot identifier.
            List<String> preferredSlot = Arrays.asList(preferredDay, preferredTime);
            // If the preferred slot doesn't exist in the map, there's no penalty.
            if (!slotsMap.containsKey(preferredSlot)) {
                return 0;
            }
            // Find the index of the class in the class list.
            int classIndex = classList.indexOf(classIdentifier);
            // If the class identifier is not found, return 0 as there's no penalty.
            if (classIndex < 0) {
                return 0;
            }
            // Retrieve the slot index assigned to the class. If it's invalid, return 0.
            try {
                List<String> assignedSlot = assignments.get(classIndex);
                // If the assigned slot doesn't match the preferred slot, apply the penalty.
                if (!assignedSlot.subList(0, 2).equals(preferredSlot)) {
                    if (this.printData) {
                        System.out.println("Penalty for class " + classIdentifier + " " + preferenceValue);
                    }
                    return preferenceValue;
                }
            } catch (IndexOutOfBoundsException e) {
                return Integer.MAX_VALUE;
            }
            // If the preferred slot matches the assigned slot, no penalty is applied.
            return 0;
        } catch (Exception e) {
            System.out.println("Error occured at method calculateIndividualPreferencePenalty in SoftConstraint.java");
            System.out.println(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Determines if the provided class identifier corresponds to a lab or tutorial.
     * Identifiers typically follow a pattern where a lab or tutorial is indicated
     * by the presence of "LAB" or "TUT" in the identifier.
     *
     * @param classIdentifier The list of strings that make up the class identifier.
     * @return true if the identifier represents a lab or tutorial, false otherwise.
     */
    private boolean isLabOrTutorial(List<String> classIdentifier) {
        return classIdentifier.contains("TUT") || classIdentifier.contains("LAB");
    }

    /**
     * Certain professors that often teach certain courses have certain preferences
     * regarding in which time slots their courses and labs should be scheduled.
     * Naturally, we see this as something that should be treated as soft
     * constraint.
     * Depending on a to-be-determined ranking scheme, each professor will be
     * awarded
     * a certain set of ranking points and he/she can distribute these points over
     * pairs
     * of (course/lab, time slots). Formally, we assume a
     * function preference: (Courses + Labs) x Slots -> Natural numbers that reports
     * those preferences.
     * For each assignment in assign, we add up the preference-values for a
     * course/lab that refer to a different slot
     * as the penalty that is added to the Eval-value of assign.
     *
     * @param courseAssignment slots assigned to courses
     * @param labAssignment    slots assigned to labs
     * @return penalty value
     */
    private int evalPref(List<List<String>> courseAssignment, List<List<String>> labAssignment) {
        try {
            int penalty = 0;
            // Iterate over each preference set by the professors.
            for (ArrayList<List<String>> preference : this.preferences) {
                // Extract preference details
                // Example preference format: [[TU], [9:00], [CPSC, 433, LEC, 01], [10]]
                String slotType = preference.get(0).get(0);
                String slotTime = preference.get(1).get(0);
                List<String> classIdentifier = preference.get(2);
                int preferenceValue = 0;
                try {
                    // Extract the preference value from the preference list.
                    preferenceValue = Integer.parseInt(preference.get(3).get(0));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid format at SoftConstraint.java");
                    return Integer.MAX_VALUE;
                }
                // Check if preference is for lab or course and calculate accordingly
                if (isLabOrTutorial(classIdentifier)) {
                    // Calculate the penalty for a lab preference.
                    penalty += calculateIndividualPreferencePenalty(labAssignment, classIdentifier,
                            slotType, slotTime, preferenceValue,
                            this.labs, this.labSlots, this.labSlotsMap);
                } else {
                    // Calculate the penalty for a course preference.
                    penalty += calculateIndividualPreferencePenalty(courseAssignment, classIdentifier,
                            slotType, slotTime, preferenceValue,
                            this.courses, this.courseSlots, this.courseSlotsMap);
                }
            }
            // Return the total penalty incurred for preferences.
            return penalty;
        } catch (Exception e) {
            System.out.println("Error occured at method evalPref in SoftConstraint.java");
            System.out.println(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Retrieves the day and time for a given class or lab based on its identifier.
     *
     * @param classIdentifier   The identifier for the class or lab.
     * @param courseAssignments Assigned slots for courses.
     * @param labAssignments    Assigned slots for labs.
     * @return A List containing the day and time of the class/lab if assigned, null
     *         otherwise.
     */
    private List<String> getClassTime(List<String> classIdentifier,
        List<List<String>> courseAssignments, List<List<String>> labAssignments) {
        try {
            boolean isLabOrTutorial = isLabOrTutorial(classIdentifier);
            // Find the index of the class in the courses list to check if it is a course.
            int classIndex = isLabOrTutorial
                    ? this.labs.indexOf(classIdentifier)
                    : this.courses.indexOf(classIdentifier);
            // If the class identifier is not found, return null.
            if (classIndex < 0) {
                return null;
            }
            try {
                // Retrieve the slot for the class based on whether it's a course or a
                // lab.
                List<String> slotInfo = isLabOrTutorial
                        ? labAssignments.get(classIndex)
                        : courseAssignments.get(classIndex);
                // Check if the class is unassigned; return null in
                // such cases.
                if (slotInfo.equals(Scheduler.UNASSIGNED_SLOT)) {
                    return null;
                }
                // Extract and return only the day and time from the slot information.
                // eg.. TU, 9:30
                return slotInfo.subList(0, 2);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        } catch (Exception e) {
            System.out.println("Error occured at method getClassTime in SoftConstraint.java");
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * For certain courses and/or labs, a department might know that there are never
     * any students that take these courses/labs in the same semester.
     * And therefore the department might find it convenient to have such
     * courses/labs scheduled
     * at the same time (this can also be used to keep students from taking certain
     * courses prematurely).
     * To facilitate this, there will be a list of pair(a,b) statements in the input
     * for your system,
     * with a,b in Courses + Labs, and a parameter pen_notpaired for your system.
     * For every pair(a,b) statement, for which assign(a) is not equal to assign(b),
     * you have to add pen_notpaired to the Eval-value of assign.
     *
     * e.g. [[[SENG, 311, LEC, 01], [CPSC, 567, LEC, 01]]]
     *
     * @param courseAssignment slots assigned to courses
     * @param labAssignment    slots assigned to labs
     * @return penalty value
     */
    private int evalPair(List<List<String>> courseAssignment, List<List<String>> labAssignment) {
        try {
            int penalty = 0;
            // Iterate over each pair of course/labs that are supposed to be paired.
            for (List<List<String>> pair : this.pairs) {
                List<String> firstClass = pair.get(0);
                List<String> secondClass = pair.get(1);
                // Get the assigned times for the first and second course/lab.
                List<String> firstClassTime = getClassTime(firstClass, courseAssignment, labAssignment);
                List<String> secondClassTime = getClassTime(secondClass, courseAssignment, labAssignment);
                // Continue if either class is unassigned
                if (firstClassTime == null || secondClassTime == null) {
                    continue;
                }
                // If the classes/labs are not assigned to the same time slot, apply the
                // penalty.
                if (!firstClassTime.equals(secondClassTime)) {
                    if (this.printData) {
                        System.out.println("Penalty for pair " + firstClass
                            + " " + secondClass + " " + this.penNotPaired);
                    }
                    // Add the penalty for each pair of classes/labs that are not assigned to
                    penalty += this.penNotPaired;
                }
            }
            return penalty;
        } catch (Exception e) {
            System.out.println("Error occured at method evalPair in SoftConstraint.java");
            System.out.println(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Different sections of a course should be scheduled at different times.
     * For each pair of sections that is scheduled into the same slot,
     * we add a penalty pen_section to the Eval-value of an assignment assign.
     *
     * @param courseAssignment slots assigned to courses
     * @return penalty value
     */
    private int evalSecDiff(List<List<String>> courseAssignment) {
        try {
            int penalty = 0;
            // A map to track the number of times a course section is assigned to a
            // particular time slot.
            Map<List<String>, Map<List<String>, Integer>> courseSchedule = new HashMap<>();
            // Loop through all courses to build a map of their assigned time slots.
            // [[MO, 8:00, 3, 2], [MO, 9:00, 3, 2], [TU, 9:30, 2, 1]]
            for (int i = 0; i < this.numCourses; i++) {
                if (courseAssignment.get(i).equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Retrieve the course name.
                List<String> courseName = this.courses.get(i);
                // Retrieve the slot information and extract the day and time for the course.
                List<String> slotInfo = courseAssignment.get(i);
                // We are interested in day and time only.
                // eg.. TU, 9:30
                List<String> slotTime = slotInfo.subList(0, 2);
                // Update the course schedule map with the slot time and increment the count.
                courseSchedule.computeIfAbsent(courseName.subList(0, 2), k -> new HashMap<>())
                        .merge(slotTime, 1, Integer::sum);
            }
            // Iterate through the course schedule map to calculate penalties.
            for (Map<List<String>, Integer> slots : courseSchedule.values()) {
                // For each time slot, if more than one section is scheduled, add a penalty.
                for (int count : slots.values()) {
                    // Add penalty for each extra section in the same time slot (excluding the first
                    // one).
                    if (count > 1) {
                        if (this.printData) {
                            System.out.println("Penalty for section " + this.penSection);
                        }
                        // Add the penalty for each pair of classes/labs that are not assigned to
                        // different time slots.
                        // eg.. 2 sections in the same time slot -> 1 penalty
                        // eg.. 3 sections in the same time slot -> 2 penalties
                        // eg.. 4 sections in the same time slot -> 3 penalties
                        penalty += (count - 1) * this.penSection;
                    }
                }
            }
            // Return the total penalty incurred for section differences.
            return penalty;
        } catch (Exception e) {
            System.out.println("Error occured at method evalSecDiff in SoftConstraint.java");
            System.out.println(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Evaluates a given schedule.
     *
     * @param pr schedule
     * @return Eval value
     */
    public int eval(List<List<String>> pr) {
        // Return max value if the schedule is null.
        if (pr == null) {
            return Integer.MAX_VALUE;
        }
        try {
            // Create sublists for courses and labs from the schedule list 'pr'
            List<List<String>> courses = new ArrayList<>(pr.subList(0, Scheduler.getCourses().size()));
            List<List<String>> labs = new ArrayList<>(pr.subList(Scheduler.getCourses().size(), pr.size()));
            // Calculate the weighted evaluation values for each soft constraint
            int minVal = this.wMinFilled * evalMinFilled(courses, labs);
            int prefVal = this.wPref * evalPref(courses, labs);
            int pairVal = this.wPair * evalPair(courses, labs);
            int secDiffVal = this.wSecDiff * evalSecDiff(courses);
            // Sum and return the weighted evaluation values
            return minVal + prefVal + pairVal + secDiffVal;
        } catch (Exception e) {
            System.out.println("Error occured at method eval in SoftConstraint.java");
            System.out.println(e.getMessage());
            return Integer.MAX_VALUE;
        }
    }
}

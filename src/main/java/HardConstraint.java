import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Evaluates the hard constraints for a given schedule.
 */
public class HardConstraint {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    private Classes[] classesArr;
    private Slots[] slotsArr;

    private final List<List<String>> courses;
    private final List<List<String>> labs;
    private final List<List<String>> classes;
    private final List<List<String>> courseSlots;
    private final List<List<String>> labSlots;
    private final List<List<String>> slots;

    private final int numCourses;
    private final int numCourseSlots;
    private final int numLabs;
    private final int numClasses;
    private final int numSlots;

    // Set to true to print the data for debugging
    private final boolean printData = false;

    /**
     * Constructor for hard constraints
     */
    public HardConstraint() {
        // Fetch data once and store it
        this.courses = Scheduler.getCourses();
        this.labs = Scheduler.getLabs();
        this.courseSlots = Scheduler.getCourseSlots();
        this.labSlots = Scheduler.getLabSlots();
        this.classes = Scheduler.getClasses();
        this.slots = Scheduler.getSlots();

        this.numCourses = this.courses.size();
        this.numLabs = this.labs.size();
        this.numCourseSlots = this.courseSlots.size();

        this.numClasses = this.classes.size();
        this.numSlots = this.slots.size();

        populateClassesAndSlots();
    }

    /**
     * Populates the classes and slots arrays with the courses and labs.
     */
    private void populateClassesAndSlots() {
        try {
            this.classesArr = new Classes[this.numClasses];
            this.slotsArr = new Slots[this.numSlots];
            // Populate classes with courses and labs
            for (int i = 0; i < this.numClasses; i++) {
                if (i < this.numCourses) {
                    this.classesArr[i] = new Classes("course", i);
                } else {
                    this.classesArr[i] = new Classes("lab", i - this.numCourses);
                }
            }
            // Populate slots with course slots and lab slots
            for (int i = 0; i < this.numSlots; i++) {
                if (i < this.numCourseSlots) {
                    this.slotsArr[i] = new Slots("course", i);
                } else {
                    this.slotsArr[i] = new Slots("lab", i - this.numCourseSlots);
                }
            }
        } catch (Exception e) {
            System.out.println("populateClassesAndSlots() failed");
            System.out.println(e);
        }
    }

    /**
     * Determines if the class is a lab or tutorial based on class indentifier
     *
     * @param classIdentifier class identifier
     * @return true if the class is a lab or tutorial, false otherwise.
     */
    private boolean isLabOrTutorial(List<String> classIdentifier) {
        return classIdentifier.contains("TUT") || classIdentifier.contains("LAB");
    }

    /**
     * Checks if the lecture and laboratory slots do not overlap.
     *
     * @param lecTimeSlot The lecture's time slot.
     * @param tutTimeSlot The laboratory's time slot.
     * @return true if there is no time overlap, false otherwise.
     */
    private boolean checkLecLab(List<String> lecTimeSlot, List<String> tutTimeSlot) {
        // Duration of each slot in minutes
        final int durationMonWedFriLecLab = 60;
        final int durationFriLab = 120;
        final int durationTuesLec = 90;
        final int durationTuesLab = 60;
        // Get the day and time of the lecture and lab
        String lecDay = lecTimeSlot.get(0);
        String tutDay = tutTimeSlot.get(0);
        String lecTime = lecTimeSlot.get(1);
        String tutTime = tutTimeSlot.get(1);
        // Check if the lecture and lab are on the same day
        if (lecDay.equals("MO") && tutDay.equals("MO")) {
            if (lecTime.equals(tutTime)) {
                return false;
            }
        } else if (lecDay.equals("TU") && tutDay.equals("TU")) {
            // Check if the lecture and lab are on the same day
            // Check if the lecture and lab are on the same time
            // Check if the lecture and lab timings overlap
            LocalTime lecStartTime = LocalTime.parse(lecTime, HardConstraint.TIME_FORMATTER);
            LocalTime lecEndTime = lecStartTime.plusMinutes(durationTuesLec);
            LocalTime tutStartTime = LocalTime.parse(tutTime, HardConstraint.TIME_FORMATTER);
            LocalTime tutEndTime = tutStartTime.plusMinutes(durationTuesLab);
            if (lecStartTime.isAfter(tutStartTime) && lecStartTime.isBefore(tutEndTime)) {
                return false;
            } else if (tutStartTime.isAfter(lecStartTime) && tutStartTime.isBefore(lecEndTime)) {
                return false;
            }
        } else if (lecDay.equals("MO") && tutDay.equals("FR")) {
            // Check if the lecture and lab are on the same time
            // Check if the lecture and lab timings overlap
            LocalTime lecStartTime = LocalTime.parse(lecTime, HardConstraint.TIME_FORMATTER);
            LocalTime lecEndTime = lecStartTime.plusMinutes(durationMonWedFriLecLab);
            LocalTime tutStartTime = LocalTime.parse(tutTime, HardConstraint.TIME_FORMATTER);
            LocalTime tutEndTime = tutStartTime.plusMinutes(durationFriLab);
            if (lecStartTime.isAfter(tutStartTime) && lecStartTime.isBefore(tutEndTime)) {
                return false;
            } else if (tutStartTime.isAfter(lecStartTime) && tutStartTime.isBefore(lecEndTime)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Not more than coursemax(s) courses can be assigned to slot s.
     * Not more than labmax(s) labs can be assigned to slot s.
     *
     * @return true if slots have not exceeded, false otherwise
     */
    private boolean checkMax(List<List<String>> coursesList, List<List<String>> labsList) {
        try {
            // Combine course and lab slots into one array to track the assignment count.
            int[] slotAssignmentCount = new int[this.numSlots];
            // Increment the count for each assigned slot.
            for (int i = 0; i < coursesList.size(); i++) {
                List<String> assignedSlot = coursesList.get(i);
                // Skip unassigned slots
                if (!assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    int assignedSlotIndex = this.courseSlots.indexOf(assignedSlot);
                    slotAssignmentCount[assignedSlotIndex]++;
                }
            }
            // Increment the count for each assigned slot.
            for (int i = 0; i < labsList.size(); i++) {
                List<String> assignedSlot = labsList.get(i);
                // Skip unassigned slots
                if (!assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    int assignedSlotIndex = this.labSlots.indexOf(assignedSlot);
                    slotAssignmentCount[assignedSlotIndex + this.numCourseSlots]++;
                }
            }

            // Check if any slot has exceeded its maximum.
            for (int i = 0; i < slotAssignmentCount.length; i++) {
                if (i < this.slotsArr.length && this.slotsArr[i].getMax() < slotAssignmentCount[i]) {
                    // Slot maximum is exceeded.
                    if (this.printData) {
                        System.out.println(
                            "checkMax() failed for slot " + this.slotsArr[i].getName() + " with max "
                                + this.slotsArr[i].getMax() + " and assignment count " + slotAssignmentCount[i]);
                    }
                    return false;
                }
            }
            // All slots are within the maximum limits.
            return true;
        } catch (Exception e) {
            System.out.println("checkMax() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * assign(ci) has to be unequal to assign(lik) for all k and i.
     *
     * @return true if slots have no conflicts, false otherwise
     */
    private boolean checkCourseLabAssignmentUnequal(List<List<String>> courseList, List<List<String>> labList) {
        try {
            for (int i = 0; i < this.numCourses; i++) {
                // Skip unassigned courses
                if (courseList.get(i).equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Check if the course is assigned to a lab slot
                ArrayList<List<String>> listLabs = this.classesArr[i].getLabList();
                // Check if assignment is unequal to all labs
                for (int j = 0; j < listLabs.size(); j++) {
                    // Skip unassigned labs
                    int labIndex = this.labs.indexOf(listLabs.get(j));
                    List<String> labSlot = labList.get(labIndex);
                    if (labSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                        continue;
                    }
                    // Check if the course and lab slots are the same
                    if (!checkLecLab(courseList.get(i), labSlot)) {
                        if (this.printData) {
                            System.out.println("checkCourseLabAssignmentUnequal() failed for course "
                                + this.courses.get(i) + " at slot " + courseList.get(i) + " and lab "
                                + this.labs.get(labIndex) + " at slot " + labSlot);
                        }
                        return false;
                    }
                }
            }
            // All labs under a course are not the same as the course slot
            return true;
        } catch (Exception e) {
            System.out.println("checkCourseLabAssignmentUnequal() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * The input for your system will contain a list of not-compatible(a,b)
     * statements,
     * with a,b in Courses + Labs. For each of those, assign(a) has to be unequal to
     * assign(b).
     *
     * @return true if slots compatible, false otherwise
     */
    private boolean checkNotCompatible(List<List<String>> courseList, List<List<String>> labList) {
        try {
            // Iterate through not compatible pairs and check assignments.
            // e.g. [[CPSC, 433, LEC, 01, TUT, 01], [CPSC, 433, LEC, 02, LAB, 02]]
            for (int i = 0; i < Scheduler.getNotCompatible().size(); i++) {
                // Get the not compatible pair
                ArrayList<List<String>> notCompatiblePair = Scheduler.getNotCompatible().get(i);
                // Get the class identifier for the left class
                List<String> leftClassIdentifier = notCompatiblePair.get(0);
                // Get the index of the left class
                int leftIndex = isLabOrTutorial(leftClassIdentifier)
                    ? this.labs.indexOf(leftClassIdentifier)
                    : this.courses.indexOf(leftClassIdentifier);
                // Skip if the left class is not in the schedule
                if (leftIndex == -1) {
                    continue;
                }
                List<String> leftAssignedSlot = isLabOrTutorial(leftClassIdentifier)
                    ? labList.get(leftIndex)
                    : courseList.get(leftIndex);
                // Skip unassigned slots
                if (leftAssignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Get the class identifier for the right class
                List<String> rightClassIdentifier = notCompatiblePair.get(1);
                int rightIndex = isLabOrTutorial(rightClassIdentifier)
                    ? this.labs.indexOf(rightClassIdentifier)
                    : this.courses.indexOf(rightClassIdentifier);
                // Skip if the right class is not in the schedule
                if (rightIndex == -1) {
                    continue;
                }
                // Retrieve the assigned slot for the right class
                List<String> rightAssignedSlot = isLabOrTutorial(rightClassIdentifier)
                    ? labList.get(rightIndex)
                    : courseList.get(rightIndex);
                // Skip unassigned slots
                if (rightAssignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Check if the left and right slots are the same
                if (!isLabOrTutorial(leftClassIdentifier) && !isLabOrTutorial(rightClassIdentifier)) {
                    // Both are lectures
                    // Check if the left and right slots are the same
                    boolean result = leftAssignedSlot.get(0).equals(rightAssignedSlot.get(0))
                        && leftAssignedSlot.get(1).equals(rightAssignedSlot.get(1));
                    if (result) {
                        if (this.printData) {
                            System.out.println("checkNotCompatible() failed for class " + leftClassIdentifier
                                + " at slot " + leftAssignedSlot + " and class " + rightClassIdentifier + " at slot "
                                + rightAssignedSlot);
                        }
                        return false;
                    }
                } else if (isLabOrTutorial(leftClassIdentifier) && isLabOrTutorial(rightClassIdentifier)) {
                    // Both are labs
                    // Check if the left and right slots are the same
                    boolean result = leftAssignedSlot.get(0).equals(rightAssignedSlot.get(0))
                        && leftAssignedSlot.get(1).equals(rightAssignedSlot.get(1));
                    if (result) {
                        if (this.printData) {
                            System.out.println("checkNotCompatible() failed for class " + leftClassIdentifier
                                + " at slot " + leftAssignedSlot + " and class " + rightClassIdentifier + " at slot "
                                + rightAssignedSlot);
                        }
                        return false;
                    }
                } else {
                    if (isLabOrTutorial(leftClassIdentifier)) {
                        // left is a lab and right is a lecture
                        // Check if the lecture and lab slots are the same
                        // Check if the lecture and lab timings overlap
                        // Check if the lecture and lab are on the same day
                        // Check if the lecture and lab are on the same time
                        boolean result = checkLecLab(rightAssignedSlot, leftAssignedSlot);
                        if (!result) {
                            if (this.printData) {
                                System.out.println("checkNotCompatible() failed for class " + leftClassIdentifier
                                    + " at slot " + leftAssignedSlot + " and class "
                                    + rightClassIdentifier + " at slot "
                                    + rightAssignedSlot);
                            }
                            return false;
                        }
                    } else {
                        // left is a lecture and right is a lab
                        // Check if the lecture and lab slots are the same
                        // Check if the lecture and lab timings overlap
                        // Check if the lecture and lab are on the same day
                        // Check if the lecture and lab are on the same time
                        boolean result = checkLecLab(leftAssignedSlot, rightAssignedSlot);
                        if (!result) {
                            if (this.printData) {
                                System.out.println("checkNotCompatible() failed for class "
                                    + leftClassIdentifier
                                    + " at slot " + leftAssignedSlot + " and class "
                                    + rightClassIdentifier + " at slot "
                                    + rightAssignedSlot);
                            }
                            return false;
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("checkNotCompatible() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * The input for your system can contain a partial assignment
     * partassign: Courses + Labs -> Slots + {$}. The assignment assign your system
     * produces has to fulfill the condition:
     * assign(a) = partassign(a) for all a in Courses + Labs with partassign(a) not
     * equal to $.
     *
     * @return true if partial assignment is included in schedule, false otherwise
     */
    private boolean checkPartialAssignment(List<List<String>> courseList, List<List<String>> labList) {
        try {
            // Map to hold the partial assignments for efficient lookup
            Map<List<String>, String[]> partialAssignmentsMap = new HashMap<>();
            // Populate the map with the partial assignments
            // e.g. [[SENG, 311, LEC, 01], [MO], [8:00]]
            List<ArrayList<List<String>>> partialAssignments = Scheduler.getPartialAssignments();
            for (List<List<String>> assignment : partialAssignments) {
                // Convert day and time to a String array for easy comparison later
                String[] dayTime = { assignment.get(1).get(0), assignment.get(2).get(0) };
                partialAssignmentsMap.put(assignment.get(0), dayTime);
            }
            // Check if the partial assignments are included in the schedule
            for (int i = 0; i < courseList.size(); i++) {
                List<String> assignedSlot = courseList.get(i);
                // Skip unassigned slots
                if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Get the class identifier for the current class
                List<String> classIdentifier = this.courses.get(i);
                // If there's a partial assignment for the current class
                if (partialAssignmentsMap.containsKey(classIdentifier)) {
                    String[] partialDayTime = partialAssignmentsMap.get(classIdentifier);
                    String assignedDay = assignedSlot.get(0);
                    String assignedTime = assignedSlot.get(1);
                    // Check if the assigned day and time match the partial assignment
                    if (!partialDayTime[0].equals(assignedDay) || !partialDayTime[1].equals(assignedTime)) {
                        if (this.printData) {
                            System.out.println(
                                "checkPartialAssignment() failed for class " + classIdentifier + " at slot "
                                    + assignedSlot + " with partial assignment " + partialDayTime[0] + " "
                                    + partialDayTime[1]);
                        }
                        return false; // The current assignment does not match the partial assignment
                    }
                }
            }
            // Check if the partial assignments are included in the schedule
            for (int i = 0; i < labList.size(); i++) {
                List<String> assignedSlot = labList.get(i);
                // Skip unassigned slots
                if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Get the class identifier for the current class
                List<String> classIdentifier = this.labs.get(i);
                // If there's a partial assignment for the current class
                if (partialAssignmentsMap.containsKey(classIdentifier)) {
                    String[] partialDayTime = partialAssignmentsMap.get(classIdentifier);
                    String assignedDay = assignedSlot.get(0);
                    String assignedTime = assignedSlot.get(1);
                    // Check if the assigned day and time match the partial assignment
                    if (!partialDayTime[0].equals(assignedDay) || !partialDayTime[1].equals(assignedTime)) {
                        if (this.printData) {
                            System.out.println(
                                "checkPartialAssignment() failed for class " + classIdentifier + " at slot "
                                    + assignedSlot + " with partial assignment " + partialDayTime[0] + " "
                                    + partialDayTime[1]);
                        }
                        return false; // The current assignment does not match the partial assignment
                    }
                }
            }
            // All partial assignments are included in the schedule
            return true;
        } catch (Exception e) {
            System.out.println("checkPartialAssignment() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * The input for your system can contain a list of unwanted(a,s) statements,
     * with a in Courses + Labs and s in Slots.
     * For each of those, assign(a) has to be unequal to s.
     *
     * @return true if no class is assigned to an unwanted slot, false otherwise.
     */
    private boolean checkUnwanted(List<List<String>> courseList, List<List<String>> labList) {
        try {
            for (int i = 0; i < courseList.size(); i++) {
                List<String> assignedSlot = courseList.get(i);
                // Skip unassigned slots
                if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Retrieve the list of unwanted slots for the current class.
                // e.g. [[CPSC, 433, LEC, 01], [MO], [8:00]]
                List<ArrayList<List<String>>> unwantedSlots = this.classesArr[i].getUnwanted();
                // Check if the assigned slot matches any unwanted slots.
                for (List<List<String>> unwanted : unwantedSlots) {
                    // Check if the time and day match
                    // e.g. [CPSC, 433, LEC, 01] is unwanted at [MO] [8:00]
                    if (unwanted.get(1).get(0).equals(assignedSlot.get(0))
                            && unwanted.get(2).get(0).equals(assignedSlot.get(1))) {
                        if (this.printData) {
                            System.out.println("checkUnwanted() failed for class " + this.classesArr[i].getName()
                                + " at slot " + assignedSlot + " with unwanted slot " + unwanted);
                        }
                        return false; // The class is assigned to an unwanted slot.
                    }
                }
            }
            // Check if the lab is assigned to an unwanted slot.
            for (int i = 0; i < labList.size(); i++) {
                List<String> assignedSlot = labList.get(i);
                // Skip unassigned slots
                if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Retrieve the list of unwanted slots for the current class.
                // e.g. [[CPSC, 433, LEC, 01], [MO], [8:00]]
                List<ArrayList<List<String>>> unwantedSlots = this.classesArr[i + this.numCourses].getUnwanted();
                // Check if the assigned slot matches any unwanted slots.
                for (List<List<String>> unwanted : unwantedSlots) {
                    // Check if the time and day match
                    // e.g. [CPSC, 433, LEC, 01] is unwanted at [MO] [8:00]
                    if (unwanted.get(1).get(0).equals(assignedSlot.get(0))
                            && unwanted.get(2).get(0).equals(assignedSlot.get(1))) {
                        if (this.printData) {
                            System.out.println("checkUnwanted() failed for class "
                                + this.classesArr[i + this.numCourses].getName()
                                + " at slot " + assignedSlot + " with unwanted slot " + unwanted);
                        }
                        return false; // The class is assigned to an unwanted slot.
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("checkUnwanted() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * All course sections with a section number starting LEC 9
     * are evening classes and have to be scheduled into evening slots.
     *
     * @return true if all evening courses are in evening slots, false otherwise.
     */
    private boolean checkEveningCourses(List<List<String>> courseList, List<List<String>> labList) {
        try {
            // Check if the course is an evening course.
            for (int i = 0; i < this.numCourses; i++) {
                // Skip unassigned courses
                if (courseList.get(i).equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Check if the course is an evening course.
                if (this.classesArr[i].isEvening()) {
                    // Check if the slot is an evening slot.
                    if (this.slotsArr[this.courseSlots.indexOf(courseList.get(i))].isEvening() == false) {
                        if (this.printData) {
                            System.out.println("checkEveningCourses() failed for course " + this.courses.get(i)
                                + " at slot " + courseList.get(i));
                        }
                        return false;
                    }
                }
            }
            // Check if the lab is an evening course. TODO: check if it is needed.
            for (int i = 0; i < this.numLabs; i++) {
                // Skip unassigned labs
                if (labList.get(i).equals(Scheduler.UNASSIGNED_SLOT)) {
                    continue;
                }
                // Check if the course is an evening course.
                if (this.classesArr[i + this.numCourses].isEvening()) {
                    // Check if the slot is an evening slot.
                    if (this.slotsArr[this.labSlots.indexOf(labList.get(i))
                        + this.numCourseSlots].isEvening() == false) {
                        if (this.printData) {
                            System.out.println("checkEveningCourses() failed for lab " + this.labs.get(i)
                                + " at slot " + labList.get(i));
                        }
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("checkEveningCourses() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * All courses (course sections) on the 500-level have to be scheduled into
     * different time slots.
     *
     * @return true if all 500-level courses are in different time slots, false if
     *         any overlap.
     */
    private boolean checkLevel500Courses(List<List<String>> courseList, List<List<String>> labList) {
        try {
            Set<List<String>> uniqueAssignedSlotsCourses = new HashSet<>();
            Set<List<String>> uniqueAssignedSlotsLabs = new HashSet<>();
            // Check if the course is a 500-level course.
            for (int i = 0; i < this.numCourses; i++) {
                // Check if the course is a 500-level course.
                if (classesArr[i].getName().get(1).startsWith("5")) {
                    List<String> assignedSlot = courseList.get(i);
                    // If the slot is unassigned, we skip it.
                    if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                        continue;
                    }
                    // If the slot is already in the set, there's an overlap.
                    if (!uniqueAssignedSlotsCourses.add(assignedSlot)) {
                        if (this.printData) {
                            System.out.println("checkLevel500Courses() failed for course " + this.courses.get(i)
                                + " at slot " + courseList.get(i));
                        }
                        return false;
                    }
                }
            }
            // Check if the lab is a 500-level course.
            for (int i = 0; i < this.numLabs; i++) {
                // Check if the course is a 500-level course.
                if (classesArr[i + this.numCourses].getName().get(1).startsWith("5")) {
                    List<String> assignedSlot = labList.get(i);
                    // If the slot is unassigned, we skip it.
                    if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                        continue;
                    }
                    // If the slot is already in the set, there's an overlap.
                    if (!uniqueAssignedSlotsLabs.add(assignedSlot)) {
                        if (this.printData) {
                            System.out.println("checkLevel500Courses() failed for lab " + this.labs.get(i)
                                + " at slot " + labList.get(i));
                        }
                        return false;
                    }
                }
            }
            // If we get here, all 500-level courses have unique slots.
            return true;
        } catch (Exception e) {
            System.out.println("checkLevel500Courses() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * All classes have to be scheduled.
     * @return true if all classes are scheduled, false otherwise
     */
    private boolean checkClassFullyAssigned(List<List<String>> courseList, List<List<String>> labList) {
        try {
            // Check if all courses are assigned
            for (int i = 0; i < this.numCourses; i++) {
                if (courseList.get(i).equals(Scheduler.UNASSIGNED_SLOT)) {
                    if (this.printData) {
                        System.out.println("checkClassFullyAssigned() failed for course " + this.courses.get(i));
                    }
                    return false;
                }
            }
            // Check if all labs are assigned
            for (int i = 0; i < this.numLabs; i++) {
                if (labList.get(i).equals(Scheduler.UNASSIGNED_SLOT)) {
                    if (this.printData) {
                        System.out.println("checkClassFullyAssigned() failed for lab " + this.labs.get(i));
                    }
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("checkClassFullyAssigned() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * Returns the indices of the classes sorted by the number of constraints.
     * Prioritize those with the most constraints
     * e.g. number of unwanted slots, number of not compatible classes, number of labs,
     * evening classes
     * @return the indices of the classes sorted by the number of constraints
     */
    public int[] getMostTightlyBoundIndices() {
        // Sort the classes by the number of constraints
        // e.g. number of unwanted slots, number of not compatible classes, number of labs,
        // evening classes
        // Return the indices of the classes sorted by the number of constraints
        // e.g. [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
        // We want to focus on the classes with the most constraints.
        return IntStream.range(0, this.classesArr.length)
                .boxed() // Box the int so we can collect to list
                .sorted((i1, i2) -> Integer.compare(this.classesArr[i2].priorityScore(),
                    this.classesArr[i1].priorityScore()))
                .mapToInt(i -> i) // Unbox to int to create int[]
                .toArray();
    }

    /**
     * Checks hard constraint for fully assigned schedule
     * @param pr the current assignment
     * @return true if the assignment is valid, false otherwise
     */
    public boolean constr(List<List<String>> pr) {
        if (pr == null) {
            return false;
        }
        try {
            // Create sublists for courses and labs from the schedule list 'pr'
            List<List<String>> courses = new ArrayList<>(pr.subList(0, this.numCourses));
            List<List<String>> labs = new ArrayList<>(pr.subList(this.numCourses, pr.size()));
            // Check if the schedule is valid
            return checkMax(courses, labs)
                && checkCourseLabAssignmentUnequal(courses, labs)
                && checkNotCompatible(courses, labs)
                && checkPartialAssignment(courses, labs)
                && checkUnwanted(courses, labs)
                && checkEveningCourses(courses, labs)
                && checkLevel500Courses(courses, labs)
                && checkClassFullyAssigned(courses, labs);
        } catch (Exception e) {
            System.out.println("constr() failed");
            System.out.println(e);
            return false;
        }
    }

    /**
     * Checks hard constraint for the current partial assignment
     * @param pr the current partial assignment
     * @return true if the assignment is valid, false otherwise
     */
    public boolean constrStar(List<List<String>> pr) {
        if (pr == null) {
            return false;
        }
        try {
            // Create sublists for courses and labs from the schedule list 'pr'
            List<List<String>> courses = new ArrayList<>(pr.subList(0, this.numCourses));
            List<List<String>> labs = new ArrayList<>(pr.subList(this.numCourses, pr.size()));
            // Check if the partial schedule is valid
            return checkMax(courses, labs)
                && checkCourseLabAssignmentUnequal(courses, labs)
                && checkNotCompatible(courses, labs)
                && checkPartialAssignment(courses, labs)
                && checkUnwanted(courses, labs)
                && checkEveningCourses(courses, labs)
                && checkLevel500Courses(courses, labs);
        } catch (Exception e) {
            System.out.println("constrStar() failed");
            System.out.println(e);
            return false;
        }
    }
}

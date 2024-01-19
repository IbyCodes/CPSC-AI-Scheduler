import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Scheduler
 */
public class Scheduler {
    public static final List<String> UNASSIGNED_SLOT = Arrays.asList("$");
    public static final long START_TIME = System.currentTimeMillis();
    private static final long MAX_DURATION = 60000 * 6 * 24; // 24 hours

    private static int initialPop = 10;
    private static int maxPop = 10;
    private static int numRemove = 1;
    private static int maxGenerations = 10;
    private static int stableThreshold = 1;
    private static int maxStableGenerations = 500;

    // Global data structures to be filled by the parser:
    private static ArrayList<List<String>> courses;
    private static ArrayList<List<String>> labs;
    private static ArrayList<List<String>> labSlots;
    private static ArrayList<List<String>> courseSlots;
    private static ArrayList<ArrayList<List<String>>> notCompatible;
    private static ArrayList<ArrayList<List<String>>> unwanted;
    private static ArrayList<ArrayList<List<String>>> preferences;
    private static ArrayList<ArrayList<List<String>>> pairs;
    private static ArrayList<ArrayList<List<String>>> partialAssignments;

    private static ArrayList<List<String>> slots;
    private static ArrayList<List<String>> classes;

    private static ArrayList<List<String>> initialPR;
    private static List<OrTree> orTrees;

    private static Parser parser;
    private static Generation solutionGenerator;
    private static HardConstraint constr;
    private static SoftConstraint eval;

    private static boolean printData = true;
    private static boolean printPR = true;

    /**
     * Tracks the time elapsed since the start of the program.
     * If the time limit is exceeded, the program exits.
     * This is to prevent the program from running indefinitely.
     */
    private static void trackTime() {
        if (System.currentTimeMillis() - Scheduler.START_TIME > Scheduler.MAX_DURATION) {
            System.out.println("Time limit exceeded. Exiting...");
            // Print the final solution
            List<List<String>> solution = Scheduler.returnOptimalSolution();
            if (solution == null) {
                System.out.println("No solution found.");
            } else {
                System.out.println("Run Time: " + (System.currentTimeMillis() - Scheduler.START_TIME) + "ms\n");
                System.out.println("Final Solution:\n" + solution + "\n");
                Scheduler.printFinalAssignment(solution);
            }
            System.exit(0);
        }
    }

    private static void writeOutputToFile(String output) {
        try (PrintWriter out = new PrintWriter(parser.getName() + "output.txt")) {
            out.println(output);
        } catch (FileNotFoundException e) {
            System.out.println("Error: Unable to write to the file.");
            e.printStackTrace();
        }
    }

    /**
     * Prints the schedule for a given solution.
     * e.g.
     * CPSC 313 LEC 01: MO, 8:00
     * CPSC 313 TUT 01: TU, 18:00
     * CPSC 413 LEC 01: MO, 8:00
     * @param solution - The solution to be printed.
     */
    private static void printFinalAssignment(List<List<String>> solution) {
        if (Scheduler.printData) {
            System.out.println("Printing final Assignment...");
        }
        try {
            StringBuilder output = new StringBuilder();
            output.append("Eval-value: ").append(Scheduler.eval.eval(solution)).append("\n\n");
            Map<String, String> slotAssignments = new TreeMap<>();
            // Process the solution into a map of slot assignments
            for (int i = 0; i < solution.size(); i++) {
                List<String> classIdentifier = i < Scheduler.courses.size()
                    ? Scheduler.courses.get(i)
                    : Scheduler.labs.get(i - courses.size());
                List<String> assignedSlot = solution.get(i);
                String classKey = String.join(" ", classIdentifier);
                String dayTime = "";
                if (assignedSlot.equals(Scheduler.UNASSIGNED_SLOT)) {
                    dayTime = "Unassigned";
                } else {
                    String time = assignedSlot.get(1);
                    if (time.contains(":")) {
                        time = time.trim();
                        // Split the string into parts before and after the colon
                        String[] parts = time.split(":", 2);
                        // Check if the hour part is a single digit and pad with zero if necessary
                        if (parts[0].length() == 1) {
                            time = "0" + time;
                        }
                    }
                    dayTime = String.format("%s, %s", assignedSlot.get(0), time);
                }
                slotAssignments.put(classKey, dayTime);
            }
            // Sort and print the assignments
            slotAssignments.forEach((key, time) -> {
                output.append(String.format("%-35s: %s\n", key, time));
            });
            Scheduler.writeOutputToFile(output.toString());
            System.out.println(output.toString());
        } catch (Exception e) {
            System.out.println("Error: Unable to print final assignment.");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Initializes the first generation of candidate solutions.
     * If partial assignments are present, they are accounted for in the PR instance.
     * Otherwise, an empty PR instance is used as the starting point.
     */
    private static void buildSolutionsForSetBased() {
        if (Scheduler.printData) {
            System.out.println("Building solutions for set based search...");
        }
        try {
            // Initialize the PR instance with empty slots
            Scheduler.initialPR = new ArrayList<>(Collections.nCopies(Scheduler.classes.size(),
                Scheduler.UNASSIGNED_SLOT));
            // Build PR instance with partial assignments if available
            if (!Scheduler.partialAssignments.isEmpty()) {
                Scheduler.buildPRwPartialAssignments();
            }
            Random rand = new Random();
            Set<List<List<String>>> uniqueSolutions = new HashSet<>();
            for (int i = 0; i < Scheduler.initialPop; i++) {
                if (Scheduler.printData || Scheduler.printPR) {
                    System.out.println("Candidate #" + (i + 1));
                }
                // Initialize the OR tree with the initial PR instance
                OrTree orTree = new OrTree(Scheduler.initialPR);
                // Clone the most tightly bound indices
                ArrayList<Integer> mostTightlyBound = Scheduler.cloneMostTightlyBoundIndices();
                if (mostTightlyBound == null) {
                    System.out.println("Error: Unable to clone most tightly bound indices.");
                    return;
                }
                // Search for a solution for the current schedule.
                List<List<String>> potentialSolution =
                    orTree.searchSolution(mostTightlyBound, 0, new ArrayList<>(), rand);
                if (potentialSolution == null) {
                    if (Scheduler.printData || Scheduler.printPR) {
                        System.out.println("No solution found at loop " + (i + 1));
                    }
                    continue; // Proceed to try the next candidate instead of exiting the entire program
                } else {
                    if (Scheduler.printData || Scheduler.printPR) {
                        System.out.println("Solution found at loop " + (i + 1));
                    }
                }
                // Check if the candidate is unique before adding
                if (uniqueSolutions.add(potentialSolution)) {
                    if (Scheduler.printData) {
                        System.out.println("Potential solution " + (i + 1) + "\tEval score: "
                            + Scheduler.eval.eval(potentialSolution));
                    }
                } else {
                    if (Scheduler.printData || Scheduler.printPR) {
                        System.out.println("Duplicate solution found for candidate "
                            + (i + 1) + " and will not be added.");
                    }
                }
                // trackTime(); no longer required
            }
            if (Scheduler.printData) {
                System.out.println();
            }
            // Check if there are any unique solutions
            // If there are no unique solutions, the program will exit.
            if (uniqueSolutions.isEmpty()) {
                return;
            }
            // Add the unique solutions to the solution generator
            // This is to avoid adding duplicates to the solution generator
            // Solution generator is used for the set-based search.
            List<List<List<String>>> deduplicatedSolutionGenerator = new ArrayList<>(uniqueSolutions);
            deduplicatedSolutionGenerator.forEach(Scheduler.solutionGenerator::add);
        } catch (StackOverflowError e) {
            System.out.println("Error: Stack overflow.");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            System.out.println("Please add more memory to the JVM using the -Xss flag.");
        } catch (Exception e) {
            System.out.println("Error: Unable to build solutions for set based search.");
        }
    }

    /**
     * Clones the array of most tightly bound indices into an ArrayList.
     * "Most tightly bound" refers to the courses with the highest number of constraints.
     * This method initializes the ArrayList with the appropriate capacity and fills it
     * with the indices from the array to avoid resizing.
     *
     * @return A new ArrayList containing the indices of the most tightly bound courses.
     */
    public static ArrayList<Integer> cloneMostTightlyBoundIndices() {
        if (Scheduler.printData) {
            System.out.println("Cloning most tightly bound indices...");
        }
        try {
            // Clone the most tightly bound indices
            // "Most tightly bound" refers to the courses with the highest number of constraints
            // This method initializes the ArrayList with the appropriate capacity and fills it
            // with the indices from the array to avoid resizing
            int[] mTB = Scheduler.constr.getMostTightlyBoundIndices();
            ArrayList<Integer> mostTightlyBound = new ArrayList<Integer>(mTB.length);
            for (int idx : mTB) {
                mostTightlyBound.add(idx);
            }
            return mostTightlyBound;
        } catch (Exception e) {
            System.out.println("Error: Unable to clone most tightly bound indices.");
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Sorts the last generation of candidate solutions.
     * @return The most optimal solution.
     */
    private static List<List<String>> returnOptimalSolution() {
        if (Scheduler.printData) {
            System.out.println("Returning optimal solution...");
        }
        try {
            // Get the last generation of candidate solutions
            List<List<List<String>>> lastGenerationList = Scheduler.solutionGenerator.getGeneration();
            // Find the most optimal solution based on the eval function
            return Collections.min(lastGenerationList,
                Comparator.comparingInt(sol -> Scheduler.eval.eval(sol)));
        } catch (Exception e) {
            System.out.println("Error: Unable to return optimal solution.");
            System.out.println(e.getMessage());
            return null;
        }
    }

    /**
     * Checks for the presence of 18:00 slot for TU.
     * Exits if not present.
     */
    private static void checkForTU18() {
        if (Scheduler.printData) {
            System.out.println("Checking for TU 18:00 slot...");
        }
        try {
            // Check if there is a TU 18:00 slot
            boolean exist = Scheduler.labSlots.stream()
                    .anyMatch(slots -> slots.get(0).equals("TU")
                    && slots.get(1).equals("18:00"));
            // Exit if there is no TU 18:00 slot
            if (!exist) {
                System.out.println("CPSC 313/413 is included, but there are no lab slots allocated at TU 18:00.");
                System.out.println("No solution found.");
                System.exit(0);
            }
        } catch (Exception e) {
            System.out.println("Error: Unable to check for TU 18:00 slot.");
        }
    }

    /**
     * Checks if special courses CPSC313 and 413 present in list.
     * Add slots to unwanted data structure to block out time.
     */
    private static void checkFor313And413() {
        if (Scheduler.printData) {
            System.out.println("Checking for CPSC313 and 413...");
        }
        try {
            // Check for TU18 only once
            boolean tu18Checked = false;
            // Create a mapping for courses to their lab numbers
            Map<String, String> courseToLab = Map.of(
                    "313", "813",
                    "413", "913");
            // Iterate through the courses only once
            String[] specialCourses = new String[] { "313", "413" };
            for (String course : specialCourses) {
                // Get the lab course number
                final String labCourse = courseToLab.get(course);
                // Filter the CPSC courses directly into the section list
                List<List<String>> sections = Scheduler.courses.stream()
                        .filter(subCourse -> subCourse.get(0).equals("CPSC") && subCourse.get(1).equals(course))
                        .collect(Collectors.toList());
                List<List<String>> labSections = Scheduler.labs.stream()
                        .filter(subLab -> subLab.get(0).equals("CPSC") && subLab.get(1).equals(course))
                        .collect(Collectors.toList());
                // Only continue if sections are not empty
                // This means that the course CPSC313 / CPSC413 is present in the input file
                if (!sections.isEmpty() || !labSections.isEmpty()) {
                    // Check for TU18 only once
                    if (!tu18Checked) {
                        // Check for TU 18:00 slot
                        Scheduler.checkForTU18();
                        tu18Checked = true;
                    }
                    List<ArrayList<List<String>>> assignments = Scheduler.partialAssignments.stream()
                        .filter(assign -> (
                            assign.get(0).get(0).equals("CPSC")
                                && assign.get(0).get(1).equals(labCourse)))
                        .collect(Collectors.toList());
                    // Check if any partial assignments to other slot than TU 18:00, exit if exists
                    boolean assignedToOtherSlotsThanTU18 = assignments.stream()
                        .anyMatch(assign -> !(assign.get(1).get(0).equals("TU")
                        && assign.get(2).get(0).equals("18:00")));
                    if (assignedToOtherSlotsThanTU18) {
                        System.out.println("CPSC " + course + " is included "
                            + "but CPSC " + labCourse + " is partially assigned to a slot that is not TU 18:00.");
                        System.out.println("No solution found.");
                        System.exit(0);
                    }
                    // Check for 813 and 913 in unwanted slots in TU 18:00 slot
                    // e.g. [[CPSC, 813, TUT, 01], [TU], [18:00]]
                    // e.g. [[CPSC, 913, TUT, 01], [TU], [18:00]]
                    List<ArrayList<List<String>>> unwantedSlots = Scheduler.unwanted.stream()
                        .filter(unwanted -> (
                            unwanted.get(0).get(0).equals("CPSC")
                                && unwanted.get(0).get(1).equals(labCourse)))
                        .collect(Collectors.toList());
                    // Check if unwanted slots to TU 18:00 slot, exit otherwise
                    boolean unwantedTU18 = unwantedSlots.stream()
                            .anyMatch(unwanted -> (unwanted.get(1).get(0).equals("TU")
                            && unwanted.get(2).get(0).equals("18:00")));
                    if (unwantedTU18) {
                        System.out.println("CPSC " + course + " is included "
                            + "but CPSC " + labCourse + " has unwanted slot at TU 18:00.");
                        System.out.println("No solution found.");
                        System.exit(0);
                    }
                    // Prepare partial assignment and labs
                    // e.g. [CPSC, 813, TUT, 01]
                    List<String> newLab = Arrays.asList("CPSC", labCourse, "TUT", "01");
                    Scheduler.labs.add(newLab);
                    // Add partial assignment for CPSC313 / CPSC413
                    // e.g. [[CPSC, 813, TUT, 01], [TU], [18:00]]
                    Scheduler.partialAssignments.add(
                        new ArrayList<>(Arrays.asList(newLab, List.of("TU"), List.of("18:00"))));
                    // Add unwanted slots for each section
                    // e.g. [[CPSC, 313, LEC, 01], [TU], [17:00]]
                    sections.forEach(s -> {
                        addUnwanted(s, "TU", "17:00");
                        addUnwanted(s, "TU", "18:30");
                    });
                    // Add unwanted slots for each lab section
                    // e.g. [[CPSC, 313, LEC, 01, TUT, 01], [TU], [17:00]]
                    labSections.forEach(s -> {
                        addUnwanted(s, "TU", "18:00");
                    });
                }
            }
        } catch (Exception e) {
            System.out.println("Error: Unable to check for CPSC313 and 413.");
        }
    }

    /**
     * Adds unwanted slots to the unwanted data structure.
     * @param section - The section to be added.
     * @param day - The day to be added.
     * @param time - The time to be added.
     */
    private static void addUnwanted(List<String> section, String day, String time) {
        if (Scheduler.printData) {
            System.out.println("Adding unwanted slot...");
        }
        try {
            // Add unwanted slots for each section
            // e.g. [[CPSC, 313, LEC, 01], [TU], [17:00]]
            Scheduler.unwanted.add(new ArrayList<>(Arrays.asList(section, List.of(day), List.of(time))));
        } catch (Exception e) {
            System.out.println("Error: Unable to add unwanted slot.");
        }
    }

    /**
     * Checks for presence of 11:00 TU slot for courses.
     * Remove if present as no lectures should be scheduled.
     */
    public static void checkForTU11() {
        if (Scheduler.printData) {
            System.out.println("Checking for TU 11:00 slot...");
        }
        try {
            // Remove TU 11:00 slot
            // No lectures should be scheduled
            // e.g. [TU, 11:00, 2, 1]
            // Remove the first occurrence of the slot, there should only be one.
            Iterator<List<String>> iterator = Scheduler.courseSlots.iterator();
            while (iterator.hasNext()) {
                List<String> slot = iterator.next();
                if (slot.get(0).equals("TU") && slot.get(1).equals("11:00")) {
                    iterator.remove();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error: Unable to remove TU 11:00 slot.");
            System.out.println(e.getMessage());
        }
    }

    /**
     * Builds the PR instance based on the partial assignments.
     */
    private static void buildPRwPartialAssignments() {
        if (Scheduler.printData) {
            System.out.println("Building PR with partial assignments...");
        }
        try {
            // Check if it is a lab
            boolean isLab = false;
            // [MO, 8:00, 4, 2], [TU, 10:00, 2, 1], [FR, 10:00, 2, 1]
            Map<List<String>, Integer> courseSlotIndexMap = new HashMap<>();
            Map<List<String>, Integer> labSlotIndexMap = new HashMap<>();
            // Prepare maps for quick lookup, using only day and time as the key
            for (int i = 0; i < Scheduler.courseSlots.size(); i++) {
                List<String> slot = Scheduler.courseSlots.get(i);
                List<String> dayTimeKey = Arrays.asList(slot.get(0), slot.get(1));
                courseSlotIndexMap.put(dayTimeKey, i);
            }
            for (int i = 0; i < Scheduler.labSlots.size(); i++) {
                List<String> slot = Scheduler.labSlots.get(i);
                List<String> dayTimeKey = Arrays.asList(slot.get(0), slot.get(1));
                labSlotIndexMap.put(dayTimeKey, i);
            }
            // Track slots for each classIdentifier
            Map<List<String>, List<String>> assignedSlotsMap = new HashMap<>();
            /*
            * Partial Assignments:
            * [[[SENG, 311, LEC, 01], [MO], [8:00]], [[SENG, 311, LEC, 01, TUT, 01], [FR], [10:00]]]
            */
            // Iterate through the partial assignments
            List<ArrayList<List<String>>> partialAssignments = Scheduler.partialAssignments;
            for (ArrayList<List<String>> assign : partialAssignments) {
                // Get the class identifier and day/time
                List<String> classIdentifier = assign.get(0);
                List<String> dayTime = Arrays.asList(assign.get(1).get(0), assign.get(2).get(0));
                // Check if this classIdentifier has already been assigned a different slot
                if (assignedSlotsMap.containsKey(classIdentifier)) {
                    List<String> previouslyAssignedSlot = assignedSlotsMap.get(classIdentifier);
                    if (!previouslyAssignedSlot.equals(dayTime)) {
                        System.out.println("Error: Multiple different slots partially-assigned to " + classIdentifier);
                        System.exit(0);
                    }
                } else {
                    // Store this slot assignment for the classIdentifier
                    assignedSlotsMap.put(classIdentifier, dayTime);
                }
                // Get the PR index
                int prIndex = Scheduler.courses.indexOf(classIdentifier);
                if (prIndex == -1) {
                    // If the class is not a course, it is a lab
                    isLab = true;
                    prIndex = Scheduler.courses.size() + Scheduler.labs.indexOf(classIdentifier);
                    if (prIndex < Scheduler.courses.size()) {
                        System.out.println("Error: The course/lab " + classIdentifier + " is not recognized.");
                        System.out.println("No valid solution can be formed.");
                        System.exit(0);
                    }
                }
                if (isLab) {
                    // Check if the lab slot exists
                    if (!labSlotIndexMap.containsKey(dayTime)) {
                        System.out.println("Error: The specified lab time slot for "
                            + classIdentifier + " does not exist.");
                        System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
                        System.exit(0);
                    }
                } else {
                    // Check if the course slot exists
                    if (!courseSlotIndexMap.containsKey(dayTime)) {
                        System.out.println("Error: The specified course time slot for "
                            + classIdentifier + " does not exist.");
                        System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
                        System.exit(0);
                    }
                }
                // Get the slot index
                Integer slotIndex = isLab ? labSlotIndexMap.get(dayTime) : courseSlotIndexMap.get(dayTime);
                // Update PR instance with the slot
                Scheduler.initialPR.set(prIndex, isLab ? labSlots.get(slotIndex) : courseSlots.get(slotIndex));
                // Reset isLab
                isLab = false;
            }
        } catch (Exception e) {
            System.out.println("Error: The specified time slot for " + " does not exist.");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            System.exit(0);
        }
    }

    /**
     * Returns the parser instance.
     * @return The parser instance.
     */
    public static Parser getParser() {
        return Scheduler.parser;
    }

    /**
     * Returns the solution generator instance.
     * @return The solution generator instance.
     */
    public static Generation getSolutionGenerator() {
        return Scheduler.solutionGenerator;
    }

    /**
     * Returns the list of courses.
     * @return The list of courses.
     */
    public static ArrayList<List<String>> getCourses() {
        return Scheduler.courses;
    }

    /**
     * Returns the list of labs.
     * @return The list of labs.
     */
    public static ArrayList<List<String>> getLabs() {
        return Scheduler.labs;
    }

    /**
     * Returns the list of lab slots.
     * @return The list of lab slots.
     */
    public static ArrayList<List<String>> getLabSlots() {
        return Scheduler.labSlots;
    }

    /**
     * Returns the list of course slots.
     * @return The list of course slots.
     */
    public static ArrayList<List<String>> getCourseSlots() {
        return Scheduler.courseSlots;
    }

    /**
     * Returns the list of not compatible constraints.
     * @return The list of not compatible constraints.
     */
    public static ArrayList<ArrayList<List<String>>> getNotCompatible() {
        return Scheduler.notCompatible;
    }

    /**
     * Returns the list of unwanted constraints.
     * @return The list of unwanted constraints.
     */
    public static ArrayList<ArrayList<List<String>>> getUnwanted() {
        return Scheduler.unwanted;
    }

    /**
     * Returns the list of preferences constraints.
     * @return The list of preferences constraints.
     */
    public static ArrayList<ArrayList<List<String>>> getPreferences() {
        return Scheduler.preferences;
    }

    /**
     * Returns the list of pairs constraints.
     * @return The list of pairs constraints.
     */
    public static ArrayList<ArrayList<List<String>>> getPairs() {
        return Scheduler.pairs;
    }

    /**
     * Returns the list of partial assignments.
     * @return The list of partial assignments.
     */
    public static ArrayList<ArrayList<List<String>>> getPartialAssignments() {
        return Scheduler.partialAssignments;
    }

    /**
     * Returns the PR instance.
     * @return The PR instance.
     */
    public static List<List<String>> getInitialPR() {
        return Scheduler.initialPR;
    }

    /**
     * Returns the list of OR trees.
     * @return The list of OR trees.
     */
    public static List<OrTree> getOrTrees() {
        return Scheduler.orTrees;
    }

    /**
     * Returns the hard constraint evaluator instance.
     * @return The hard constraint evaluator instance.
     */
    public static HardConstraint getConstr() {
        return Scheduler.constr;
    }

    /**
     * Returns the soft constraint evaluator instance.
     * @return The soft constraint evaluator instance.
     */
    public static SoftConstraint getEval() {
        return Scheduler.eval;
    }

    /**
     * Returns the number of initial population.
     * @return The number of initial population.
     */
    public static int getInitialPop() {
        return Scheduler.initialPop;
    }

    /**
     * Returns the maximum population.
     * @return The maximum population.
     */
    public static int getMaxPop() {
        return Scheduler.maxPop;
    }

    /**
     * Returns the number of solutions to remove.
     * @return The number of solutions to remove.
     */
    public static int getNumRemove() {
        return Scheduler.numRemove;
    }

    /**
     * Returns the maximum number of generations.
     * @return The maximum number of generations.
     */
    public static int getMaxGenerations() {
        return Scheduler.maxGenerations;
    }

    /**
     * Sets the initial population.
     * @param initialPop - The initial population.
     */
    public static void setInitialPop(int initialPop) {
        Scheduler.initialPop = initialPop;
    }

    /**
     * Sets the maximum population.
     * @param maxPop - The maximum population.
     */
    public static void setMaxPop(int maxPop) {
        Scheduler.maxPop = maxPop;
    }

    /**
     * Sets the number of solutions to remove.
     * @param numRemove - The number of solutions to remove.
     */
    public static void setNumRemove(int numRemove) {
        Scheduler.numRemove = numRemove;
    }

    /**
     * Sets the maximum number of generations.
     * @param maxGenerations - The maximum number of generations.
     */
    public static void setMaxGenerations(int maxGenerations) {
        Scheduler.maxGenerations = maxGenerations;
    }

    /**
     * Returns whether to print data.
     * @return Whether to print data.
     */
    public static boolean isPrintData() {
        return Scheduler.printData;
    }

    /**
     * Returns whether to print PR.
     * @return Whether to print PR.
     */
    public static boolean isPrintPR() {
        return Scheduler.printPR;
    }

    /**
     * Sets whether to print data.
     * @param printData - Whether to print data.
     */
    public static void setPrintData(boolean printData) {
        Scheduler.printData = printData;
    }

    /**
     * Sets whether to print PR.
     * @param printPR - Whether to print PR.
     */
    public static void setPrintPR(boolean printPR) {
        Scheduler.printPR = printPR;
    }

    /**
     * Returns the list of slots.
     * @return The list of slots.
     */
    public static ArrayList<List<String>> getSlots() {
        return Scheduler.slots;
    }

    /**
     * Returns the list of classes.
     * @return The list of classes.
     */
    public static ArrayList<List<String>> getClasses() {
        return Scheduler.classes;
    }

    /**
     * Returns the stable threshold.
     * @return The stable threshold.
     */
    public static int getStableThreshold() {
        return Scheduler.stableThreshold;
    }

    /**
     * Sets the stable threshold.
     * @param stableThreshold - The stable threshold.
     */
    public static void setStableThreshold(int stableThreshold) {
        Scheduler.stableThreshold = stableThreshold;
    }

    /**
     * Returns the maximum number of stable generations.
     * @return The maximum number of stable generations.
     */
    public static int getMaxStableGenerations() {
        return Scheduler.maxStableGenerations;
    }

    /**
     * Sets the maximum number of stable generations.
     * @param maxStableGenerations - The maximum number of stable generations.
     */
    public static void setMaxStableGenerations(int maxStableGenerations) {
        Scheduler.maxStableGenerations = maxStableGenerations;
    }

    /**
     * Performs checks for the presence of special courses and slots.
     */
    private static void performChecks() {
        // Check for special courses and slots
        Scheduler.checkFor313And413();
        Scheduler.checkForTU11();
    }

    /**
     * Main method. Runs the scheduler.
     * @param args - Command line arguments.
     */
    public static void main(String[] args) {
        String configFileName;
        String fileName;
        // Scheduling the trackTime() task to run every minute to save thread space
        ScheduledExecutorService timerScheduler = Executors.newScheduledThreadPool(1);
        timerScheduler.scheduleAtFixedRate(Scheduler::trackTime, 0, 1, TimeUnit.MINUTES);
        try {
            System.out.println("Starting scheduler...");
            configFileName = args[0];
            fileName = args[1];

            // Initialize the parser
            Scheduler.parser = new Parser(fileName);
            Scheduler.parser.parse();

            // Initialize global data structures
            Scheduler.courseSlots = Scheduler.parser.getCourseSlots();
            Scheduler.labSlots = Scheduler.parser.getLabSlots();
            Scheduler.courses = Scheduler.parser.getCourses();
            Scheduler.labs = Scheduler.parser.getLabs();
            Scheduler.notCompatible = Scheduler.parser.getNotCompatible();
            Scheduler.unwanted = Scheduler.parser.getUnwanted();
            Scheduler.preferences = Scheduler.parser.getPreferences();
            Scheduler.pairs = Scheduler.parser.getPairs();
            Scheduler.partialAssignments = Scheduler.parser.getPartAssign();

            // Check for special courses and slots
            performChecks();

            Scheduler.slots = new ArrayList<>(courseSlots);
            Scheduler.slots.addAll(labSlots);
            Scheduler.classes = new ArrayList<>(courses);
            Scheduler.classes.addAll(labs);

            // Initialize the soft constraints evaluator
            Scheduler.eval = new SoftConstraint(configFileName);
        } catch (Exception e) {
            System.out.println("Failed at Parser");
            System.out.println(e.getMessage());
            System.out.println("Error occurred. Arguments: java Scheduler [configFile] [inputFile]");
            System.exit(0);
        }
        try {
            // Initialize hard constraints evaluator
            Scheduler.constr = new HardConstraint();
            // Initialize the solution generator
            Scheduler.solutionGenerator = new Generation();
        } catch (Exception e) {
            System.out.println("Failed at Constraints");
            System.out.println(e.getMessage());
            System.exit(0);
        }
        try {
            // Initialize the first generation of candidate solutions:
            // If partial assignments are present, they are accounted for in the PR instance.
            Scheduler.buildSolutionsForSetBased();
            if (Scheduler.solutionGenerator.getGeneration().isEmpty()) {
                System.out.println("No solution found.");
                System.exit(0);
            }
            boolean hasMultipleSolutions = Scheduler.solutionGenerator.getGeneration().size() > 1;
            // Check if there is only one solution
            if (!hasMultipleSolutions) {
                if (Scheduler.printData) {
                    System.out.println("Only one solution found.");
                    System.out.println("No need to run set based search.");
                }
            } else {
                if (Scheduler.printData) {
                    System.out.print("Initial");
                    Scheduler.solutionGenerator.printData(true);
                }
                int bestEval = Integer.MAX_VALUE;
                int stableCounter = 0;
                boolean isStable = false;
                // Run set based search for the specified number of generations:
                for (int i = 0; i < Scheduler.maxGenerations && !isStable; i++) {
                    if (Scheduler.printData || Scheduler.printPR) {
                        System.out.println("Generation #" + (i + 1));
                    }
                    // Run the control function to generate the next generation of candidate solutions:
                    Scheduler.solutionGenerator.control(i + 1);
                    if (Scheduler.printData) {
                        System.out.print("Generation #" + (i + 1));
                        Scheduler.solutionGenerator.printData(false);
                    }
                    try {
                        // Get the current evaluation value
                        int currentEval = Integer.parseInt(Scheduler.solutionGenerator.getMin());
                        // Check if current evaluation is better than the best so far
                        if (currentEval < bestEval) {
                            // If improvement is minor, increment stability counter
                            if (bestEval - currentEval <= Scheduler.stableThreshold) {
                                stableCounter++;
                            } else {
                                stableCounter = 0; // Reset counter if there's a significant improvement
                            }
                            bestEval = currentEval; // Update best eval
                        } else if (currentEval == bestEval) {
                            // Increment if there is no improvement, indicating potential stability
                            stableCounter++;
                        }
                    } catch (Exception e) {
                        System.out.println("Error: Unable to parse current eval value.");
                        System.out.println(e.getMessage());
                    }
                    // Check if the algorithm has stabilized
                    if (stableCounter >= Scheduler.maxStableGenerations) {
                        isStable = true;
                        System.out.println("Eval value stabilized after " + (i + 1) + " generations.");
                        break; // Exit the loop if the eval value is stable
                    }
                }
                // Check if the algorithm has stabilized
                if (!isStable && Scheduler.printData) {
                    System.out.println("Max generations reached without stability.");
                }
            }
            // Print the final solution
            List<List<String>> solution = Scheduler.returnOptimalSolution();
            if (Scheduler.printData) {
                Scheduler.solutionGenerator.print();
            }
            // Check if there is a solution
            // If there is no solution, the program will exit.
            // Otherwise, the final solution will be printed.
            if (solution == null) {
                System.out.println("No solution found.");
            } else {
                System.out.println("Run Time: " + (System.currentTimeMillis() - Scheduler.START_TIME) + "ms\n");
                System.out.println("Final Solution:\n" + solution + "\n");
                Scheduler.printFinalAssignment(solution);
            }
            System.exit(0);
        } catch (Exception e) {
            System.out.println("Failed at Generation");
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }
}

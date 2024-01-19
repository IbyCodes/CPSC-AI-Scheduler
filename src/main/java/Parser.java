import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parser that parses the input file and populates the data
 * structures needed for the scheduler.
 *
 * Sample output from Parser:
 * Course Slots:
 * [[MO, 8:00, 3, 2], [MO, 9:00, 3, 2], [TU, 9:30, 2, 1]]
 *
 * Lab Slots:
 * [[MO, 8:00, 4, 2], [TU, 10:00, 2, 1], [FR, 10:00, 2, 1]]
 *
 * Courses:
 * [[CPSC, 433, LEC, 01], [CPSC, 433, LEC, 02], [SENG, 311, LEC, 01], [CPSC, 567, LEC, 01]]
 *
 * Labs:
 * [[CPSC, 433, LEC, 01, TUT, 01], [CPSC, 433, LEC, 02, LAB, 02], [SENG, 311, LEC, 01, TUT, 01], [CPSC, 567, TUT, 01]]
 *
 * Not compatible:
 * [[[CPSC, 433, LEC, 01, TUT, 01], [CPSC, 433, LEC, 02, LAB, 02]],
 * [[CPSC, 567, LEC, 01], [CPSC, 433, LEC, 01]],
 * [[CPSC, 567, LEC, 01], [CPSC, 433, LEC, 02]],
 * [[CPSC, 567, TUT, 01], [CPSC, 433, LEC, 02]],
 * [[CPSC, 433, LEC, 01], [CPSC, 567, TUT, 01]]]
 *
 * Unwanted:
 * [[[CPSC, 433, LEC, 01], [MO], [8:00]]]
 *
 * Preferences:
 * [[[TU], [9:00], [CPSC, 433, LEC, 01], [10]],
 * [[MO], [8:00], [CPSC, 433, LEC, 01, TUT, 01], [3]],
 * [[TU], [9:30], [CPSC, 433, LEC, 02], [10]],
 * [[TU], [10:00], [CPSC, 433, LEC, 01, LAB, 01], [5]],
 * [[MO], [8:00], [CPSC, 433, LEC, 02, LAB, 02], [1]],
 * [[MO], [10:00], [CPSC, 433, LEC, 02, LAB, 02], [7]]]
 *
 * Pairs:
 * [[[SENG, 311, LEC, 01], [CPSC, 567, LEC, 01]]]
 *
 * Partial Assignments:
 * [[[SENG, 311, LEC, 01], [MO], [8:00]], [[SENG, 311, LEC, 01, TUT, 01], [FR], [10:00]]]
 */
public class Parser {
    private final String fileName;
    private String name;
    private final StringBuilder fileContentBuilder = new StringBuilder();

    private final ArrayList<List<String>> courseSlots = new ArrayList<>();
    private final ArrayList<List<String>> labSlots = new ArrayList<>();

    private final ArrayList<List<String>> courses = new ArrayList<>();
    private final ArrayList<List<String>> labs = new ArrayList<>();

    private final ArrayList<ArrayList<List<String>>> notCompatible = new ArrayList<>();
    private final ArrayList<ArrayList<List<String>>> unwanted = new ArrayList<>();
    private final ArrayList<ArrayList<List<String>>> preferences = new ArrayList<>();
    private final ArrayList<ArrayList<List<String>>> pairs = new ArrayList<>();
    private final ArrayList<ArrayList<List<String>>> partialAssignments = new ArrayList<>();

    // For testing purposes
    private final boolean printData = false;

    /**
     * Constructor takes in the name of the file to be parsed.
     * @param name - Name of the file to be parsed.
     */
    public Parser(String name) {
        this.fileName = name;
    }

    /**
     * Validates the class slots.
     *
     * The available time slots depend on the day of the week and
     * whether we look at lectures or labs/tutorials. Mondays and wednesdays, the slots available
     * for lectures and labs/tutorials are 8:00-9:00, 9:00-10:00, 10:00-11:00, 11:00-12:00, 12:00-13:00,
     * 13:00-14:00, 14:00-15:00, 15:00-16:00, 16:00-17:00. For lectures we additionally have 17:00-18:00,
     * 18:00-19:00, 19:00-20:00 and 20:00-21:00. The same slots are available for lectures also on fridays
     * (but see the hard constraints for connections between slots on these three days of the week).
     * The slots available for labs/tutorials on fridays are 8:00-10:00, 10:00-12:00, 12:00-14:00, 14:00-16:00,
     * 16:00-18:00, 18:00-20:00. To complicate matters a little bit more, mondays and wednesday we have 17:00-18:00,
     * 18:00-19:00, 19:00-20:00 and 20:00-21:00 available for labs/tutorials. The available time slots for tuesdays
     * and thursdays for lectures are 8:00-9:30, 9:30-11:00, 11:00-12:30, 12:30-14:00, 14:00-15:30, 15:30-17:00,
     * 17:00-18:30 and 18:30-20:00. For labs/tutorials, the available time slots are the same on tuesdays and thursdays
     * as on mondays and wednesdays. All slots beginning at 18:00 or later are so-called evening slots.
     */
    private void validateClassSlots() throws IllegalArgumentException {
        // Check if the class slots are valid
        // [[MO, 8:00, 3, 2], [MO, 9:00, 3, 2], [TU, 9:30, 2, 1]]
        for (List<String> courseSlot : this.courseSlots) {
            // Check if the day is valid
            if (!courseSlot.get(0).equals("MO") && !courseSlot.get(0).equals("TU")) {
                throw new IllegalArgumentException("Invalid day in course slots. Only MO and TU are allowed.");
            }
            // Check if the time is valid
            if (courseSlot.get(0).equals("MO")
                && !courseSlot.get(1).equals("8:00")
                && !courseSlot.get(1).equals("9:00")
                && !courseSlot.get(1).equals("10:00")
                && !courseSlot.get(1).equals("11:00")
                && !courseSlot.get(1).equals("12:00")
                && !courseSlot.get(1).equals("13:00")
                && !courseSlot.get(1).equals("14:00")
                && !courseSlot.get(1).equals("15:00")
                && !courseSlot.get(1).equals("16:00")
                && !courseSlot.get(1).equals("17:00")
                && !courseSlot.get(1).equals("18:00")
                && !courseSlot.get(1).equals("19:00")
                && !courseSlot.get(1).equals("20:00")) {
                throw new IllegalArgumentException("Invalid time in course slots. "
                    + "Only 8:00, 9:00, 10:00, 11:00, 12:00, 13:00, 14:00, 15:00, "
                    + "16:00, 17:00, 18:00, 19:00, 20:00 are allowed for Monday/Wednesday/Friday.");
            }
            // Check if the time is valid
            if (courseSlot.get(0).equals("TU")
                && !courseSlot.get(1).equals("8:00")
                && !courseSlot.get(1).equals("9:30")
                && !courseSlot.get(1).equals("11:00")
                && !courseSlot.get(1).equals("12:30")
                && !courseSlot.get(1).equals("14:00")
                && !courseSlot.get(1).equals("15:30")
                && !courseSlot.get(1).equals("17:00")
                && !courseSlot.get(1).equals("18:30")) {
                throw new IllegalArgumentException("Invalid time in course slots. "
                    + "Only 8:00, 9:30, 11:00, 12:30, 14:00, "
                    + "15:30, 17:00, 18:30 are allowed for Tuesday/Thursday.");
            }
        }
        // Check if the lab slots are valid
        // [[MO, 8:00, 4, 2], [TU, 10:00, 2, 1], [FR, 10:00, 2, 1]]
        for (List<String> labSlot : this.labSlots) {
            // Check if the day is valid
            if (!labSlot.get(0).equals("MO") && !labSlot.get(0).equals("TU")
                && !labSlot.get(0).equals("FR")) {
                throw new IllegalArgumentException("Invalid day in lab slots. Only MO, TU, FR are allowed.");
            }
            // Check if the time is valid
            if (labSlot.get(0).equals("MO")
                && !labSlot.get(1).equals("8:00")
                && !labSlot.get(1).equals("9:00")
                && !labSlot.get(1).equals("10:00")
                && !labSlot.get(1).equals("11:00")
                && !labSlot.get(1).equals("12:00")
                && !labSlot.get(1).equals("13:00")
                && !labSlot.get(1).equals("14:00")
                && !labSlot.get(1).equals("15:00")
                && !labSlot.get(1).equals("16:00")
                && !labSlot.get(1).equals("17:00")
                && !labSlot.get(1).equals("18:00")
                && !labSlot.get(1).equals("19:00")
                && !labSlot.get(1).equals("20:00")) {
                throw new IllegalArgumentException("Invalid time in lab slots. "
                    + "Only 8:00, 9:00, 10:00, 11:00, 12:00, 13:00, 14:00, 15:00, "
                    + "16:00, 17:00, 18:00, 19:00, 20:00 are allowed for Monday/Wednesday.");
            }
            // Check if the time is valid
            if (labSlot.get(0).equals("TU")
                && !labSlot.get(1).equals("8:00")
                && !labSlot.get(1).equals("9:00")
                && !labSlot.get(1).equals("10:00")
                && !labSlot.get(1).equals("11:00")
                && !labSlot.get(1).equals("12:00")
                && !labSlot.get(1).equals("13:00")
                && !labSlot.get(1).equals("14:00")
                && !labSlot.get(1).equals("15:00")
                && !labSlot.get(1).equals("16:00")
                && !labSlot.get(1).equals("17:00")
                && !labSlot.get(1).equals("18:00")
                && !labSlot.get(1).equals("19:00")
                && !labSlot.get(1).equals("20:00")) {
                throw new IllegalArgumentException("Invalid time in lab slots. "
                    + "Only 8:00, 9:00, 10:00, 11:00, 12:00, 13:00, 14:00, 15:00, "
                    + "16:00, 17:00, 18:00, 19:00, 20:00 are allowed for Tuesday/Thursday.");
            }
            // Check if the time is valid
            if (labSlot.get(0).equals("FR")
                && !labSlot.get(1).equals("8:00")
                && !labSlot.get(1).equals("10:00")
                && !labSlot.get(1).equals("12:00")
                && !labSlot.get(1).equals("14:00")
                && !labSlot.get(1).equals("16:00")
                && !labSlot.get(1).equals("18:00")) {
                throw new IllegalArgumentException("Invalid time in lab slots. "
                    + "Only 8:00, 10:00, 12:00, 14:00, "
                    + "16:00, 18:00 are allowed for Friday.");
            }
        }
    }

    /**
     * Parses the file specified in the constructor.
     */
    public void parse() throws Exception {
        // Try with resources to close the BufferedReader
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(this.fileName))) {
            // Read the file line by line
            String content;
            while ((content = bufferedReader.readLine()) != null) {
                this.fileContentBuilder.append(content).append("\n");
            }
            // Split the file content by double new line
            String[] fileContent = this.fileContentBuilder.toString().split("\n\n");
            // Parse each section of the file
            for (String fileItems : fileContent) {
                parse(fileItems);
            }
        } finally {
            if (this.printData) {
                printDataForTesting();
            }
            validateClassSlots();
        }
    }

    /**
     * Parses the file content and populates the data structures.
     * @param fileContent - The content of the file to be parsed.
     */
    private void parse(String fileContent) {
        // Split by colon
        String[] fileStructure = fileContent.split(":\n");
        // Trim and convert to lowercase
        String identifier = fileStructure[0].toLowerCase(Locale.ROOT).trim();
        // Populate the data structures based on the identifier
        switch (identifier) {
        case "name":
            this.name = fileStructure[1];
            break;
        case "course slots":
            buildArrayList(this.courseSlots, fileStructure, ",");
            break;
        case "lab slots":
            buildArrayList(this.labSlots, fileStructure, ",");
            break;
        case "courses":
            buildArrayList(this.courses, fileStructure, " ");
            break;
        case "labs":
            buildArrayList(this.labs, fileStructure, " ");
            break;
        case "not compatible":
            buildArrayList(this.notCompatible, fileStructure);
            break;
        case "unwanted":
            buildArrayList(this.unwanted, fileStructure);
            break;
        case "preferences":
            buildArrayList(this.preferences, fileStructure);
            break;
        case "pair":
            buildArrayList(this.pairs, fileStructure);
            break;
        case "partial assignments":
            buildArrayList(this.partialAssignments, fileStructure);
            break;
        default:
            break;
        }
    }

    /**
     * Builds an ArrayList of ArrayLists of Strings.
     * @param variable - The ArrayList to be populated.
     * @param content - The content of the file to be parsed.
     * @param delimiter - The delimiter to be used to split the content.
     */
    private void buildArrayList(ArrayList<List<String>> variable, String[] content, String delimiter) {
        if (content.length > 1 && content[1] != null) {
            // Precompile patterns
            Pattern splitNewLinePattern = Pattern.compile("\n");
            Pattern splitDelimiterPattern = Pattern.compile(delimiter + "+");
            // Split by new line
            String[] lines = splitNewLinePattern.split(content[1]);
            for (String line : lines) {
                // Split by delimiter
                String[] items = splitDelimiterPattern.split(line);
                List<String> value = new ArrayList<>(items.length);
                // Trim and add to ArrayList
                for (String item : items) {
                    value.add(item.trim());
                }
                // Add to ArrayList of ArrayLists
                variable.add(value);
            }
        }
    }

    /**
     * Builds an ArrayList of ArrayLists of ArrayLists of Strings.
     * @param variable - The ArrayList to be populated.
     * @param content - The content of the file to be parsed.
     */
    private void buildArrayList(ArrayList<ArrayList<List<String>>> variable, String[] content) {
        if (content.length > 1 && content[1] != null) {
            // Precompile patterns
            Pattern newLinePattern = Pattern.compile("\n");
            Pattern spacePattern = Pattern.compile(" +");
            Pattern commaPattern = Pattern.compile(",");
            // Split by new line
            String[] lines = newLinePattern.split(content[1]);
            for (String line : lines) {
                ArrayList<List<String>> value = new ArrayList<>();
                // Trim and split by comma
                String[] items = commaPattern.split(line.trim());
                for (String item : items) {
                    // Split by spaces after trimming the item
                    List<String> val = new ArrayList<>(Arrays.asList(spacePattern.split(item.trim())));
                    value.add(val);
                }
                variable.add(value);
            }
        }
    }

    /**
     * Prints the data for testing purposes.
     */
    private void printDataForTesting() {
        System.out.println("Course Slots: \n" + this.courseSlots);
        System.out.println("Lab Slots: \n" + this.labSlots);
        System.out.println("Courses: \n" + this.courses);
        System.out.println("Labs: \n" + this.labs);
        System.out.println("Not compatible: \n" + this.notCompatible);
        System.out.println("Unwanted: \n" + this.unwanted);
        System.out.println("Preferences: \n" + this.preferences);
        System.out.println("Pairs: \n" + this.pairs);
        System.out.println("Partial Assignments: \n" + this.partialAssignments);
    }

    /*
     * Returns the name of the example.
     */
    public String getName() {
        return this.name;
    }

    /*
     * Returns the course slots populated by the parser.
     */
    public ArrayList<List<String>> getCourseSlots() {
        return this.courseSlots;
    }

    /*
     * Returns the lab slots populated by the parser.
     */
    public ArrayList<List<String>> getLabSlots() {
        return this.labSlots;
    }

    /*
     * Returns the courses populated by the parser.
     */
    public ArrayList<List<String>> getCourses() {
        return this.courses;
    }

    /*
     * Returns the labs populated by the parser.
     */
    public ArrayList<List<String>> getLabs() {
        return this.labs;
    }

    /*
     * Returns the not compatible course/labs populated by the parser.
     */
    public ArrayList<ArrayList<List<String>>> getNotCompatible() {
        return this.notCompatible;
    }

    /*
     * Returns the unwanted course/labs populated by the parser.
     */
    public ArrayList<ArrayList<List<String>>> getUnwanted() {
        return this.unwanted;
    }

    /*
     * Returns the preferences populated by the parser.
     */
    public ArrayList<ArrayList<List<String>>> getPreferences() {
        return this.preferences;
    }

    /*
     * Returns the pairs populated by the parser.
     */
    public ArrayList<ArrayList<List<String>>> getPairs() {
        return this.pairs;
    }

    /*
     * Returns the partial assignments populated by the parser.
     */
    public ArrayList<ArrayList<List<String>>> getPartAssign() {
        return this.partialAssignments;
    }
}

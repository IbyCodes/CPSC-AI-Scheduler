import java.util.List;

/**
 * Data structure for Slots
 */
public class Slots {
    // A list of lists representing the course and lab slots.
    private static final List<List<String>> courseSlots = Scheduler.getCourseSlots();
    private static final List<List<String>> labSlots = Scheduler.getLabSlots();

    // A list representing the slot's name and details.
    private final List<String> name;

    // A flag to indicate if the slot is in the evening.
    private boolean isEvening;

    // The maximum number of classes or labs that can be assigned to this slot.
    private final int max;

    /**
     * Constructor takes in a type (course or lab) and an index.
     * @param type the type of slot (course or lab)
     * @param index the index of the slot
     */
    public Slots(String type, int index) {
        // Retrieve the appropriate list of slots based on the type.
        List<List<String>> slots = type.equals("course")
            ? Slots.courseSlots
            : Slots.labSlots;
        // Get the name details from the slots list.
        // e.g. [MO, 8:00, 4, 2]
        try {
            this.name = slots.get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException("Invalid index for slot " + index + " at Slots.java");
        }
        // Parse the maximum capacity from the name list.
        try {
            this.max = Integer.parseInt(this.name.get(2));
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid max capacity for slot " + this.name + " at Slots.java");
        }

        setEvening();
    }

    private void setEvening() {
        // Parse the time from the name list.
        String time = this.name.get(1);
        // Check if the time is in the evening.
        int hourDelimiterIndex = time.indexOf(':');
        if (hourDelimiterIndex != -1) {
            // If the time is in the evening, set the flag to true.
            // Evening is defined as 6pm onwards.
            try {
                int hr = Integer.parseInt(time.substring(0, hourDelimiterIndex));
                this.isEvening = hr >= 18;
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Invalid time for slot " + this.name + " at Slots.java");
            }
        }
    }

    /**
     * Method returns the name of the slot.
     * @return The name of the slot.
     */
    public List<String> getName() {
        return this.name;
    }

    /**
     * Method returns whether the slot is in the evening.
     * @return Whether the slot is in the evening.
     */
    public boolean isEvening() {
        return this.isEvening;
    }

    /**
     * Method returns the max number of course/labs that can be in the slot.
     * @return The max number of course/labs that can be in the slot.
     */
    public int getMax() {
        return this.max;
    }
}

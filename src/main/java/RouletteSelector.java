import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Selects a solution candidate from the generation using the roulette wheel selection method.
 */
public class RouletteSelector {
    private List<List<List<String>>> generation;
    private float[] cumulativeProbabilities;
    private Random rand;
    private int lastChoice = -1;

    /**
     * Constructor for the roulette selector.
     * @param generation - the generation
     * @param rand - the random number generator
     */
    public RouletteSelector(List<List<List<String>>> generation, Random rand) {
        this.rand = rand;
        this.generation = generation;
        if (this.generation.size() == 0) {
            throw new IllegalArgumentException("Generation "
                + "must contain at least one solution candidate.");
        }
        // Calculate the total evaluation value
        double totalEval = this.generation.stream()
                            .mapToDouble(Scheduler.getEval()::eval)
                            .sum();
        // Avoid division by zero
        if (totalEval == 0) {
            totalEval = 1;
        }
        this.cumulativeProbabilities = new float[this.generation.size()];
        float cumulative = 0;
        SoftConstraint eval = Scheduler.getEval();
        // Calculate the cumulative probabilities
        for (int i = 0; i < this.generation.size(); i++) {
            float evalValue = eval.eval(this.generation.get(i));
            this.cumulativeProbabilities[i] = (float) ((cumulative += evalValue) / totalEval);
        }
    }

    /**
     * Get the last choice.
     * @return last choice
     */
    public List<List<String>> getSelection() {
        // If the last choice is -1, return null
        if (this.lastChoice == -1) {
            return null;
        } else {
            // Otherwise, return the solution candidate at the last choice
            try {
                return this.generation.get(lastChoice);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    /**
     * Select a solution candidate from the generation.
     * @param ignoreFactIndex - the index of the fact to ignore
     * @return index of the selected solution candidate
     */
    public int select(int ignoreFactIndex) {
        // Generate a random float between 0 and 1
        float randFloat = rand.nextFloat();
        // Collect all the indices that match the condition
        // (i.e. the cumulative probability is greater than the random float)
        // and are not the index of the fact to ignore
        // into a list
        List<Integer> validIndices = IntStream.range(0, this.cumulativeProbabilities.length)
            .filter(i -> i != ignoreFactIndex)
            .filter(i -> this.cumulativeProbabilities[i] >= randFloat)
            .boxed()
            .collect(Collectors.toList());
        // If there are no valid indices, set the last choice to -1
        if (validIndices.isEmpty()) {
            // This will cause the crossover method to return
            this.lastChoice = -1;
            return this.lastChoice;
        }
        // Randomly select one of the valid indices
        this.lastChoice = validIndices.get(rand.nextInt(validIndices.size()));
        return this.lastChoice;
    }
}

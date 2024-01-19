import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Set-based search for an optimal solution to the problem instance.
 * THIS IS WHERE THE CODE RESIDES FOR SET BASED SEARCH
 */
public class Generation {
    private List<List<List<String>>> generation = new ArrayList<>();
    private int maxPop = 0;
    private List<OrTree> orTrees;
    private int classSize;
    private SoftConstraint eval;

    /**
     * Constructor for the Generation class.
     */
    public Generation() {
        this.maxPop = Scheduler.getMaxPop(); // getting the max population
        this.eval = Scheduler.getEval(); // getting the eval value
        this.classSize = Scheduler.getClasses().size(); // getting the size of all the classes
    }

    /**
     * Add a solution candidate to the generation.
     * @param candidate - the solution candidate
     */
    public void add(List<List<String>> candidate) {
        this.generation.add(candidate);
    }

    /**
     * Assess the current size of the population relative to the predefined
     * threshold. Depending on the current population size, the function will cause
     * the search control to use the Reduce operation, or continue with the Crossover operation.
     * @return score
     */
    private int fWert() {
        // this returns some sort of fwert value using crossover or reduction
        // if the max_pop is exceeded, then reduce is guaranteed to occur
        return this.generation.size() > this.maxPop ? 1 : 0;
    }

    /**
     * Reduce the generation to the predefined threshold.
     */
    private void reduce() { // REDUCTION OCCURS HERE in the set-based search
        // THE GOAL OF REDUCTION IS TO PRUNE the current set of potential solutions, and therefore
        // REMOVE some individual fact f from F (whatever is the WORST)
        try {
            // Create a priority queue to find the worst individuals
            PriorityQueue<List<List<String>>> worstIndividuals = new PriorityQueue<>(
                Scheduler.getNumRemove(),
                Comparator.comparingInt(sol -> this.eval.eval(sol))
            );
            // Add individuals to the priority queue; it will keep the worst ones at the TOP
            for (List<List<String>> individual : this.generation) {
                worstIndividuals.offer(individual);
                // Ensure the queue never grows beyond the number of individuals to remove
                if (worstIndividuals.size() > Scheduler.getNumRemove()) {
                    worstIndividuals.poll();
                }
            }
            // Remove the worst INDIVIDUALS from the generation (individual f's)
            // Remember we need to remove individuals until max pop is no longer exceeded
            for (List<List<String>> worstIndividual : worstIndividuals) {
                this.generation.remove(worstIndividual);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }

    /**
     * Perform crossover on the generation.
     * @param genNum - the generation number
     */
    private void crossover(int genNum) {
        // REMEMBER crossover does the following:
        /*
        * It takes two facts f (two schedules) and COMBINES traits together using an OR-tree based search
        * However here the fscore is a bit different:
        * 0 = solution is solvable
        * 1 = assigned slot is in both f1 AND f2 at the same index and Constr* holds
        * 2 = assigned slot is in either f1 OR f2 at the same index and Constr* holds
        * 3 = only Constr* is true
        * infinity = otherwise
        * The lower fscore, the better.
        */
        try {
            // Select two facts from the generation:
            Random rand = new Random();
            RouletteSelector selector = new RouletteSelector(this.generation, rand);
            // Select the first parent
            int f1Index = selector.select(-1);
            List<List<String>> f1 = selector.getSelection();
            // Select the second parent
            selector.select(f1Index);
            List<List<String>> f2 = selector.getSelection();
            // Ensure f1 and f2 are not null and not the same
            // If they are, select again
            // This is to prevent the same parent from being selected twice
            // This is also to prevent null parents from being selected
            while (f1 == null || f2 == null || f1.equals(f2)) {
                if (f1Index == -1) {
                    return;
                }
                if (f1 == null) {
                    f1Index = selector.select(-1);
                    f1 = selector.getSelection();
                }
                if (f2 == null || f2.equals(f1)) {
                    selector.select(f1Index);
                    f2 = selector.getSelection();
                }
            }
            // Perform crossover on the two facts:
            OrTree tree = new OrTree(this.classSize);
            // Create a deep copy of the original PR
            List<List<String>> original = Scheduler.getInitialPR();
            List<List<String>> child = new ArrayList<>(original.size());
            for (List<String> sublist : original) {
                child.add(new ArrayList<>(sublist));
            }
            // Create a list of or-trees to be used in the search
            this.orTrees = new ArrayList<>();
            // Perform an or-tree-based search to build a solution candidate:
            List<List<String>> sol = tree.searchSolutionWParents(child, 0, f1, f2, this.orTrees, rand);
            // Checks whether the new solution is null
            if (sol == null) {
                return;
            } else {
                // Add the new solution to the generation:
                this.generation.add(sol);
            }
        } catch (Exception e) {
            System.out.println("Exception at crossover: " + e);
        }
    }

    /**
     * Search control for the generation.
     * @param genNum - the generation number
     */
    public void control(int genNum) {
        // remember that fwert = 1 or 0.
        // if its 1: the population size is greater then max_pop, so we MUST do reduce
        // else, its 0: so we have a valid amount of population size and can do crossover
        // Check whether the population size is greater than the predefined threshold
        // If so, reduce the population size to the predefined threshold
        // Otherwise, perform crossover on the population
        if (fWert() == 1) {
            reduce();
        } else {
            crossover(genNum);
        }
    }

    /**
     * Get the generation.
     * @return generation
     */
    public List<List<List<String>>> getGeneration() {
        return this.generation;
    }

    /**
     * Print the generation's data.
     * @param initialFlag - whether or not this is the initial generation
     */
    public void printData(boolean initialFlag) {
        String output = "\t\tAvg: " + getAvg() + "\t\tMin: " + getMin() + "\t\tMax: " + getMax() + "\n";
        if (initialFlag) {
            output = "\t" + output;
        }
        System.out.print(output);
    }

    /**
     * Get the average eval value of the generation.
     * @return avg
     */
    public String getAvg() { // Eval value AVERAGE calculated here
        float avg = 0;
        for (int i = 0; i < this.generation.size(); i++) {
            avg += this.eval.eval(this.generation.get(i));
        }
        avg = avg / this.generation.size();
        return String.valueOf(avg);
    }

    /**
     * Get the minimum eval value of the generation.
     * @return min
     */
    public String getMin() {
        int min = this.eval.eval(this.generation.get(0));
        for (int i = 1; i < this.generation.size(); i++) {
            if (min > this.eval.eval(this.generation.get(i))) {
                min = this.eval.eval(this.generation.get(i));
            }
        }
        return String.valueOf(min);
    }

    /**
     * Get the maximum eval value of the generation.
     * @return max
     */
    public String getMax() { // MAXIMUM eval value calculated here
        int max = this.eval.eval(this.generation.get(0));
        for (int i = 1; i < this.generation.size(); i++) {
            if (max < this.eval.eval(this.generation.get(i))) {
                max = this.eval.eval(this.generation.get(i));
            }
        }
        return String.valueOf(max);
    }

    /**
     * Print the generation.
     */
    public void print() {
        System.out.println("\nFinal Generation:");
        for (List<List<String>> sol : this.generation) {
            // Print each solution, which is a List<List<String>>.
            System.out.println(sol.toString());
        }
        System.out.print("\n");
    }
}

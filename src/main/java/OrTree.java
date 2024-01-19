import java.util.ArrayList;
import java.util.Collections;
// import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
// import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * The OrTree is used to perform an or-tree based search of the solution space.
 */
public class OrTree {
    private static final int MAX_OR_TREE_SIZE = 28000;
    // Enum to represent the solvability of a given node
    private enum Solvability {
        YES, NO, UNKNOWN // the sol value can only be one of these 3 options.
    }
    // The data list of the node
    private List<List<String>> data;
    // The children of the node
    private List<OrTree> children;
    // The solvability of the node
    private Solvability solvable = Solvability.UNKNOWN;
    // The score of the node
    private int score = -1;

    /**
     * Constructor for beginning with a partial solution.
     *
     * @param pr - A list representing a partial course/lab section
     *           assignment.
     */
    public OrTree(List<List<String>> pr) {
        // Create a deep copy of the List of Lists
        this.data = new ArrayList<>(pr.size());
        for (List<String> list : pr) {
            this.data.add(new ArrayList<>(list)); // Copy each nested list
        }
        // Initialize the children list
        this.children = new LinkedList<>();
    }

    /**
     * Constructor for beginning with an empty solution.
     *
     * @param length - The length of the problem instance.
     */
    public OrTree(int length) { // if starting with a EMPTY pr
        // Initialize the data list
        this.data = new ArrayList<>(Collections.nCopies(length, Scheduler.UNASSIGNED_SLOT));
        this.children = new LinkedList<>();
    }

    /**
     * Add a child to the node.
     *
     * @param child - An list representing a course/lab section assignment.
     * @return childNode
     */
    public OrTree addChild(List<List<String>> child) {
        // Create a new child node
        OrTree childNode = new OrTree(child);
        // Add the child node to the children list
        this.children.add(childNode);
        // Return the child node
        return childNode;
    }

    /**
     * Get the data list of the node.
     *
     * @return data
     */
    public List<List<String>> getData() {
        return this.data;
    }

    /**
     * Generates all possible slots for a given class or lab section and creates
     * child nodes for each valid assignment.
     *
     * @param classIndex the index of the class or lab section in the PR list.
     */
    private void altern(int classIndex) {
        // Get the list of slots for the course or lab section
        List<List<String>> slots = classIndex < Scheduler.getCourses().size()
                ? Scheduler.getCourseSlots()
                : Scheduler.getLabSlots();
        if (Scheduler.isPrintData()) {
            System.out.println("Altern for class index: " + classIndex);
        }
        // Iterate through all possible slots for the section
        for (List<String> slot : slots) {
            // Create a deep copy of the data for the new child
            List<List<String>> newChild = new ArrayList<>(this.data.size());
            for (List<String> list : this.data) {
                newChild.add(new ArrayList<>(list));
            }
            // Assign slot to the class/lab
            newChild.set(classIndex, new ArrayList<>(slot)); // Ensuring a deep copy of the slot
            // Add new child if it satisfies constraints
            if (Scheduler.getConstr().constrStar(newChild)) {
                this.addChild(newChild);
            }
        }
    }

    // Helper functions for scoring
    /*
     * Returns 1 if the slot is unassigned, 0 otherwise.
     */
    private int help(List<String> slot) { // just returns 0 or 1
        return slot.equals(Scheduler.UNASSIGNED_SLOT) ? 1 : 0;
    }

    /*
     * Returns the sum of the number of unassigned slots for each course/lab.
     */
    private int sum(List<List<String>> pr) {
        return pr.stream().mapToInt(this::help).sum();
    }

    /*
     * Returns the score of a given node.
     */
    private int score(List<List<String>> pr) {
        if (this.solvable(pr)) {
            return 0;
        } else if (this.unsolvable(pr)) {
            // Pr fails hard constraints
            return Integer.MAX_VALUE;
        } else {
            return this.sum(pr);
        }
    }

    /*
     * Returns the score of a given node.
     */
    private int score(List<List<String>> child, List<List<String>> f1, List<List<String>> f2, int index) {
        // Check if child is solvable
        if (this.solvable(child)) {
            return 0;
        } else if (this.unsolvable(child)) {
            // Child fails hard constraints
            return Integer.MAX_VALUE;
        } else {
            // Check child slot is same as parents at slot index
            if (child.get(index).equals(f1.get(index)) && child.get(index).equals(f2.get(index))) {
                // Child slot is same as both parents
                return 1;
            } else if (child.get(index).equals(f1.get(index)) || child.get(index).equals(f2.get(index))) {
                // Child slot is same as one parent
                return 2;
            } else {
                // Child slot is different from both parents
                return 3;
            }
        }
    }

    /**
     * Perform an or-tree-based search to find a solution.
     * @param arrList - An ArrayList of indices of the most tightly bound elements,
     *      e.g. highest number of constraints.
     * @param index - The index to populate
     * @param orTrees - A list of leaf nodes
     * @param rand - A random number generator
     * @return sol - An list which is a pr-solved instance.
     */
    public List<List<String>> searchSolution(ArrayList<Integer> arrList, int index, List<OrTree> orTrees,
            Random rand) {
        try {
            if (Scheduler.isPrintData()) {
                System.out.println("Searching for solution with orTrees... LEAF HEAP SIZE: " + orTrees.size());
            }
            if (orTrees.size() > OrTree.MAX_OR_TREE_SIZE) {
                if (Scheduler.isPrintData()) {
                    System.out.println("Too many nodes in orTrees");
                }
                orTrees.clear();
                OrTree tree = new OrTree(Scheduler.getInitialPR());
                return tree.searchSolution(arrList, 0, orTrees, rand);
            }
            // Return the data list once it is complete:
            if (this.solvable == Solvability.YES) {
                return this.data;
            } else if (this.solvable == Solvability.NO) {
                // If it is unsolvable, return null
                return null;
            }
            // Determine the score of the current node:
            this.score = this.score(this.data);
            // Return a solution when one is found:
            if (this.score == 0) {
                this.solvable = Solvability.YES;
                return this.data;
            } else if (this.score == Integer.MAX_VALUE) {
                this.solvable = Solvability.NO;
                orTrees.remove(this);
                // If the orTrees is empty, there is no solution.
                if (orTrees.isEmpty()) {
                    return null;
                }
                // Randomly select one
                int randIndex = rand.nextInt(orTrees.size());
                OrTree selectedNode = orTrees.get(randIndex);
                return selectedNode.searchSolution(arrList, 0, orTrees, rand);
            } else {
                // Determine index of element of pr that will be expanded by altern.
                int selectedIndex = arrList.get(index % arrList.size());
                // Avoid over-writing values designed by partial assignments:
                if (!this.data.get(selectedIndex).equals(Scheduler.UNASSIGNED_SLOT)) {
                    if (Scheduler.isPrintData()) {
                        System.out.println("Skipping over " + this.data.get(selectedIndex));
                    }
                    return this.searchSolution(arrList, index + 1, orTrees, rand);
                }
                // Clear the children list:
                this.children.clear();
                // Generate successor nodes for current course/lab:
                altern(selectedIndex);
                // Remove the current node from orTrees, as it has been expanded:
                orTrees.remove(this);
                // Add the successor nodes to orTrees:
                orTrees.addAll(this.children.stream().filter(c -> !orTrees.contains(c)
                    && !c.equals(this)).collect(Collectors.toList()));
                // If there are children, continue searching:
                if (this.children.size() > 0) {
                    // Choose a random successor node to expand:
                    // they all have the same score, so it doesn't matter which one we pick
                    if (Scheduler.isPrintData()) {
                        System.out.println("Choosing a random successor node to expand from the children");
                    }
                    int randIndex = rand.nextInt(this.children.size());
                    OrTree child = this.children.get(randIndex);
                    // Recursively expand successor nodes until completion:
                    return child.searchSolution(arrList, index + 1, orTrees, rand);
                } else if (!orTrees.isEmpty()) {
                    // Randomly select a node from orTrees to expand with the lowest score
                    if (Scheduler.isPrintData()) {
                        System.out.println("No children. We try to find a solution from the orTrees. ");
                    }
                    // Randomly select one
                    int randIndex = rand.nextInt(orTrees.size());
                    // OrTree selectedNode = lowestScoreNodes.get(rand.nextInt(lowestScoreNodes.size()));
                    OrTree selectedNode = orTrees.get(randIndex);
                    return selectedNode.searchSolution(arrList, 0, orTrees, rand);
                } else {
                    // If the orTrees is empty and there are no children, there is no solution.
                    return null;
                }
            }
        } catch (StackOverflowError e) {
            System.out.println("Error: Stack overflow in OrTree.searchSolution");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            System.out.println("Please add more memory to the JVM using the -Xss flag.");
            return null;
        } catch (OutOfMemoryError e) {
            System.out.println("Out of memory error in OrTree.searchSolution");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            System.out.println("Please add more memory to the JVM using the -Xss flag.");
            return null;
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Index out of bounds error in OrTree.searchSolution");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            return null;
        } catch (Exception e) {
            System.out.println("Unknown error in OrTree.searchSolution");
            System.out.println(e);
            return null;
        }
    }

    /**
     * Perform an or-tree-based search to find a solution.
     * @param child - An list which is a class assignment.
     * @param index - The index to populate.
     * @param par1 - A list representing a course/lab section assignment.
     * @param par2 - A list representing a course/lab section assignment.
     * @param orTrees - A list of leaf nodes.
     * @param rand - A random number generator.
     * @return child - An list which is a class assignment.
     */
    // this must be for or-tree searches where you already have partial solutions
    public List<List<String>> searchSolutionWParents(List<List<String>> child,
        int index, List<List<String>> par1, List<List<String>> par2, List<OrTree> orTrees, Random rand) {
        try {
            if (Scheduler.isPrintData()) {
                System.out.println("Searching with orTrees crossover... index: "
                    + index + " LEAF HEAP SIZE: " + orTrees.size());
            }
            // Return null if child is null:
            if (child == null) {
                return null;
            }
            // Return the child once it is complete:
            if (index >= child.size()) {
                // Check whether child fulfils hard constraints
                if (this.solvable(child)) {
                    return child;
                }
                return null;
            }
            // Skip over values designed by partial assignments:
            if (!child.get(index).equals(Scheduler.UNASSIGNED_SLOT)) {
                return searchSolutionWParents(child, index + 1, par1, par2, orTrees, rand);
            }
            // Generate successor nodes for current class assignment:
            child = combineTraits(child, index, par1, par2, orTrees, rand);
            // Recursively increment index until completion:
            return searchSolutionWParents(child, index + 1, par1, par2, orTrees, rand);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Index out of bounds error in OrTree.searchSolutionWParents");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            return null;
        } catch (StackOverflowError e) {
            System.out.println("Error: Stack overflow in OrTree.searchSolutionWParents");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            System.out.println("Please add more memory to the JVM using the -Xss flag.");
            return null;
        } catch (OutOfMemoryError e) {
            System.out.println("Out of memory error in OrTree.searchSolutionWParents");
            System.out.println("Unable to proceed with scheduling as no valid solution can be formed.");
            System.out.println("Please add more memory to the JVM using the -Xss flag.");
            return null;
        } catch (Exception e) {
            System.out.println("Unknown error in OrTree.searchSolutionWParents");
            System.out.println(e);
            return null;
        }
    }

    /**
     * Combine traits from two parents to create a child.
     * @param child A list representing a course/lab section assignment.
     * @param index The index of the current element of the list.
     * @param par1 A list representing a course/lab section assignment.
     * @param par2 A list representing a course/lab section assignment.
     * @param orTrees A list of leaf nodes.
     * @param rand A random number generator.
     * @return child A list representing a course/lab section assignment.
     */
    public List<List<String>> combineTraits(List<List<String>> child, int index,
        List<List<String>> par1, List<List<String>> par2, List<OrTree> orTrees, Random rand) {
        if (Scheduler.isPrintData()) {
            System.out.println("Combining traits...");
        }
        try {
            if (child == null) {
                return null;
            }
            // Assess the viability of selecting each parent's assignment:
            child.set(index, par1.get(index));
            this.score = score(child, par1, par2, index);
            // If the child is viable, continue searching:
            if (this.score == 0 || this.score == 1) {
                if (Scheduler.isPrintData()) {
                    System.out.println("Parent 1 is viable");
                }
                return this.searchSolutionWParents(child, index + 1, par1, par2, orTrees, rand);
            }
            // this.score either 2, 3 or MAX_VALUE
            child.set(index, par2.get(index));
            int par2Score = score(child, par1, par2, index);
            // If the child is viable, continue searching:
            if (par2Score < 3) {
                if (par2Score == 1) {
                    if (Scheduler.isPrintData()) {
                        System.out.println("Parent 2 is viable");
                    }
                    return this.searchSolutionWParents(child, index + 1, par1, par2, orTrees, rand);
                }
                if (Scheduler.isPrintData()) {
                    System.out.println("Both parents are equally viable. We randomly select one of the parents");
                }
                // If both parents are equally viable, randomly select one:
                boolean chooseParent1 = rand.nextBoolean();
                if (chooseParent1) {
                    child.set(index, par1.get(index));
                } else {
                    child.set(index, par2.get(index));
                }
                return this.searchSolutionWParents(child, index + 1, par1, par2, orTrees, rand);
            } else {
                // If neither parent is viable, randomly select a node from orTrees to expand with the lowest score
                if (Scheduler.isPrintData()) {
                    System.out.println("Neither parent is viable. We try to find a solution from the orTrees");
                }
                this.solvable = Solvability.NO;
                // Clear the children list:
                this.children.clear();
                // Generate successor nodes for current course/lab:
                altern(index);
                // Remove the current node from orTrees, as it has been expanded:
                orTrees.remove(this);
                // Add the successor nodes to orTrees:
                orTrees.addAll(this.children.stream().filter(c -> !orTrees.contains(c)
                    && !c.equals(this)).collect(Collectors.toList()));
                // If there are children, continue searching:
                if (this.children.size() > 0) {
                    // Choose a random successor node to expand:
                    int randIndex = rand.nextInt(this.children.size());
                    OrTree randChild = this.children.get(randIndex);
                    // Recursively expand successor nodes until completion:
                    return randChild.searchSolutionWParents(randChild.data, index + 1, par1, par2, orTrees, rand);
                } else {
                    if (Scheduler.isPrintData()) {
                        System.out.println("No children. We find new parents from the orTrees");
                    }
                    // Find another 2 parents from orTrees
                    return null;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Index out of bounds error in OrTree.combineTraits");
            return null;
        } catch (Exception e) {
            System.out.println("Unknown error in OrTree.combineTraits");
            System.out.println(e);
            return null;
        }
    }

    /**
     * Determines if a given list represents a complete assignment solution.
     * @param data A list representing a course/lab section assignment.
     * @return True if the list represents a complete solution, false otherwise.
     */
    private boolean solvable(List<List<String>> data) {
        // Check if all slots are assigned and the list satisfies hard constraints
        boolean allSlotsAssigned = data.stream()
                                       .noneMatch(slot -> slot.equals(Scheduler.UNASSIGNED_SLOT));
        return allSlotsAssigned && Scheduler.getConstr().constr(data);
    }

    /**
     * Determines if a given list violates any hard constraints.
     * @param data A list representing a course/lab section assignment.
     * @return True if the data list violates hard constraints, false otherwise.
     */
    private boolean unsolvable(List<List<String>> data) {
        // If the data list violates hard constraints, return true
        return !Scheduler.getConstr().constrStar(data);
    }
}

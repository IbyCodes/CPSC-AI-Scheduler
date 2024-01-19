## Introduction

A scheduler to find the optimal assignment of courses and labs to specific time slots in the week for the Department of Computer Science at the University of Calgary.

## Running the Scheduler

### Steps to Execute

1. **Navigate to Code Directory:**
   - Open Command Prompt.
   - Change directory to the Java code location:
     ```sh
     cd CPSC-433-Bin\src\main\java
     ```

2. **Compile the Java Code:**
   - Use the `javac` command for compilation:
     ```sh
     javac *.java
     ```

3. **Execute the Scheduler:**
   - Run the scheduler using:
     ```sh
     java -Xss1g Scheduler [config_file_name] [input_file_name]
     ```
   - Example (assuming current directory is `CPSC-433-Bin\src\main\java`):
     ```sh
     java Scheduler ..\..\..\config.txt ..\..\..\shortExample.txt
     ```

### Configuration and Input Files
- The `config.txt` file and example input files are located in the master folder.

## Configuration Parameters

### Soft Constraint Parameters
- **wMinFilled:** Weighting for minimum fill penalties of courses and labs.
- **wPref:** Weighting for preference-based penalties.
- **wPair:** Weighting for penalties related to unpaired courses/labs.
- **wSecDiff:** Weighting for penalties due to overlapping course sections.
- **penCourseMin:** Penalty for courses not reaching minimum fill.
- **penLabMin:** Penalty for labs not reaching minimum fill.
- **penNotPaired:** Penalty for not pairing specified courses/labs.
- **penSection:** Penalty for overlapping sections of a course.

### Set Based Search Parameters
- **initialPop:** Starting population size for candidate solutions.
- **maxPop:** Maximum allowable size of a generation.
- **numRemove:** Number of solutions to remove upon reaching maxPop.
- **maxGeneration:** The cap on the number of generations in the algorithm.

### Output Parameters
- **printPr:** If enabled, prints the average, minimum, and maximum Eval scores per generation.
- **printData:** If enabled, provides detailed information about the scheduling process.

### Stability Parameters
- **stableThreshold:** A threshold value for assessing the stability of the Eval score.
- **maxStableGeneration:** The maximum number of generations to establish complete stabilization of the Eval value.

## Additional Resources
For comprehensive details on input and output formats, please refer to the [assignment input description page](https://pages.cpsc.ucalgary.ca/~denzinge/courses/433-fall2021/assigninput.html).
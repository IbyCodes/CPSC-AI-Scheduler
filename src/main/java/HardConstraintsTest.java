import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;


public class HardConstraintsTest {
	
	// ALL OF THESE TESTS ARE BASED ON "shortExample.txt"
	// MAY REQUIRE ALTERING of privated methods for tests to run.
	
    @Before
    public void testParser() { // will parse the "shortExample.txt" file
        // Provide the path to your test file
    	// for my windows pc:
        String fileName = "shortExample.txt";
    	// for my mac laptop:
    	//String fileName = "/Users/mibrahimkhan/Documents/GitHub/CPSC-433-Bin/src/main/java/shortExample.txt";
        // Create a new instance of the Parser
        Parser parser = new Parser(fileName);
        Scheduler.parser = parser;
        try {
            // Parse the file
            parser.parse();
            // Test various aspects of the parsed data
            assertEquals("ShortExample", parser.getName());
            // Add more assertions for other data structures as needed
            Scheduler.courseSlots = Scheduler.parser.getCourseSlots();
            Scheduler.labSlots = Scheduler.parser.getLabSlots();
            Scheduler.courses = Scheduler.parser.getCourses();
            Scheduler.labs = Scheduler.parser.getLabs();
            Scheduler.notCompatible = Scheduler.parser.getNotCompatible();
            Scheduler.unwanted = Scheduler.parser.getUnwanted();
            Scheduler.preferences = Scheduler.parser.getPreferences();
            Scheduler.pairs = Scheduler.parser.getPairs();
            Scheduler.partialAssignments = Scheduler.parser.getPartAssign();

            Scheduler.slots = new ArrayList<>(Scheduler.courseSlots);
            Scheduler.slots.addAll(Scheduler.labSlots);
            Scheduler.classes = new ArrayList<>(Scheduler.courses);            		
            Scheduler.classes.addAll(Scheduler.labs);
            
            Scheduler.constr = new HardConstraint();
            
            
        } catch (Exception e) {
            fail("Exception thrown during parsing: " + e.getMessage());
        }
            
    }

   
    
    @Test
	public void testCoursemaxFalse() { // bad input test case for making sure we CANNOT assign more then coursemax(s) courses to slot s
		List<List<String>> coursesList = Arrays.asList( // courseMax at 9:00 on Monday is 3 using the parsed info, so can't have 4 classes
			    Arrays.asList("MO", "9:00", "3", "2"),
			   Arrays.asList("MO", "9:00", "3", "2"),
			   Arrays.asList("MO", "9:00", "3", "2"),
			   Arrays.asList("MO", "9:00", "3", "2")
			);
		
		 List<List<String>> labsList = Arrays.asList(
				    Arrays.asList("MO", "8:00", "4", "2"),
				    Arrays.asList("TU", "10:00", "2", "1"),
				    Arrays.asList("MO", "10:00", "2", "1")
				);
		
		assertFalse(Scheduler.getConstr().checkMax(coursesList, labsList)); // should return false as the slots have been exceeded
	}
    
    @Test
   	public void testCoursemaxTrue() { // good input test case for making sure we CANNOT assign more then coursemax(s) courses to slot s
   		List<List<String>> coursesList = Arrays.asList( // courseMax at 9:00 on Monday is 3 using the parsed info, so CAN have up to 3 classes
   			    Arrays.asList("MO", "9:00", "3", "2"),
   			   Arrays.asList("MO", "9:00", "3", "2"),
   			   Arrays.asList("MO", "9:00", "3", "2")
   			);
   		
   		 List<List<String>> labsList = Arrays.asList(
   				    Arrays.asList("MO", "8:00", "4", "2"),
   				    Arrays.asList("TU", "10:00", "2", "1"),
   				    Arrays.asList("MO", "10:00", "2", "1")
   				);
   		
   		assertTrue(Scheduler.getConstr().checkMax(coursesList, labsList)); // should return TRUE as no slots have been exceeded
   	}
    
    

    
    @Test
	public void testLabmaxFalse() {  //bad input test case for making sure we CANNOT assign more then labmax(s) labs to slot s
		List<List<String>> coursesList = Arrays.asList(
			    Arrays.asList("MO", "9:00", "3", "2")
			);
		
		 List<List<String>> labsList = Arrays.asList( // labmax for Monday's at 8:00 am using the parsed info is 4 so shouldn't be allowed
				    Arrays.asList("MO", "8:00", "4", "2"),
				    Arrays.asList("MO", "8:00", "4", "2"),
				    Arrays.asList("MO", "8:00", "4", "2"),
				    Arrays.asList("MO", "8:00", "4", "2"),
				   Arrays.asList("MO", "8:00", "4", "2")
				);
		 
		 assertFalse(Scheduler.getConstr().checkMax(coursesList, labsList)); // should return false as the labmax has been exceeded
	}
    
    @Test
	public void testLabmaxTrue() {  //good test case for making sure we CANNOT assign more then labmax(s) labs to slot s
		List<List<String>> coursesList = Arrays.asList(
			    Arrays.asList("MO", "9:00", "3", "2")
			);
		
		 List<List<String>> labsList = Arrays.asList( // labmax for Monday's at 8:00 am using the parsed info is 4 so should be ALLOWED
				    Arrays.asList("MO", "8:00", "4", "2"),
				    Arrays.asList("MO", "8:00", "4", "2"),
				    Arrays.asList("MO", "8:00", "4", "2"),
				    Arrays.asList("MO", "8:00", "4", "2")
				);
		 
		 assertTrue(Scheduler.getConstr().checkMax(coursesList, labsList)); // should return TRUE as the labmax for monday at 08:00 am has NOT been exceeded
	}
    

    @Test
	public void testAssignCourseAndLabFalse() { // bad input test case for assign(ci) has to be unequal to assign(lik) for ALL k and i
    	// note that basically what this test is saying is that NO course and lab can be scheduled during the same time as it would not work. 
    	
    	List<List<String>> coursesList = Arrays.asList(
			    Arrays.asList("MO", "10:00", "3", "2"), // cpsc 433 lec 01 (WILL BE PUT IN AS SAME TIME AS CPSC 433 LEC 01 TUT)
			    Arrays.asList("TU", "09:00", "3", "2"), // cpsc 433 lec 02
			    Arrays.asList("MO", "08:00", "3", "2"), // seng 311 lec 01
			    Arrays.asList("MO", "10:00", "3", "2") // cpsc 567 lec 01 
			);
		
		 List<List<String>> labsList = Arrays.asList( 
				    Arrays.asList("MO", "10:00", "3", "2"), // cpsc 433 lec 01 tut 01 (SAME TIME AS CPSC 433 LEC 01 COURSE SLOT)
				    Arrays.asList("TU", "10:00", "3", "2"), // cpsc 433 lec 02 lab 02
				    Arrays.asList("MO", "1:00", "3", "2"), // seng 311 lec 01 tut 01
				    Arrays.asList("MO", "09:00", "3", "2") // cpsc 567 tut 01

				);
		 
		 // note here that the CPSC LEC 01 course and lab have been assigned same time, so should return false
    	assertFalse(Scheduler.getConstr().checkCourseLabAssignmentUnequal(coursesList, labsList));
	}
	
    
    
    @Test
	public void testAssignCourseAndLabTrue() { // good input test for assign(ci) has to be unequal to assign(lik) for ALL k and i

    	List<List<String>> coursesList = Arrays.asList(
			    Arrays.asList("MO", "08:00", "3", "1"), // cpsc 433 lec 01 
			    Arrays.asList("TU", "09:00", "3", "1"), // cpsc 433 lec 02
			    Arrays.asList("MO", "10:00", "3", "1"), // seng 311 lec 01
			    Arrays.asList("TU", "12:00", "3", "1") // cpsc 567 lec 01 
			);
		
		 List<List<String>> labsList = Arrays.asList( 
				    Arrays.asList("MO", "10:00", "3", "1"), // cpsc 433 lec 01 tut 01 
				    Arrays.asList("TU", "11:00", "3", "1"), // cpsc 433 lec 02 lab 02
				    Arrays.asList("MO", "08:00", "3", "1"), // seng 311 lec 01 tut 01
				    Arrays.asList("MO", "09:00", "3", "1") // cpsc 567 tut 01

				);
		 
		 // note here that NO lab and course have been assigned the same time together, so should return TRUE with no conflicts
    	assertTrue(Scheduler.getConstr().checkCourseLabAssignmentUnequal(coursesList, labsList));
	}
	
	
    
    
	@Test
	public void testNotCompatibleFalse() { // Bad input test. The input for your system will contain a list of not-compatible(a,b) statements, with a,b in Courses + Labs. assign(a) MUST be unequal to assign(b), if not, then program should reject input
		/* As a reminder: Here is the course list:
		 * CPSC 433 LEC 01
			CPSC 433 LEC 02
			SENG 311  LEC  01
			CPSC 567 LEC 01
		 * Lab list:
		 * CPSC 433 LEC 01 TUT 01
			CPSC 433 LEC  02 LAB   02
			SENG 311 LEC 01 TUT 01
			CPSC 567 TUT 01
		 * 
		 * 
		 * 
		 */
		/* in the parsed info, we're given the following for non-compatible:
		CPSC 433 LEC 01 TUT 01, CPSC 433 LEC 02 LAB 02
		CPSC 567 LEC 01, CPSC 433 LEC 01
		CPSC 567 LEC 01, CPSC 433 LEC 02
		CPSC 567 TUT 01, CPSC 433 LEC 02
		CPSC 433 LEC 01, CPSC 567 TUT 01
		*/
		
		// lets try to force some classes into the same slot
		
		// for example, I'm going to try to force CPSC 433 LEC 01 and CPSC 567 LEC 01 in the same slot
		List<List<String>> coursesList = Arrays.asList(
			    Arrays.asList("MO", "10:00", "3", "2"), // cpsc 433 lec 01 (same as cpsc 567 lec 01 to test scenario)
			    Arrays.asList("TU", "09:00", "3", "2"), // cpsc 433 lec 02
			    Arrays.asList("MO", "08:00", "3", "2"), // seng 311 lec 01
			    Arrays.asList("MO", "10:00", "3", "2") // cpsc 567 lec 01 (same as cpsc 433 lec 01 to test scenario)
			);
		
		 List<List<String>> labsList = Arrays.asList( 
				    Arrays.asList("MO", "12:00", "4", "2"), // cpsc 433 lec 01 tut 01
				    Arrays.asList("TU", "10:00", "3", "2"), // cpsc 433 lec 02 lab 02
				    Arrays.asList("MO", "1:00", "3", "2"), // seng 311 lec 01 tut 01
				    Arrays.asList("MO", "09:00", "3", "2") // cpsc 567 tut 01

				);
		
		 // should return false as cpsc 433 lec 01 and cpsc 567 lec 01 have been assigned same slot
		assertFalse(Scheduler.getConstr().checkNotCompatible(coursesList, labsList));
	}
	
	
	@Test
	public void testNotCompatibleTrue() { // Good input test
		
		/* in the parsed info, we're given the following for non-compatible:
		CPSC 433 LEC 01 TUT 01, CPSC 433 LEC 02 LAB 02
		CPSC 567 LEC 01, CPSC 433 LEC 01
		CPSC 567 LEC 01, CPSC 433 LEC 02
		CPSC 567 TUT 01, CPSC 433 LEC 02
		CPSC 433 LEC 01, CPSC 567 TUT 01
		*/
		
		// lets try to NOT force any classes into the same slot
		
		// for example, I'm going to try to force CPSC 433 LEC 01 and CPSC 567 LEC 01 in the same slot
		List<List<String>> coursesList = Arrays.asList(
			    Arrays.asList("MO", "11:00", "3", "2"), // cpsc 433 lec 01 
			    Arrays.asList("TU", "09:00", "3", "2"), // cpsc 433 lec 02
			    Arrays.asList("MO", "08:00", "3", "2"), // seng 311 lec 01
			    Arrays.asList("MO", "10:00", "3", "2") // cpsc 567 lec 01 
			);
		
		 List<List<String>> labsList = Arrays.asList( 
				    Arrays.asList("MO", "12:00", "4", "2"), // cpsc 433 lec 01 tut 01
				    Arrays.asList("TU", "10:00", "3", "2"), // cpsc 433 lec 02 lab 02
				    Arrays.asList("MO", "1:00", "3", "2"), // seng 311 lec 01 tut 01
				    Arrays.asList("MO", "09:00", "3", "2") // cpsc 567 tut 01

				);
		
		 // should return TRUE as NO uncompatible courses/labs have been assigned same slot
		assertTrue(Scheduler.getConstr().checkNotCompatible(coursesList, labsList));
	}
	

	@Test
	public void doUnwantedCheckFalse() { // Bad input test. The input for your system can contain a list of unwanted(a,s) statements, with a in Courses + Labs and s in Slots. For each of those, assign(a) has to be unequal to s.
		/* in the parsed info, we're given the following for unwanted assignments:
		 * 
		Unwanted:
		CPSC 433 LEC 01, MO, 8:00
		*/
		
		/* As a reminder: Here is the course list:
		 * CPSC 433 LEC 01
			CPSC 433 LEC 02
			SENG 311  LEC  01
			CPSC 567 LEC 01

		 * Lab list:
		 * CPSC 433 LEC 01 TUT 01
			CPSC 433 LEC  02 LAB   02
			SENG 311 LEC 01 TUT 01
			CPSC 567 TUT 01
		 * 
		 * 
		 */
		
		// note that we will give cpsc 433 the assignment of lec 01 MON, 08:00 to make it return false
			List<List<String>> coursesList = Arrays.asList(
					Arrays.asList("MO", "8:00", "2", "2") // cpsc 433 lec 01 (UNWANTED ASSIGNMENT)
					);
				
				 List<List<String>> labsList = Arrays.asList( 
						// does not really matter, just need something to input.
					Arrays.asList("MO", "08:00", "2", "2") // cpsc 433 lec 01 tut 01
						);
				 
		 assertFalse(Scheduler.getConstr().checkUnwanted(coursesList, labsList)); // should return false as we have a unwanted assignment	
	}
	
	
	
	@Test
	public void doUnwantedCheckTrue() { // Good input test case. Input for your system can contain a list of unwanted(a,s) statements, with a in Courses + Labs and s in Slots. For each of those, assign(a) has to be unequal to s.
		/* in the parsed info, we're given the following for unwanted assignments:
		 * 
		Unwanted:
		CPSC 433 LEC 01, MO, 8:00
		*/
		
		
		// We will NOT give cpsc 433 the assignment of lec 01 MON, 08:00, and it will return true.
			List<List<String>> coursesList = Arrays.asList(
					Arrays.asList("MO", "10:00", "2", "2") // cpsc 433 lec 01 
					);
				
				 List<List<String>> labsList = Arrays.asList( 
					// does not really matter, just need something to input.
					Arrays.asList("MO", "08:00", "2", "2") // cpsc 433 lec 01 tut 01
						);
				 
		 assertTrue(Scheduler.getConstr().checkUnwanted(coursesList, labsList)); // should return TRUE as we have NO UNWANTED ASSIGNMENTS.
}

	// REFER TO TIFFANY BRANCH FOR TEST ON THIS.
public void doPartialAssignmentPassCheck() { //if partial assignment in input, the assignment assign your system produces has to fulfill the condition: assign(a) = partassign(a) for all a in Courses + Labs with partassign(a) not equal to $.
		/* in the parsed info, we're given the following for partial assignment:
		Partial assignments:
		SENG 311 LEC 01, MO, 8:00
		SENG 311 LEC 01 TUT 01, FR, 10:00
		*/
		// SENG 311 is the third course in the couses list, same for labs
		
		// the partial assignments above should be satisfied by being put into the slots below
		
		List<List<String>> coursesList = Arrays.asList(
				Arrays.asList("MO", "9:00", "3", "2"),
				Arrays.asList("TU", "9:30", "2", "1"),
				Arrays.asList("MO", "8:00", "3", "2"),
				Arrays.asList("TU", "9:30", "2", "1")
			);
		
		 List<List<String>> labsList = Arrays.asList( 
				    Arrays.asList("TU", "10:00", "2", "1"),
					Arrays.asList("MO", "8:00", "4", "2"),
					Arrays.asList("FR", "10:00", "2", "1"),
					Arrays.asList("TU", "10:00", "2", "1")
			);
		
		
		 assertTrue(Scheduler.getConstr().checkPartialAssignment(coursesList, labsList));
	}
	
	@Test
	public void doPartialAssignmentFailCheck() { //if partial assignment in input, the assignment assign your system produces has to fulfill the condition: assign(a) = partassign(a) for all a in Courses + Labs with partassign(a) not equal to $.
		/* in the parsed info, we're given the following for partial assignment:
		Partial assignments:
		SENG 311 LEC 01, MO, 8:00
		SENG 311 LEC 01 TUT 01, FR, 10:00
		*/
		// SENG 311 is the third course in the couses list, same for labs
		
		// the partial assignments above should be satisfied by being put into the slots below
		
		List<List<String>> coursesList = Arrays.asList(
				Arrays.asList("MO", "9:00", "3", "2"),
				Arrays.asList("TU", "9:30", "2", "1"),
				Arrays.asList("MO", "9:00", "3", "2"),
				Arrays.asList("TU", "9:30", "2", "1")
			);
		
		 List<List<String>> labsList = Arrays.asList( 
				    Arrays.asList("TU", "10:00", "2", "1"),
					Arrays.asList("MO", "8:00", "4", "2"),
					Arrays.asList("FR", "10:00", "2", "1"),
					Arrays.asList("TU", "10:00", "2", "1")
			);
		
		
		 assertFalse(Scheduler.getConstr().checkPartialAssignment(coursesList, labsList));
	}
	
	
		/*
	//DEPARTMENTAL HARD CONSTRAINTS (refer to TIFFANY BRANCH.)
	
	@Test
	public void testMondayCourse() { // If a course (course section) is put into a slot on mondays, it has to be put into the corresponding time slots on wednesdays and fridays. So, these three time slots are treated as one abstract slot, which allows us to see our Department problem as an instantiation of the general problem!
		fail("Not yet implemented");
	}
	
	@Test
	public void testTuesdayCourse() { // If a course (course section) is put into a slot on tuesdays, it has to be put into the corresponding time slots on thursdays.
		fail("Not yet implemented");
	}
	
	@Test
	public void testMondayLab() { // If a lab/tutorial is put into a slot on mondays, it has to be put into the corresponding time slots on wednesdays.
		fail("Not yet implemented");
	}
	
	@Test 
	public void testTuesdayLab() { // If a lab/tutorial is put into a slot on tuesdays, it has to be put into the corresponding time slots on thursdays.
		fail("Not yet implemented");
	}
	*/
	@Test
	public void testLec9() { // All course sections with a section number starting LEC 9 are evening classes and have to be scheduled into evening slots.
		
		
		//fail("Not yet implemented");
	}
	
	@Test
	public void test500Level() { // All courses (course sections) on the 500-level have to be scheduled into different time slots.
		fail("Not yet implemented");
	}
	
	@Test
	public void testTuesdayNoCourses() { // No courses can be scheduled at tuesdays 11:00-12:30.
		fail("Not yet implemented");
	}
	
	@Test
	public void testSpecialCourses() { // There are two special "courses" CPSC 813 and CPSC 913 that have to be scheduled tuesdays/thursdays 18:00-19:00 and CPSC 813 is not allowed to overlap with any labs/tutorials of CPSC 313 or with any course section of CPSC 313 (and transitively with any other courses that are not allowed to overlap with CPSC 313) and CPSC 913 is not allowed to overlap with any labs/tutorials of CPSC 413 or with any course section of CPSC 413 (and transitively with any other courses that are not allowed to overlap with CPSC 413).
		fail("Not yet implemented");
	}
	


}



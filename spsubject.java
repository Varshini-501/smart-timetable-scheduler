import java.util.*;

class WeeklySchedule {
    static int hoursPerDay, numberOfDays, M;
    static String EMPTY = ".";
    static String[] THEORY;
    static String[] LABS;
    static Map<String, Integer> THEORY_HOURS = new HashMap<>();
    static Map<String, Integer> LAB_LEN = new HashMap<>();
    static Map<String, String> LAB_RULE = new HashMap<>();
    static String[][][] matrices;
    static Map<String, Set<String>> globalPositions = new HashMap<>();
    static Random rand = new Random(); 
    static Map<String, Integer> arr[] ;// randomness

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Input
        System.out.print("Enter number of hours per day: ");
        hoursPerDay = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter number of days in a week: ");
        numberOfDays = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter number of schedules (matrices): ");
        M = sc.nextInt();
        sc.nextLine();
          arr=new HashMap[M] ;
        for(int i=0;i<M;i++){
         arr[i]=new HashMap<>();
         
        }
          

        // Theory
        System.out.print("Enter theory subjects (comma-separated): ");
        THEORY = sc.nextLine().trim().toUpperCase().split("\\s*,\\s*");
        for (String t : THEORY) {
            System.out.print("Enter number of hours for " + t + ": ");
            int hours = sc.nextInt();
            sc.nextLine();
            THEORY_HOURS.put(t, hours);
            for(int i=0;i<4;i++){
            arr[i].put(t, 0);
            }
            globalPositions.put(t, new HashSet<>());
        }

        // Labs
        System.out.print("Enter lab subjects (comma-separated): ");
        LABS = sc.nextLine().trim().toUpperCase().split("\\s*,\\s*");
        for (String lab : LABS) {
            System.out.print("Enter number of hours for lab " + lab + ": ");
            int len = sc.nextInt();
            sc.nextLine();
            LAB_LEN.put(lab, len);

            System.out.print("Enter placement rule for " + lab + " (TOP4 / LAST2 / TOP4_ONLY): ");
            String rule = sc.nextLine().trim().toUpperCase();
            LAB_RULE.put(lab, rule);

            globalPositions.put(lab, new HashSet<>());
        }
        
        
        // Initialize
        matrices = new String[M][hoursPerDay][numberOfDays];
        for (int m = 0; m < M; m++)
            clearMatrix(m);
            
        //New Section Specific subjects insertion
        while(true){
        
               //ask the user wants to insert another subject
               System.out.println("\nDo you want insert a subject? (yes/no)");
               String response = sc.nextLine().trim().toLowerCase();
               if (!response.equals("yes")){
                    break;
               } 
        
               System.out.print("Enter the Subject name to be inserted:");
               String sub= sc.nextLine().trim().toUpperCase();
               //subject should match with subjects in String[] THEORY
               if(!THEORY_HOURS.containsKey(sub)){
                     System.out.println("Enter the Subjects which are listed intially \n");
               }
               else{
                  System.out.print("Enter the class(Schedule) number to place the subject: ");
                  int classNumber=sc.nextInt()-1;
                  sc.nextLine();
         
                   if(classNumber<0 || classNumber>=M){
                        System.out.println("Invalid class(schedule) number.please try again\n ");
                   }
                   else{
                      System.out.print("Enter the Specific Day(1-"+numberOfDays+") and Hour(1-"+hoursPerDay+") to place the subject : ");
                       int sDay=sc.nextInt()-1;
                       int sHour=sc.nextInt()-1;
                       sc.nextLine();
        
                          if (sDay < 0 || sDay >= numberOfDays || sHour < 0 || sHour >= hoursPerDay ) {
                          System.out.println("Invalid day or hour for the slot to insert\n");
                          }
                         else{
                             String sKey= sHour + "-" + sDay;
                             
                              if (globalPositions.get(sub).contains(sKey)) {
                              System.out.println("Slot is not empty. Please try another slot\n");
                             } 
                              else if (arr[classNumber].get(sub)>=THEORY_HOURS.get(sub)) {
                                   System.out.println("Maximum slots for the subject " + sub + " are already placed.");
                              }
                              else {
                                   matrices[classNumber][sHour][sDay] = sub;
                                    arr[classNumber].put(sub,arr[classNumber].get(sub)+1);
                                    globalPositions.get(sub).add(sKey);
                                    System.out.println("Successfully placed subject " + sub);
                               }
                               
                        }
                 }
            }
      }

        // Fill using randomized backtracking
        for (int m = 0; m < M; m++) {
            if (!fillMatrix(m)) {
                System.out.println("Could not fill schedule " + (m + 1));
            }
        }

        printMatrices();

        // New section for swapping
        while (true) {
            System.out.println("\nDo you want to modify a schedule? (yes/no)");
            String response = sc.nextLine().trim().toLowerCase();
            if (!response.equals("yes")) {
                break;
            }

            System.out.println("Choose an action:");
            System.out.println("1. Swap a subject with an empty slot");
            System.out.println("2. Swap two subjects");
            System.out.print("Enter your choice (1 or 2): ");
            int choice = sc.nextInt();
            sc.nextLine();

            System.out.print("Enter the schedule number (1-" + M + ") you want to modify: ");
            int scheduleNumber = sc.nextInt();
            sc.nextLine();

            if (scheduleNumber < 1 || scheduleNumber > M) {
                System.out.println("Invalid schedule number. Please try again.");
                continue;
            }

            if (choice == 1) {
                swapSubjectWithEmptySlot(sc, scheduleNumber - 1);
            } else if (choice == 2) {
                swapTwoSubjects(sc, scheduleNumber - 1);
            } else {
                System.out.println("Invalid choice.");
            }
        }
        sc.close();
    }

    static void clearMatrix(int m) {
        for (int i = 0; i < hoursPerDay; i++)
            for (int j = 0; j < numberOfDays; j++)
                matrices[m][i][j] = EMPTY;
    }

    static boolean fillMatrix(int m) {
        return placeLabs(m, 0);
    }

    // Backtracking for labs (with shuffling)
    static boolean placeLabs(int m, int idx) {
        if (idx == LABS.length)
            return placeTheories(m, 0, arr[m]);
        String lab = LABS[idx];
        int len = LAB_LEN.get(lab);
        String rule = LAB_RULE.get(lab);

        List<int[]> candidates = new ArrayList<>();
        for (int col = 0; col < numberOfDays; col++) {
            for (int row = 0; row <= hoursPerDay - len; row++) {
                if (rule.equals("TOP4_ONLY") && row > 3 - (len - 1))
                    continue;
                if (rule.equals("TOP4") && row > 3 - (len - 1))
                    continue;
                if (rule.equals("LAST2") && row != hoursPerDay - len)
                    continue;
                if (canPlaceLab(m, row, col, len, lab)) {
                    candidates.add(new int[] { row, col });
                }
            }
        }

        Collections.shuffle(candidates, rand); // shuffle candidates

        for (int[] pos : candidates) {
            placeLabAt(m, lab, pos[0], pos[1], len);
            if (placeLabs(m, idx + 1))
                return true;
            removeLabAt(m, lab, pos[0], pos[1], len);
        }
        return false;
    }

    static void placeLabAt(int m, String lab, int row, int col, int len) {
        for (int k = 0; k < len; k++) {
            matrices[m][row + k][col] = lab;
            globalPositions.get(lab).add((row + k) + "-" + col);
        }
    }

    static void removeLabAt(int m, String lab, int row, int col, int len) {
        for (int k = 0; k < len; k++) {
            matrices[m][row + k][col] = EMPTY;
            globalPositions.get(lab).remove((row + k) + "-" + col);
        }
    }

    static boolean canPlaceLab(int m, int row, int col, int len, String lab) {
        for (int k = 0; k < len; k++) {
            if (!matrices[m][row + k][col].equals(EMPTY))
                return false;
            if (globalPositions.get(lab).contains((row + k) + "-" + col))
                return false;
        }
        return true;
    }

    // Backtracking for theories (with shuffling)
    static boolean placeTheories(int m, int idx, Map<String, Integer> count) {
        if (idx == hoursPerDay * numberOfDays) {
            for (String t : THEORY) {
                if (count.getOrDefault(t, 0) != THEORY_HOURS.get(t))
                    return false;
            }
            return true;
        }

        int row = idx / numberOfDays;
        int col = idx % numberOfDays;

        if (!matrices[m][row][col].equals(EMPTY))
            return placeTheories(m, idx + 1, count);

        List<String> subjects = new ArrayList<>(Arrays.asList(THEORY));
        Collections.shuffle(subjects, rand); // shuffle order

        for (String t : subjects) {
            String posKey = row + "-" + col;
            if (count.getOrDefault(t, 0) < THEORY_HOURS.get(t) &&
                    !globalPositions.get(t).contains(posKey) &&
                    !columnContains(m, col, t)) {

                matrices[m][row][col] = t;
                globalPositions.get(t).add(posKey);
                count.put(t, count.getOrDefault(t, 0) + 1);

                if (placeTheories(m, idx + 1, count))
                    return true;

                // backtrack
                matrices[m][row][col] = EMPTY;
                globalPositions.get(t).remove(posKey);
                count.put(t, count.get(t) - 1);
            }
        }

        return placeTheories(m, idx + 1, count);
    }

    static boolean columnContains(int m, int col, String subject) {
        for (int row = 0; row < hoursPerDay; row++) {
            if (matrices[m][row][col].equals(subject))
                return true;
        }
        return false;
    }

    static void printMatrices() {
        for (int m = 0; m < M; m++) {
            System.out.println("Schedule " + (m + 1));
            for (int i = 0; i < hoursPerDay; i++) {
                for (int j = 0; j < numberOfDays; j++)
                    System.out.printf("%-10s", matrices[m][i][j]);
                System.out.println();
            }
            System.out.println();
        }
    }

    static void swapSubjectWithEmptySlot(Scanner sc, int m) {
        System.out.print("Enter the day (1-" + numberOfDays + ") of the subject you want to swap: ");
        int dayToSwap = sc.nextInt() - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the subject: ");
        int hourToSwap = sc.nextInt() - 1;
        sc.nextLine(); // Consume newline

        if (dayToSwap < 0 || dayToSwap >= numberOfDays || hourToSwap < 0 || hourToSwap >= hoursPerDay) {
            System.out.println("Invalid day or hour. Please try again.");
            return;
        }

        String subjectToSwap = matrices[m][hourToSwap][dayToSwap];
        if (subjectToSwap.equals(EMPTY)) {
            System.out.println("The selected slot is empty. Cannot perform swap.");
            return;
        }

        // Find and list all empty slots
        List<int[]> emptySlots = new ArrayList<>();
        for (int j = 0; j < numberOfDays; j++) {
            for (int i = 0; i < hoursPerDay; i++) {
                String newPosKey = i + "-" + j;
                // Check if the slot is empty AND not occupied globally by the same subject
                if (matrices[m][i][j].equals(EMPTY) && !globalPositions.get(subjectToSwap).contains(newPosKey)) {
                    // Validate if the lab can be placed in this empty slot
                    if (isLab(subjectToSwap)) {
                        String rule = LAB_RULE.get(subjectToSwap);
                        int len = LAB_LEN.get(subjectToSwap);
                        if ((rule.equals("TOP4_ONLY") || rule.equals("TOP4")) && (i > 3 - (len - 1))) {
                            continue;
                        }
                        if (rule.equals("LAST2") && i != hoursPerDay - len) {
                            continue;
                        }
                    }
                    emptySlots.add(new int[] { i, j });
                }
            }
        }

        if (emptySlots.isEmpty()) {
            System.out.println("No available empty slot found to perform the swap that satisfies global constraints.");
            return;
        }

        // Present empty slots to the user
        System.out.println("Available empty slots to swap with:");
        for (int i = 0; i < emptySlots.size(); i++) {
            int row = emptySlots.get(i)[0];
            int col = emptySlots.get(i)[1];
            System.out.println(" " + (i + 1) + ". Day " + (col + 1) + ", Hour " + (row + 1));
        }

        System.out.print("Choose a slot to swap with (enter number): ");
        int choice = sc.nextInt();
        sc.nextLine(); // Consume newline

        if (choice < 1 || choice > emptySlots.size()) {
            System.out.println("Invalid choice. Swap cancelled.");
            return;
        }

        int[] chosenSlot = emptySlots.get(choice - 1);
        int emptyRow = chosenSlot[0];
        int emptyCol = chosenSlot[1];

        // Perform the swap
        String oldPosKey = hourToSwap + "-" + dayToSwap;
        String newPosKey = emptyRow + "-" + emptyCol;

        // Update the matrices
        matrices[m][emptyRow][emptyCol] = subjectToSwap;
        matrices[m][hourToSwap][dayToSwap] = EMPTY;

        // Update global positions
        globalPositions.get(subjectToSwap).remove(oldPosKey);
        globalPositions.get(subjectToSwap).add(newPosKey);
        
        System.out.println("Swap successful! " + subjectToSwap + " moved from (Day " + (dayToSwap + 1) + ", Hour " + (hourToSwap + 1) + ") to (Day " + (emptyCol + 1) + ", Hour " + (emptyRow + 1) + ").");
        System.out.println("Updated Schedule " + (m + 1) + ":");
        printMatrices();
    }
    
    // New method to swap two subjects
    static void swapTwoSubjects(Scanner sc, int m) {
        // Get details for the first subject to swap
        System.out.print("Enter the day (1-" + numberOfDays + ") of the first subject: ");
        int day1 = sc.nextInt() - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the first subject: ");
        int hour1 = sc.nextInt() - 1;
        sc.nextLine();

        // Get details for the second subject to swap
        System.out.print("Enter the day (1-" + numberOfDays + ") of the second subject: ");
        int day2 = sc.nextInt() - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the second subject: ");
        int hour2 = sc.nextInt() - 1;
        sc.nextLine();

        // Validate the input slots
        if (day1 < 0 || day1 >= numberOfDays || hour1 < 0 || hour1 >= hoursPerDay ||
            day2 < 0 || day2 >= numberOfDays || hour2 < 0 || hour2 >= hoursPerDay) {
            System.out.println("Invalid day or hour for one of the slots. Swap cancelled.");
            return;
        }

        // Get subjects from the slots
        String subject1 = matrices[m][hour1][day1];
        String subject2 = matrices[m][hour2][day2];

        // Ensure both slots are occupied
        if (subject1.equals(EMPTY) || subject2.equals(EMPTY)) {
            System.out.println("One or both of the selected slots are empty. Cannot perform a subject swap.");
            return;
        }

        // Check for conflicts before swapping
        if (!canSwap(m, subject1, hour1, day1, subject2, hour2, day2)) {
            System.out.println("Swap is not possible due to a conflict with an existing class.");
            return;
        }

        // Perform the swap
        matrices[m][hour1][day1] = subject2;
        matrices[m][hour2][day2] = subject1;

        // Update global positions
        // Remove old positions
        globalPositions.get(subject1).remove(hour1 + "-" + day1);
        globalPositions.get(subject2).remove(hour2 + "-" + day2);
        
        // Add new positions
        globalPositions.get(subject1).add(hour2 + "-" + day2);
        globalPositions.get(subject2).add(hour1 + "-" + day1);

        System.out.println("Swap successful!");
        System.out.println("Updated Schedule " + (m + 1) + ":");
        printMatrices();
    }
    
    static boolean canSwap(int m, String subject1, int hour1, int day1, String subject2, int hour2, int day2) {
        // Ensure subject types are compatible (lab with lab, theory with theory)
        if (isLab(subject1) != isLab(subject2)) {
            return false;
        }

        // Check if subject1 is a multi-hour lab
        if (isLab(subject1)) {
            int len = LAB_LEN.get(subject1);
            String rule = LAB_RULE.get(subject1);
            if ((rule.equals("TOP4_ONLY") || rule.equals("TOP4")) && (hour2 > 3 - (len - 1))) {
                return false;
            }
            if (rule.equals("LAST2") && hour2 != hoursPerDay - len) {
                return false;
            }
        }

        // Check if subject2 is a multi-hour lab
        if (isLab(subject2)) {
            int len = LAB_LEN.get(subject2);
            String rule = LAB_RULE.get(subject2);
            if ((rule.equals("TOP4_ONLY") || rule.equals("TOP4")) && (hour1 > 3 - (len - 1))) {
                return false;
            }
            if (rule.equals("LAST2") && hour1 != hoursPerDay - len) {
                return false;
            }
        }
        
        return true;
    }
    
    static boolean isLab(String subject) {
        for (String lab : LABS) {
            if (lab.equals(subject)) {
                return true;
            }
        }
        return false;
    }
}

//6 5 4 OP,DB,CO,DE,EI,ME 3 3 3 3 2 3 OOPS,DEC,DBMS 3 TOP4 2 LAST2 3 TOP4

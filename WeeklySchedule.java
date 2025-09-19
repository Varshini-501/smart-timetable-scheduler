import java.util.*;

class WeeklySchedule {
    static int hoursPerDay, numberOfDays, M;
    static String EMPTY = ".";
    static String[] THEORY;
    static String[] LABS;
    static Map<String, Integer> THEORY_HOURS = new HashMap<>();
    static Map<String, Integer> LAB_LEN = new HashMap<>();
    static Map<String, String> LAB_RULE = new HashMap<>();

    // 🔹 Faculty-related data structures
    static Map<String, List<String>> SUBJECT_FACULTIES = new HashMap<>();
    static Map<String, Set<Integer>> FACULTY_ALLOWED_SCHEDULES = new HashMap<>();
    static Map<String, Set<String>> FACULTY_OCCUPIED = new HashMap<>();
    static Map<Integer, Map<String, String>> MATRIX_SUBJECT_FACULTY = new HashMap<>();

    static String[][][] matrices;
    
    // 🔹 Modified position tracking
    static Map<String, Set<String>> subjectFacultyPositions = new HashMap<>();
    static Map<String, Set<String>> globalLabPositions = new HashMap<>();
    
    // 🔹 Per-schedule subject count tracking
    static Map<String, Integer> arr[];
    
    static Random rand = new Random();

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

        // Initialize per-schedule tracking
        arr = new HashMap[M];
        for (int i = 0; i < M; i++) {
            arr[i] = new HashMap<>();
            MATRIX_SUBJECT_FACULTY.put(i, new HashMap<>());
        }

        // Theory
        System.out.print("Enter theory subjects (comma-separated): ");
        THEORY = sc.nextLine().trim().toUpperCase().split("\\s*,\\s*");
        for (String t : THEORY) {
            System.out.print("Enter number of hours for " + t + ": ");
            int hours = sc.nextInt();
            THEORY_HOURS.put(t, hours);
            
            // Initialize per-schedule counters
            for (int i = 0; i < M; i++) {
                arr[i].put(t, 0);
            }
            
            // 🔹 Input faculties for this subject
            sc.nextLine();
            System.out.print("Enter faculties for " + t + " (comma-separated): ");
            String[] faculties = sc.nextLine().trim().split("\\s*,\\s*");
            SUBJECT_FACULTIES.put(t, Arrays.asList(faculties));
            
            // Initialize faculty data structures
            for (String faculty : faculties) {
                if (!FACULTY_OCCUPIED.containsKey(faculty)) {
                    FACULTY_OCCUPIED.put(faculty, new HashSet<>());
                }
                if (!FACULTY_ALLOWED_SCHEDULES.containsKey(faculty)) {
                    System.out.print("Enter allowed schedules for " + faculty + " (comma-separated, 0-indexed): ");
                    String[] schedules = sc.nextLine().trim().split("\\s*,\\s*");
                    Set<Integer> allowedSchedules = new HashSet<>();
                    for (String s : schedules) {
                        allowedSchedules.add(Integer.parseInt(s));
                    }
                    FACULTY_ALLOWED_SCHEDULES.put(faculty, allowedSchedules);
                }
            }
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

            globalLabPositions.put(lab, new HashSet<>());
        }

        // Initialize matrices
        matrices = new String[M][hoursPerDay][numberOfDays];
        for (int m = 0; m < M; m++) clearMatrix(m);

        // 🔹 NEW: Fixed slot insertion section
        while (true) {
            System.out.println("\nDo you want to insert a subject at a specific slot? (yes/no)");
            String response = sc.nextLine().trim().toLowerCase();
            if (!response.equals("yes")) {
                break;
            }

            System.out.print("Enter the Subject name to be inserted: ");
            String sub = sc.nextLine().trim().toUpperCase();
            
            if (!THEORY_HOURS.containsKey(sub)) {
                System.out.println("Enter the Subjects which are listed initially\n");
                continue;
            }

            System.out.print("Enter the class(Schedule) number to place the subject: ");
            int classNumber = sc.nextInt() - 1;
            sc.nextLine();

            if (classNumber < 0 || classNumber >= M) {
                System.out.println("Invalid class(schedule) number. Please try again\n");
                continue;
            }

            System.out.print("Enter the Specific Day(1-" + numberOfDays + ") and Hour(1-" + hoursPerDay + ") to place the subject: ");
            int sDay = sc.nextInt() - 1;
            int sHour = sc.nextInt() - 1;
            sc.nextLine();

            if (sDay < 0 || sDay >= numberOfDays || sHour < 0 || sHour >= hoursPerDay) {
                System.out.println("Invalid day or hour for the slot to insert\n");
                continue;
            }

            if (!matrices[classNumber][sHour][sDay].equals(EMPTY)) {
                System.out.println("Slot is not empty. Please try another slot\n");
                continue;
            }

            if (arr[classNumber].get(sub) >= THEORY_HOURS.get(sub)) {
                System.out.println("Maximum slots for the subject " + sub + " are already placed.");
                continue;
            }

            // 🔹 Try to assign faculty for this fixed placement
            String assignedFaculty = tryAssignFaculty(classNumber, sub, sHour, sDay);
            if (assignedFaculty != null) {
                matrices[classNumber][sHour][sDay] = sub;
                arr[classNumber].put(sub, arr[classNumber].get(sub) + 1);
                
                String facultyKey = sub + ":" + assignedFaculty;
                String posKey = sHour + "-" + sDay;
                subjectFacultyPositions.computeIfAbsent(facultyKey, k -> new HashSet<>()).add(posKey);
                FACULTY_OCCUPIED.get(assignedFaculty).add(posKey);
                MATRIX_SUBJECT_FACULTY.get(classNumber).put(sub, assignedFaculty);
                
                System.out.println("Successfully placed subject " + sub + " with faculty " + assignedFaculty);
            } else {
                System.out.println("No available faculty for " + sub + " in schedule " + (classNumber + 1) + " at this time slot.");
            }
        }

        // Fill using randomized backtracking
        for (int m = 0; m < M; m++) {
            if (!fillMatrix(m)) {
                System.out.println("Could not fill schedule " + (m + 1));
            }
        }

        printMatrices();

        // 🔹 NEW: Swapping section
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

        printFacultyAssignments();
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

    static boolean placeLabs(int m, int idx) {
        if (idx == LABS.length) return placeTheories(m, 0, arr[m]);
        String lab = LABS[idx];
        int len = LAB_LEN.get(lab);
        String rule = LAB_RULE.get(lab);

        List<int[]> candidates = new ArrayList<>();
        for (int col = 0; col < numberOfDays; col++) {
            for (int row = 0; row <= hoursPerDay - len; row++) {
                if (rule.equals("TOP4_ONLY") && row > 3 - (len-1)) continue;
                if (rule.equals("TOP4") && row > 3 - (len-1)) continue;
                if (rule.equals("LAST2") && row != hoursPerDay - len) continue;
                if (canPlaceLab(m, row, col, len, lab)) {
                    candidates.add(new int[]{row, col});
                }
            }
        }

        Collections.shuffle(candidates, rand);

        for (int[] pos : candidates) {
            placeLabAt(m, lab, pos[0], pos[1], len);
            if (placeLabs(m, idx + 1)) return true;
            removeLabAt(m, lab, pos[0], pos[1], len);
        }
        return false;
    }

    static void placeLabAt(int m, String lab, int row, int col, int len) {
        for (int k = 0; k < len; k++) {
            matrices[m][row+k][col] = lab;
            globalLabPositions.get(lab).add((row+k) + "-" + col);
        }
    }
    
    static void removeLabAt(int m, String lab, int row, int col, int len) {
        for (int k = 0; k < len; k++) {
            matrices[m][row+k][col] = EMPTY;
            globalLabPositions.get(lab).remove((row+k) + "-" + col);
        }
    }

    static boolean canPlaceLab(int m, int row, int col, int len, String lab) {
        for (int k = 0; k < len; k++) {
            if (!matrices[m][row+k][col].equals(EMPTY)) return false;
            if (globalLabPositions.get(lab).contains((row+k) + "-" + col)) return false;
        }
        return true;
    }

    static boolean placeTheories(int m, int idx, Map<String,Integer> count) {
        if (idx == hoursPerDay * numberOfDays) {
            for (String t : THEORY) {
                if (count.getOrDefault(t,0) != THEORY_HOURS.get(t)) return false;
            }
            return true;
        }

        int row = idx / numberOfDays;
        int col = idx % numberOfDays;

        if (!matrices[m][row][col].equals(EMPTY))
            return placeTheories(m, idx+1, count);

        List<String> subjects = new ArrayList<>(Arrays.asList(THEORY));
        Collections.shuffle(subjects, rand);

        for (String t : subjects) {
            if (count.getOrDefault(t,0) >= THEORY_HOURS.get(t)) continue;
            if (columnContains(m, col, t)) continue;

            String assignedFaculty = tryAssignFaculty(m, t, row, col);
            if (assignedFaculty != null) {
                matrices[m][row][col] = t;
                String facultyKey = t + ":" + assignedFaculty;
                String posKey = row + "-" + col;
                
                subjectFacultyPositions.computeIfAbsent(facultyKey, k -> new HashSet<>()).add(posKey);
                FACULTY_OCCUPIED.get(assignedFaculty).add(posKey);
                MATRIX_SUBJECT_FACULTY.get(m).put(t, assignedFaculty);
                count.put(t, count.getOrDefault(t,0)+1);

                if (placeTheories(m, idx+1, count)) return true;

                // Backtrack with faculty cleanup
                matrices[m][row][col] = EMPTY;
                subjectFacultyPositions.get(facultyKey).remove(posKey);
                FACULTY_OCCUPIED.get(assignedFaculty).remove(posKey);
                MATRIX_SUBJECT_FACULTY.get(m).remove(t);
                count.put(t, count.get(t)-1);
            }
        }

        return placeTheories(m, idx+1, count);
    }

    static String tryAssignFaculty(int m, String subject, int row, int col) {
        List<String> availableFaculties = SUBJECT_FACULTIES.get(subject);
        if (availableFaculties == null) return null;

        String posKey = row + "-" + col;
        
        List<String> shuffledFaculties = new ArrayList<>(availableFaculties);
        Collections.shuffle(shuffledFaculties, rand);

        for (String faculty : shuffledFaculties) {
            if (!FACULTY_ALLOWED_SCHEDULES.get(faculty).contains(m)) continue;
            if (FACULTY_OCCUPIED.get(faculty).contains(posKey)) continue;
            
            String facultyKey = subject + ":" + faculty;
            if (subjectFacultyPositions.getOrDefault(facultyKey, new HashSet<>()).contains(posKey)) continue;
            
            return faculty;
        }
        
        return null;
    }

    static boolean columnContains(int m, int col, String subject) {
        for (int row=0; row<hoursPerDay; row++) {
            if (matrices[m][row][col].equals(subject)) return true;
        }
        return false;
    }

    // 🔹 NEW: Swap subject with empty slot
    static void swapSubjectWithEmptySlot(Scanner sc, int m) {
        System.out.print("Enter the day (1-" + numberOfDays + ") of the subject you want to swap: ");
        int dayToSwap = sc.nextInt() - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the subject: ");
        int hourToSwap = sc.nextInt() - 1;
        sc.nextLine();

        if (dayToSwap < 0 || dayToSwap >= numberOfDays || hourToSwap < 0 || hourToSwap >= hoursPerDay) {
            System.out.println("Invalid day or hour. Please try again.");
            return;
        }

        String subjectToSwap = matrices[m][hourToSwap][dayToSwap];
        if (subjectToSwap.equals(EMPTY)) {
            System.out.println("The selected slot is empty. Cannot perform swap.");
            return;
        }

        // Find all empty slots compatible with faculty constraints
        List<int[]> emptySlots = new ArrayList<>();
        String currentFaculty = MATRIX_SUBJECT_FACULTY.get(m).get(subjectToSwap);
        
        for (int j = 0; j < numberOfDays; j++) {
            for (int i = 0; i < hoursPerDay; i++) {
                if (!matrices[m][i][j].equals(EMPTY)) continue;
                
                String newPosKey = i + "-" + j;
                // Check faculty availability at new slot
                if (FACULTY_OCCUPIED.get(currentFaculty).contains(newPosKey)) continue;
                
                // Check global position constraints for subject-faculty combination
                String facultyKey = subjectToSwap + ":" + currentFaculty;
                if (subjectFacultyPositions.getOrDefault(facultyKey, new HashSet<>()).contains(newPosKey)) continue;
                
                // Lab placement rules
                if (isLab(subjectToSwap)) {
                    String rule = LAB_RULE.get(subjectToSwap);
                    int len = LAB_LEN.get(subjectToSwap);
                    if ((rule.equals("TOP4_ONLY") || rule.equals("TOP4")) && (i > 3 - (len - 1))) continue;
                    if (rule.equals("LAST2") && i != hoursPerDay - len) continue;
                }
                
                emptySlots.add(new int[]{i, j});
            }
        }

        if (emptySlots.isEmpty()) {
            System.out.println("No available empty slot found to perform the swap that satisfies faculty and global constraints.");
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
        sc.nextLine();

        if (choice < 1 || choice > emptySlots.size()) {
            System.out.println("Invalid choice. Swap cancelled.");
            return;
        }

        int[] chosenSlot = emptySlots.get(choice - 1);
        int emptyRow = chosenSlot[0];
        int emptyCol = chosenSlot[1];

        // Perform the swap with faculty tracking
        String oldPosKey = hourToSwap + "-" + dayToSwap;
        String newPosKey = emptyRow + "-" + emptyCol;
        String facultyKey = subjectToSwap + ":" + currentFaculty;

        matrices[m][emptyRow][emptyCol] = subjectToSwap;
        matrices[m][hourToSwap][dayToSwap] = EMPTY;

        // Update faculty position tracking
        subjectFacultyPositions.get(facultyKey).remove(oldPosKey);
        subjectFacultyPositions.get(facultyKey).add(newPosKey);
        FACULTY_OCCUPIED.get(currentFaculty).remove(oldPosKey);
        FACULTY_OCCUPIED.get(currentFaculty).add(newPosKey);
        
        System.out.println("Swap successful! " + subjectToSwap + " (taught by " + currentFaculty + ") moved from (Day " + (dayToSwap + 1) + ", Hour " + (hourToSwap + 1) + ") to (Day " + (emptyCol + 1) + ", Hour " + (emptyRow + 1) + ").");
        printMatrices();
    }

    // 🔹 NEW: Swap two subjects
    static void swapTwoSubjects(Scanner sc, int m) {
        System.out.print("Enter the day (1-" + numberOfDays + ") of the first subject: ");
        int day1 = sc.nextInt() - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the first subject: ");
        int hour1 = sc.nextInt() - 1;
        sc.nextLine();

        System.out.print("Enter the day (1-" + numberOfDays + ") of the second subject: ");
        int day2 = sc.nextInt() - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the second subject: ");
        int hour2 = sc.nextInt() - 1;
        sc.nextLine();

        if (day1 < 0 || day1 >= numberOfDays || hour1 < 0 || hour1 >= hoursPerDay ||
            day2 < 0 || day2 >= numberOfDays || hour2 < 0 || hour2 >= hoursPerDay) {
            System.out.println("Invalid day or hour for one of the slots. Swap cancelled.");
            return;
        }

        String subject1 = matrices[m][hour1][day1];
        String subject2 = matrices[m][hour2][day2];

        if (subject1.equals(EMPTY) || subject2.equals(EMPTY)) {
            System.out.println("One or both of the selected slots are empty. Cannot perform a subject swap.");
            return;
        }

        if (!canSwapWithFaculty(m, subject1, hour1, day1, subject2, hour2, day2)) {
            System.out.println("Swap is not possible due to faculty or placement rule conflicts.");
            return;
        }

        // Get current faculty assignments
        String faculty1 = MATRIX_SUBJECT_FACULTY.get(m).get(subject1);
        String faculty2 = MATRIX_SUBJECT_FACULTY.get(m).get(subject2);

        // Perform the swap with faculty tracking
        matrices[m][hour1][day1] = subject2;
        matrices[m][hour2][day2] = subject1;

        // Update faculty position tracking
        String posKey1 = hour1 + "-" + day1;
        String posKey2 = hour2 + "-" + day2;
        String facultyKey1 = subject1 + ":" + faculty1;
        String facultyKey2 = subject2 + ":" + faculty2;

        // Remove old positions
        subjectFacultyPositions.get(facultyKey1).remove(posKey1);
        subjectFacultyPositions.get(facultyKey2).remove(posKey2);
        FACULTY_OCCUPIED.get(faculty1).remove(posKey1);
        FACULTY_OCCUPIED.get(faculty2).remove(posKey2);

        // Add new positions
        subjectFacultyPositions.get(facultyKey1).add(posKey2);
        subjectFacultyPositions.get(facultyKey2).add(posKey1);
        FACULTY_OCCUPIED.get(faculty1).add(posKey2);
        FACULTY_OCCUPIED.get(faculty2).add(posKey1);

        System.out.println("Swap successful!");
        System.out.println(subject1 + " (taught by " + faculty1 + ") and " + subject2 + " (taught by " + faculty2 + ") have been swapped.");
        printMatrices();
    }

    static boolean canSwapWithFaculty(int m, String subject1, int hour1, int day1, String subject2, int hour2, int day2) {
        // Ensure subject types are compatible
        if (isLab(subject1) != isLab(subject2)) {
            return false;
        }

        String faculty1 = MATRIX_SUBJECT_FACULTY.get(m).get(subject1);
        String faculty2 = MATRIX_SUBJECT_FACULTY.get(m).get(subject2);
        
        String posKey1 = hour1 + "-" + day1;
        String posKey2 = hour2 + "-" + day2;

        // Check if faculties are available at swapped positions (excluding current positions)
        Set<String> faculty1Occupied = new HashSet<>(FACULTY_OCCUPIED.get(faculty1));
        Set<String> faculty2Occupied = new HashSet<>(FACULTY_OCCUPIED.get(faculty2));
        
        faculty1Occupied.remove(posKey1);
        faculty2Occupied.remove(posKey2);
        
        if (faculty1Occupied.contains(posKey2) || faculty2Occupied.contains(posKey1)) {
            return false;
        }

        // Check lab placement rules
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

    static void printMatrices() {
        for (int m=0; m<M; m++) {
            System.out.println("Schedule " + (m+1));
            for (int i=0;i<hoursPerDay;i++) {
                for (int j=0;j<numberOfDays;j++)
                    System.out.printf("%-10s",matrices[m][i][j]);
                System.out.println();
            }
            System.out.println();
        }
    }

    static void printFacultyAssignments() {
        System.out.println("=== FACULTY ASSIGNMENTS ===");
        for (int m = 0; m < M; m++) {
            System.out.println("Schedule " + (m+1) + " Faculty Assignments:");
            Map<String, String> assignments = MATRIX_SUBJECT_FACULTY.get(m);
            for (String subject : assignments.keySet()) {
                System.out.println("  " + subject + " → " + assignments.get(subject));
            }
            System.out.println();
        }

        System.out.println("Faculty Workload:");
        for (String faculty : FACULTY_OCCUPIED.keySet()) {
            System.out.println("  " + faculty + " occupied slots: " + FACULTY_OCCUPIED.get(faculty).size());
        }
    }
}

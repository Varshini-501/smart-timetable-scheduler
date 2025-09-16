import java.util.*;

class WeeklySchedule {
    static int hoursPerDay, numberOfDays, M;
    static String EMPTY = ".";
    static String[] THEORY;
    static String[] LABS;
    static Map<String, Integer> THEORY_HOURS = new HashMap<>();
    static Map<String, Integer> LAB_LEN = new HashMap<>();
    static Map<String, String> LAB_RULE = new HashMap<>();
    static Map<String, Set<String>> globalPositions = new HashMap<>();
    static Random rand = new Random();

    // New data structures for rooms
    static List<Classroom> classrooms = new ArrayList<>();
    static List<Lab> labs = new ArrayList<>();
    
    // Track permanent classroom assignments if M == numClassrooms
    static Map<Integer, String> permanentRoomAssignment = new HashMap<>();

    // Room occupancy matrix: roomId -> [day][hour] = isOccupied
    static Map<String, boolean[][]> roomOccupancy = new HashMap<>();

    static TimetableSlot[][][] matrices; 
    static boolean[][][] isFixed;

    // New: TimetableSlot class to hold full class information
    static class TimetableSlot {
        String subject;
        String roomId;

        TimetableSlot(String subject, String roomId) {
            this.subject = subject;
            this.roomId = roomId;
        }
    }
    
    static class FixedSlot {
        String subject;
        int day;
        int hour;

        FixedSlot(String subject, int day, int hour) {
            this.subject = subject;
            this.day = day;
            this.hour = hour;
        }
    }

    static class Classroom {
        String id;
        String type;
        int capacity;

        Classroom(String id, String type, int capacity) {
            this.id = id;
            this.type = type;
            this.capacity = capacity;
        }
    }

    static class Lab {
        String id;
        String type;
        int capacity;

        Lab(String id, String type, int capacity) {
            this.id = id;
            this.type = type;
            this.capacity = capacity;
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Input
        System.out.print("Enter number of hours per day: ");
        hoursPerDay = Integer.parseInt(sc.nextLine());
        System.out.print("Enter number of days in a week: ");
        numberOfDays = Integer.parseInt(sc.nextLine());
        System.out.print("Enter number of schedules (sections): ");
        M = Integer.parseInt(sc.nextLine());

        // Theory
        System.out.print("Enter theory subjects (comma-separated): ");
        THEORY = sc.nextLine().trim().toUpperCase().split("\\s*,\\s*");
        for (String t : THEORY) {
            System.out.print("Enter number of hours for " + t + ": ");
            int hours = Integer.parseInt(sc.nextLine());
            THEORY_HOURS.put(t, hours);
            globalPositions.put(t, new HashSet<>());
        }

        // Labs
        System.out.print("Enter lab subjects (comma-separated): ");
        LABS = sc.nextLine().trim().toUpperCase().split("\\s*,\\s*");
        for (String lab : LABS) {
            System.out.print("Enter number of hours for lab " + lab + ": ");
            int len = Integer.parseInt(sc.nextLine());
            LAB_LEN.put(lab, len);

            System.out.print("Enter placement rule for " + lab + " (TOP4 / LAST2 / TOP4_ONLY): ");
            String rule = sc.nextLine().trim().toUpperCase();
            LAB_RULE.put(lab, rule);

            globalPositions.put(lab, new HashSet<>());
        }
        
        // Input for classrooms
        System.out.print("Enter number of classrooms: ");
        int numClassrooms = Integer.parseInt(sc.nextLine());
        System.out.println("Format: ID|Type|Capacity (e.g., CR01|LECTURE|60)");
        for (int i = 0; i < numClassrooms; i++) {
            System.out.print("Classroom " + (i+1) + ": ");
            String[] parts = sc.nextLine().split("\\|");
            classrooms.add(new Classroom(parts[0], parts[1], Integer.parseInt(parts[2])));
            roomOccupancy.put(parts[0], new boolean[numberOfDays][hoursPerDay]);
        }

        // Input for labs
        System.out.print("Enter number of labs: ");
        int numLabs = Integer.parseInt(sc.nextLine());
        System.out.println("For lab types, please use the subject name (e.g., OOPS for OOPSLAB).");
        System.out.println("Format: ID|Type|Capacity (e.g., LAB01|COMPUTER|30)");
        for (int i = 0; i < numLabs; i++) {
            System.out.print("Lab " + (i+1) + ": ");
            String[] parts = sc.nextLine().split("\\|");
            labs.add(new Lab(parts[0], parts[1], Integer.parseInt(parts[2])));
            roomOccupancy.put(parts[0], new boolean[numberOfDays][hoursPerDay]);
        }
        
        // Check if sections == classrooms
        if (M == numClassrooms) {
            System.out.println("\nNumber of sections equals number of classrooms. Assigning one classroom per section.");
            for (int i = 0; i < M; i++) {
                permanentRoomAssignment.put(i, classrooms.get(i).id);
            }
        }
        
        // Input fixed slots
        List<FixedSlot> fixedSlots = new ArrayList<>();
        String fixedSlotResponse = "";
        do {
            System.out.print("Do you want to add a fixed slot? (yes/no): ");
            fixedSlotResponse = sc.nextLine().trim().toLowerCase();
            
            if (fixedSlotResponse.equals("yes")) {
                System.out.print("Enter the subject you want to fix: ");
                String fixedSubject = sc.nextLine().trim().toUpperCase();
                System.out.print("Enter the day (1-" + numberOfDays + ") for the fixed slot: ");
                int fixedDay = Integer.parseInt(sc.nextLine()) - 1;
                System.out.print("Enter the hour (1-" + hoursPerDay + ") for the fixed slot: ");
                int fixedHour = Integer.parseInt(sc.nextLine()) - 1;
                fixedSlots.add(new FixedSlot(fixedSubject, fixedDay, fixedHour));
            } else if (!fixedSlotResponse.equals("no")) {
                System.out.println("Invalid input. Please enter 'yes' or 'no'.");
            }
        } while (fixedSlotResponse.equals("yes"));

        int totalTheoryHours = THEORY_HOURS.values().stream().mapToInt(Integer::intValue).sum();
        int totalLabHours = LAB_LEN.values().stream().mapToInt(Integer::intValue).sum();
        int totalRequired = totalTheoryHours + totalLabHours;
        int totalAvailable = hoursPerDay * numberOfDays;
        
        System.out.println("Total hours required: " + totalRequired);
        System.out.println("Total slots available: " + totalAvailable);
        
        if (totalRequired > totalAvailable) {
            System.out.println("ERROR: Not enough slots to accommodate all subjects!");
            System.exit(1);
        }

        // Initialize matrices and fixed slot tracker
        matrices = new TimetableSlot[M][hoursPerDay][numberOfDays];
        isFixed = new boolean[M][hoursPerDay][numberOfDays];
        
        for (int m = 0; m < M; m++) {
            clearMatrix(m);
            for (FixedSlot slot : fixedSlots) {
                if (slot.day >= 0 && slot.day < numberOfDays && slot.hour >= 0 && slot.hour < hoursPerDay) {
                    matrices[m][slot.hour][slot.day] = new TimetableSlot(slot.subject, "FIXED");
                    isFixed[m][slot.hour][slot.day] = true;
                }
            }
        }

        for (int m = 0; m < M; m++) {
            System.out.println("Generating schedule " + (m + 1) + "...");
            
            for (String subject : globalPositions.keySet()) {
                globalPositions.get(subject).clear();
            }
            
            for (FixedSlot slot : fixedSlots) {
                if (globalPositions.containsKey(slot.subject)) {
                    globalPositions.get(slot.subject).add(slot.hour + "-" + slot.day);
                }
            }
            
            int attempts = 0;
            boolean success = false;
            
            while (!success && attempts < 100) {
                attempts++;
                clearMatrix(m);
                
                for (FixedSlot slot : fixedSlots) {
                    if (slot.day >= 0 && slot.day < numberOfDays && slot.hour >= 0 && slot.hour < hoursPerDay) {
                        matrices[m][slot.hour][slot.day] = new TimetableSlot(slot.subject, "FIXED");
                        isFixed[m][slot.hour][slot.day] = true;
                    }
                }
                
                if (fillMatrix(m)) {
                    success = true;
                    System.out.println("Schedule " + (m + 1) + " generated successfully! (Attempt " + attempts + ")");
                } else if (attempts % 20 == 0) {
                    System.out.println("Still trying... (Attempt " + attempts + ")");
                }
            }
            
            if (!success) {
                System.out.println("Could not generate schedule " + (m + 1) + " after " + attempts + " attempts.");
                System.out.println("This might be due to overly restrictive global constraints.");
            }
        }

        printMatrices();

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
            int choice = Integer.parseInt(sc.nextLine());

            System.out.print("Enter the schedule number (1-" + M + ") you want to modify: ");
            int scheduleNumber = Integer.parseInt(sc.nextLine());
            
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
        for (int i = 0; i < hoursPerDay; i++) {
            for (int j = 0; j < numberOfDays; j++) {
                if (!isFixed[m][i][j]) {
                    matrices[m][i][j] = null;
                }
            }
        }
    }

    static void printMatrices() {
        for (int m = 0; m < M; m++) {
            System.out.println("Schedule " + (m + 1));
            System.out.printf("%-10s", "");
            for (int j = 0; j < numberOfDays; j++) {
                System.out.printf("%-20s", "Day " + (j + 1));
            }
            System.out.println();
            
            for (int i = 0; i < hoursPerDay; i++) {
                System.out.printf("%-10s", "Hour " + (i + 1));
                for (int j = 0; j < numberOfDays; j++) {
                    if (matrices[m][i][j] != null) {
                        String display = matrices[m][i][j].subject;
                        if (matrices[m][i][j].roomId != null && !matrices[m][i][j].roomId.equals("FIXED")) {
                            display += "(" + matrices[m][i][j].roomId + ")";
                        } else if (matrices[m][i][j].roomId != null && matrices[m][i][j].roomId.equals("FIXED")) {
                             display += "(FIXED)";
                        }
                        System.out.printf("%-20s", display);
                    } else {
                        System.out.printf("%-20s", EMPTY);
                    }
                }
                System.out.println();
            }
            System.out.println();
        }
    }
    
    static boolean fillMatrix(int m) {
        if (!placeLabs(m, 0, m)) {
            return false;
        }
        
        Map<String, Integer> theoryCount = new HashMap<>();
        
        for (int i = 0; i < hoursPerDay; i++) {
            for (int j = 0; j < numberOfDays; j++) {
                if (matrices[m][i][j] != null) {
                    String subject = matrices[m][i][j].subject;
                    if (THEORY_HOURS.containsKey(subject)) {
                        theoryCount.put(subject, theoryCount.getOrDefault(subject, 0) + 1);
                    }
                }
            }
        }
        
        return placeTheories(m, theoryCount, m);
    }

    static boolean placeLabs(int m, int idx, int scheduleNum) {
        if (idx == LABS.length) {
            return true;
        }
        
        String lab = LABS[idx];
        int len = LAB_LEN.get(lab);
        String rule = LAB_RULE.get(lab);

        List<int[]> candidates = new ArrayList<>();
        for (int col = 0; col < numberOfDays; col++) {
            for (int row = 0; row <= hoursPerDay - len; row++) {
                if (rule.equals("TOP4_ONLY") && row + len > 4) {
                    continue;
                }
                if (rule.equals("TOP4") && row + len > 4) {
                    continue;
                }
                if (rule.equals("LAST2") && row != hoursPerDay - len) {
                    continue;
                }
                
                if (canPlaceLab(m, row, col, len, lab)) {
                    candidates.add(new int[] { row, col });
                }
            }
        }

        Collections.shuffle(candidates, rand);

        for (int[] pos : candidates) {
            placeLabAt(m, lab, pos[0], pos[1], len);
            if (placeLabs(m, idx + 1, scheduleNum)) {
                return true;
            }
            removeLabAt(m, lab, pos[0], pos[1], len);
        }
        
        return false;
    }

    static void placeLabAt(int m, String lab, int row, int col, int len) {
        String roomId = findSuitableLabRoom(m, lab, row, col, len);
        
        for (int k = 0; k < len; k++) {
            matrices[m][row + k][col] = new TimetableSlot(lab, roomId);
            if (globalPositions.containsKey(lab)) {
                globalPositions.get(lab).add((row + k) + "-" + col);
            }
            if (roomOccupancy.containsKey(roomId)) {
                 roomOccupancy.get(roomId)[col][row + k] = true;
            }
        }
    }

    static void removeLabAt(int m, String lab, int row, int col, int len) {
        String roomId = matrices[m][row][col].roomId;
        for (int k = 0; k < len; k++) {
            if (!isFixed[m][row + k][col]) {
                matrices[m][row + k][col] = null;
                if (roomOccupancy.containsKey(roomId)) {
                    roomOccupancy.get(roomId)[col][row + k] = false;
                }
            }
        }
        if (globalPositions.containsKey(lab)) {
            for (int k = 0; k < len; k++) {
                globalPositions.get(lab).remove((row + k) + "-" + col);
            }
        }
    }

    static boolean canPlaceLab(int m, int row, int col, int len, String lab) {
        for (int k = 0; k < len; k++) {
            if (row + k >= hoursPerDay || matrices[m][row + k][col] != null) {
                return false;
            }
            if (globalPositions.containsKey(lab) && globalPositions.get(lab).contains((row + k) + "-" + col)) {
                return false;
            }
        }
        
        String roomId = findSuitableLabRoom(m, lab, row, col, len);
        return roomId != null;
    }

    static boolean placeTheories(int m, Map<String, Integer> count, int scheduleNum) {
        List<int[]> emptySlots = new ArrayList<>();
        for (int i = 0; i < hoursPerDay; i++) {
            for (int j = 0; j < numberOfDays; j++) {
                if (matrices[m][i][j] == null) {
                    emptySlots.add(new int[]{i, j});
                }
            }
        }
        
        List<String> theoryToPlace = new ArrayList<>();
        for (String t : THEORY) {
            int needed = THEORY_HOURS.get(t) - count.getOrDefault(t, 0);
            for (int i = 0; i < needed; i++) {
                theoryToPlace.add(t);
            }
        }
        
        Collections.shuffle(theoryToPlace, rand);
        Collections.shuffle(emptySlots, rand);
        
        return placeTheoryRecursive(m, emptySlots, theoryToPlace, 0, scheduleNum);
    }
    
    static boolean placeTheoryRecursive(int m, List<int[]> emptySlots, List<String> theoryToPlace, int idx, int scheduleNum) {
        if (idx == theoryToPlace.size()) {
            return true;
        }
        
        String subject = theoryToPlace.get(idx);
        
        for (int i = 0; i < emptySlots.size(); i++) {
            int[] slot = emptySlots.get(i);
            int row = slot[0];
            int col = slot[1];
            
            if (matrices[m][row][col] == null && !columnContains(m, col, subject)) {
                String roomId = findSuitableClassroom(m, subject, row, col);
                
                if (roomId != null) {
                    matrices[m][row][col] = new TimetableSlot(subject, roomId);
                    if (roomOccupancy.containsKey(roomId)) {
                        roomOccupancy.get(roomId)[col][row] = true;
                    }
                    emptySlots.remove(i);
                    
                    if (placeTheoryRecursive(m, emptySlots, theoryToPlace, idx + 1, scheduleNum)) {
                        return true;
                    }
                    
                    matrices[m][row][col] = null;
                    if (roomOccupancy.containsKey(roomId)) {
                        roomOccupancy.get(roomId)[col][row] = false;
                    }
                    emptySlots.add(i, slot);
                }
            }
        }
        
        return false;
    }

    static boolean columnContains(int m, int col, String subject) {
        for (int row = 0; row < hoursPerDay; row++) {
            if (matrices[m][row][col] != null && matrices[m][row][col].subject.equals(subject)) {
                return true;
            }
        }
        return false;
    }

    static String findSuitableClassroom(int m, String subject, int hour, int day) {
        if (permanentRoomAssignment.containsKey(m)) {
            String roomId = permanentRoomAssignment.get(m);
            if (isRoomAvailable(roomId, day, hour)) {
                return roomId;
            }
        } else {
            for (Classroom classroom : classrooms) {
                if (isRoomAvailable(classroom.id, day, hour)) {
                    return classroom.id;
                }
            }
        }
        return null;
    }
    
    static String findSuitableLabRoom(int m, String lab, int row, int col, int len) {
        String labType = lab.replace("LAB", "").toUpperCase();
        for (Lab labRoom : labs) {
            if (labRoom.type.toUpperCase().equals(labType)) {
                boolean isAvailable = true;
                for (int k = 0; k < len; k++) {
                    if (!isRoomAvailable(labRoom.id, col, row + k)) {
                        isAvailable = false;
                        break;
                    }
                }
                if (isAvailable) {
                    return labRoom.id;
                }
            }
        }
        return null;
    }
    
    static boolean isRoomAvailable(String roomId, int day, int hour) {
        if (roomOccupancy.containsKey(roomId)) {
            return !roomOccupancy.get(roomId)[day][hour];
        }
        return false;
    }

    static void swapSubjectWithEmptySlot(Scanner sc, int m) {
        System.out.print("Enter the day (1-" + numberOfDays + ") of the subject you want to swap: ");
        int dayToSwap = Integer.parseInt(sc.nextLine()) - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the subject: ");
        int hourToSwap = Integer.parseInt(sc.nextLine()) - 1;

        if (dayToSwap < 0 || dayToSwap >= numberOfDays || hourToSwap < 0 || hourToSwap >= hoursPerDay) {
            System.out.println("Invalid day or hour. Please try again.");
            return;
        }

        TimetableSlot slotToSwap = matrices[m][hourToSwap][dayToSwap];
        if (slotToSwap == null) {
            System.out.println("The selected slot is empty. Cannot perform swap.");
            return;
        }
        
        if (isFixed[m][hourToSwap][dayToSwap]) {
            System.out.println("The selected slot is fixed and cannot be moved.");
            return;
        }
        
        String subjectToSwap = slotToSwap.subject;

        List<int[]> emptySlots = new ArrayList<>();
        for (int j = 0; j < numberOfDays; j++) {
            for (int i = 0; i < hoursPerDay; i++) {
                if (matrices[m][i][j] == null && !columnContains(m, j, subjectToSwap)) {
                    String roomId = findSuitableRoomForSwap(subjectToSwap, i, j);
                    if (roomId != null) {
                        emptySlots.add(new int[] { i, j });
                    }
                }
            }
        }

        if (emptySlots.isEmpty()) {
            System.out.println("No available empty slot found to perform the swap that satisfies constraints.");
            return;
        }

        System.out.println("Available empty slots to swap with:");
        for (int i = 0; i < emptySlots.size(); i++) {
            int row = emptySlots.get(i)[0];
            int col = emptySlots.get(i)[1];
            System.out.println(" " + (i + 1) + ". Day " + (col + 1) + ", Hour " + (row + 1));
        }

        System.out.print("Choose a slot to swap with (enter number): ");
        int choice = Integer.parseInt(sc.nextLine());

        if (choice < 1 || choice > emptySlots.size()) {
            System.out.println("Invalid choice. Swap cancelled.");
            return;
        }

        int[] chosenSlot = emptySlots.get(choice - 1);
        int emptyRow = chosenSlot[0];
        int emptyCol = chosenSlot[1];

        String newRoomId = findSuitableRoomForSwap(subjectToSwap, emptyRow, emptyCol);
        if (newRoomId == null) {
            System.out.println("Error: A suitable room could not be found for the new slot. Swap cancelled.");
            return;
        }
        
        if (roomOccupancy.containsKey(slotToSwap.roomId)) {
            roomOccupancy.get(slotToSwap.roomId)[dayToSwap][hourToSwap] = false;
        }
        if (roomOccupancy.containsKey(newRoomId)) {
            roomOccupancy.get(newRoomId)[emptyCol][emptyRow] = true;
        }
        
        matrices[m][emptyRow][emptyCol] = new TimetableSlot(subjectToSwap, newRoomId);
        matrices[m][hourToSwap][dayToSwap] = null;
        
        System.out.println("Swap successful! " + subjectToSwap + " moved from (Day " + (dayToSwap + 1) + ", Hour " + (hourToSwap + 1) + ") to (Day " + (emptyCol + 1) + ", Hour " + (emptyRow + 1) + ").");
        printMatrices();
    }
    
    static void swapTwoSubjects(Scanner sc, int m) {
        System.out.print("Enter the day (1-" + numberOfDays + ") of the first subject: ");
        int day1 = Integer.parseInt(sc.nextLine()) - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the first subject: ");
        int hour1 = Integer.parseInt(sc.nextLine()) - 1;

        System.out.print("Enter the day (1-" + numberOfDays + ") of the second subject: ");
        int day2 = Integer.parseInt(sc.nextLine()) - 1;
        System.out.print("Enter the hour (1-" + hoursPerDay + ") of the second subject: ");
        int hour2 = Integer.parseInt(sc.nextLine()) - 1;

        if (day1 < 0 || day1 >= numberOfDays || hour1 < 0 || hour1 >= hoursPerDay ||
            day2 < 0 || day2 >= numberOfDays || hour2 < 0 || hour2 >= hoursPerDay) {
            System.out.println("Invalid day or hour for one of the slots. Swap cancelled.");
            return;
        }

        TimetableSlot slot1 = matrices[m][hour1][day1];
        TimetableSlot slot2 = matrices[m][hour2][day2];
        
        if (slot1 == null || slot2 == null) {
            System.out.println("One or both of the selected slots are empty. Cannot perform a subject swap.");
            return;
        }
        
        if (isFixed[m][hour1][day1] || isFixed[m][hour2][day2]) {
            System.out.println("One or both of the selected slots are fixed and cannot be moved.");
            return;
        }

        String subject1 = slot1.subject;
        String subject2 = slot2.subject;

        if (!canSwap(m, subject1, hour1, day1, subject2, hour2, day2)) {
            System.out.println("Swap is not possible due to constraint violations.");
            return;
        }

        if (!isRoomAvailableForSwap(subject2, slot1.roomId, day1, hour1) ||
            !isRoomAvailableForSwap(subject1, slot2.roomId, day2, hour2)) {
                System.out.println("Swap is not possible due to room conflicts.");
                return;
        }

        matrices[m][hour1][day1] = slot2;
        matrices[m][hour2][day2] = slot1;
        
        if (roomOccupancy.containsKey(slot1.roomId)) {
            roomOccupancy.get(slot1.roomId)[day1][hour1] = false;
        }
        if (roomOccupancy.containsKey(slot2.roomId)) {
            roomOccupancy.get(slot2.roomId)[day2][hour2] = false;
        }
        
        if (roomOccupancy.containsKey(slot1.roomId)) {
            roomOccupancy.get(slot1.roomId)[day2][hour2] = true;
        }
        if (roomOccupancy.containsKey(slot2.roomId)) {
            roomOccupancy.get(slot2.roomId)[day1][hour1] = true;
        }

        System.out.println("Swap successful!");
        printMatrices();
    }
    
    static boolean canSwap(int m, String subject1, int hour1, int day1, String subject2, int hour2, int day2) {
        if (isLab(subject1) != isLab(subject2)) {
            return false;
        }
        
        for (int row = 0; row < hoursPerDay; row++) {
            if (row != hour2 && matrices[m][row][day2] != null && 
                matrices[m][row][day2].subject.equals(subject1)) {
                return false;
            }
        }
        
        for (int row = 0; row < hoursPerDay; row++) {
            if (row != hour1 && matrices[m][row][day1] != null && 
                matrices[m][row][day1].subject.equals(subject2)) {
                return false;
            }
        }
        
        return true;
    }

    static boolean isRoomAvailableForSwap(String subject, String roomId, int day, int hour) {
        if (!roomOccupancy.containsKey(roomId)) {
            return false;
        }
        
        if (isLab(subject)) {
            String labType = subject.replace("LAB", "").toUpperCase();
            for (Lab labRoom : labs) {
                if (labRoom.id.equals(roomId) && labRoom.type.toUpperCase().equals(labType)) {
                    return true;
                }
            }
            return false;
        } else {
            for (Classroom classroom : classrooms) {
                if (classroom.id.equals(roomId) && classroom.type.toUpperCase().equals("LECTURE")) {
                    return true;
                }
            }
            return false;
        }
    }

    static String findSuitableRoomForSwap(String subject, int hour, int day) {
        if (isLab(subject)) {
            String labType = subject.replace("LAB", "").toUpperCase();
            for (Lab labRoom : labs) {
                if (labRoom.type.toUpperCase().equals(labType) && isRoomAvailable(labRoom.id, day, hour)) {
                    return labRoom.id;
                }
            }
        } else {
            for (Classroom classroom : classrooms) {
                 if (isRoomAvailable(classroom.id, day, hour)) {
                    return classroom.id;
                }
            }
        }
        return null;
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

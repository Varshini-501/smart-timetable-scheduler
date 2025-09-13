
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
    static Random rand = new Random(); // randomness

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Input
        System.out.print("Enter number of hours per day: ");
        hoursPerDay = sc.nextInt();
        System.out.print("Enter number of days in a week: ");
        numberOfDays = sc.nextInt();
        System.out.print("Enter number of schedules (matrices): ");
        M = sc.nextInt();
        sc.nextLine();

        // Theory
        System.out.print("Enter theory subjects (comma-separated): ");
        THEORY = sc.nextLine().trim().split("\\s*,\\s*");
        for (String t : THEORY) {
            System.out.print("Enter number of hours for " + t + ": ");
            int hours = sc.nextInt(); sc.nextLine();
            THEORY_HOURS.put(t, hours);
            globalPositions.put(t, new HashSet<>());
        }

        // Labs
        System.out.print("Enter lab subjects (comma-separated): ");
        LABS = sc.nextLine().trim().split("\\s*,\\s*");
        for (String lab : LABS) {
            System.out.print("Enter number of hours for lab " + lab + ": ");
            int len = sc.nextInt(); sc.nextLine();
            LAB_LEN.put(lab, len);

            System.out.print("Enter placement rule for " + lab + " (TOP4 / LAST2 / TOP4_ONLY): ");
            String rule = sc.nextLine().trim().toUpperCase();
            LAB_RULE.put(lab, rule);

            globalPositions.put(lab, new HashSet<>());
        }

        // Initialize
        matrices = new String[M][hoursPerDay][numberOfDays];
        for (int m = 0; m < M; m++) clearMatrix(m);

        // Fill using randomized backtracking
        for (int m = 0; m < M; m++) {
            if (!fillMatrix(m)) {
                System.out.println("Could not fill schedule " + (m+1));
            }
        }

        printMatrices();
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
        if (idx == LABS.length) return placeTheories(m, 0, new HashMap<>());
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

       // Collections.shuffle(candidates, rand); // shuffle candidates

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
            globalPositions.get(lab).add((row+k) + "-" + col);
        }
    }
    
  static void removeLabAt(int m, String lab, int row, int col, int len) {
        for (int k = 0; k < len; k++)

{
            matrices[m][row+k][col] = EMPTY;
            globalPositions.get(lab).remove((row+k) + "-" + col);
        }
    }

    static boolean canPlaceLab(int m, int row, int col, int len, String lab) {
        for (int k = 0; k < len; k++) {
            if (!matrices[m][row+k][col].equals(EMPTY)) return false;
            if (globalPositions.get(lab).contains((row+k) + "-" + col)) return false;
        }
        return true;
    }

    // Backtracking for theories (with shuffling)
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
        Collections.shuffle(subjects, rand); // shuffle order

        for (String t : subjects) {
            String posKey = row + "-" + col;
            if (count.getOrDefault(t,0) < THEORY_HOURS.get(t) &&
                !globalPositions.get(t).contains(posKey) &&
                !columnContains(m, col, t)) {
                
                matrices[m][row][col] = t;
                globalPositions.get(t).add(posKey);
                count.put(t, count.getOrDefault(t,0)+1);

                if (placeTheories(m, idx+1, count)) return true;

                // backtrack
                matrices[m][row][col] = EMPTY;
                globalPositions.get(t).remove(posKey);
                count.put(t, count.get(t)-1);
            }
        }

        return placeTheories(m, idx+1, count);
    }

    static boolean columnContains(int m, int col, String subject) {
        for (int row=0; row<hoursPerDay; row++) {
            if (matrices[m][row][col].equals(subject)) return true;
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
}

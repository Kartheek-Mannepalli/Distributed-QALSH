import java.io.*;
import java.util.*;

public class Main {

    // atoi method start
    public static int myAtoi(String str) {
        String sTrim = str.trim();
        if (sTrim.length() == 0)
            return 0;

        boolean pos = true;
        if (sTrim.charAt(0) == '-')
            pos = false;

        boolean signed = false;
        if (sTrim.charAt(0) == '+' || sTrim.charAt(0) == '-')
            signed = true;

        int num = 0;

        for (int i = 0; i < sTrim.length(); i++) {
            if (signed && i == 0)
                continue;
            if (Character.isDigit(sTrim.charAt(i))) {
                if (i == 0 || (signed && i == 1))
                    num = sTrim.charAt(i) - '0';
                else {
                    if (num < 0 || num >= Integer.MAX_VALUE / 10.0) {
                        return (pos) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                    }
                    num = num * 10 + (sTrim.charAt(i) - '0');
                }
            } else
                break;
        }
        // System.out.println(num);
        if (num < 0)
            return (pos) ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        return (pos) ? num : -num;
    }
    // atoi method end

    static void usage() // display the usage of qalsh
    {
        System.out.println("\nParameters of QALSH:\n" + "    -alg  (integer)   options of algorithms (0 - 3)\n"
                + "    -d    (integer)   dimensionality of the dataset\n"
                + "    -n    (integer)   cardinality of the dataset\n" + "    -qn   (integer)   number of queries\n"
                + "    -B    (integer)   page size\n" + "    -c    (real)      approximation ratio\n"
                + "    -ds   (string)    file path of the dataset\n"
                + "    -qs   (string)    file path of the query set\n"
                + "    -ts   (string)    file path of the ground truth set\n"
                + "    -of   (string)    output folder to store info of qalsh\n\n");

        System.out.println("\n" + "The options of algorithms (-alg) are:\n" + "    0 - Ground-Truth\n"
                + "        Parameters: -alg 0 -n -qn -d -ds -qs -ts\n\n" + "    1 - Indexing\n"
                + "        Parameters: -alg 1 -n -d -B -c -ds -of\n\n" + "    2 - QALSH\n"
                + "        Parameters: -alg 2 -qn -d -qs -ts -of\n\n" + "    3 - Linear Scan\n"
                + "        Parameters: -alg 3 -n -qn -d -B -qs -ts -of\n\n");

        System.out.println("NOTE: Each parameter is required to be separated" + "by one space.\n\n\n");
    }

    // -----------------------------------------------------------------------------

    public static void main(String args[]) {
        // WE HAVE TO WRITE RANDOM SEED
        int alg = -1; // option of algorithm
        int n = -1; // cardinality
        int qn = -1; // query number
        int d = -1; // dimensionality
        int B = -1; // page size

        float ratio = -1.0f; // approximation ratio

        char[] data_set = new char[200]; // address of data set
        char[] query_set = new char[200]; // address of query set
        char[] truth_set = new char[200]; // address of ground truth file
        char[] output_folder = new char[200]; // output folder

        // Global variable start
        char[] INDEX_PATH = new char[1000];
        char[] DATA_BIN_PATH = new char[1000];
        boolean isQinDS;
        double INDEXING_TIME;
        double GT_TIME;
        // Global variable end

        boolean failed = false;
        int cnt = 1;

        // while start
        while (cnt < args.length && !failed) {
            if (args[cnt].compareTo("-alg") == 0) {
                alg = myAtoi(args[++cnt]);
                System.out.println("alg = " + alg);

                if (alg < 0 || alg > 5) {
                    failed = true;
                    break;
                }
            } else if (args[cnt].compareTo("-n") == 0) {
                n = myAtoi(args[++cnt]);
                System.out.println("n = " + n);
                if (n <= 0) {
                    failed = true;
                    break;
                }
            } else if (args[cnt].compareTo("-d") == 0) {
                d = myAtoi(args[++cnt]);
                System.out.println("d = " + d);
                if (d <= 0) {
                    failed = true;
                    break;
                }
            } else if (args[cnt].compareTo("-qn") == 0) {
                qn = myAtoi(args[++cnt]);
                System.out.println("qn = " + qn);
                if (qn <= 0) {
                    failed = true;
                    break;
                }
            } else if (args[cnt].compareTo("-B") == 0) {
                B = myAtoi(args[++cnt]);
                System.out.println("B = " + B);
                if (B <= 0) {
                    failed = true;
                    break;
                }
            } else if (args[cnt].compareTo("-c") == 0) {
                ratio = Float.parseFloat(args[++cnt]);
                System.out.printf("c = %.2f\n", ratio);
                if (ratio <= 1.0f) {
                    failed = true;
                    break;
                }
            } else if (args[cnt].compareTo("-ds") == 0) {
                // strncpy(data_set, args[++cnt], sizeof(data_set));
                // data_set = args[++cnt];
                for (int z = 0; z < args[++cnt].length(); z++) {
                    data_set[z] = args[++cnt].charAt(z);
                }
                System.out.print("dataset = ");
                for (char c : data_set) {
                    System.out.print(c);
                }
                System.out.println();

            } else if (args[cnt].compareTo("-qs") == 0) {
                // strncpy(query_set, args[++cnt], sizeof(query_set));
                // query_set = args[++cnt];
                for (int z = 0; z < args[++cnt].length(); z++) {
                    query_set[z] = args[++cnt].charAt(z);
                }
                System.out.print("query set = ");
                for (char c : query_set) {
                    System.out.print(c);
                }
                System.out.println();
            } else if (args[cnt].compareTo("-ts") == 0) {
                // strncpy(truth_set, args[++cnt], sizeof(truth_set));
                // truth_set = args[++cnt];
                for (int z = 0; z < args[++cnt].length(); z++) {
                    truth_set[z] = args[++cnt].charAt(z);
                }
                System.out.print("truth set = ");
                for (char c : truth_set) {
                    System.out.print(c);
                }
                System.out.println();
            } else if (args[cnt].compareTo("-isQinDS") == 0) {
                int temp = myAtoi(args[++cnt]);
                if (temp != 1 && temp != 0) {
                    failed = true;
                    System.out.println("isQinDS could only equal to 1 or 0.\n");
                    break;
                } else {
                    if (temp == 0)
                        isQinDS = false;
                    else
                        isQinDS = true;
                    System.out.println("isQinDS = " + isQinDS);
                }
            } else if (args[cnt].compareTo("-indexPath") == 0) {
                // strncpy(INDEX_PATH, args[++cnt], sizeof(INDEX_PATH));
                // INDEX_PATH = args[++cnt];
                for (int z = 0; z < args[++cnt].length(); z++) {
                    INDEX_PATH[z] = args[++cnt].charAt(z);
                }
                System.out.print("INDEX_PATH = ");
                for (char c : INDEX_PATH) {
                    System.out.print(c);
                }
                System.out.println();
            } else if (args[cnt].compareTo("-dataBinPath") == 0) {
                // strncpy(DATA_BIN_PATH, args[++cnt], sizeof(DATA_BIN_PATH));
                // DATA_BIN_PATH = args[++cnt];
                for (int z = 0; z < args[++cnt].length(); z++) {
                    DATA_BIN_PATH[z] = args[++cnt].charAt(z);
                }
                System.out.print("DATA_BIN_PATH = ");
                for (char c : DATA_BIN_PATH) {
                    System.out.print(c);
                }
                System.out.println();
            } else if (args[cnt].compareTo("-of") == 0) {
                // strncpy(output_folder, args[++cnt], sizeof(output_folder));
                // output_folder = args[++cnt];
                for (int z = 0; z < args[++cnt].length(); z++) {
                    output_folder[z] = args[++cnt].charAt(z);
                }
                System.out.print("output folder = ");
                for (char c : output_folder) {
                    System.out.print(c);
                }
                System.out.println();
                // ensure the path is a folder
                int len = output_folder.length;
                if (output_folder[len - 1] != '/') {
                    output_folder[len] = '/';
                    output_folder[len + 1] = '\0';
                }
            } else {
                failed = true;

                System.out.println("Command line parameters error!\n");
                usage();
                break;
            }
            cnt++;
        }

        // while end

        // omid (begin)
        INDEXING_TIME = 0;
        GT_TIME = 0;
        // omid (end)

        switch (alg) {
        case 0:
            // ground_truth(n, qn, d, data_set, query_set, truth_set);
            break;
        case 1:
            // indexing(n, d, B, ratio, data_set, output_folder);
            break;
        case 2:
            // lshknn(qn, d, query_set, truth_set, output_folder);
            break;
        case 3:
            // linear_scan(n, qn, d, B, query_set, truth_set, data_set, output_folder);
            break;
        default:
            System.out.println("Incorrect command line alg parameter!\n");
            // exit(EXIT_FAILURE);
            System.exit(1);
            usage();
            break;
        }

    }
}
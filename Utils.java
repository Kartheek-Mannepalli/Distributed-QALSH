import java.util.*;
import java.io.*;
import java.lang.Math;

public class Utils extends Global {

    // -----------------------------------------------------------------------------
    //  Utility functions
    // -----------------------------------------------------------------------------
    public int compfloats(						// compare two real values 
	    float v1,							// 1st value (type float)
	    float v2)							// 2nd value (type float)
    {
	    if (v1 - v2 < -FLOATZERO) return -1;
	    else if (v1 - v2 > FLOATZERO) return 1;
	    else return 0;
    }


    // -----------------------------------------------------------------------------
    public boolean check_mem() // check memory is enough
    {
        if (g_memory > MAXMEMORY) { // default maximum memory is 1 GB
            System.out.printf("I am going to need around %.2f MB memory.\n", (float) g_memory / (1024.0f * 1024.0f));

            System.out.println("Can you afford it (y/n)?\n");
            // char c = getchar(); // ask for more memory
            // getchar();
            Scanner myObj = new Scanner(System.in);
            char c = myObj.next().charAt(0);
            myObj.close();
            if (c != 'y' && c != 'Y') {
                return true; // fail to return
            }
        }
        return false;
    }

    // -----------------------------------------------------------------------------

    public float calc_l2_dist(					// calculate l2 distance of 2 vectors
	    float[] p1,							// 1st point
	    float[] p2,							// 2nd point
	    int dim)							// dimension
    {
	    float diff = 0.0f;
	    float ret  = 0.0f;
	    for (int i = 0; i < dim; i++) {
		    diff = p1[i] - p2[i];
		    ret += diff * diff;
	    }
	    return Math.sqrt(ret);
}


    // -----------------------------------------------------------------------------
    // Read the original dataset (or query set) from disk
    // -----------------------------------------------------------------------------
    public float[][] read_set( // read (data or query) set from disk
            int n, // number of data points
            int d, // dimensionality
            char[] set, // address of dataset
            float[][] points) { // data or queries (return)
        int i = 0, j = 0, l = 1;
        // try block
        try {
            // parsing a CSV file into BufferedReader class constructor
            FileReader fp = new FileReader(String.valueOf(set));
            BufferedReader br = new BufferedReader(fp);

            String line = "";
            while ((line = br.readLine()) != null) { // returns a Boolean value
                String[] temp = line.split(" ");
                j = 0;
                l = 1;
                // use comma as separator
                while (l < d) {
                    points[i][j] = Integer.parseInt(temp[l]);
                    j++;
                    l++;
                }
                i++;
                // System.out.println("Employee [First Name=" + employee[0] + ", Last Name=" +
                // employee[1] + ", Designation=" + employee[2] + ", Contact=" + employee[3] +
                // ", Salary= " + employee[4] + ", City= " + employee[5] +"]");
            }

        } catch (Exception e) {
            System.out.println("Exception occured: " + e);
        }
        // try or close

        if (i < n) { // check the size of set
            System.out.println("The size of set is larger than you input\n");
            System.exit(1);
        }
        return points;
    }

}
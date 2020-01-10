import java.util.*;
import java.io.*;
import java.lang.Math;

public class Utils extends Global {

    private static String OS = System.getProperty("os.name").toLowerCase();

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

    //OS VALIDATOR START
    public int osValidator() {
	
	
		if (isWindows()) {
			return 1;
		} else if (isMac()) {
			return 2;
		} else if (isUnix()) {
			return 3;
		} else if (isSolaris()) {
			return 4;
		} else {
			return 5;
		}
	
    }
    //OS VALIDATOR END

    //OS Validator utilities START
	public static boolean isWindows() {

		return (OS.indexOf("win") >= 0);

	}

	public static boolean isMac() {

		return (OS.indexOf("mac") >= 0);

	}

	public static boolean isUnix() {

		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
		
	}

	public static boolean isSolaris() {

		return (OS.indexOf("sunos") >= 0);

	}
    
    //OS Validator utilities END

    // -----------------------------------------------------------------------------
    //  Read data in new format from disk
    // -----------------------------------------------------------------------------
    public int read_data(						// read data from page
        int id,								// index of data
        int d,								// dimensionality
        int B,								// page size
        float[] data,						// real data (return)
        char[] output_path)					// output path
    {
        // -------------------------------------------------------------------------
        //  Get file name of data
        // -------------------------------------------------------------------------
        char[] fname     = new char[200];
        char[] data_path = new char[200];
        g_memory += SIZECHAR * 400;

        data_path = Arrays.copyOf(output_path);
        data_path = String.valueOf(data_path).concat("data/").toCharArray();

                                        // number of data in one data file
        int num = (int) Math.floor((float) B / (d * SIZEFLOAT));
                                        // data file id
        int file_id = (int) floor((float) id / num);

        get_data_filename(file_id, data_path, fname);

        // -------------------------------------------------------------------------
        //  Read buffer (one page of data) in new format from disk
        // -------------------------------------------------------------------------
        byte[] buffer = new byte[B];		// allocate one page size
        g_memory += SIZECHAR * B;
        for (int i = 0; i < B; i++) {
            buffer[i] = 0;
        }
        if (read_buffer_from_page(B, fname, buffer) == 1) {
            System.out.printf("read_data() error to read a page\n");
            System.exit(1);
        }

        // -------------------------------------------------------------------------
        //  Read data from buffer
        // -------------------------------------------------------------------------
        int index = id % num;
        read_data_from_buffer(index, d, data, buffer);

        if (buffer != null) {
            buffer = null;
            g_memory -= SIZECHAR * B;
        }
        if (data_path != null || fname != null) {
            data_path = null;
            fname = null;
            g_memory -= SIZECHAR * 400;
        }
        return 0;
    }


    // -----------------------------------------------------------------------------
    public int read_buffer_from_page(			// read buffer from page
        int B,								// page size
        char[] fname,						// file name of data
        byte[] buffer)						// buffer to store data
    {
        if (fname == null || buffer == null) return 1;

        RandomAccessFile fp = new RandomAccessFile(String.valueOf(fname));

        if (!fp) {
            System.out.printf("read_buffer_from_page could not open %s.\n", fname);
            return 1;					// fail to return
        }

        fp.read(buffer, 0, B);
        fp.close();
        return 0;
    }

    // -----------------------------------------------------------------------------
    public void read_data_from_buffer(			// read data from buffer
        int index,							// index of data in buffer
        int d,								// dimensionality
        float[] data,						// data set
        byte[] buffer)						// buffer to store data
    {
        int c = index * d * SIZEFLOAT;
        for (int i = 0; i < d; i++) {
            data[i] = Float.intBitsToFloat( buffer[c] ^ buffer[c+1]<<8 ^ buffer[c+2]<<16 ^ buffer[c+3]<<24 );
            //memcpy(&data[i], &buffer[c], SIZEFLOAT);
            c += SIZEFLOAT;
        }
    }


    // -----------------------------------------------------------------------------
    //  Write the data set in new format to the disk
    // -----------------------------------------------------------------------------
    public int write_data_new_form(			// write dataset with new format
        int n,								// cardinality
        int d,								// dimensionality
        int B,								// page size
        float[][] data,						// data set
        char[] output_path)					// output path
    {
                                        // number of data in one data file
        int num = (int) Math.floor((float) B / (d * SIZEFLOAT));
                                        // total number of data file
        int total_file = (int) Math.ceil((float) n / num);
        if (total_file == 0) {
            return 1;					// fail to return
        }

        // -------------------------------------------------------------------------
        //  Check whether the directory exists. If the directory does not exist, we 
        //  create the directory for each folder.
        // -------------------------------------------------------------------------
        char[] data_path = new char[200];
        g_memory += SIZECHAR * 200;

        data_path = Arrays.copyOf(output_path);
        data_path = String.valueOf(data_path).concat("data/").toCharArray();

        //if it is Linux
		if(osValidator() == 3) {
			int len = data_path.length();
			for (int i = 0; i < len; i++) {
				if (data_path[i] == '/') {
					char ch = data_path[i + 1];
					data_path[i + 1] = '\0';
									// check whether the directory exists
					File f = new File(String.valueOf(data_path));
					//int ret = access(data_path, F_OK);
					if (!f.exists() && !f.isDirectory()) {			// create directory
						
						System.out.println("Could not create directory" + data_path);
						System.out.println("write_data_new_form error");
						System.exit(2);
						
					}
					data_path[i + 1] = ch;
				}
			}
		}
        //if it is Windows
		if(utils_obj.osValidator() == 1) {
			int len = data_path.length();
			for (int i = 0; i < len; i++) {
				if (data_path[i] == '/') {
					char ch = data_path[i + 1];
					data_path[i + 1] = '\0';
									// check whether the directory exists
					File f = new File(String.valueOf(data_path));
					//int ret = access(data_path, F_OK);
					if (!f.exists() && !f.isDirectory()) {			// create directory
						
						System.out.println("Could not create directory" + data_path);
						System.out.println("QALSH::bulkload error");
						System.exit(2);
						
					}
					data_path[i + 1] = ch;
				}
			}
		}

        // -------------------------------------------------------------------------
        //  Write data of qalsh
        // -------------------------------------------------------------------------
        char[] fname = new char[200];
        byte[] buffer = new char[B];		// allocate one page size
        for (int i = 0; i < B; i++) {
            buffer[i] = 0;
        }
        g_memory += SIZECHAR * (200 + B);

        int left  = 0;
        int right = 0;
        for (int i = 0; i < total_file; i++) {
                                        // get file name of data
            get_data_filename(i, data_path, fname);

            left = i * num;
            right = left + num;
            if (right > n) right = n;	// write data to buffer
            write_data_to_buffer(d, left, right, data, buffer);

                                        // write one page of data to disk
            if (write_buffer_to_page(B, fname, buffer) == 1) {
                System.out.printf("write_data_new_form error to write a page\n");
                System.exit(1);
            }
        }

        // -------------------------------------------------------------------------
        //  Release space
        // -------------------------------------------------------------------------
        if (buffer != null) {
            buffer = null;
            g_memory -= SIZECHAR * 200;
        }
        if (data_path != null || fname != null) {
            data_path = null;
            fname = null;
            g_memory -= SIZECHAR * (200 + B);
        }
        return 0;

    }

    // -----------------------------------------------------------------------------
    public void get_data_filename(				// get file name of data
        int file_id,						// data file id
        char[] data_path,					// path to store data in new format
        char[] fname)						// file name of data (return)
    {
        char c[20];

        fname = Arrays.copyOf(data_path);
		c = String.valueOf(file_id).toCharArray();
		fname = String.valueOf(fname).concat(c).toCharArray();
		fname = String.valueOf(fname).concat(".data").toCharArray();

    }

    // -----------------------------------------------------------------------------
    public void write_data_to_buffer(			// write data to buffer
        int d,								// dimensionality
        int left,							// left data id
        int right,							// right data id
        float[][] data,						// data set
        byte[] buffer)						// buffer to store data
    {
        int c = 0;
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        for (int i = left; i < right; i++) {
            for (int j = 0; j < d; j++) {
                byteBuffer.putFloat(data[i][j]);
                //memcpy(&buffer[c], &data[i][j], SIZEFLOAT);
                c += SIZEFLOAT;
            }
        }
    }

    // -----------------------------------------------------------------------------
    public int write_buffer_to_page(			// write buffer to one page
        int B,								// page size
        char[] fname,						// file name of data
        byte[] buffer)						// buffer to store data
    {
        if (fname == null || buffer == null) return 1;

        RandomAccessFile fp = new RandomAccessFile(String.valueOf(fname));

        if (!fp) {
            System.out.printf("I could not create %s.\n", fname);
            return 1;					// fail to return
        }

        fp.write(buffer,0, B);
        fp.close()
        return 0;
    }


    /*****************************************************************
    this function gets the part of the given path up to the last folder.
    e.g, given ./ex/ex/1.zip, the function returns ./ex/ex/

    para:
    - path
    - (out) folder:

    Code from SRS
    *****************************************************************/

    public void get_leading_folder(char[] _path, char[] _folder) { // omid
        int len = _path.length();
        int pos = -1;

        for (int i = len - 1; i >= 0; i--) {
            if (_path[i] == '/') {
                pos = i;
                break;
            }
        }

        int i = 0; //modified by Yifang
        for (; i <= pos; i++)//modified by Yifang
        {
            _folder[i] = _path[i];
        }

        _folder[i] = '\0';
    }

}
import java.io.*;

public class Ann extends Global {

    // Instances of other class
    Utils utils_obj = new Utils();

    // ground_truth start
    public int ground_truth( // output the ground truth results
            int n, // number of data points
            int qn, // number of query points
            int d, // dimension of space
            char[] data_set, // address of data set
            char[] query_set, // address of query set
            char[] truth_set) {

        // current time in millis
        long start_ground_truth_time = System.currentTimeMillis();

        int i, j;

        // -------------------------------------------------------------------------
        // Read data set and query set
        // -------------------------------------------------------------------------
        // startTime = clock();
        g_memory += SIZEFLOAT * (n + qn) * d;
        if (utils_obj.check_mem())
            return 1;

        float[][] data = new float[n][d];
        // for (i = 0; i < n; i++) data[i] = new float[d];
        data = utils_obj.read_set(n, d, data_set, data);
        if (data == null) {
            System.out.println("Reading Dataset Error!\n");
            System.exit(1);
        }

        float[][] query = new float[qn][d];
        // for (i = 0; i < n; i++) data[i] = new float[d];
        query = utils_obj.read_set(qn, d, query_set, query);
        if (data == null) {
            System.out.println("Reading Queryset Error!\n");
            System.exit(1);
        }

        // -------------------------------------------------------------------------
	    //  output ground truth results (using linear scan method)
	    // -------------------------------------------------------------------------
	    int maxk = MAXK;

        if (isQinDS) // whether query is in dataset or not (omid)
            maxk++;

        float dist = -1.0F;
	    float[] knndist = new float[maxk];
	    g_memory += SIZEFLOAT * maxk;

        File file = new File(truth_set);
        file.getParentFile().mkdirs();
        FileWriter fp = new FileWriter(file);

        fp.write(String.valueOf(qn));
        fp.write(String.valueOf(maxk));


        for (i = 0; i < qn; i++) {
		    for (j = 0; j < maxk; j++) {
			    knndist[j] = MAXREAL;
		    }   
									// find k-nn points of query
		    for (j = 0; j < n; j++) {
			    dist = utils_obj.calc_l2_dist(data[j], query[i], d);

			    int ii, jj;
			    for (jj = 0; jj < maxk; jj++) {
				    if (utils_obj.compfloats(dist, knndist[jj]) == -1) {
					    break;
				    }
			    }
			    if (jj < maxk) {
				    for (ii = maxk - 1; ii >= jj + 1; ii--) {
					    knndist[ii] = knndist[ii - 1];
				    }
				    knndist[jj] = dist;
			    }
		    }

		    fp.write(String.valueOf(i+1));	// output Lp dist of k-nn points
		    for (j = 0; j < maxk; j++) {
			    fp.write(String.valueOf(knndist[j]));
		    }
		    fp.write("\n");
	    }

        long end_ground_truth_time = System.currentTimeMillis();

        long elapsedTime = end_ground_truth_time - start_ground_truth_time;
        System.out.println("*** Ground truth time:" + GT_TIME + "\n");

        return 0;

    }
    // ground_truth end

    public int indexing(						// build hash tables for the dataset
	    int   n,							// number of data points
	    int   d,							// dimension of space
	    int   B,							// page size
	    float ratio,						// approximation ratio
	    char[] data_set,						// address of data set
	    char[] output_folder)				// folder to store info of qalsh
    {

        long start_ground_truth_time = System.currentTimeMillis(); // Indexing time (omid)

        // Initialize timers (omid)
        READ_DS_TIME = 0;
        WRITE_DS_BIN_TIME = 0;
        INIT_PARAMS_TIME = 0;
        INIT_HASH_TIME = 0;
        PROJ_POINTS_TIME = 0;
        SORT_TIME = 0;
        BUILD_WRITE_TREE_TIME = 0;

	    // -------------------------------------------------------------------------
	    //  Read data set
	    // -------------------------------------------------------------------------

        g_memory += SIZEFLOAT * n * d;	
	    if (utils_obj.check_mem()) return 1;

        long read_ds_time_start = System.currentTimeMillis();
        float[][] data = new float[n][d];
	    //for (int i = 0; i < n; i++) data[i] = new float[d];
        data = utils_obj.read_set(n, d, data_set, data);
        if (data == null) {
            System.out.println("Reading Dataset Error!\n");
            System.exit(1);
        }
	    
        long read_ds_time_end = System.currentTimeMillis();
        READ_DS_TIME += read_ds_time_end - read_ds_time_start;


        long t_write_ds_bin_start = System.currentTimeMillis();

        DataOutputStream dos = new DataOutputStream(new FileOutputStream(DATA_BIN_PATH));

        for (int pid = 0; pid < n; pid++)
            for (int dim = 0; dim < d; dim++)
                dos.writeFloat(data[pid][dim]);

        long t_write_ds_bin_end = System.currentTimeMillis();
        WRITE_DS_BIN_TIME += t_write_ds_bin_end - t_write_ds_bin_start;

        Qalsh lsh = new Qalsh();
        lsh.init(n, d, B, ratio, output_folder);
	    lsh.bulkload(data);

    }
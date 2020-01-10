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
        if (query == null) {
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

        File file = new File(String.valueOf(truth_set));
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

        long start_indexing_time = System.currentTimeMillis(); // Indexing time (omid)

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

        //DataOutputStream dos = new DataOutputStream(new FileOutputStream(DATA_BIN_PATH));
        try{
            RandomAccessFile raf = new RandomAccessFile(DATA_BIN_PATH, "rw");

            for (int pid = 0; pid < n; pid++)
                for (int dim = 0; dim < d; dim++)
                    raf.writeUTF(data[pid][dim]);

            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        

        long t_write_ds_bin_end = System.currentTimeMillis();
        WRITE_DS_BIN_TIME += t_write_ds_bin_end - t_write_ds_bin_start;

        Qalsh lsh = new Qalsh();
        lsh.init(n, d, B, ratio, output_folder);
	    lsh.bulkload(data);

        long end_indexing_time = System.currentTimeMillis(); // Indexing time (omid)
        INDEXING_TIME += end_indexing_time - start_indexing_time; // indexing time (omid)

        System.out.printf("*** Indexing time:\t%0.6lf\n", INDEXING_TIME); // Indexing time (omid)

        System.out.printf("READ_DS_TIME: %.6f\n", READ_DS_TIME);
        System.out.printf("WRITE_DS_BIN_TIME: %.6f\n", WRITE_DS_BIN_TIME);
        System.out.printf("INIT_PARAMS_TIME: %.6f\n", INIT_PARAMS_TIME);
        System.out.printf("INIT_HASH_TIME: %.6f\n", INIT_HASH_TIME);
        System.out.printf("PROJ_POINTS_TIME: %.6f\n", PROJ_POINTS_TIME);
        System.out.printf("SORT_TIME: %.6f\n", SORT_TIME);
        System.out.printf("BUILD_WRITE_TREE_TIME: %.6f\n", BUILD_WRITE_TREE_TIME);
        
	    // -------------------------------------------------------------------------
	    //  Release space
	    // -------------------------------------------------------------------------
	    if (data != null) {
		    for (int i = 0; i < n; i++) {
			    data[i] = null;
		    }
		    data = null;
		    g_memory -= SIZEFLOAT * n * d;
	    }
	    if (lsh != null) {
		    lsh = null;
	    }

	    //printf("memory = %.2f MB\n", (float) g_memory / (1024.0f * 1024.0f));
	    return 0;
    }

    // -----------------------------------------------------------------------------
    public int lshknn(							// k-nn via qalsh (data in disk)
	    int   qn,							// number of query points
	    int   d,							// dimensionality
	    char[] query_set,					// path of query set
	    char[] truth_set,					// groundtrue file
	    char[] output_folder)				// output folder
    {
	    int ret = 0;
	    int maxk = MAXK;

        if (isQinDS) // whether query is in dataset or not (omid)
            maxk++;

        int i, j;
	    //FILE* fp = null;				// file pointer

	    // -------------------------------------------------------------------------
	    //  Read query set
	    // -------------------------------------------------------------------------
	    g_memory += SIZEFLOAT * qn * d;
	    float[][] query = new float[qn][d];
	    //for (i = 0; i < qn; i++) query[i] = new float[d];
	    //if (utils_obj.read_set(qn, d, query_set, query)) {
        query = utils_obj.read_set(qn, d, query_set, query);
        if (query == null) {
            System.out.println("Reading Query Set Error!\n");
            System.exit(1);
        }
	    
	    // -------------------------------------------------------------------------
	    //  Read the ground truth file
	    // -------------------------------------------------------------------------
	    g_memory += SIZEFLOAT * qn * maxk;
    //	float* R = new float[qn * maxk];

        int i = 0, j = 0, l = 1;
        // try block
        try {
            // parsing a CSV file into BufferedReader class constructor
            FileReader fp = new FileReader(String.valueOf(truth_set));
            BufferedReader br = new BufferedReader(fp);

            String line = "";
            line = br.readLine();
            String[] temp = line.split(" ");
            temp_qn = temp[0];
            maxk = temp[1];
            float[] R = new float[temp_qn * maxk];
            while ((line = br.readLine()) != null) { // returns a Boolean value
                String[] temp = line.split(" ");
                j = 0;
                l = 1;
                // use comma as separator
                while (l < maxk) {
                    R[i * maxk + j] = Float.parseFloat(temp[l]);
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

        // -------------------------------------------------------------------------
	    //  K-nearest neighbor (k-nn) search via qalsh
	    // -------------------------------------------------------------------------
	    int kNNs[] = {1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
	    int maxRound = 11;
	    int top_k = 0;

	    float allTime   = -1.0f;
	    float allRatio  = -1.0f;
	    float thisRatio = -1.0f;
	    float knnStartTime = -1.0f;
	    float knnEndTime = -1.0f;
	    float knnTime = 0.0f;

        //FILE* exact_fp = null; // omid
        int exact_results[101]; // id (omid)
        char gt_dir[100] = ""; // omid
        utils_obj.get_leading_folder(truth_set, gt_dir); // omid
        float map = -1; // omid
            								// init the results
	    g_memory += (long) (SIZEFLOAT + SIZEINT) * maxk;
	    Qalsh.ResultItem[] rslt = new ResultItem[maxk];
	    for (i = 0; i < maxk; i++) {
		    rslt[i].id_ = -1;
		    rslt[i].dist_ = MAXREAL;
	    }

	    QALSH lsh = new QALSH();		// restore QALSH
	    if (lsh.restore(output_folder)) {
	        System.out.printf("Could not restore qalsh\n");
		    System.exit(1);
	    }

        // Use QALSH output format (omid) (begin)
        System.out.printf("TOP_K\tRATIO\tmAP\tQUERY_TIME(ms)\tALG_TIME(ms)\t"
                "INDEX_IO_TIME(ms)\tINDEX_IO_SIZE(KB)\tINDEX_IO_NUM\t"
                "DATA_IO_TIME(ms)\tDATA_IO_SIZE(KB)\tDATA_IO_NUM\t"
                "DIST_CALC_TIME\t"
                "VR_ALG_TIME\tVR_IO_TIME\tVR_TIME\tVR_COUNT\n");
        // Use QALSH output format (omid) (end)

        // Excel-ready output (omid) (begin)
        char excel_output[200];
        excel_output = Arrays.copyOf(output_folder);

        if (excel_output[excel_output.length() - 1] == '/')
            excel_output = String.valueOf(excel_output).concat("excel.out").toCharArray();
        else
            excel_output = String.valueOf(excel_output).concat("/excel.out").toCharArray();

        file = new File(String.valueOf(excel_output));			// open "para" file to write
		FileWriter fp = new FileWriter(file);
		PrintWriter pw = new PrintWriter(fp);

        if (!fp) {
            System.out.printf("Can't open %s\n",excel_output);
            System.exit(1);
        }

        String ds_name = String.valueOf(query_set);
        ds_name = ds_name.substring(ds_name.lastIndexOf("/") + 1, ds_name.lastIndexOf(".") - ds_name.lastIndexOf("/") - 1);
        pw.printf("QALSH_%s\n", ds_name);

        pw.printf("TOP_K\tRATIO\tmAP\tQUERY_TIME(ms)\tALG_TIME(ms)\t"
                       "INDEX_IO_TIME(ms)\tINDEX_IO_SIZE(KB)\tINDEX_IO_NUM\t"
                       "DATA_IO_TIME(ms)\tDATA_IO_SIZE(KB)\tDATA_IO_NUM\t"
                       "DIST_CALC_TIME\t"
                       "VR_ALG_TIME\tVR_IO_TIME\tVR_TIME\tVR_COUNT\n");
        // Excel-ready output (omid) (end)

        for (int num = 0; num < maxRound; num++) {
		    int output_k = top_k = kNNs[num];

            // Fix ratio calculation if query is inside the dataset (omid)
            if (isQinDS)
                top_k++;

		    allRatio = 0.0f;
		    map = 0.0f; // omid
		    knnTime = 0;

            // Initialize timers (begin) (omid)
            QUERY_TIME = 0;
            ALG_TIME = 0;
            DIST_CALC_TIME = 0;
            INDEX_IO_TIME = 0;
            DATA_IO_TIME = 0;
            INDEX_IO_NUM = 0;
            DATA_IO_NUM = 0;
            INDEX_IO_SIZE = 0;
            DATA_IO_SIZE = 0;
            VR_ALG_TIME = 0;
            VR_IO_TIME = 0;
            VR_TIME = 0;
            VR_COUNT = 0;
            VR_FLAG = false;
            // Initialize timers (end) (omid)

            for (i = 0; i < qn; i++) {
//			knnStartTime = clock();

                long start_query_time = System.currentTimeMillis(); // Total query time (omid)

			    lsh.knn(query[i], top_k, rslt, output_folder);

                long end_query_time = System.currentTimeMillis(); // Total query time (omid)
                QUERY_TIME += end_query_time - start_query_time; // Total query time (omid)

                thisRatio = 0.0f;

                int zeroCount = 0; // omid
                for (j = 0; j < top_k; j++) {
                    // omid
                    if (R[i * maxk + j] == 0)
                        zeroCount++;
                    else
                        // omid
                        thisRatio += rslt[j].dist_ / R[i * maxk + j];
                }
    //			thisRatio /= top_k;
                thisRatio /= (top_k - zeroCount); // Keep query in the ds (omid)
                allRatio += thisRatio;

                // Calc mAP (omid) (begin)
                char temp_path[100];
                char temp_buff[50];
                temp_path = Arrays.copyOf(gt_dir);
                String x = "exact_results/" + String.valueOf(qn);
                temp_buff = x.toCharArray();
                temp_path = String.valueOf(temp_path).concat(String.valueOf(temp_buff)).toCharArray();

                try {
                    // parsing a CSV file into BufferedReader class constructor
                    FileReader fp1 = new FileReader(String.valueOf(truth_set));
                    BufferedReader br1 = new BufferedReader(fp1);
                
                    if (!fp1) {
                        System.out.printf("Could not open the file of exact results.\n");
                        System.exit(1);
                    }
                    String line1;
                    int i = 0, j = 0;
                    while ((line1 = br.readLine()) != null) { // returns a Boolean value
                        String[] temp1 = line1.split(" ");
                        for(int j = 0; j < temp1.length() ;j ++){
                            exact_results[i] = Integer.parseInt(temp1[j]);
                            i++;
                        } 
                    }
                } catch (Exception e) {
                    System.out.println("Exception occured: " + e);
                }

                float ap_score = 0.0f;
                float ap_num_hits = 0.0f;

                for (i = 0; i < top_k; i++) {
                    for (j = 0; j < top_k; j++) {
                        if (rslt[i].id_ == exact_results[j]) {
                            ap_num_hits += 1.0f;
                            ap_score += ap_num_hits / (i + 1.0f);
                        }
                    }
                }

                ap_score /= top_k;
                map += ap_score;

                br.close();
                // Calc mAP (omid) (end)
            }

            //average the times to a per query basis
		    allRatio = allRatio / qn;
            map /= qn; // omid

            VR_TIME = VR_ALG_TIME + VR_IO_TIME; // omid

            System.out.printf("%d\t%0.6lf\t%0.6lf\t%0.6lf\t%0.6lf\t"
                "%0.6lf\t%0.2lf\t%d\t"
                "%0.6lf\t%0.2lf\t%d\t"
                "%0.6lf\t"
                "%0.6lf\t%0.6lf\t%0.6lf\t%d\n",
                output_k, allRatio, map, QUERY_TIME, ALG_TIME,
                INDEX_IO_TIME, INDEX_IO_SIZE, INDEX_IO_NUM,
                DATA_IO_TIME, DATA_IO_SIZE, DATA_IO_NUM,
                DIST_CALC_TIME,
                VR_ALG_TIME, VR_IO_TIME, VR_TIME, VR_COUNT);
            
            // Excel-ready output (omid) (begin)
            pw.printf("%d\t%0.6lf\t%0.6lf\t%0.6lf\t%0.6lf\t"
                            "%0.6lf\t%0.2lf\t%d\t"
                            "%0.6lf\t%0.2lf\t%d\t"
                            "%0.6lf\t"
                            "%0.6lf\t%0.6lf\t%0.6lf\t%d\n",
                    output_k, allRatio, map, QUERY_TIME, ALG_TIME,
                    INDEX_IO_TIME, INDEX_IO_SIZE, INDEX_IO_NUM,
                    DATA_IO_TIME, DATA_IO_SIZE, DATA_IO_NUM,
                    DIST_CALC_TIME,
                    VR_ALG_TIME, VR_IO_TIME, VR_TIME, VR_COUNT);
            // Excel-ready output (omid) (end)

        }
        System.out.printf("\n");

        pw.close(); // omid
        if (lsh.write_para_to_excel(excel_output)) return 1; // omid

        // -------------------------------------------------------------------------
        //  Release space
        // -------------------------------------------------------------------------
        if (query != null) {			// release <query>
            for (i = 0; i < qn; i++) {
                query[i] = null;
            }
            query = null;
            g_memory -= SIZEFLOAT * qn * d;
        }
        if (lsh != null) {				// release <lsh>
            lsh = null;
        }
                                        // release <R> and (/or) <rslt>
        if (R != null || rslt != null) {
            R = null;
            rslt = null;
            g_memory -= (SIZEFLOAT * qn * maxk + (SIZEFLOAT + SIZEINT) * maxk);
        }

        //printf("memory = %.2f MB\n", (float) g_memory / (1024.0f * 1024.0f));
        return ret;
    }

    // -----------------------------------------------------------------------------
    public int linear_scan(					// brute-force linear scan (data in disk)
        int   n,							// number of data points
        int   qn,							// number of query points
        int   d,							// dimension of space
        int   B,							// page size
        char[] query_set,					// address of query set
        char[] truth_set,					// address of ground truth file
        char[] data_set,						// address of data set
        char[] output_folder)				// output folder
    {
        // -------------------------------------------------------------------------
        //  Allocation and initialzation.
        // -------------------------------------------------------------------------
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();

        int kNNs[] = {1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        int maxRound = 11;

        int i, j, top_k;
        int maxk = MAXK;

        if (isQinDS) // whether query is in dataset or not (omid)
            maxk++;

        float allTime   = -1.0f;
        float thisRatio = -1.0f;
        float allRatio  = -1.0f;

        g_memory += (SIZEFLOAT * (d + d + (qn + 1) * maxk) + SIZECHAR * (600 + B));
        
        float[] knn_dist = new float[maxk];
        for (i = 0; i < maxk; i++) {
            knn_dist[i] = MAXREAL;
        }

        float[][] R = new float[qn][maxk];

        float[] data     = new float[d];	// one data object
        float[] query    = new float[d];	// one query object

        byte[] buffer    = new byte[B];	// every time can read one page
        char[] fname     = new char[200];// file name for data
        char[] data_path = new char[200];// data path
        char[] out_set	= new char[200];// output file

        float[][] temp_data = new float[n][d];

        temp_data = utils_obj.read_set(n, d, data_set, temp_data);
        if (temp_data == null) {
            System.out.println("Reading Dataset Error!\n");
            System.exit(1);
        }
        
        utils_obj.write_data_new_form(n, d, B, temp_data, output_folder);

        								// open ground true file

        try {
                FileReader tfp = new FileReader(String.valueOf(truth_set));
                BufferedReader tbr = new BufferedReader(tfp);
                
                if (!tfp) {
                    System.out.printf("I could not create %s.\n", truth_set);
                    return 1;
                }
                                                        // read top-k nearest distance
                String line1;
                int i = 0, j = 0,l = 1;
                line1 = tbr.readLine();
                String temp1[] = line1.split(" ");
                qn = Integer.parseInt(temp1[0]);
                maxk = Integer.parseInt(temp1[1]);
                
                while ((line1 = tbr.readLine()) != null) { // returns a Boolean value
                    String[] temp1 = line1.split(" ");
                    j = 0;
                    l = 1;
                    // use comma as separator
                    while (l < maxk) {
                        R[i][j] = Float.parseFloat(temp1[l]);
                        j++;
                        l++;
                    }
                    i++;
                }
            } catch (Exception e) {
                System.out.println("Exception occured: " + e);
            }
        
        tfp.close();					// close ground true file

        // -------------------------------------------------------------------------
        //  Calc the number of data object in one page and the number of data file.
        //  <num> is the number of data in one data file
        //  <total_file> is the total number of data file
        // -------------------------------------------------------------------------
        int num = (int) Math.floor((float) B / (d * SIZEFLOAT));
        int total_file = (int) Math.ceil((float) n / num);
        if (total_file == 0) return 1;

        // -------------------------------------------------------------------------
        //  Brute-force linear scan method (data in disk)
        //  For each query, we limit that we can ONLY read one page of data.
        // -------------------------------------------------------------------------
        int count = 0;
        float dist = -1.0F;
                                        // generate the data path
        data_path = Arrays.copyOf(output_folder);
        data_path = String.valueOf(data_path).concat("data/").toCharArray();
        
        System.out.printf("Linear Scan Search:\n");
        System.out.printf("    Top-k\tRatio\t\tI/O\t\tTime (ms)\n");

        for (int round = 0; round < maxRound; round++) {
            top_k = kNNs[round];
            allRatio = 0.0f;

    //		startTime = clock();

            try {
                FileReader qfp = new FileReader(String.valueOf(query_set));
                BufferedReader qbr = new BufferedReader(qfp);
                
                if (!qfp) {
                    System.out.printf("Could not open the query set.\n");
                    System.exit(1);
                }

                String line1;
                int i = 0, j = 0,l = 1;
                // -----------------------------------------------------------------
                //  Step 1: read a query from disk and init the k-nn results
                // -----------------------------------------------------------------
                
                while ((line1 = qbr.readLine()) != null) { // returns a Boolean value
                    String[] temp1 = line1.split(" ");
                    j = 0;
                    l = 1;
                    // use comma as separator
                    while (l < d) {
                        query[j] = Float.parseFloat(temp1[l]);
                        j++;
                        l++;
                    }

                    for (j = 0; j < top_k; j++) {
                        knn_dist[j] = MAXREAL;
                    }

                    // -----------------------------------------------------------------
                    //  Step 2: find k-nn results for the query
                    // -----------------------------------------------------------------
                    for (j = 0; j < total_file; j++) {
                        // -------------------------------------------------------------
                        //  Step 2.1: get the file name of current data page
                        // -------------------------------------------------------------
                        utils_obj.get_data_filename(j, data_path, fname);

                        // -------------------------------------------------------------
                        //  Step 2.2: read one page of data into buffer
                        // -------------------------------------------------------------
                        if (utils_obj.read_buffer_from_page(B, fname, buffer) == 1) {
                            System.out.printf("error to read a data page\n");
                            System.exit(1);
                        }

                        // -------------------------------------------------------------
                        //  Step 2.3: find the k-nn results in this page. NOTE: the 
                        // 	number of data in the last page may be less than <num>
                        // -------------------------------------------------------------
                        if (j < total_file - 1) count = num;
                        else count = n % num;

                        for (int z = 0; z < count; z++) {
                            utils_obj.read_data_from_buffer(z, d, data, buffer);
                            dist = utils_obj.calc_l2_dist(data, query, d);

                            int ii, jj;
                            for (jj = 0; jj < top_k; jj++) {
                                if (utils_obj.compfloats(dist, knn_dist[jj]) == -1) {
                                    break;
                                }
                            }
                            if (jj < top_k) {
                                for (ii = top_k - 1; ii >= jj + 1; ii--) {
                                    knn_dist[ii] = knn_dist[ii - 1];
                                }
                                knn_dist[jj] = dist;
                            }
                        }
                    }

                    thisRatio = 0.0f;

                    int zeroCount = 0; // Keep query in the ds (omid)
                    for (j = 0; j < top_k; j++) {
                        // Keep query in the ds (omid) (begin)
                        if (R[i * maxk + j] == 0)
                            zeroCount++;
                        else
                            // Keep query in the ds (omid) (end)
                            thisRatio += knn_dist[j] / R[i][j];
                    }
        //			thisRatio /= top_k;
                    thisRatio /= (top_k - zeroCount); // Keep query in the ds (omid)

                    allRatio += thisRatio;
                    
                }
            } catch (Exception e) {
                System.out.println("Exception occured: " + e);
            }

            // -----------------------------------------------------------------
            //  Step 3: output result of top-k nn points
            // -----------------------------------------------------------------
            qfp.close();				// close query file
    //		endTime  = clock();
            allTime  = ((float) endTime - startTime);
            allTime = (allTime * 1000.0f) / qn;
            allRatio = allRatio / qn;
                                        // output results
            System.out.printf("    %3d\t\t%.4f\t\t%d\t\t%.2f\n", top_k, allRatio, 
                total_file, allTime);
    //		fprintf(ofp, "%d\t%f\t%d\t%f\n", top_k, allRatio, total_file, allTime);
        }
        System.out.printf("\n");
    //	fclose(ofp);					// close output file

        // -------------------------------------------------------------------------
        //  Release space
        // -------------------------------------------------------------------------
        if (R != null) {
            for (i = 0; i < qn; i++) {
                R[i] = null;
            }
            R = null;
        }
        if (knn_dist != null || buffer != null || data != null || query != null) {
            knn_dist = null;
            buffer = null;
            data = null;
            query = null;
        }
        if (fname != null || data_path != null || out_set != null) {
            fname = null;
            data_path = null;
            out_set = null;
        }
        g_memory -= (SIZEFLOAT * (d + d + (qn + 1) * maxk) + SIZECHAR * (600 + B));
        
        //printf("memory = %.2f MB\n", (float) g_memory / (1024.0f * 1024.0f));
        return 0;
  
    }

}
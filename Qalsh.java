import java.util.*;
import java.io.*;
import java.lang.Math;
//import java.nio.*;

// -----------------------------------------------------------------------------
//  QALSH: the hash tables of qalsh are indexed by b+ tree. QALSH is used to 
//  solve the problem of high-dimensional c-Approximate Nearest Neighbor (c-ANN)
//  search.
// -----------------------------------------------------------------------------

public class Qalsh extends Global{

	char[] index_path_ = new char[];
	int page_io_ = 0;
	int dist_io_ = 0;
	Utils utils_obj = new Utils();

	// -----------------------------------------------------------------------------
	//  ResultItem: structure of result item which is used to k-nn search
	// -----------------------------------------------------------------------------
	class ResultItem {
		int   id_;						// id of the point
		float dist_;					// l2 distance to query

		void setto(ResultItem * item) {
			id_   = item.id_;
			dist_ = item.dist_;
		}
	}

	// -----------------------------------------------------------------------------
	//  PageBuffer: buffer of a page for the ANN search of qalsh
	// -----------------------------------------------------------------------------
	class PageBuffer {
		BNode.BLeafNode leaf_node_;			// leaf node (level = 0)
		int index_pos_;					// cur pos of index key
		int leaf_pos_;					// cur pos of leaf node
		int size_;						// size for one scan
	}

	//c++ struct
	class HashValue {
	
		int id_;						// object id
		float proj_;					// projection of the object

	}

    // Instances of other class
    Random rand_obj = new Random();
    public Qalsh()
    {
        int n_pts_ = -1;
        int dim_ = -1;
        int B_ = -1;
	    float appr_ratio_ = -1.0;
        float beta_ = -1.0;
        float delta_ = -1.0;

	    float w_ = -1.0;
        float p1_ = -1.0;
        float p2_ = -1.0;
        float alpha_ = -1.0;
	    int m_ = -1;
        int l_ = -1;

	    float[] a_array_;
	    Btree[] trees_;

    }
    
    // -----------------------------------------------------------------------------
    public void init(					// init params of qalsh
	    int   n,							// number of points
	    int   d,							// dimension of space
	    int   B,							// page size
	    float ratio,						// approximation ratio
	    char[] output_folder)				// folder of info of qalsh
    {

        n_pts_ = n;						// init <n_pts_>
	    dim_   = d;						// init <dim_>
	    B_     = B;						// init <B_>
	    appr_ratio_ = ratio;			// init <appr_ratio_>

        
        index_path_ = Arrays.copyOf(INDEX_PATH);
        index_path_ = String.valueOf(index_path_).concat("indexes/").toCharArray();

        long t_init_params_start = System.currentTimeMillis();
        calc_params();
        long t_init_params_end = System.currentTimeMillis();
        INIT_PARAMS_TIME += t_init_params_end - t_init_params_start;

        long t_init_hash_start = System.currentTimeMillis();

	    gen_hash_func();				// init <a_array_>

        long t_init_hash_end = System.currentTimeMillis();
        INIT_HASH_TIME += t_init_hash_end - t_init_hash_start;


        display_params();				// display params
        trees_ = null;					// init <trees_>

    }

    // -----------------------------------------------------------------------------
    public void calc_params()			// calc params of qalsh
    {
	    // -------------------------------------------------------------------------
	    //  init <delta_> and <beta_>
	    // -------------------------------------------------------------------------

        delta_ = 1.0f / E;
	    beta_  = 100.0f / n_pts_;

	    // -------------------------------------------------------------------------
	    //  init <w_> <p1_> and <p2_>
	    // -------------------------------------------------------------------------
									// <w> <p1> and <p2> of L2 distance
									// init <w> (auto tuning-w)

        w_ = Math.sqrt((8.0f * appr_ratio_ * appr_ratio_ * Math.log(appr_ratio_))
		/ (appr_ratio_ * appr_ratio_ - 1.0f));

	    p1_ = calc_l2_prob(w_ / 2.0f);
	    p2_ = calc_l2_prob(w_ / (2.0f * appr_ratio_));

	    // -------------------------------------------------------------------------
	    //  init <alpha_> <m_> and <l_>
	    // -------------------------------------------------------------------------

        float para1 = Math.sqrt(Math.log(2.0f / beta_));
	    float para2 = Math.sqrt(Math.log(1.0f / delta_));
	    float para3 = 2.0f * (p1_ - p2_) * (p1_ - p2_);
	
	    float eta = para1 / para2;		// init <alpha_>
	    alpha_ = (eta * p1_ + p2_) / (1.0f + eta);
									// init <m_> and <l_>
	    m_ = (int) Math.ceil((para1 + para2) * (para1 + para2) / para3);
	    l_ = (int) Math.ceil((p1_ * para1 + p2_ * para2) * (para1 + para2) / para3);

    }

    // -----------------------------------------------------------------------------
    public float calc_l2_prob(			// calc prob <p1_> and <p2_> of L2 dist
	    float x)							// x = w / (2.0 * r)
    {
	    return rand_obj.new_gaussian_prob(x);
    }

    // -----------------------------------------------------------------------------
    public void display_params()		// display params of qalsh
    {

	    System.out.print("Parameters of QALSH (L_2 Distance):\n");

	    System.out.printf("    n          = %d\n", n_pts_);
	    System.out.printf("    d          = %d\n", dim_);
	    System.out.prinf("    B          = %d\n", B_);
	    System.out.printf("    ratio      = %0.2f\n", appr_ratio_);
	    System.out.printf("    w          = %0.4f\n", w_);
	    System.out.printf("    p1         = %0.4f\n", p1_);
	    System.out.printf("    p2         = %0.4f\n", p2_);
	    System.out.printf("    alpha      = %0.6f\n", alpha_);
	    System.out.printf("    beta       = %0.6f\n", beta_);
	    System.out.printf("    delta      = %0.6f\n", delta_);
	    System.out.printf("    m          = %d\n", m_);
	    System.out.printf("    l          = %d\n", l_);
	    System.out.printf("    beta * n   = %d\n", 100);
	    System.out.printf("    index path = %s\n\n", index_path_);
    }

    // -----------------------------------------------------------------------------
    public void gen_hash_func()			// generate hash function <a_array>
    {
	    int sum = m_ * dim_;

	    g_memory += SIZEFLOAT * sum;
	    a_array_ = new float[sum];

	    for (int i = 0; i < sum; i++) {
		    a_array_[i] = rand_obj.gaussian(0.0f, 1.0f);
	    }
    }

		// -----------------------------------------------------------------------------
	public int bulkload(						// build m b-trees by bulkloading
		float[][] data) 						//data set
	{ 	 

		// -------------------------------------------------------------------------
		//  Check whether the default maximum memory is enough
		// -------------------------------------------------------------------------
		g_memory += (SIZEFLOAT + SIZEINT) * n_pts_;
		if (utils_obj.check_mem()) {
			System.out.printf("*** memory = %.2f MB\n\n", g_memory / (1024.0f * 1024.0f));
			return 1;
		}

		// -------------------------------------------------------------------------
		//  Check whether the directory exists. If the directory does not exist, we
		//  create the directory for each folder.
		// -------------------------------------------------------------------------
		//if it is Linux
		if(utils_obj.osValidator() == 3) {
			int len = index_path_.length();
			for (int i = 0; i < len; i++) {
				if (index_path_[i] == '/') {
					char ch = index_path_[i + 1];
					index_path_[i + 1] = '\0';
									// check whether the directory exists
					File f = new File(String.valueOf(index_path_));
					//int ret = access(index_path_, F_OK);
					if (!f.exists() && !f.isDirectory()) {			// create directory
						
						System.out.println("Could not create directory" + index_path_);
						System.out.println("QALSH::bulkload error");
						System.exit(2);
						
					}
					index_path_[i + 1] = ch;
				}
			}
		}
		//if it is Windows
		if(utils_obj.osValidator() == 1) {
			int len = index_path_.length();
			for (int i = 0; i < len; i++) {
				if (index_path_[i] == '/') {
					char ch = index_path_[i + 1];
					index_path_[i + 1] = '\0';
									// check whether the directory exists
					File f = new File(String.valueOf(index_path_));
					//int ret = access(index_path_, F_OK);
					if (!f.exists() && !f.isDirectory()) {			// create directory
						
						System.out.println("Could not create directory" + index_path_);
						System.out.println("QALSH::bulkload error");
						System.exit(2);
						
					}
					index_path_[i + 1] = ch;
				}
			}
		}

		// -------------------------------------------------------------------------
		//  Write the file "para" where the parameters and hash functions are 
		//  stored in it.
		// -------------------------------------------------------------------------

		char[] fname = new char[200];
		fname = Arrays.copyOf(index_path_);			// write the "para" file
        fname = String.valueOf(fname).concat("para").toCharArray();
		
		if (write_para_file(fname)) return 1;

		// -------------------------------------------------------------------------
		//  Write the hash tables (indexed by b+ tree) to the disk
		// -------------------------------------------------------------------------
										// dataset sorted by hash value
		HashValue[] hashtable = new HashValue[n_pts_];
		for (int i = 0; i < m_; i++) {
	//		printf("    Tree %3d (out of %d)\n", i + 1, m_);

	//		printf("        Computing Hash Values...\n");

        	long t_proj_points_start = System.currentTimeMillis();

			for (int j = 0; j < n_pts_; j++) {
				hashtable[j].id_ = j;
				hashtable[j].proj_ = calc_hash_value(i, data[j]);
			}

        	long t_proj_points_end = System.currentTimeMillis();
        	PROJ_POINTS_TIME += t_proj_points_end - t_proj_points_start;

	//		printf("        Sorting...\n");

			long t_sort_start = System.currentTimeMillis();

			Arrays.sort(hashtable, new HashTableComparator());

        	long t_sort_end = System.currentTimeMillis();
        	SORT_TIME += t_sort_end - t_sort_start;

	//		printf("        Bulkloading...\n");

			long t_build_tree_start = System.currentTimeMillis();

        	get_tree_filename(i, fname);

			BTree bt = new BTree();
			bt.init(fname, B_);
			if (bt.bulkload(hashtable, n_pts_)) {
				return 1;
			}

        	long t_build_tree_end = System.currentTimeMillis();
        	BUILD_WRITE_TREE_TIME += t_build_tree_end - t_build_tree_start;

			bt = null
		}

		// -------------------------------------------------------------------------
		//  Release space
		// -------------------------------------------------------------------------
		if (hashtable != null) {
			hashtable = null;
			g_memory -= SIZEFLOAT + SIZEINT * n_pts_;
		}
		return 0;						// success to return
	}



	}

	// -----------------------------------------------------------------------------
	public int write_para_file(			// write "para" file from disk
		char[] fname)						// file name of "para" file
	{
		File file = new File(String.valueOf(fname));
		
		if (file.exists()) {						// ensure the file not exist
			System.out.println("QALSH: hash tables exist.\n");
	    	System.exit(1);
		}

		file = new File(String.valueOf(fname));			// open "para" file to write
		FileWriter fp = new FileWriter(file);
		PrintWriter pw = new PrintWriter(fp);

		if (!fp.exists()) {
			System.out.printf("I could not create %s.\n", fname);
			System.out.printf("Perhaps no such folder %s?\n", index_path_);
			return 1;					// fail to return
		}

		pw.printf("n = %d\n", n_pts_);// write <n_pts_>
		pw.printf("d = %d\n", dim_);	// write <dim_>
		pw.printf("B = %d\n", B_);	// write <B_>
									// write <appr_ratio_>
		pw.printf("ratio = %f\n", appr_ratio_);
		pw.printf("w = %f\n", w_);	// write <w_>
		pw.printf("p1 = %f\n", p1_);	// write <p1_>
		pw.printf("p2 = %f\n", p2_);	// write <p2_>
									// write <alpha_>
		pw.printf("alpha = %f\n", alpha_);
									// write <beta_>
		pw.printf("beta = %f\n", beta_);
									// write <delta_>
		pw.printf("delta = %f\n", delta_);

		pw.printf("m = %d\n", m_);	// write <m_>
		pw.printf("l = %d\n", l_);	// write <l_>

		int count = 0;
		for (int i = 0; i < m_; i++) {	// write <a_array_>
			pw.printf("%f", a_array_[count++]);
			for (int j = 1; j < dim_; j++) {
				pw.printf(" %f", a_array_[count++]);
			}
			pw.printf("\n");
		}
			
		return 0;						// success to return
	}

	// Excel-ready output (omid) (begin)
	public int write_para_to_excel(			// write "para" file from disk
			char[] fname)						// file name of "para" file
	{
		File file = new File(String.valueOf(fname));
		
		FileWriter fp = new FileWriter(file, true);
		PrintWriter pw = new PrintWriter(fp);

		if (!fp.exists()) {
			System.out.printf("I could not create %s.\n", fname);
			System.out.printf("Perhaps no such folder %s?\n", index_path_);
			return 1;					// fail to return
		}

		pw.printf("\n\n\n\n\n");
		pw.printf("nPoints\t%d\n", n_pts_);// write <n_pts_>
		pw.printf("dimension\t%d\n", dim_);	// write <dim_>
		pw.printf("pageSize\t%d\n", B_);	// write <B_>
		// write <appr_ratio_>
		pw.printf("ratio\t%f\n", appr_ratio_);
		pw.printf("w\t%f\n", w_);	// write <w_>
		pw.printf("p1\t%f\n", p1_);	// write <p1_>
		pw.printf("p2\t%f\n", p2_);	// write <p2_>
		// write <alpha_>
		pw.printf("alpha\t%f\n", alpha_);
		// write <beta_>
		pw.printf("beta\t%f\n", beta_);
		// write <delta_>
		pw.printf("delta\t%f\n", delta_);

		pw.printf("m\t%d\n", m_);	// write <m_>
		pw.printf("l\t%d\n", l_);	// write <l_>

		pw.printf("isQinDS:\t%d", isQinDS); // omid


		if (fp) fp.close();				// close para file

		return 0;						// success to return
	}
	// Excel-ready output (omid) (end)


	// -----------------------------------------------------------------------------
	public float calc_hash_value(		// calc hash value
		int table_id,						// hash table id
		float[] point)						// a point
	{
		float ret = 0.0f;
		for (int i = 0; i < dim_; i++) {
			ret += (a_array_[table_id * dim_ + i] * point[i]);
		}
		return ret;
	}

	public class HashTableComparator implements Comparator<HashValue> {
 
    	@Override
    	public int compare(HashValue t1, HashValue t2) {

			int ret = 0;
	
			if (t1.proj_ < t2.proj_) {
				ret = -1;
			} else if (t1.proj_ > t2.proj_) {
				ret = 1;
			} else {
				if (t1.id_ < t2.id_) ret = -1;
				else if (t1.id_ > t2.id_) ret = 1;
			}
			return ret;
        }
	}

	// -----------------------------------------------------------------------------
	public void get_tree_filename(		// get file name of b-tree
		int tree_id,						// tree id, from 0 to m-1
		char[] fname)						// file name (return)
	{
		fname = Arrays.copyOf(index_path_);
		String c = Integer.toString(tree_id);
		fname = String.valueOf(index_path_).concat(c).toCharArray();
		fname = String.valueOf(index_path_).concat(".qalsh").toCharArray();
	}

	// -----------------------------------------------------------------------------
	public int restore(					// load existing b-trees.
		char[] output_folder)				// folder of info of qalsh
	{
										// init <index_path_>
	//	strcpy(index_path_, output_folder);
	//	strcat(index_path_, "L2_indices/");
		index_path_ = Arrays.copyOf(INDEX_PATH);
        index_path_ = String.valueOf(index_path_).concat("indexes/").toCharArray();

		char[] fname = new char[200];
		fname = Arrays.copyOf(index_path_);
        fname = String.valueOf(fname).concat("para").toCharArray();

		if (read_para_file(fname)) {	// read "para" file and init params
			return 1;					// fail to return
		}

		trees_ = new BTree[m_];		// allocate <trees>
		for (int i = 0; i < m_; i++) {
			get_tree_filename(i, fname);// get filename of tree

			trees_[i] = new BTree();	// init <trees>
			trees_[i].init_restore(fname);
		}
		return 0;						// success to return
	}

	// -----------------------------------------------------------------------------
	public int read_para_file(			// read "para" file
		char[] fname)						// file name of "para" file
	{

		try {
            FileReader fp = new FileReader(String.valueOf(fname));
            BufferedReader br = new BufferedReader(fp);

            String line = "";
			String[] temp;
			
			line = br.readLine();
            temp = line.split(" ");
			n_pts_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			dim_ = Integer.parseInt(temp[2]);
			
			line = br.readLine();
            temp = line.split(" ");
			B_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			appr_ratio_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			w_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			p1_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			p2_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			alpha_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			beta_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			delta_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			m_ = Integer.parseInt(temp[2]);

			line = br.readLine();
            temp = line.split(" ");
			l_ = Integer.parseInt(temp[2]);

			a_array_ = new float[m_ * dim_];// read <a_array_>
			g_memory += SIZEFLOAT * m_ * dim_;
			int count = 0;
			for (int i = 0; i < m_; i++) {
				line = br.readLine();
				temp = line.split(" ");
				for (int j = 0; j < dim_; j++) {
					a_array_[count++] = Float.parseFloat(temp[j]);
				}
			}
		} catch (Exception e) {
            System.out.println("Exception occured: " + e);
        }
	
		display_params();				// display params
		return 0;						// success to return
	}

	// -----------------------------------------------------------------------------
	public void knn(						// k-nn search
		float[] query,						// query point
		int top_k,							// top-k value
		ResultItem[] rslt,					// k-nn results
		char[] output_folder)				// output folder
	{
		// -------------------------------------------------------------------------
		//  Space allocation and initialization
		// -------------------------------------------------------------------------

    	long start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

    	for (int i = 0; i < top_k; i++) {
			rslt[i].id_   = -1;
			rslt[i].dist_ = MAXREAL;
		}
										// objects frequency
		int[] frequency  = new int[n_pts_];
		for (int i = 0; i < n_pts_; i++) {
			frequency[i]  = 0;
		}
										// whether an object is checked
		boolean[] is_checked = new boolean[n_pts_];
		for (int i = 0; i < n_pts_; i++) {
			is_checked[i] = false;
		}

		float[] data = new float[dim_];	// one object data
	//	for (int i = 0; i < dim_; i++) {
	//		data[i] = 0.0f;
	//	}
	//	g_memory += ((SIZEBOOL + SIZEINT) * n_pts_ + SIZEFLOAT * dim_);

		boolean[] flag = new boolean[m_];		// whether a hash table is finished
		for (int i = 0; i < m_; i++) {
			flag[i] = true;
		}

		float[] q_val = new float[m_];	// hash value of query
		for (int i = 0; i < m_; i++) {
			q_val[i] = -1.0f;
		}
		g_memory += (SIZEFLOAT + 1) * m_;
										// left and right page buffer
		PageBuffer[] lptr = new PageBuffer[m_];
		PageBuffer[] rptr = new PageBuffer[m_];
		g_memory += (SIZECHAR * B_ * m_ * 2 + SIZEINT * m_ * 6);

		for (int i = 0; i < m_; i++) {
			lptr[i].leaf_node_ = null;
			lptr[i].index_pos_ = -1;
			lptr[i].leaf_pos_  = -1;
			lptr[i].size_      = -1;

			rptr[i].leaf_node_ = null;
			rptr[i].index_pos_ = -1;
			rptr[i].leaf_pos_  = -1;
			rptr[i].size_      = -1;
		}

		// -------------------------------------------------------------------------
		//  Compute hash value <q_dist> of query and init the page buffers 
		//  <lptr> and <rptr>.
		// -------------------------------------------------------------------------
		page_io_ = 0;					// num of page i/os
		dist_io_ = 0;					// num of dist cmpt
		init_buffer(lptr, rptr, q_val, query);

		// -------------------------------------------------------------------------
		//  Determine the basic <radius> and <bucket_width> 
		// -------------------------------------------------------------------------
		float radius = find_radius(lptr, rptr, q_val);
		float bucket_width = (w_ * radius / 2.0f);

		// -------------------------------------------------------------------------
		//  K-nn search
		// -------------------------------------------------------------------------
		boolean again = true;			// stop flag
		int candidates = 99 + top_k;	// threshold of candidates
		int flag_num   = 0;			// used for bucket bound
		int scanned_id = 0;			// num of scanned id

		int id    = -1;					// current object id
		int count = -1;					// count size in one page
		int start = -1;					// start position
		int end   = -1;					// end position

		float left_dist = -1.0f;		// left dist with query
		float right_dist = -1.0f;		// right dist with query
		float knn_dist = MAXREAL;		// kth nn dist
									// result entry for update
		ResultItem item = new ResultItem();
		g_memory += (long) (SIZEFLOAT + SIZEINT);

		double read_dataTime = 0;
		double calc_l2_distTime = 0;
		int lineCount = 1;
		//*****************************************************
		
		long end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
    	ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

    	// Open data and index files (omid) (begin)
    	RandomAccessFile raf = new RandomAccessFile(DATA_BIN_PATH, "rw");
		
        if (!raf) {
        	System.out.printf("Could not open binary data file: %s\n", DATA_BIN_PATH);
        	System.exit(1);
    	}
    	// Open data and index files (omid) (end)

		while (again) {
			// ---------------------------------------------------------------------
			//  Step 1: initialize the stop condition for current round
			// ---------------------------------------------------------------------

        	start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

        	flag_num = 0;
			for (int i = 0; i < m_; i++) {
				flag[i] = true;
			}

			end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
        	ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

        	// ---------------------------------------------------------------------
			//  Step 2: find frequent objects
			// ---------------------------------------------------------------------

			while (true) {
				for (int i = 0; i < m_; i++) {
					if (!flag[i]) continue;

					// -------------------------------------------------------------
					//  Step 2.1: compute <left_dist> and <right_dist>
					// -------------------------------------------------------------
                	start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

                	left_dist = -1.0f;
					if (lptr[i].size_ != -1) {
						left_dist = calc_proj_dist(&lptr[i], q_val[i]);
					}

					right_dist = -1.0f;
					if (rptr[i].size_ != -1) {
						right_dist = calc_proj_dist(&rptr[i], q_val[i]);
					}

                	end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
                	ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

					// -------------------------------------------------------------
					//  Step 2.2: determine the closer direction (left or right)
					//  and do collision counting to find frequent objects.
					//
					//  For the frequent object, we calc the L2 distance with
					//  query, and update the k-nn result.
					// -------------------------------------------------------------
					if (left_dist >= 0 && left_dist < bucket_width && 
						((right_dist >= 0 && left_dist <= right_dist) ||
						right_dist < 0)) {

                    	start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

                    	count = lptr[i].size_;
						end = lptr[i].leaf_pos_;
						start = end - count;

						end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
						ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

						for (int j = end; j > start; j--) {
							start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

							id = lptr[i].leaf_node_.get_entry_id(j);
							frequency[id]++;
							scanned_id++;

							end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
							ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

							if (frequency[id] > l_ && !is_checked[id]) {
								is_checked[id] = true;

								start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
								for (int dim = 0; dim < dim_; dim++) {
									data[dim] = 0.0f;
								}
								end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
								ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

								DATA_IO_NUM++; // Data I/O (omid)
								DATA_IO_SIZE += dim_* SIZEFLOAT / 1024.0;

								long seek_pos = (long) id * (long) dim_ * (long) SIZEFLOAT;

								long start_data_io_time = System.currentTimeMillis();
								raf.seek(0);
								raf.seek(seek_pos);
								raf.read(data, 0, dim_ * SIZEFLOAT);
								long end_data_io_time = System.currentTimeMillis();
								DATA_IO_TIME += end_data_io_time - start_data_io_time;

								if (!raf) {
									System.out.printf("Data read() Error!\n");
									System.exit(1);
								}

								long start_dist_calc_time = System.currentTimeMillis(); // Distance calculation time (omid)
								item.dist_ = utils_obj.calc_l2_dist(data, query, dim_);
								long end_dist_calc_time = System.currentTimeMillis(); // Distance calculation time (omid)
								DIST_CALC_TIME += end_dist_calc_time - start_dist_calc_time; // Distance calculation time (omid)


								start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

								item.id_ = id;
								knn_dist = update_result(rslt, item, top_k);

								end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
								ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

								// -------------------------------------------------
								//  Terminating condition 2
								// -------------------------------------------------
								dist_io_++;
								if (dist_io_ >= candidates) {
									again = false;
									flag_num += m_;
									break;
								}
							}
						}

						update_left_buffer(&lptr[i], &rptr[i]);
					}
					else if (right_dist >= 0 && right_dist < bucket_width && 
						((left_dist >= 0 && left_dist > right_dist) || 
						left_dist < 0)) {

						start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

						count = rptr[i].size_;
						start = rptr[i].leaf_pos_;
						end = start + count;

						end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
						ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

						for (int j = start; j < end; j++) {
							start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

							id = rptr[i].leaf_node_.get_entry_id(j);
							frequency[id]++;
							scanned_id++;

							end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
							ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

							if (frequency[id] > l_ && !is_checked[id]) {
								is_checked[id] = true;
							
								start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
								for (int dim = 0; dim < dim_; dim++) {
									data[dim] = 0.0f;
								}
								end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
								ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

								DATA_IO_NUM++; // Data I/O (omid)

								DATA_IO_SIZE += dim_* SIZEFLOAT / 1024.0;

								long seek_pos = (long) id * (long) dim_ * (long) SIZEFLOAT;

								long start_data_io_time = System.currentTimeMillis();
								raf.seek(0);
								raf.seek(seek_pos);
								raf.read(data, 0, dim_ * SIZEFLOAT);
								long end_data_io_time = System.currentTimeMillis();
								DATA_IO_TIME += end_data_io_time - start_data_io_time;

								if (!raf) {
									System.out.printf("Data read() Error!\n");
									System.exit(1);
								}

								long start_dist_calc_time = System.currentTimeMillis(); // Distance calculation time (omid)
								item.dist_ = utils_obj.calc_l2_dist(data, query, dim_);
								long end_dist_calc_time = System.currentTimeMillis(); // Distance calculation time (omid)
								DIST_CALC_TIME += end_dist_calc_time - start_dist_calc_time; // Distance calculation time (omid)

								start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

								item.id_ = id;
								knn_dist = update_result(rslt, item, top_k);

								end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
								ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

								// -------------------------------------------------
								//  Terminating condition 2
								// -------------------------------------------------
								dist_io_++;//TODO this is the number of candidates
								if (dist_io_ >= candidates) {
									again = false;
									flag_num += m_;
									break;
								}
							}
						}
						update_right_buffer(&lptr[i], &rptr[i]);
					}
					else {
						flag[i] = false;
						flag_num++;
					}
					if (flag_num >= m_) break;
				}
				if (flag_num >= m_) break;
			}

			// ---------------------------------------------------------------------
			//  Terminating condition 1
			// ---------------------------------------------------------------------
			if (knn_dist < appr_ratio_ * radius && dist_io_ >= top_k) {
				again = false;
				break;
			}

			// ---------------------------------------------------------------------
			//  Step 3: auto-update <radius>
			// ---------------------------------------------------------------------
			VR_FLAG = true; // omid
			VR_COUNT++; // omid

			long start_vr_alg_time = System.currentTimeMillis(); // VR Algorithm time (omid)

			start_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)

			radius = update_radius(lptr, rptr, q_val, radius);
			bucket_width = radius * w_ / 2.0f;

			end_algrthm_time = System.currentTimeMillis(); // Algorithm time (omid)
			ALG_TIME += end_algrthm_time - start_algrthm_time; // Algorithm time (omid)

			auto end_vr_alg_time = System.currentTimeMillis(); // VR Algorithm time (omid)
			VR_ALG_TIME += end_vr_alg_time - start_vr_alg_time; // VR Algorithm time (omid)
		}
		raf.close();

		// -------------------------------------------------------------------------
		//  Release space
		// -------------------------------------------------------------------------
	//	if (data != null || frequency != null || is_checked != null) {
			data = null;
			frequency  = null;
			is_checked = null;
			g_memory -= ((SIZEBOOL + SIZEINT) * n_pts_ + SIZEFLOAT * dim_);
	//	}
		if (q_val != null || flag != null || item != null) {
			q_val = null;
			flag = null;
			item = null;
			g_memory -= (SIZEFLOAT + SIZEBOOL) * m_;
			g_memory -= (long) (SIZEINT + SIZEFLOAT);
		}

		for (int i = 0; i < m_; i++) {
			// ---------------------------------------------------------------------
			//  CANNOT remove the condition
			//              <lptrs[i].leaf_node != rptrs[i].leaf_node>
			//  Because <lptrs[i].leaf_node> and <rptrs[i].leaf_node> may point 
			//  to the same address, then we would delete it twice and receive 
			//  the runtime error or segmentation fault.
			// ---------------------------------------------------------------------
			if (lptr[i].leaf_node_ && lptr[i].leaf_node_ != rptr[i].leaf_node_) {
				lptr[i].leaf_node_ = null;
			}
			if (rptr[i].leaf_node_) {
				rptr[i].leaf_node_ = null;
			}
		}
		lptr = null;
		rptr = null;
		g_memory -= (SIZECHAR * B_ * m_ * 2 + SIZEINT * m_ * 6);

	}

	// -----------------------------------------------------------------------------
	public void init_buffer(			// init page buffer (loc pos of b-treee)
		PageBuffer[] lptr,					// left buffer page (return)
		PageBuffer[] rptr,					// right buffer page (return)
		float[] q_dist,						// hash value of query (return)
		float[] query)						// query point
	{
		int  block   = -1;				// tmp vars for index node
		int  follow  = -1;
		boolean lescape = false;

		int pos = -1;					// tmp vars for leaf node
		int increment = -1;
		int num_entries = -1;

		BNode.BIndexNode index_node = null;

		for (int i = 0; i < m_; i++) {	// calc hash value of query
			q_dist[i] = calc_hash_value(i, query);
			block = trees_[i].root_;

			index_node = new BIndexNode();
			index_node.init_restore(trees_[i], block);
			page_io_++;

			// ---------------------------------------------------------------------
			//  Find the leaf node whose value is closest and larger than the key
			//  of query q <qe.key>
			// ---------------------------------------------------------------------
			lescape = false;			// locate the position of branch
			while (index_node.get_level() > 1) {
				follow = index_node.find_position_by_key(q_dist[i]);

				if (follow == -1) {		// if in the most left branch
					if (lescape) {		// scan the most left branch
						follow = 0;
					}
					else {
						if (block != trees_[i].root_) {
					    	System.out.printf("QALSH::knn_bucket No branch found\n");
							System.exit(1);
						}
						else {
							follow = 0;
							lescape = true;
						}
					}
				}
				block = index_node.get_son(follow);
				index_node = null;

				index_node = new BIndexNode();
				index_node.init_restore(trees_[i], block);
				page_io_++;				// access a new node (a new page)
			}

			// ---------------------------------------------------------------------
			//  After finding the leaf node whose value is closest to the key of
			//  query, initialize <lptrs[i]> and <rptrs[i]>.
			//
			//  <lescape> = true is that the query has no <lptrs>, the query is 
			//  the smallest value.
			// ---------------------------------------------------------------------
			follow = index_node.find_position_by_key(q_dist[i]);
			if (follow < 0) {
				lescape = true;
				follow = 0;
			}

			if (lescape) {				// only init right buffer
				block = index_node.get_son(0);
				rptr[i].leaf_node_ = new BLeafNode();
				rptr[i].leaf_node_.init_restore(trees_[i], block);
				rptr[i].index_pos_ = 0;
				rptr[i].leaf_pos_ = 0;

				increment = rptr[i].leaf_node_.get_increment();
				num_entries = rptr[i].leaf_node_.get_num_entries();
				if (increment > num_entries) {
					rptr[i].size_ = num_entries;
				} else {
					rptr[i].size_ = increment;
				}
				page_io_++;
			}
			else {						// init left buffer
				block = index_node.get_son(follow);
				lptr[i].leaf_node_ = new BLeafNode();
				lptr[i].leaf_node_.init_restore(trees_[i], block);

				pos = lptr[i].leaf_node_.find_position_by_key(q_dist[i]);
				if (pos < 0) pos = 0;
				lptr[i].index_pos_ = pos;

				increment = lptr[i].leaf_node_.get_increment();
				if (pos == lptr[i].leaf_node_.get_num_keys() - 1) {
					num_entries = lptr[i].leaf_node_.get_num_entries();
					lptr[i].leaf_pos_ = num_entries - 1;
					lptr[i].size_ = num_entries - pos * increment;
				}
				else {
					lptr[i].leaf_pos_ = pos * increment + increment - 1;
					lptr[i].size_ = increment;
				}
				page_io_++;
										// init right buffer
				if (pos < lptr[i].leaf_node_.get_num_keys() - 1) {
					rptr[i].leaf_node_ = lptr[i].leaf_node_;
					rptr[i].index_pos_ = (pos + 1);
					rptr[i].leaf_pos_ = (pos + 1) * increment;
				
					if ((pos + 1) == rptr[i].leaf_node_.get_num_keys() - 1) {
						num_entries = rptr[i].leaf_node_.get_num_entries();
						rptr[i].size_ = num_entries - (pos + 1) * increment;
					}
					else {
						rptr[i].size_ = increment;
					}
				}
				else {
					rptr[i].leaf_node_ = lptr[i].leaf_node_.get_right_sibling();
					if (rptr[i].leaf_node_) {
						rptr[i].index_pos_ = 0;
						rptr[i].leaf_pos_ = 0;

						increment = rptr[i].leaf_node_.get_increment();
						num_entries = rptr[i].leaf_node_.get_num_entries();
						if (increment > num_entries) {
							rptr[i].size_ = num_entries;
						} else {
							rptr[i].size_ = increment;
						}
						page_io_++;
					}
				}
			}

			if (index_node != null) {
				index_node = null;
			}
		}
	}

	// -----------------------------------------------------------------------------
	public float find_radius(			// find proper radius
		PageBuffer[] lptr,					// left page buffer
		PageBuffer[] rptr,					// right page buffer
		float[] q_dist)						// hash value of query
	{
		float radius = update_radius(lptr, rptr, q_dist, 1.0f/appr_ratio_);
		if (radius < 1.0f) radius = 1.0f;

		return radius;
	}

	// -----------------------------------------------------------------------------
	public float update_result(			// update knn results
		ResultItem[] rslt,					// k-nn results
		ResultItem[] item,					// new result
		int top_k)							// top-k value
	{
		int i = -1;
		int pos = -1;
		boolean alreadyIn = false;

		for (i = 0; i < top_k; i++) {
										// ensure the id is not exist before
			if (item.id_ == rslt[i].id_) {
				alreadyIn = true;
				break;
			}							// find the position to insert
			else if (utils_obj.compfloats(item.dist_, rslt[i].dist_) == -1) {
				break;
			}
		}
		pos = i;

		if (!alreadyIn && pos < top_k) {// insertion
			for (i = top_k - 1; i > pos; i--) {
				rslt[i].setto(&(rslt[i - 1]));
			}
			rslt[pos].setto(item);
		}
		return rslt[top_k - 1].dist_;
	}


	// -----------------------------------------------------------------------------
	public float update_radius(			// update radius
		PageBuffer[] lptr,					// left page buffer
		PageBuffer[] rptr,					// right page buffer
		float[] q_dist,						// hash value of query
		float  old_radius)					// old radius
	{
		float dist = 0.0f;				// tmp vars
		ArrayList<Float> list = new ArrayList<Float>;
		
		for (int i = 0; i < m_; i++) {	// find an array of proj dist
			if (lptr[i].size_ != -1) {
				dist = calc_proj_dist(lptr[i], q_dist[i]);
				list.add(dist);		
			}
			if (rptr[i].size_ != -1) {
				dist = calc_proj_dist(rptr[i], q_dist[i]);
				list.add(dist);
			}
		}
		Collections.sort(list);	// sort the array

		int num = (int) list.size();
		if (num == 0) return appr_ratio_ * old_radius;
	
		if (num % 2 == 0) {				// find median dist
			dist = (lis.get([num/2 - 1]) + list.get([num/2])) / 2.0f;
		} else {
			dist = list.get([num/2]);
		}
		list.clear();

		int kappa = (int) Math.ceil(Math.log(2.0f * dist / w_) / Math.log(appr_ratio_));
		dist = Math.pow(appr_ratio_, kappa);
	
		return dist;
	}

	// -----------------------------------------------------------------------------
	public void update_left_buffer(		// update left buffer
		PageBuffer[] lptr,					// left buffer
		final PageBuffer[] rptr)				// right buffer
	{
		BNode.BLeafNode leaf_node = null;
		BNode.BLeafNode old_leaf_node = null;

		if (lptr.index_pos_ > 0) {
			lptr.index_pos_--;

			int pos = lptr.index_pos_;
			int increment = lptr.leaf_node_.get_increment();
			lptr.leaf_pos_ = pos * increment + increment - 1;
			lptr.size_ = increment;
		}
		else {
			old_leaf_node = lptr.leaf_node_;
			leaf_node = lptr.leaf_node_.get_left_sibling();

			if (leaf_node) {
				lptr.leaf_node_ = leaf_node;
				lptr.index_pos_ = lptr.leaf_node_.get_num_keys() - 1;

				int pos = lptr.index_pos_;
				int increment = lptr.leaf_node_.get_increment();
				int num_entries = lptr.leaf_node_.get_num_entries();
				lptr.leaf_pos_ = num_entries - 1;
				lptr.size_ = num_entries - pos * increment;
				page_io_++;
			}
			else {
				lptr.leaf_node_ = null;
				lptr.index_pos_ = -1;
				lptr.leaf_pos_ = -1;
				lptr.size_ = -1;
			}

			if (rptr.leaf_node_ != old_leaf_node) {
				old_leaf_node = null;
			}
		}
	}

	// -----------------------------------------------------------------------------
	public void update_right_buffer(	// update right buffer
		final PageBuffer[] lptr,				// left buffer
		PageBuffer[] rptr)					// right buffer
	{
		BNode.BLeafNode leaf_node = null;
		BNode.BLeafNode old_leaf_node = null;

		if (rptr.index_pos_ < rptr.leaf_node_.get_num_keys() - 1) {
			rptr.index_pos_++;

			int pos = rptr.index_pos_;
			int increment = rptr.leaf_node_.get_increment();
			rptr.leaf_pos_ = pos * increment;
			if (pos == rptr.leaf_node_.get_num_keys() - 1) {
				int num_entries = rptr.leaf_node_.get_num_entries();
				rptr.size_ = num_entries - pos * increment;
			}
			else {
				rptr.size_ = increment;
			}
		}
		else {
			old_leaf_node = rptr.leaf_node_;
			leaf_node = rptr.leaf_node_.get_right_sibling();

			if (leaf_node) {
				rptr.leaf_node_ = leaf_node;
				rptr.index_pos_ = 0;
				rptr.leaf_pos_ = 0;

				int increment = rptr.leaf_node_.get_increment();
				int num_entries = rptr.leaf_node_.get_num_entries();
				if (increment > num_entries) {
					rptr.size_ = num_entries;
				} else {
					rptr.size_ = increment;
				}
				page_io_++;
			}
			else {
				rptr.leaf_node_ = null;
				rptr.index_pos_ = -1;
				rptr.leaf_pos_ = -1;
				rptr.size_ = -1;
			}

			if (lptr.leaf_node_ != old_leaf_node) {
				old_leaf_node = null;
			}
		}
	}

	// -----------------------------------------------------------------------------
	public float calc_proj_dist(		// calc proj dist
		final PageBuffer[] ptr,				// page buffer
		float q_val)						// hash value of query
	{
		int pos = ptr.index_pos_;
		float key = ptr.leaf_node_.get_key(pos);
		float dist = Math.abs(key - q_val);

		return dist;
	}

}
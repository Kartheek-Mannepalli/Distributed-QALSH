import java.util.*;
import java.io.*;
import java.lang.Math;
//import java.nio.*;

public class Qalsh extends Global{

	Utils utils_obj = new Utils();

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
	    Btree[][] trees_;

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

        char[] index_path_ = new char[];
        index_path_ = Arrays.copyOf(INDEX_PATH);
        index_path_ = index_path_.concat("indexes/");

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
		    a_array_[i] = gaussian(0.0f, 1.0f);
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
					File f = new File(index_path_);
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
					File f = new File(index_path_);
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
        fname = fname.concat("para");
		
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

		
		}


	}

	// -----------------------------------------------------------------------------
	public int write_para_file(			// write "para" file from disk
		char[] fname)						// file name of "para" file
	{
		File file = new File(fname);
		
		if (file.exists()) {						// ensure the file not exist
			System.out.println("QALSH: hash tables exist.\n");
	    	System.exit(1);
		}

		file = new File(fname);			// open "para" file to write
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



	// -----------------------------------------------------------------------------
	public void get_tree_filename(		// get file name of b-tree
		int tree_id,						// tree id, from 0 to m-1
		char[] fname)						// file name (return)
	{
		char c[20];

		fname = Arrays.copyOf(index_path_);
		c = String.valueOf(tree_id).toCharArray();
		fname = fname.concat(c);
		fname = fname.concat(".qalsh");
		
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


}
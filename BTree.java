import java.util.*;
import java.io.*;
import java.lang.Math;

public class BTree extends Global {
    // -----------------------------------------------------------------------------
    //  BTree: b-tree to index hash values produced by qalsh
    // -----------------------------------------------------------------------------
    int root_;						// address of disk for root

	BlockFile[] file_;				// file in disk to store
	BNode[] root_ptr_;				// pointer of root
    public BTree()						// constructor
    {
	    root_ = -1;
	    file_ = null;
	    root_ptr_ = null;
    }

    // -----------------------------------------------------------------------------
    public void init(					// init a new tree
	    char fname,						// file name
	    int b_length)						// block length
    {
	    File fp = new File(fname);
	    if (fp.exists()) {						// check whether the file exist
		    System.out.printf("The file \"%s\" exists. Replace? (y/n)", fname);
		
		    Scanner myObj = new Scanner(System.in);
            char c = myObj.next().charAt(0);        // input 'Y' or 'y' or others
            if (c != 'y' && c != 'Y') {	// if not remove existing file
			    System.exit(1);		// program will be stopped.
		    }
		    fname.delete();			// otherwise, remove existing file
	    }
	    								// init <file>, b-tree store here
	    file_ = new BlockFile(fname, b_length);

	    // -------------------------------------------------------------------------
	    //  Init the first node: to store <blocklength> (page size of a node),
	    //  <number> (number of nodes including both index node and leaf node), 
	    //  and <root> (address of root node)
	    // -------------------------------------------------------------------------
	    root_ptr_ = new BIndexNode();
	    root_ptr_.init(0, this);
	    root_ = root_ptr_.get_block();
	    delete_root();
    }



}
import java.util.*;
import java.io.*;
import java.lang.Math;

public class BTree extends Global,Qalsh {
    // -----------------------------------------------------------------------------
    //  BTree: b-tree to index hash values produced by qalsh
    // -----------------------------------------------------------------------------
    int root_;						// address of disk for root

	BlockFile file_;				// file in disk to store
	BNode root_ptr_;				// pointer of root
    public BTree()						// constructor
    {
	    root_ = -1;
	    file_ = null;
	    root_ptr_ = null;
    }

    // -----------------------------------------------------------------------------
    public void init(					// init a new tree
	    char[] fname,						// file name
	    int b_length)						// block length
    {
	    File fp = new File(String.valueOf(fname));
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
	    root_ptr_ = new BNode();
        root_ptr_.new BIndexNode();
	    root_ptr_.init(0, this);
	    root_ = root_ptr_.get_block();
	    delete_root();
    }

	// -----------------------------------------------------------------------------
	public void init_restore(			// load the tree from a tree file
		char[] fname)						// file name
	{
		// -------------------------------------------------------------------------
		//  It doesn't matter to initialize blocklength to 0.
		//  After reading file, <blocklength> will be reinitialized by file.
		// -------------------------------------------------------------------------
											// init <file>
		file_ = new BlockFile(fname, 0);
		root_ptr_ = null;				// init <root_ptr>

		// -------------------------------------------------------------------------
		//  Read the content after first 8 bytes of first block into <header>
		// -------------------------------------------------------------------------
		byte[] header = new char[file_.get_blocklength()];
		file_.read_header(header);		// read remain bytes from header
		read_header(header);			// init <root> from <header>

		if (header != null) {			// release space
			header = null;
		}
	}

	// -----------------------------------------------------------------------------
	public int read_header(				// read <root> from buffer
		byte[] buf)							// buffer
	{
		//memcpy(&root_, buf, SIZEINT);
		root_ = byteArrayToInt(buf,0);
		return SIZEINT;
	}

	//------------------------------------------------------------------------------
    public int byteArrayToInt(byte[] b, int i) 
    {
        return   b[i+3] & 0xFF |
                (b[i+2] & 0xFF) << 8 |
                (b[i+1] & 0xFF) << 16 |
                (b[i] & 0xFF) << 24;
    }

	// -----------------------------------------------------------------------------
	public int write_header(			// write <root> to buffer
		byte[] buf)							// buffer (return)
	{
		ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
		//memcpy(buf, &root_, SIZEINT);
		byteBuffer.putInt(root_);
		return SIZEINT;
	}

	// -----------------------------------------------------------------------------
	public void load_root()				// load <root_ptr> of b-tree
	{
		if (root_ptr_ == null)  {
			root_ptr_.new BIndexNode();
			root_ptr_.init_restore(this, root_);
		}
	}


    // -----------------------------------------------------------------------------
    void delete_root()			// delete <root_ptr>
    {
	    if (root_ptr_ != null) {
		    root_ptr_ = null;
	    }
    }

    // -----------------------------------------------------------------------------
    public int bulkload(				// bulkload a tree from memory
	    HashValue[] hashtable,				// hash table
	    int n)								// number of entries
    {
	    BNode.BIndexNode index_child;
	    BNode.BIndexNode index_prev_nd;
	    BNode.BIndexNode index_act_nd;

	    BNode.BLeafNode leaf_child;
	    BNode.BLeafNode leaf_prev_nd;
	    BNode.BLeafNode leaf_act_nd;

        int id = -1;
	    int block = -1;
	    float key = MINREAL;

	    boolean first_node  = false;		// determine relationship of sibling
	    int start_block = -1;			// position of first node
	    int end_block = -1;			// position of last node

	    int current_level = -1;		// current level (leaf level is 0)
	    int last_start_block = -1;		// to build b-tree level by level
	    int last_end_block = -1;		// to build b-tree level by level

	    // -------------------------------------------------------------------------
	    //  Build leaf node from <_hashtable> (level = 0)
	    // -------------------------------------------------------------------------

        start_block = 0;
	    end_block   = 0;
	    first_node  = true;

	    for (int i = 0; i < n; i++) {
		    id = hashtable[i].id_;
		    key = hashtable[i].proj_;

		    if (!leaf_act_nd) {
			    leaf_act_nd = root_ptr_.new BLeafNode();
			    leaf_act_nd.init(0, this);

                if (first_node) {
				    first_node = false;	// init <start_block>
				    start_block = leaf_act_nd.get_block();
			    }
			    else {					// label sibling
				    leaf_act_nd.set_left_sibling(leaf_prev_nd.get_block());
				    leaf_prev_nd.set_right_sibling(leaf_act_nd.get_block());

				    leaf_prev_nd = null;
			    }
                end_block = leaf_act_nd.get_block();
		    }							// add new entry
		    leaf_act_nd.add_new_child(id, key);	

		    if (leaf_act_nd.isFull()) {// change next node to store entries
			    leaf_prev_nd = leaf_act_nd;
			    leaf_act_nd = null;
		    }
	    }
        if (leaf_prev_nd != null) {		// release the space
		    leaf_prev_nd = null;
	    }
	    if (leaf_act_nd != null) {
		    leaf_act_nd = null;
	    }

        // -------------------------------------------------------------------------
	    //  Stop consition: lastEndBlock == lastStartBlock (only one node, as root)
	    // -------------------------------------------------------------------------
	    current_level = 1;				// build the b-tree level by level
	    last_start_block = start_block;
	    last_end_block = end_block;
        while (last_end_block > last_start_block) {
		    first_node = true;
		    for (int i = last_start_block; i <= last_end_block; i++) {
			    block = i;				// get <block>
			    if (current_level == 1) {
				    leaf_child = root_ptr_.new BLeafNode();
				    leaf_child.init_restore(this, block);
				    key = leaf_child.get_key_of_node();

				    leaf_child = null;
			    }
				else {
					index_child = root_ptr_.new BIndexNode();
					index_child.init_restore(this, block);
					key = index_child.get_key_of_node();

					index_child = null;
				}
				
				if (!index_act_nd) {
					index_act_nd = root_ptr_.new BIndexNode();
					index_act_nd.init(current_level, this);

					if (first_node) {
						first_node = false;
						start_block = index_act_nd.get_block();
					}
					else {
						index_act_nd.set_left_sibling(index_prev_nd.get_block());
						index_prev_nd.set_right_sibling(index_act_nd.get_block());

						index_prev_nd = null;
					}
					end_block = index_act_nd.get_block();
				}							//add new entry
				index_act_nd.add_new_child(key, block);

				if (index_act_nd.isFull()) {
					index_prev_nd = index_act_nd;
					index_act_nd = null;
				}
			}
			if (index_prev_nd != null) {// release the space
				index_prev_nd = null;
			}
			if (index_act_nd != null) {
				index_act_nd = null;
			}
										// update info
			last_start_block = start_block;
			last_end_block = end_block;	// build b-tree of higher level
			current_level++;
		}
		root_ = last_start_block;		// update the <root>

		if (index_prev_nd != null) {
			index_prev_nd = null;
		}
		if (index_act_nd != null) {
			index_act_nd = null;
		}
		if (index_child != null) {
			index_child = null;
		}
		if (leaf_prev_nd != null) {
			leaf_prev_nd = null;
		}
		if (leaf_act_nd != null) {
			leaf_act_nd = null;
		}
		if (leaf_child != null) {
			leaf_child = null;
		}

		return 0;						// success to return
	}

}
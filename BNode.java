import java.util.*;
import java.io.*;
import java.lang.Math;

public class BNode extends Global {

    // -----------------------------------------------------------------------------
    //  BNode: basic structure of node in b-tree
    // -----------------------------------------------------------------------------
    public BNode()						// constructor
    {
	    char level_ = '-1';
	    int num_entries_ = -1;
	    int left_sibling_ = -1;
        int right_sibling_ = -1;
	    float[] key_;

	    int block_ = -1;
        int capacity_ = -1;
	    boolean dirty_ = false;
	    Btree btree_;
    }

    // -----------------------------------------------------------------------------
    public void init(					// init a new node, which not exist
	    int level,							// level (depth) in b-tree
	    BTree btree)						// b-tree of this node
    {
	    btree_ = btree;					// init <btree_>
	    level_ = (char) level;			// init <level_>

	    dirty_ = true;					// init <dirty_>
	    left_sibling_ = -1;				// init <left_sibling_>
	    right_sibling_ = -1;				// init <right_sibling_>
	    //key_ = nullptr;					// init <key_>

	    num_entries_ = 0;				// init <num_entries_>
	    block_ = -1;						// init <block_>
	    capacity_ = -1;					// init <capacity_>
    }

    // -----------------------------------------------------------------------------
    public void init_restore(			// load an exist node from disk to init
        BTree btree,						// b-tree of this node
        int block)							// addr of disk for this node
    {
        btree_ = btree;					// init <btree_>
        block_ = block;					// init <block_>

        dirty_ = false;					// init <dirty_>
        left_sibling_ = -1;				// init <left_sibling_>
        right_sibling_ = -1;				// init <right_sibling_>
        key_ = null;					// init <key_>

        num_entries_ = 0;				// init <num_entries_>
        level_ = -1;						// init <block_>
        capacity_ = -1;					// init <capacity_>
    }

    // -----------------------------------------------------------------------------
    public int get_entry_size()			// get entry size of b-node
    {
        return 0;						// return nothing
    }

    // -----------------------------------------------------------------------------
    public void read_from_buffer(		// do nothing
        byte[] buf)
    {
    }

    // -----------------------------------------------------------------------------
    public void write_to_buffer(		// do nothing
        byte[] buf)
    {
    }

    // -----------------------------------------------------------------------------
    public int find_position_by_key(	// find pos just less than input key
        float key)							// input key
    {
        return -1;						// do nothing
    }

    // -----------------------------------------------------------------------------
    public float get_key(				// get <key> indexed by <index>
        int index)							// input <index>
    {
        return -1.0f;					// do nothing
    }

    // -----------------------------------------------------------------------------
    public BNode get_left_sibling()	// get the left-sibling node
    {
        BNode node = null;
        if (left_sibling_ != -1) {		// left sibling node exist
            node = new BNode();			// read left-sibling from disk
            node.init_restore(btree_, left_sibling_);
        }
        return node;
    }

    // -----------------------------------------------------------------------------
    public BNode get_right_sibling()	// get the right-sibling node
    {
        BNode node = null;
        if (right_sibling_ != -1) {		// right sibling node exist
            node = new BNode();			// read right-sibling from disk
            node.init_restore(btree_, right_sibling_);
        }
        return node;
    }

    // -----------------------------------------------------------------------------
    public int get_block()				// get <block_> (address of this node)
    {
	    return block_;
    }

    // -----------------------------------------------------------------------------
    public int get_level()				// get <level_>
    {
	    return level_;
    }

    // -----------------------------------------------------------------------------
    public int get_num_entries()		// get <num_entries_>
    {
	    return num_entries_;
    }

    // -----------------------------------------------------------------------------
    public int get_header_size()		// get header size of b-node
    {  
	    int header_size = SIZECHAR + SIZEINT * 3;
	    return header_size;
    }

    // -----------------------------------------------------------------------------
    public float get_key_of_node()		// get key of this node
    {
	    return key_[0];
    }


    // -----------------------------------------------------------------------------
    public void set_left_sibling(		// set addr of left sibling node
	    int left_sibling)					// addr of left sibling node
    {
	    left_sibling_ = left_sibling;
    }

    // -----------------------------------------------------------------------------
    public void set_right_sibling(		// set addr of right sibling node
	    int right_sibling)					// addr of right sibling node
    {
	    right_sibling_ = right_sibling;
    }

    // -----------------------------------------------------------------------------
    public boolean isFull()				// whether is full?
    {
	    if (num_entries_ >= capacity_) return true;
	    else return false;
    }

    // ---------------------------------------------------------------------------------
    public class BIndexNode
    {
        // -----------------------------------------------------------------------------
        //  BIndexNode: structure of index node for b-tree
        // -----------------------------------------------------------------------------
        public BIndexNode()			// constructor
        {
	        char level_ = '-1';
	        int num_entries_ = -1;
	        int left_sibling_ = -1;
            int right_sibling_ = -1;

	        int block_ = -1;
            int capacity_ = -1;
	        boolean dirty_ = false;
	        BTree btree_;

	        float[] key_;
	        int[] son_;
        }

        // -----------------------------------------------------------------------------
        public void init(				// init a new node, which not exist
	        int level,							// level (depth) in b-tree
	        BTree btree)						// b-tree of this node
        {
	        btree_ = btree;					// init <btree_>
	        level_ = (char) level;			// init <level_>

	        num_entries_ = 0;				// init <num_entries_>
	        left_sibling_ = -1;				// init <left_sibling_>
	        right_sibling_ = -1;				// init <right_sibling_>
	        dirty_ = true;					// init <dirty_>

			        						// init <capacity_>
	        int b_length = btree_.file_.get_blocklength();
	        capacity_ = (b_length - get_header_size()) / get_entry_size();
            if (capacity_ < 50) {			// ensure at least 50 entries
		        System.out.printf("capacity = %d\n", capacity_);
		        System.out.printf("BIndexNode::init() capacity too small.\n");
		        System.exit(1);
	        }

	        key_ = new float[capacity_];	// init <key_>
	        for (int i = 0; i < capacity_; i++) {
		        key_[i] = MINREAL;
	        }
	        son_ = new int[capacity_];		// init <son_>
	        for (int i = 0; i < capacity_; i++) {
		        son_[i] = -1;
	        }

	        byte[] blk = new char[b_length];	// init <block_>, get new addr
	        block_ = btree_.file_.append_block(blk);
	        blk = null;
        }


        // -----------------------------------------------------------------------------
        public void init_restore(		// load an exist node from disk to init
	        BTree btree,						// b-tree of this node
	        int block)							// addr of disk for this node
        {
	        btree_ = btree;					// init <btree_>
	        block_ = block;					// init <block_>
	        dirty_ = false;					// init <dirty_>

				        					// get block length
	        int b_len = btree_.file_.get_blocklength();

									        // init <capacity_>
	        capacity_ = (b_len - get_header_size()) / get_entry_size();
            if (capacity_ < 50) {			// at least 50 entries
		        System.out.printf("capacity = %d\n", capacity_);
		        System.out.printf("BIndexNode::init_restore capacity too small.\n");
		        System.exit(1);
	        }

	        key_ = new float[capacity_];	// init <key_>
	        for (int i = 0; i < capacity_; i++) {
		        key_[i] = MINREAL;
	        }
	        son_ = new int[capacity_];		// init <son_>
	        for (int i = 0; i < capacity_; i++) {
		        son_[i] = -1;
	        }
            
	        // -------------------------------------------------------------------------
	        //  Read the buffer <blk> to init <level_>, <num_entries_>, <left_sibling_>,
	        //  <right_sibling_>, <key_> and <son_>.
	        // -------------------------------------------------------------------------
	        byte[] blk = new char[b_len];
	        btree_.file_.read_block(blk, block);
	        read_from_buffer(blk);

	        blk = null;
        }

        // -----------------------------------------------------------------------------
        //  Read info from buffer to initialize <level_>, <num_entries_>,
        //  <left_sibling_>, <right_sibling_>, <key_> and <son_> of b-index node
        // -----------------------------------------------------------------------------
        public void read_from_buffer(	// read a b-node from buffer
	        byte[] buf)							// store info of a b-index node
        {
	        int i = 0;						// read <level_>
	        //memcpy(&level_, &buf[i], SIZECHAR);
	        level_ = (char) buf[i];
            i += SIZECHAR;
							        		// read <num_entries_>
	        //memcpy(&num_entries_, &buf[i], SIZEINT);
            num_entries_ = byteArrayToInt(buf,i);
            i += SIZEINT;
									        // read <left_sibling_>
	        //memcpy(&left_sibling_, &buf[i], SIZEINT);
	        left_sibling_ = byteArrayToInt(buf,i);
            i += SIZEINT;
									        // read <right _sibling_>
	        //memcpy(&right_sibling_, &buf[i], SIZEINT);
	        right_sibling_ = byteArrayToInt(buf,i);
            i += SIZEINT;
            
	        for (int j = 0; j < num_entries_; j++) {
			        						// read <key_>
		        //memcpy(&key_[j], &buf[i], SIZEFLOAT);
		        key_[j] = Float.intBitsToFloat( buf[i] ^ buf[i+1]<<8 ^ buf[i+2]<<16 ^ buf[i+3]<<24 );
                i += SIZEFLOAT;
				        					// read <son_>
		        //memcpy(&son_[j], &buf[i], SIZEINT);
		        son_[j] = byteArrayToInt(buf,i);
                i += SIZEINT;
	        }
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
        public void write_to_buffer(	// write info of node into buffer
            byte[] buf)							// store info of this node (return)
        {
            int i = 0;						// write <level_>
            ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
            //memcpy(&buf[i], &level_, SIZECHAR);
            byteBuffer.putChar(level_);
            i += SIZECHAR;
                                            // write <num_entries_>
            //memcpy(&buf[i], &num_entries_, SIZEINT);
            byteBuffer.putInt(num_entries_);
            i += SIZEINT;
                                            // write <left_sibling_>
            //memcpy(&buf[i], &left_sibling_, SIZEINT);
            byteBuffer.putInt(left_sibling_);
            i += SIZEINT;
                                            // write <right_sibling_>
            //memcpy(&buf[i], &right_sibling_, SIZEINT);
            byteBuffer.putInt(right_sibling_);
            i += SIZEINT;

            for (int j = 0; j < num_entries_; j++) {
                                            // write <key_>
                //memcpy(&buf[i], &key_[j], SIZEFLOAT);
                byteBuffer.putFloat(key_[j]);
                i += SIZEFLOAT;
                                            // write <son_>
                //memcpy(&buf[i], &son_[j], SIZEINT);
                byteBuffer.putInt(son_[j]);
                i += SIZEINT;
            }
        }

        // -----------------------------------------------------------------------------
        public void add_new_child(		// add a new entry from its child node
	        float key,							// input key
	        int son)							// input son
        {
	        if (num_entries_ >= capacity_) {
	            System.out.printf("BIndexNode::add_new_child overflow\n");
		        System.exit(1);
	        }

	        key_[num_entries_] = key;		// add new entry into its pos
	        son_[num_entries_] = son;

	        num_entries_++;					// update <num_entries_>
	        dirty_ = true;					// node modified, <dirty_> is true
        }

        // -----------------------------------------------------------------------------
        //  Find position of entry that is just less than or equal to input entry.
        //  If input entry is smaller than all entry in this node, we'll return -1.
        //  The scan order is from right to left.
        // -----------------------------------------------------------------------------
        public int find_position_by_key(
	        float key)							// input key
        {
	        int pos = -1;
			        						// linear scan (right to left)
	        for (int i = num_entries_ - 1; i >= 0; i--) {
		        if (key_[i] <= key) {
			        pos = i;
			        break;
		        }
	        }
	        return pos;
        }

        // -----------------------------------------------------------------------------
        public float get_key(			// get <key> indexed by <index>
            int index)							// input index
        {
            if (index < 0 || index >= num_entries_) {
                System.out.printf("BIndexNode::get_key out of range.\n");
                System.exit(1);
            }
            return key_[index];
        }

        // -----------------------------------------------------------------------------
                                            // get the left-sibling node
        public BIndexNode get_left_sibling()
        {
            BIndexNode node = null;
            if (left_sibling_ != -1) {		// left sibling node exist
                node = new BIndexNode();	// read left-sibling from disk
                node.init_restore(btree_, left_sibling_);
            }
            return node;
        }

        // -----------------------------------------------------------------------------
                                            // get the right-sibling node
        public BIndexNode get_right_sibling()
        {
            BIndexNode node = null;
            if (right_sibling_ != -1) {		// right sibling node exist
                node = new BIndexNode();	// read right-sibling from disk
                node.init_restore(btree_, right_sibling_);
            }
            return node;
        }

        // -----------------------------------------------------------------------------
        public int get_son(			// get son indexed by <index>
	        int index)							// input index
        {
	        if (index < 0 || index >= num_entries_) {
	            System.out.printf("BIndexNode::get_son out of range.\n");
		        System.exit(1);
	        }
	        return son_[index];
        }

        // -----------------------------------------------------------------------------
        //  entry: <key_>: SIZEFLOAT and <son_>: SIZEINT
        // -----------------------------------------------------------------------------
        public int get_entry_size()	// get entry size of b-node
        {
	        int entry_size = SIZEFLOAT + SIZEINT;
	        return entry_size;
        }

    }

    public class BLeafNode
    {
        // -----------------------------------------------------------------------------
        //  BLeafNode: structure of leaf node in b-tree
        // -----------------------------------------------------------------------------
        public BLeafNode()				// constructor
        {
	        char level_ = '-1';
	        int num_entries_ = -1;
	        int left_sibling_ = -1;
            int right_sibling_ = -1;

	        int block_ = -1;
            int capacity_ = -1;
	        boolean dirty_ = false;
	        BTree btree_;

	        int num_keys_ = -1;
	        int capacity_keys_ = -1;
	        float[] key_;
	        int[] id_;
        }

        // -----------------------------------------------------------------------------
        public void init(				// init a new node, which not exist
	        int level,							// level (depth) in b-tree
	        BTree btree)						// b-tree of this node
        {
	        btree_ = btree;					// init <btree_>
	        level_ = (char) level;			// init <level_>

	        num_entries_ = 0;				// init <num_entries_>
	        num_keys_ = 0;					// init <num_keys_>
	        left_sibling_ = -1;				// init <left_sibling_>
	        right_sibling_ = -1;				// init <right_sibling_>
	        dirty_ = true;					// init <dirty_>
            
									// get block length
	        int b_length = btree_.file_.get_blocklength();

	        // -------------------------------------------------------------------------
	        //  Init <capacity_keys_> and calc key size
	        // -------------------------------------------------------------------------
	        int key_size = get_key_size(b_length);
			        						// init <key>
	        key_ = new float[capacity_keys_];
	        for (int i = 0; i < capacity_keys_; i++) {
		        key_[i] = MINREAL;
	        }
            								// calc header size
	        int header_size = super.get_header_size();
									        // calc entry size
	        int entry_size = get_entry_size();	
									        // init <capacity>
	        capacity_ = (b_length - header_size - key_size) / entry_size;
	        if (capacity_ < 100) {			// at least 100 entries
		        System.out.printf("capacity = %d\n", capacity_);
		        System.out.printf("BLeafNode::init capacity too small.\n");
		        System.exit(1);
	        }
	        id_ = new int[capacity_];		// init <id>
	        for (int i = 0; i < capacity_; i++) {
		        id_[i] = -1;
	        }

	        char[] blk = new char[b_length];	// init <block>
	        block_ = btree_.file_.append_block(blk);
	        blk = null;

        }

        // -----------------------------------------------------------------------------
        public void init_restore(		// load an exist node from disk to init
	        BTree btree,						// b-tree of this node
	        int block)							// addr of disk for this node
        {
	        btree_ = btree;					// init <btree_>
	        block_ = block;					// init <block_>
	        dirty_ = false;					// init <dirty_>

			        						// get block length
	        int b_length = btree_.file_.get_blocklength();

	        // -------------------------------------------------------------------------
	        //  Init <capacity_keys> and calc key size
	        // -------------------------------------------------------------------------
	        int key_size = get_key_size(b_length);
									        // init <key>
	        key_ = new float[capacity_keys_];
	        for (int i = 0; i < capacity_keys_; i++) {
		        key_[i] = MINREAL;
	        }
			        						// calc header size
            int header_size = super.get_header_size();
								    	    // calc entry size
	        int entry_size = get_entry_size();	
			        						// init <capacity>
	        capacity_ = (b_length - header_size - key_size) / entry_size;
	        if (capacity_ < 100) {			// at least 100 entries
		        System.out.printf("capacity = %d\n", capacity_);
		        System.out.printf("BLeafNode::init_store capacity too small.\n");
		        System.exit(1);
	        }
	        id_ = new int[capacity_];		// init <id>
	        for (int i = 0; i < capacity_; i++) {
		        id_[i] = -1;
	        }
            
	        // -------------------------------------------------------------------------
	        //  Read the buffer <blk> to init <level_>, <num_entries_>, <left_sibling_>,
	        //  <right_sibling_>, <num_keys_> <key_> and <id_>
	        // -------------------------------------------------------------------------
	        byte[] blk = new byte[b_length];
	        btree_.file_.read_block(blk, block);
	        read_from_buffer(blk);

	        blk = null;
        }


        // -----------------------------------------------------------------------------
        public int get_key_size(		// get key size of this node
	        int _block_length)					// block length
        {
	        capacity_keys_ = (int) ceil((float) _block_length / INDEX_SIZE_LEAF_NODE);

	        // -------------------------------------------------------------------------
	        //  Array of <key_> with number <capacity_keys_> + <number_keys_> (SIZEINT)
	        // -------------------------------------------------------------------------
	        int key_size = capacity_keys_ * SIZEFLOAT + SIZEINT;
	        return key_size;
        }

        // -----------------------------------------------------------------------------
        public int get_increment()		// get <increment>
        {
	        int entry_size = get_entry_size();
	        int increment = INDEX_SIZE_LEAF_NODE / entry_size;

	        return increment;
        }

        // -----------------------------------------------------------------------------
        public int get_entry_size()		// get entry size in b-node
        {
	        return SIZEINT;						// <id>: sizeof(int)
        }

        // -----------------------------------------------------------------------------
        public int find_position_by_key(// find pos just less than input key
	        float key)							// input key
        {
	        int pos = -1;
			        						// linear scan (right to left)
	        for (int i = num_keys_ - 1; i >= 0; i--) {
		        if (key_[i] <= key) {
			        pos = i;				// position of corresponding id
			        break;
		        }
	        }
	        return pos;
        }

        // -----------------------------------------------------------------------------
        public int get_num_keys()		// get <num_keys_>
        {
	        return num_keys_;
        }

        // -----------------------------------------------------------------------------
        public float get_key(			// get <key_> indexed by <index>
	        int index)							// input <index>
        {
	        if (index < 0 || index >= num_keys_) {
	            System.out.printf("BLeafNode::get_key out of range.\n");
	            System.exit(1);
	        }
	        return key_[index];
        }

        // -----------------------------------------------------------------------------
        public int get_entry_id(		// get entry id indexed by <index>
            int index)							// input <index>
        {
            if (index < 0 || index >= num_entries_) {
                System.out.printf("BLeafNode::get_entry_id out of range.\n");
                System.exit(1);
            }
            return id_[index];
        }

        // -----------------------------------------------------------------------------
									// get right sibling node
        public BLeafNode get_right_sibling()
        {
	        BLeafNode node = null;
	        if (right_sibling_ != -1) {		// right sibling node exist
		        node = new BLeafNode();		// read right-sibling from disk
		        node.init_restore(btree_, right_sibling_);
	        }
	        return node;
        }

        // -----------------------------------------------------------------------------
									// get left-sibling node
        public BLeafNode get_left_sibling()
        {
            BLeafNode node = null;
            if (left_sibling_ != -1) {		// left sibling node exist
                node = new BLeafNode();		// read left-sibling from disk
                node.init_restore(btree_, left_sibling_);
            }
            return node;
        }

        // -----------------------------------------------------------------------------
        public void read_from_buffer(	// read a b-node from buffer
	        byte[] buf)							// store info of a b-node
        {
	        int i = 0;
	        // -------------------------------------------------------------------------
	        //  Read header: <level_> <num_entries_> <left_sibling_> <right_sibling_>
	        // -------------------------------------------------------------------------

            //memcpy(&level_, &buf[i], SIZECHAR);
            level_ = (char) buf[i];
	        i += SIZECHAR;

	        //memcpy(&num_entries_, &buf[i], SIZEINT);
            num_entries_ = byteArrayToInt(buf,i);
	        i += SIZEINT;

	        //memcpy(&left_sibling_, &buf[i], SIZEINT);
            left_sibling_ = byteArrayToInt(buf,i);
	        i += SIZEINT;

	        //memcpy(&right_sibling_, &buf[i], SIZEINT);
            right_sibling_ = byteArrayToInt(buf,i);
	        i += SIZEINT;

            // -------------------------------------------------------------------------
	        //  Read keys: <num_keys_> and <key_>
	        // -------------------------------------------------------------------------
	        //memcpy(&num_keys_, &buf[i], SIZEINT);
            num_keys_ = byteArrayToInt(buf,i);
            i += SIZEINT;

	        for (int j = 0; j < capacity_keys_; j++) {
		        //memcpy(&key_[j], &buf[i], SIZEFLOAT);
		        //key_[j] = byteArrayToInt(buf,i);
                key_[j] = Float.intBitsToFloat( buf[i] ^ buf[i+1]<<8 ^ buf[i+2]<<16 ^ buf[i+3]<<24 );
                i += SIZEFLOAT;
	        }

	        // -------------------------------------------------------------------------
	        //  Read entries: <id_>
	        // -------------------------------------------------------------------------
	        for (int j = 0; j < num_entries_; j++) {
		        //memcpy(&id_[j], &buf[i], SIZEINT);
		        id_[j] = byteArrayToInt(buf,i);
                i += SIZEINT;
	        }
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
        public void write_to_buffer(	// write a b-node into buffer
            byte[] buf)							// store info of a b-node
        {
            int i = 0;
            ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
            // -------------------------------------------------------------------------
            //  Write header: <level_> <num_entries_> <left_sibling_> <right_sibling_>
            // -------------------------------------------------------------------------
            //memcpy(&buf[i], &level_, SIZECHAR);
            byteBuffer.putChar(level_);
            i += SIZECHAR;

            //memcpy(&buf[i], &num_entries_, SIZEINT);
            byteBuffer.putInt(num_entries_);
            i += SIZEINT;

            //memcpy(&buf[i], &left_sibling_, SIZEINT);
            byteBuffer.putInt(left_sibling_);
            i += SIZEINT;

            //memcpy(&buf[i], &right_sibling_, SIZEINT);
            byteBuffer.putInt(right_sibling_);
            i += SIZEINT;

            // -------------------------------------------------------------------------
            //  Write keys: <num_keys_> and <key_>
            // -------------------------------------------------------------------------
            //memcpy(&buf[i], &num_keys_, SIZEINT);
            byteBuffer.putInt(num_keys_);
            i += SIZEINT;

            for (int j = 0; j < capacity_keys_; j++) {
                //memcpy(&buf[i], &key_[j], SIZEFLOAT);
                byteBuffer.putFloat(key_[j]);
                i += SIZEFLOAT;
            }

            // -------------------------------------------------------------------------
            //  Write entries: <id_>
            // -------------------------------------------------------------------------
            for (int j = 0; j < num_entries_; j++) {
                //memcpy(&buf[i], &id_[j], SIZEINT);
                byteBuffer.putInt(id_[j]);
                i += SIZEINT;
            }
        }


        // -----------------------------------------------------------------------------
        public void add_new_child(		// add new child by input id and key
	        int id,								// input object id
	        float key)							// input key
        {
	        if (num_entries_ >= capacity_) {
	            System.out.printf("BLeafNode::add_new_child entry overflow\n");
	            System.exit(1);
	        }

	        id_[num_entries_] = id;			// add new id into its pos

	        if ((num_entries_ * SIZEINT) % INDEX_SIZE_LEAF_NODE == 0) {
		        if (num_keys_ >= capacity_keys_) {
		            System.out.printf("BLeafNode::add_new_child key overflow\n");
			        System.exit(1);
		        }

		        key_[num_keys_] = key;		// add new key into its pos
		        num_keys_++;				// update <num_keys>
	        }

	        num_entries_++;					// update <num_entries>
	        dirty_ = true;					// node modified, <dirty> is true
        }



    }

}
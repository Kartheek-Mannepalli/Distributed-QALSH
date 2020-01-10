import java.util.*;
import java.io.*;
import java.lang.Math;

// -----------------------------------------------------------------------------
//  Some points to NOTE:
//  1) 2 types of block # are used (i.e. the internal # (e.g. act_block) 
//     and external # (e.g. pos)). internal # is one larger than external #
//     because the first block of the file is used to store header info. 
//     data info is stored starting from the 2nd block (excluding the header
//     block).both types of # start from 0.
//
//  2) "act_block" is internal block #. "number" is the # of data block (i.e. 
//     excluding the header block). maximum actblock equals to number. Maximum 
//     external block # equals to number - 1 
//
//  3) cache_cont records the internal numbers. 
// -----------------------------------------------------------------------------

public class BlockFile extends Global {

    RandomAccessFile fp_;						// file pointer
	char[] file_name_;				// file name
	boolean  new_flag_;				// specifies if this is a new file
	
	int block_length_;				// length of a block
	int act_block_;					// block num of fp position
	int num_blocks_;				// total num of blocks

    // -----------------------------------------------------------------------------
    //  BlockFile: structure of reading and writing file for b-tree
    // -----------------------------------------------------------------------------
    
    public BlockFile(				// constructor
	    char[] name,							// file name
	    int b_length)						// block length
    {
	    file_name_ = new char[name.length + 1];
	    file_name_ = Arrays.copyOf(name);
        block_length_ = b_length;

	    num_blocks_ = 0;				// num of blocks, init to 0
	    // -------------------------------------------------------------------------
	    //  Init <fp> and open <file_name_>. If <file_name_> exists, then fp != 0,
	    //  and we excute if-clause program. Otherwise, we excute else-clause 
	    //  program.
	    //
	    //  "rb+": read or write data from or into binary doc. if the file not 
	    //  exist, it will return nullptr.
	    // -------------------------------------------------------------------------
        fp_ = new RandomAccessFile(String.valueOf(name));

	    if (fp_.exists()) {
		// ---------------------------------------------------------------------
		//  Init <new_flag_> (since the file exists, <new_flag_> is false).
		//  Reinit <block_length_> (determined by the doc itself).
		//  Reinit <num_blocks_> (number of blocks in doc itself).
		// ---------------------------------------------------------------------
		new_flag_ = false;			// reinit <block_length_> by file
		block_length_ = fread_number();
		num_blocks_ = fread_number();
	    }
	    else {
		    // ---------------------------------------------------------------------
		    //  <file_name_> not exists. we construct new file and reinit paras.
		    // ---------------------------------------------------------------------
		    if (block_length_ < BFHEAD_LENGTH) {
			    // -----------------------------------------------------------------
			    //  Ensure <block_length_> is larger than or equal to 8 bytes.
			    //  8 bytes = 4 bypes <block_length_> + 4 bytes <num_blocks_>.
			    // -----------------------------------------------------------------
			    System.out.println("BlockFile::BlockFile couldnot open file.\n");
			    System.exit(1);
		    }

		    // ---------------------------------------------------------------------
		    //  "wb+": read or write data from or into binary doc. if file not
		    //  exist, we will construct a new file.
		    // ---------------------------------------------------------------------
		    fp_ = new RandomAccessFile(String.valueOf(file_name_));

		    if (!fp_.exists()) {
		        System.out.println("BlockFile::BlockFile could not create file.\n");
			    System.exit(1);
		    }

		    // ---------------------------------------------------------------------
		    //  Init <new_flag_>: as file is just constructed (new), it is true.
		    //  Write <block_length_> and <num_blocks_> to the header of file.
		    //  Since the file is empty (new), <num_blocks_> is 0 (no blocks in it).
		    // ---------------------------------------------------------------------
		    new_flag_ = true;
		    fwrite_number(block_length_);
		    fwrite_number(0);

		    // ---------------------------------------------------------------------
		    //  Since <block_length_> >= 8 bytes, for the remain bytes, we will 
		    //  init 0 to them.
		    //
		    //  ftell() return number of bytes from current position to the 
		    //  beginning position of the file.
		    // ---------------------------------------------------------------------
		    char[] buffer;
		    int len = -1;				// cmpt remain length of a block
		    buffer = new char[(len = block_length_ - (int) fp_.getFilePointer())];
									// set to 0 to remain bytes
            Arrays.fill(buffer,0);
		    //memset(buffer, 0, sizeof(buffer));
		    put_bytes(buffer, len);

		    buffer = null;
	    }
	    // -------------------------------------------------------------------------
	    //  Redirect file pointer to the start position of the file
	    // -------------------------------------------------------------------------
	    fp_.seek(0);
	    act_block_ = 0;					// init <act_block_> (no blocks)
    }

    // -----------------------------------------------------------------------------
    public int fread_number() 		// read an <int> value from bin file
    {
	    char ca[SIZEINT];
        
	    get_bytes(ca, SIZEINT);
        int number = Integer.parseInt(new String(ca));
        return number;
	    //return *((int *)ca);
    }

    // -----------------------------------------------------------------------------
    public void fwrite_number(		// write an <int> value to bin file
	    int value)							// a value of type <int>
    {
	    fp_.write(value);
        //put_bytes((char *) &value, SIZEINT);
    }

    // write <bytes> of length <num>
	void put_bytes(byte[] bytes, int num)
	{ 
        //fwrite(bytes, num, 1, fp_); 
        fp_.write(bytes,0,num);
    }

    // read <bytes> of length <num>
	public byte[] get_bytes(byte[] bytes, int num)
	{ 
        //fread(bytes, num, 1, fp_); 
        fp_.read(bytes,0,num);
        return bytes;
    }

    public int get_blocklength()			// get block length
	{ 
        return block_length_; 
    }

    void seek_block(int bnum) 		// move <fp_> to the right with <bnum>
	{ 
        fp_.seek(1); 
    }

    // -----------------------------------------------------------------------------
    //  Note that this func does not read the header of blockfile. It fetches the 
    //  info in the first block excluding the header of blockfile.
    // -----------------------------------------------------------------------------
    public void read_header(		// read remain bytes excluding header
	    byte[] buffer)						// contain remain bytes (return)
    {
	        								// jump out of first 8 bytes
	    fp_.seek(0);
		        							// read remain bytes into <buffer>
	    get_bytes(buffer, block_length_ - BFHEAD_LENGTH); 

	    if (num_blocks_ < 1) {			// no remain bytes
		    fp_.seek(0);                // fp return to beginning pos
		    act_block_ = 0;				// no act block
	    } else {
		    // ---------------------------------------------------------------------
		    //  Since we have read the first block (header block) of block file,
		    //  thus <act_block_> = 1, and the file pointer point to the 2nd block
		    //  (first block to store real data).
		    // ---------------------------------------------------------------------
		    act_block_ = 1;
	    }
    }

    // -----------------------------------------------------------------------------
    //  Note that this func does not write the header of blockfile. It writes the 
    //  info in the first block excluding the header of blockfile.
    // -----------------------------------------------------------------------------
    public void set_header(			// set remain bytes excluding header
        byte[] header)						// contain remain bytes
    {
                                        // jump out of first 8 bytes
        fp_.seek(0);
                                        // write remain bytes into <buffer>
        put_bytes(header, block_length_ - BFHEAD_LENGTH);
        
        if (num_blocks_ < 1) {			// no remain bytes
            fp_.seek(0);	            // fp return to beginning pos
            act_block_ = 0;				// no act block
        }
        else {
            // ---------------------------------------------------------------------
            //  Since we have write the first block (header block) of block file,
            //  thus <act_block_> = 1, and the file pointer point to the 2nd block 
            //  (first block to store real data).
            // ---------------------------------------------------------------------
            act_block_ = 1;
        }
    }


    // -----------------------------------------------------------------------------
    //  Read a <block> from <index>
    //
    //  We point out the difference of counting among the <number>, <act_block> 
    //  and <pos>.
    //  (1) <num_blocks_>: record the number of blocks, excluding the block
    //      of header. start from 1. (internal block)
    //  (2) <act_block_>: record the number of blocks currently read or written,
    //      including the block of header, thus when we read or write file,
    //      current <act_block> equal to 1. <act_block> is corresponding to
    //      file pointer.
    //  (3) <index> : record position of block we want to read or write, excluding
    //      the block of header. start from 0. (external block), i.e., when 
    //      <index> = 0, the file pointer is pointed to next block after the 
    //      block of header, at this time, <act_block_> equals to 1.
    //
    //  i.e. if number = 3, there are 4 blocks in the file, 1 header block +
    //  3 data block.
    //
    //  When file is opened, <act_block_> = 1. if <index> = 1, it means that we 
    //  want to read or write the 3rd block (2nd data block), thus firstly index++,
    //  then <index> = 2, then fseek move to 2nd data block.
    //
    //  After reading or writing the 2nd data block, file pointer is pointed to 
    //  the 3rd data block. As we know it has read or written 3 blocks, thus 
    //  currently <act_block> = <index> + 1 = 2 + 1 = 3.
    // -----------------------------------------------------------------------------
    public boolean read_block(			// read a <block> from <index>
	    byte[] block,						// a <block> (return)
	    int index)							// pos of the block
    {
	    index++;						// extrnl block to intrnl block
		    							// move to the position

        INDEX_IO_NUM++;
        INDEX_IO_SIZE += block_length_ / 1024.0;

        long start_index_io_time = System.currentTimeMillis();

        if (index <= num_blocks_ && index > 0) {
		    seek_block(index);
	    }
	    else {
		    System.out.printf("BlockFile::read_block request the block %d "
			    "which is illegal.\n", index - 1);
		    System.exit(1);
	    }
        
	    get_bytes(block, block_length_);// read the block

        long end_index_io_time = System.currentTimeMillis();
        INDEX_IO_TIME += end_index_io_time - start_index_io_time;

        if (VR_FLAG)
            VR_IO_TIME += end_index_io_time - start_index_io_time; // VR Index IO time (omid)

        if (index + 1 > num_blocks_) {	// <fp_> reaches the end of file
            start_index_io_time = System.currentTimeMillis();

            fp.seek(0);

            end_index_io_time = System.currentTimeMillis();
            INDEX_IO_TIME += end_index_io_time - start_index_io_time;

            if (VR_FLAG)
                VR_IO_TIME += end_index_io_time - start_index_io_time; // VR Index IO time (omid)

            act_block_ = 0;				// <act_block_> rewind to start pos
	    }
	    else {
		    act_block_ = index + 1;		// <act_block_> to next pos
	    }
	    return true;
    }

    // -----------------------------------------------------------------------------
    //  Note that this function can only write to an already "allocated" block (in 
    //  the range of <num_blocks>).
    //  If you allocate a new block, please use "append_block" instead.
    // -----------------------------------------------------------------------------
    public boolean write_block(		// write a <block> into <index>
        char[] block,						// a <block>
        int index)							// position of the blocks
    {
        index++;						// extrnl block to intrnl block
                                        // move to the position
        if (index <= num_blocks_ && index > 0) {
            seek_block(index);
        }
        else {
            System.out.printf("BlockFile::write_block request the block %d "
                "which is illegal.\n", index - 1);
            System.exit(1);
        }
        
        put_bytes(block, block_length_);// write this block
        if (index + 1 > num_blocks_) {	// update <act_block_>
            fp_.seek(0);
            act_block_ = 0;
        }
        else {
            act_block_ = index + 1;
        }
        return true;
    }

    // -----------------------------------------------------------------------------
    //  Append a new block at the end of file (out of the range of <num_blocks_>).
    //  The file pointer is pointed to the new appended block and return its pos.
    // -----------------------------------------------------------------------------
    public int append_block(		// append new block at the end of file
	    char[] block)						// the new block
    {
	    fp_.seek(fp_.length());		// <fp_> point to the end of file
	    put_bytes(block, block_length_);// write a <block>
	    num_blocks_++;					// add 1 to <num_blocks_>
	
	    fp_.seek(0);	// <fp_> point to pos of header
	    fwrite_number(num_blocks_);		// update <num_blocks_>

	    // -------------------------------------------------------------------------
	    //  <fp_> point to the pos of new added block. 
	    //  The equation <act_block_> = <num_blocks_> indicates the file pointer 
	    //  point to new added block.
	    //  Return index of new added block
	    // -------------------------------------------------------------------------
	    fp_.seek(fp_.length());
	    return (act_block_ = num_blocks_) - 1;
    }

    // -----------------------------------------------------------------------------
    //  Delete last <num> block in the file.
    //
    //  Notice: we just logically delete the data (only modifying the total number
    //  of blcoks), the real data is still stored in file and the size of file is 
    //  not changed.
    // -----------------------------------------------------------------------------
    public boolean delete_last_blocks(	// delete last <num> blocks
        int num)							// number of blocks to be deleted
    {
        if (num > num_blocks_) {		// check whether illegal?
            return false;
        }

        num_blocks_ -= num;				// update <number>
        fp_.seek(0);
        fwrite_number(num_blocks_);

        fp_.seek(0);		// <fp> point to beginning of file
        act_block_ = 0;					// <act_block> = 0
        return true;
    }

}
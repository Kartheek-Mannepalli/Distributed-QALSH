import java.util.*;
import java.io.*;
import java.lang.Math;

public class BlockFile extends Global {

    RandomAccessFile fp_;						// file pointer
	char[] file_name_;				// file name
	boolean  new_flag_;				// specifies if this is a new file
	
	int block_length_;				// length of a block
	int act_block_;					// block num of fp position
	int num_blocks_;				// total num of blocks
    
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
        fp_ = new RandomAccessFile(name);

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
		    fp_ = new RandomAccessFile(file_name_);

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
	void put_bytes(char[] bytes, int num)
	{ 
        //fwrite(bytes, num, 1, fp_); 
        fp_.write(bytes,0,1);
    }

    // read <bytes> of length <num>
	public char[] get_bytes(char[] bytes, int num)
	{ 
        //fread(bytes, num, 1, fp_); 
        fp_.read(bytes,0,num);
        return bytes;
    }




}
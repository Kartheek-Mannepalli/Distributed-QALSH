public class Global {

    static long g_memory;

    // -----------------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------------
    final float E = 2.7182818F; // math constants
    final float PI = 3.141592654F;

    final static float FLOATZERO = 1e-6F; // accuracy
    // max real value
    final static float MAXREAL = 3.402823466e+38F;
    final static float MINREAL = -MAXREAL; // min real value

    final static long MAXMEMORY = 1073741824; // max memory, 1 GB
    final static int MAXINT = 2147483647; // max integer value
    final static int MININT = -MAXINT; // min integer value

    final static int SIZEINT = Integer.BYTES;
    // size of type <char>
    final static int SIZECHAR = Character.BYTES;
    // size of type <float>
    final static int SIZEFLOAT = Float.BYTES;
    // size of type <bool>
    // final static int SIZEBOOL = Boolean.BYTES;

    									// file header size
    final int BFHEAD_LENGTH = (int) (Integer.BYTES * 2);
    									// index size of leaf node
    const int INDEX_SIZE_LEAF_NODE = 4096;

    final static int MAXK = 100;    // max top-k value

    char[] INDEX_PATH = new char[1000];
    char[] DATA_BIN_PATH = new char[1000];
    boolean isQinDS;
    double INDEXING_TIME;
    double GT_TIME;


    // Timers for indexing (omid) (begin)
    final double READ_DS_TIME;
    final double WRITE_DS_BIN_TIME;
    final double INIT_PARAMS_TIME;
    final double INIT_HASH_TIME;
    final double PROJ_POINTS_TIME;
    final double SORT_TIME;
    final double BUILD_WRITE_TREE_TIME;
    // Timers for indexing (omid) (end)

}
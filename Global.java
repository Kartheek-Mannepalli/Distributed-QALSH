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

    final static int MAXK = 100;    // max top-k value

}
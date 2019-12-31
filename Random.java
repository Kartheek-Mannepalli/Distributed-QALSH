import java.util.*;
import java.io.*;
import java.lang.Math;

public class Random extends Global {

    public final static Random RANDOM = new Random(1);

    // -----------------------------------------------------------------------------
    //  Functions used for generating random variables (r.v.).
    // -----------------------------------------------------------------------------
    public float uniform(						// r.v. from Uniform(min, max)
	    float min,							// min value
	    float max)							// max value
    {
	    if (min > max) {
	        System.out.println("uniform() parameters error.\n");
            System.exit(1);
	    }

	    float x = min + (max - min) * RANDOM.nextFloat();
	    if (x < min || x > max) {
            System.out.println("uniform() func error.\n");
            System.exit(1);
        }

	    return x;
    }

    
    // -----------------------------------------------------------------------------
    //  Use Box-Muller transform to generate a r.v. from Gaussian(mean, sigma)
    //
    //  Standard Gaussian distribution is Gaussian(0, 1), where mean = 0 and 
    //  sigma = 1.
    // -----------------------------------------------------------------------------
    public float gaussian(						// r.v. from Gaussian(mean, sigma)
	    float mu,							// mean (location)
	    float sigma)						// stanard deviation (scale > 0)
    {
	    if (sigma <= 0.0f) {
            System.out.println("gaussian() parameters error.\n");
            System.exit(1);
        }

	    float u1, u2;
	    do {
		    u1 = uniform(0.0f, 1.0f);
	    } while (u1 < FLOATZERO);
	    u2 = uniform(0.0f, 1.0f);
	
	    float x = mu + sigma * Math.sqrt(-2.0f * Math.log(u1)) * Math.cos(2.0f * PI * u2);
	    //float x = mu + sigma * sqrt(-2.0f * log(u1)) * sin(2.0f * PI * u2);
	    return x;
    }

    
    // -----------------------------------------------------------------------------
    //  Functions used for calculating probability distribution function (pdf) and 
    //  cumulative distribution function (cdf).
    // -----------------------------------------------------------------------------
    public float gaussian_pdf(					// pdf of N(0, 1)
	    float x)							// variable
    {
	    float ret = Math.exp(-x * x / 2.0f) / Math.sqrt(2.0f * PI);
	    return ret;
    }


    // -----------------------------------------------------------------------------
    public float new_gaussian_cdf(				// cdf of N(0, 1) in range [-x, x]
	    float x,							// integral border (x > 0)
	    float step)							// step increment
    {
	    if (x <= 0.0f) {
            System.out.println("new_gaussian_cdf() parameters error.\n");
            System.exit(1);
        }

	    float ret = 0.0f;
	    for (float i = -x; i <= x; i += step) {
		    ret += step * gaussian_pdf(i);
	    }
	    return ret;
    }





    // -----------------------------------------------------------------------------
    public float new_gaussian_prob(			// calc new gaussian probability
	    float x)							// x = w / (2 * r)
    {
	    float p = new_gaussian_cdf(x, 0.001F);
	    return p;
    }




}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package getregioncolour;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 *
 * @author bpt
 */
public class GetRegionColour implements Runnable {

    /** reference white D50 in XYZ coordinates */
    public static final double[] D50 = {96.4212, 100.0, 82.5188};
    /** reference white D55 in XYZ coordinates */
    public static final double[] D55 = {95.6797, 100.0, 92.1481};
    /** reference white D65 in XYZ coordinates */
    public static final double[] D65 = {95.0429, 100.0, 108.8900};
    /** reference white D75 in XYZ coordinates */
    public static final double[] D75 = {94.9722, 100.0, 122.6394};
    static double[] whitePoint = D65;
    int progress=0;
    BufferedImage bimg, mask;
    double[] avCol = new double[9];
        
    /** Convert XYZ to L*a*b*
     *
     * @param col the XYZ colour
     * @param wp the whitepoint
     * @return the colour in L*a*b*
     */
    public static double[] XYZtoLab(double[] col, double[] wp) {
        double f0 = f(col[1]/wp[1]);
        double L = 116.0*f0-16.0; // index 0 = Y
        double a = 500*(f(col[0]/wp[0])-f0);
        double b = 200*(f0-f(col[2]/wp[2]));
        double[] res = {L, a, b};
        return res;
    }

    /** Convert  L*a*b* to  XYZ
     *
     * @param col the  L*a*b* colour
     * @param wp the whitepoint
     * @return the colour in XYZ
     */
    public static double[] LabtoXYZ(double[] col, double[] wp) {
        double f0 = (col[0]+16.0)/116.0;
        double Y = wp[1]*finv(f0); // index 0 = Y
        double X = wp[0]*finv(f0+col[1]/500.0);
        double Z = wp[2]*finv(f0-col[2]/200.0);
        double[] res = {X, Y, Z};
        return res;
    }


    /**
     * XYZ to sRGB conversion matrix
     *
    double[][] Mi  = {{ 3.2406, -1.5372, -0.4986},
                             {-0.9689,  1.8758,  0.0415},
                             { 0.0557, -0.2040,  1.0570}};
                             */
     /**
     * XYZ to sRGB conversion matrix
     */
    static double[][] Mi  = {{ 3.240479, -1.537150, -0.498535},
                             {-0.969256,  1.875992,  0.041556},
                             { 0.055648, -0.204043,  1.057311}};


        /**
     * Convert XYZ to RGB.
     * @param X X
     * @param Y Y
     * @param Z Z
     * @return RGB in int array.
     */
    public static int[] XYZtoRGB(double X, double Y, double Z) {
      int[] result = new int[3];

      double x = X / 100.0;
      double y = Y / 100.0;
      double z = Z / 100.0;

      // [r g b] = [X Y Z][Mi]
      double r = (x * Mi[0][0]) + (y * Mi[0][1]) + (z * Mi[0][2]);
      double g = (x * Mi[1][0]) + (y * Mi[1][1]) + (z * Mi[1][2]);
      double b = (x * Mi[2][0]) + (y * Mi[2][1]) + (z * Mi[2][2]);

      // assume sRGB
      if (r > 0.0031308) {
        r = ((1.055 * Math.pow(r, 1.0 / 2.4)) - 0.055);
      }
      else {
        r = (r * 12.92);
      }
      if (g > 0.0031308) {
        g = ((1.055 * Math.pow(g, 1.0 / 2.4)) - 0.055);
      }
      else {
        g = (g * 12.92);
      }
      if (b > 0.0031308) {
        b = ((1.055 * Math.pow(b, 1.0 / 2.4)) - 0.055);
      }
      else {
        b = (b * 12.92);
      }

      r = (r < 0) ? 0 : (r>1 ? 1 : r);
      g = (g < 0) ? 0 : (g>1 ? 1 : g);
      b = (b < 0) ? 0 : (b>1 ? 1 : b);

      // convert 0..1 into 0..255
      result[0] = (int) Math.round(r * 255);
      result[1] = (int) Math.round(g * 255);
      result[2] = (int) Math.round(b * 255);

      return result;
    }

    /**
     * Convert XYZ to RGB
     * @param XYZ in a double array.
     * @return RGB in int array.
     */
    public static int[] XYZtoRGB(double[] XYZ) {
      return XYZtoRGB(XYZ[0], XYZ[1], XYZ[2]);
    }

    /** Convert RGB to XYZ
     * 
     * @param RGB the RGB array, range 0-255 per channel
     * @return returns the XYZ values for the colour
     */
    public static double[] RGBtoXYZ(double[] RGB) {
        double r = ( RGB[0] / 255.0 );        //R from 0 to 255
        double g = ( RGB[1] / 255.0 );        //G from 0 to 255
        double b = ( RGB[2] / 255.0 );        //B from 0 to 255

        if ( r > 0.04045 ) r = Math.pow((r + 0.055)/1.055 ,  2.4);
        else r = r / 12.92;
        if ( g > 0.04045 ) g = Math.pow( ( g + 0.055 ) / 1.055, 2.4);
        else g = g / 12.92;
        if (b > 0.04045) b = Math.pow((b + 0.055)/1.055, 2.4);
        else b = b / 12.92;

        r = r * 100;
        g = g * 100;
        b = b * 100;

        double[] XYZ = new double[3];
        //Observer. = 2°, Illuminant = D65
        XYZ[0] = r * 0.4124 + g * 0.3576 + b * 0.1805;
        XYZ[1] = r * 0.2126 + g * 0.7152 + b * 0.0722;
        XYZ[2] = r * 0.0193 + g * 0.1192 + b * 0.9505;

        return XYZ;
    }
    
    static double finv(double t) {
        double split = (6.0/29.0);
        //split *= split*split;
        if (t>split) {
            return Math.pow(t, 3.0);
        }
        return (t-4.0/29.0)*(6.0/29.0)*(6.0/29.0)*3.0;
    }

    static double f(double t) {
        double split = (6.0/29.0);
        split *= split*split;
        if (t>split) {
            return Math.pow(t, 1.0/3.0);
        }
        return t*(29.0/6.0)*(29.0/6.0)/3.0 + 4.0/29.0;
    }

    public void setImages(BufferedImage bimg, BufferedImage mask) {
        this.bimg = bimg;
        this.mask = mask;
    }
    
    public  double[] getRegionAverage() {
        avCol = new double[9];
        double count = 0;
        progress = 0;
        for (int y=0; y<bimg.getHeight(); y++) {
            for (int x = 0; x<bimg.getWidth(); x++) {
                int maskRGB = mask.getRGB(x, y);
                Color maskCol = new Color(maskRGB);
                if (maskCol.getRed()>127) {
                       int imgRGB = bimg.getRGB(x, y);
                       Color imgCol = new Color(imgRGB);
                       double[] rgb = new double[3];
                       rgb[0] = imgCol.getRed();
                       rgb[1] = imgCol.getGreen();
                       rgb[2] = imgCol.getBlue();
                       avCol[0] += rgb[0];
                       avCol[1] += rgb[1];
                       avCol[2] += rgb[2];
                       double[] xyz = RGBtoXYZ(rgb);
                       double[] lab = XYZtoLab(xyz, whitePoint);
                       avCol[3] += xyz[0];
                       avCol[4] += xyz[1];
                       avCol[5] += xyz[2];
                       avCol[6] += lab[0];
                       avCol[7] += lab[1];
                       avCol[8] += lab[2];
                       
                       count++;
                }
                    
            }
            progress = Math.round((100*y)/bimg.getHeight());
        }
        for (int i=0; i<9; i++) avCol[i]/=count;
        progress=100;
        return avCol;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public double[] getResults() {
        progress=0;
        return avCol;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        Scanner sc = new Scanner(new FileReader(args[0]));
        
        FileWriter fw = new FileWriter(args[1]);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter pw = new PrintWriter(bw);
        GetRegionColour crg = new GetRegionColour();
        while (sc.hasNextLine()) {
            String baseName = sc.nextLine();
            System.out.println("Processing " + baseName);
            File f1 = new File(baseName + ".jpg");
            BufferedImage bimg = javax.imageio.ImageIO.read(f1);
            File f2 = new File(baseName + ".png");
            BufferedImage mask = javax.imageio.ImageIO.read(f2);
            crg.setImages(bimg, mask);
            double[] avCol = crg.getRegionAverage();
            pw.print(baseName);
            for (int i=0; i<9; i++) pw.print(", " + avCol[i]);
            pw.print("\n");
        }
        pw.flush();
        pw.close();
    }

    @Override
    public void run() {
        getRegionAverage();
    }
}

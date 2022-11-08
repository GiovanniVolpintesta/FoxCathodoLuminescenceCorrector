package foxinhead.foxcathodoluminescencecorrector;

import org.opencv.core.*;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;

public class ImageConverter
{
    public enum ConversionType
    {
        NONE
        , GREYSCALE
        , CATHODO_LUMINESCENCE_CORRECTION
        , BLURRED_FILTER
    }

    public enum ConversionParameter
    {
        NONE
        , PARAM_SIGMA
        , NOISE_REDUCTION_ACTIVATED
    }

    // png, bmp, jpg, jpeg, webp, tif, ppm, pnm: conversion supported
    // gif, jfif, pbm, pgm: conversion tested and not supported
    // other formats have not been tested (probably not supported)
    private static final String[] inputSupportedTypes = { "png", "bmp", "jpg", "jpeg", "tif", "ppm", "pnm", "webp" };
    private static final String[] outputSupportedTypes = { "png", "bmp", "jpg", "jpeg", "tif", "ppm", "pnm", "webp" };
    private static final String[] inputFileFilters; // initialized in static constructor
    private static final String[] outputFileFilters; // initialized in static constructor

    private static final String defaultOutputType = "png";

    public static String[] getSupportedInputTypes() { return inputSupportedTypes; }
    public static String[] getSupportedOutputTypes() { return outputSupportedTypes; }

    public static String[] getInputFileFilters() { return inputFileFilters; }
    public static String[] getOutputFileFilters() { return outputFileFilters; }

    public static String getDefaultOutputType() { return defaultOutputType; }

    static
    {
        inputFileFilters = new String[inputSupportedTypes.length];
        for (int i = 0; i < inputSupportedTypes.length; ++i)
        {
            inputFileFilters[i] = "*." + inputSupportedTypes[i];
        }

        outputFileFilters = new String[outputSupportedTypes.length];
        for (int i = 0; i < outputSupportedTypes.length; ++i)
        {
            outputFileFilters[i] = "*." + outputSupportedTypes[i];
        }
    }

    public static boolean isTypeSupportedAsInput (String type)
    {
        for (String supportedType : getSupportedInputTypes())
        {
            if (supportedType.equals(type))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isTypeSupportedAsOutput (String type)
    {
        for (String supportedType : getSupportedOutputTypes())
        {
            if (supportedType.equals(type))
            {
                return true;
            }
        }
        return false;
    }

    public static ByteArrayInputStream convertImageInMemory (String srcImageFileName, ConversionType conversionType, Map<ConversionParameter, String> params)
    {
        return internalConvertImageInMemory(srcImageFileName, conversionType, defaultOutputType, params);
    }

    public static ByteArrayInputStream convertImageInMemory (String srcImageFileName, ConversionType conversionType, String outputType, Map<ConversionParameter, String> params) throws IllegalArgumentException
    {
        if (isTypeSupportedAsOutput(outputType))
        {
            return internalConvertImageInMemory(srcImageFileName, conversionType, outputType, params);
        }
        else
        {
            throw new IllegalArgumentException("\"" + outputType + "\" is not a valid output type. Please, use one any of the following types: " + Arrays.toString(getSupportedOutputTypes()));
        }
    }

    private static ByteArrayInputStream internalConvertImageInMemory (String srcImageFileName, ConversionType conversionType, String outputType, Map<ConversionParameter, String> params)
    {
        Mat m = Imgcodecs.imread(srcImageFileName);
        m = ConvertMat(m, conversionType, params);
        MatOfByte encodedImageBytes = new MatOfByte();
        ByteArrayInputStream inputStream = null;
        try
        {
            Imgcodecs.imencode("." + outputType, m, encodedImageBytes);
            inputStream = new ByteArrayInputStream(encodedImageBytes.toArray());
        }
        catch (CvException e)
        {
            // caused by error: (-215:Assertion failed) !image.empty() in function 'cv::imencode'
            // the file cannot be read
            encodedImageBytes = new MatOfByte();
            inputStream = null;
        }
        finally
        {
            m.release();
        }
        return inputStream;
    }

    private static Mat ConvertMat (Mat source, ConversionType conversionType, Map<ConversionParameter, String> params)
    {
        double sigma = params.containsKey(ConversionParameter.PARAM_SIGMA) ? Double.parseDouble(params.get(ConversionParameter.PARAM_SIGMA)) : 0.0;
        boolean performNoiseReduction = params.containsKey(ConversionParameter.NOISE_REDUCTION_ACTIVATED) && Boolean.parseBoolean(params.get(ConversionParameter.NOISE_REDUCTION_ACTIVATED));

        switch (conversionType)
        {
            case GREYSCALE:
                return ConvertToGreyScale(source);
            case CATHODO_LUMINESCENCE_CORRECTION:
                return PerformCathodoLuminescenceCorrection(source, sigma, performNoiseReduction);
            case BLURRED_FILTER:
                return PerformCathodoLuminescenceCorrectionBlur(source, sigma);
            case NONE:
            default:
                Mat dest = Mat.zeros(source.rows(), source.cols(), source.type());
                source.copyTo(dest);
                return dest;
        }
    }

    private static Mat ConvertToGreyScale (Mat source)
    {
        // Convert in greyscale
        Mat greyscaleMat = Mat.zeros(source.rows(), source.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(source, greyscaleMat, Imgproc.COLOR_RGB2GRAY);
        return greyscaleMat;
    }

    private static Mat PerformCathodoLuminescenceCorrection (Mat source, double sigmaMultiplier, boolean performNoiseReduction)
    {
        int nRows = source.rows();
        int nCols = source.cols();

        // Convert to three 32-bit float components ranging in [0-255]
        Mat source32F = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        source.convertTo(source32F, CvType.CV_32FC3);

        // Convert in HSV (ranging in [0-255])
        Mat hsvMat = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Imgproc.cvtColor(source32F, hsvMat, Imgproc.COLOR_RGB2HSV);
        source32F.release();

        // Extract channels
        Mat hChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Mat sChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Mat vChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.extractChannel(hsvMat, hChannel, 0);
        Core.extractChannel(hsvMat, sChannel, 1);
        Core.extractChannel(hsvMat, vChannel, 2);
        hsvMat.release();
        Core.MinMaxLocResult vChannelMinMax = Core.minMaxLoc(vChannel);

        // Apply gaussian blur with a big sigma that is dependent on the image size
        Mat blurred = ComputeBlurredVChannel(vChannel, sigmaMultiplier);

        // Result of Brightness
        Mat vChannelDivided = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        // Add 1 to the whole image to avoid divisions with 0 (which produce a NaN result).
        // This does not introduce a problem, because the image is in 0-255 range
        // and the result is remapped in 0-255 range later.
        Core.add(blurred, new Scalar(1.0), blurred);
        Core.divide(vChannel, blurred, vChannelDivided);
        vChannel.release();
        blurred.release();
        Core.MinMaxLocResult vChannelDividedMinMax = Core.minMaxLoc(vChannelDivided);
        //System.out.println("vChannelDivided min = " + vChannelDividedMinMax.minVal);
        //System.out.println("vChannelDivided max = " + vChannelDividedMinMax.maxVal);

        // As vChannelDivided has been computed with a division,
        // is has very low values, resulting in a pitch black image.
        // Here the image is remapped linearly in the 0-255 range to make it useful.
        Mat vChannelDivided_0_255 = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.subtract(vChannelDivided, new Scalar(vChannelDividedMinMax.minVal), vChannelDivided_0_255);
        vChannelDivided.release();
        Core.multiply(vChannelDivided_0_255, new Scalar(255.0 / (vChannelDividedMinMax.maxVal-vChannelDividedMinMax.minVal)), vChannelDivided_0_255);
        Core.MinMaxLocResult vChannelDivided_0_255_MinMax = Core.minMaxLoc(vChannelDivided_0_255);
        //System.out.println("vChannelDivided_0_255 min = " + vChannelDivided_0_255_MinMax.minVal);
        //System.out.println("vChannelDivided_0_255 max = " + vChannelDivided_0_255_MinMax.maxVal);

        Mat vChannelNew_0_255;
        if (performNoiseReduction)
        {
            Mat vChannelNew = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
            vChannelNew_0_255 = Mat.zeros(nRows, nCols, CvType.CV_32FC1);

            double sigma2 = 10;
            // Filter minimo
            Mat vChannelDividedLowBlur = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
            Imgproc.GaussianBlur(vChannelDivided_0_255, vChannelDividedLowBlur, new Size(0, 0), sigma2, sigma2, Core.BORDER_REPLICATE); // the size of the filter is computed using the sigma
            Core.MinMaxLocResult vChannelDividedLowBlurMinMax = Core.minMaxLoc(vChannelDividedLowBlur);
            vChannelDividedLowBlur.release();
            //System.out.println("vChannelDividedLowBlur min = " + vChannelDividedLowBlurMinMax.minVal);
            //System.out.println("vChannelDividedLowBlur max = " + vChannelDividedLowBlurMinMax.maxVal);

            // subtract the low blurred image minimum and apply a threshold to zero to negative values to
            // correct some noise in the lower values of the image
            Core.subtract(vChannelDivided_0_255, new Scalar(vChannelDividedLowBlurMinMax.minVal), vChannelNew);

            Imgproc.threshold(vChannelNew, vChannelNew, 0, 0, Imgproc.THRESH_TOZERO);
            Core.MinMaxLocResult vChannelNewMinMax = Core.minMaxLoc(vChannelNew);
            //System.out.println("vChannelNew min = " + vChannelNewMinMax.minVal);
            //System.out.println("vChannelNew max = " + vChannelNewMinMax.maxVal);

            Core.multiply(vChannelNew, new Scalar(255.0 / (vChannelNewMinMax.maxVal - vChannelNewMinMax.minVal)), vChannelNew_0_255);
            vChannelNew.release();
            Core.MinMaxLocResult vChannelNew_0_255_MinMax = Core.minMaxLoc(vChannelNew_0_255);
            //System.out.println("vChannelNew_0_255 min = " + vChannelNew_0_255_MinMax.minVal);
            //System.out.println("vChannelNew_0_255 max = " + vChannelNew_0_255_MinMax.maxVal);

            vChannelDivided_0_255.release();
        }
        else
        {
            vChannelNew_0_255 = vChannelDivided_0_255;
            // Don't release vChannelDivided_0_255 yet, otherwise also vChannelNew_0_255 is released
        }

        // recombine channels
        Mat hsvResult = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Core.insertChannel(hChannel, hsvResult, 0);
        Core.insertChannel(sChannel, hsvResult, 1);
        Core.insertChannel(vChannelNew_0_255, hsvResult, 2);
        hChannel.release();
        sChannel.release();
        vChannelNew_0_255.release();

        Mat rgbResult = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Imgproc.cvtColor(hsvResult, rgbResult, Imgproc.COLOR_HSV2RGB);
        hsvResult.release();

        // Convert back to three 8-bit float components in [0-255] range
        Mat result = Mat.zeros(nRows, nCols, CvType.CV_8UC3);
        rgbResult.convertTo(result, CvType.CV_8UC3);
        rgbResult.release();

        // To uncomment these debug outputs, the Mat.release() methods in the above code should be commented,
        // otherwise the matrices will be cleared before the debug outputs and "null" will be printed.
        //System.out.println("source: " + Arrays.toString(source.get(100, 100)));
        //System.out.println("source32F: " + Arrays.toString(source32F.get(100, 100)));
        //System.out.println("hsvMat: " + Arrays.toString(hsvMat.get(100, 100)));
        //System.out.println("vChannel: " + Arrays.toString(vChannel.get(100, 100)));
        //System.out.println("sigma1: " + sigma1);
        //System.out.println("blurred (filter): " + Arrays.toString(blurred.get(100, 100)));
        //System.out.println("vChannelDivided (Result of Brightness): " + Arrays.toString(vChannelDivided.get(100, 100)));
        //System.out.println("vChannelDivided_0_255 (Result of Brightness [0-255]): " + Arrays.toString(vChannelDivided_0_255.get(100, 100)));
        //System.out.println("vChannelDividedLowBlur (filter minimo): " + Arrays.toString(vChannelDividedLowBlur.get(100, 100)));
        //System.out.println("vChannelNew: " + Arrays.toString(vChannelNew.get(100, 100)));
        //System.out.println("vChannelNew_0_255: " + Arrays.toString(vChannelNew_0_255.get(100, 100)));
        //System.out.println("hsvResult: " + Arrays.toString(hsvResult.get(100, 100)));
        //System.out.println("rgbResult: " + Arrays.toString(rgbResult.get(100, 100)));

        return result;
    }

    private static Mat PerformCathodoLuminescenceCorrectionBlur (Mat source, double sigmaMultiplier)
    {
        int nRows = source.rows();
        int nCols = source.cols();

        // Convert to three 32-bit float components ranging in [0-255]
        Mat source32F = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        source.convertTo(source32F, CvType.CV_32FC3);

        // Convert in HSV (ranging in [0-255])
        Mat hsvMat = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Imgproc.cvtColor(source32F, hsvMat, Imgproc.COLOR_RGB2HSV);
        source32F.release();

        // Extract v channel
        Mat vChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.extractChannel(hsvMat, vChannel, 2);
        hsvMat.release();

        // Apply gaussian blur with a big sigma that is dependent on the image size
        vChannel = ComputeBlurredVChannel(vChannel, sigmaMultiplier);

        // Remap the blur background in 0-255 range
        Core.MinMaxLocResult vChannelNewMinMax = Core.minMaxLoc(vChannel);
        Core.subtract(vChannel, new Scalar(vChannelNewMinMax.minVal), vChannel);
        Core.multiply(vChannel, new Scalar(255.0 / (vChannelNewMinMax.maxVal - vChannelNewMinMax.minVal)), vChannel);

        return vChannel;
    }

    /**
     * Performs an image blurring using a radius that is dependent on the image size.
     * The image must be a greyscale image represented with float pixels in the [0, 1] range.
     * @param vChannel (Mat of type CvType.CV_32FC1)
     * @param sigmaMultiplier Multiplier of the image size. The blur radius will be computed applying this multiplier to the image size.
     * @return The blurred image (Mat of type CvType.CV_32FC1)
     */
    private static Mat ComputeBlurredVChannel (Mat vChannel, double sigmaMultiplier)
    {
        assert (vChannel.type() == CvType.CV_32FC1);

        int nRows = vChannel.rows();
        int nCols = vChannel.cols();

        Core.MinMaxLocResult vChannelMinMax = Core.minMaxLoc(vChannel);

        // Apply gaussian blur with a big sigma that is dependent on the image size
        double sigma1 = Math.min(nRows, nCols) * sigmaMultiplier;
        Mat blurred = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Imgproc.GaussianBlur(vChannel, blurred, new Size(0, 0), sigma1, sigma1, Core.BORDER_REPLICATE); // the size of the filter is computed using the sigma

        return blurred;
    }
}

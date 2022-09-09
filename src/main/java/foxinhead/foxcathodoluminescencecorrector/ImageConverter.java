package foxinhead.foxcathodoluminescencecorrector;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class ImageConverter
{
    public enum ConversionType
    {
        NONE
        , GREYSCALE
        , CATHODO_LUMINESCENCE_CORRECTION
    }

    private static final String[] inputSupportedTypes = { "gif", "png", "bmp", "jpg", "jpeg", "jfif", "tif"};
    private static final String[] outputSupportedTypes = { "gif", "png", "bmp", "jpg", "jpeg" };
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

    public static ByteArrayInputStream convertImageInMemory (String srcImageFileName, ConversionType conversionType)
    {
        return internalConvertImageInMemory(srcImageFileName, conversionType, defaultOutputType);
    }

    public static ByteArrayInputStream convertImageInMemory (String srcImageFileName, ConversionType conversionType, String outputType) throws IllegalArgumentException
    {
        if (isTypeSupportedAsOutput(outputType))
        {
            return internalConvertImageInMemory(srcImageFileName, conversionType, outputType);
        }
        else
        {
            throw new IllegalArgumentException("\"" + outputType + "\" is not a valid output type. Please, use one any of the following types: " + Arrays.toString(getSupportedOutputTypes()));
        }
    }

    private static ByteArrayInputStream internalConvertImageInMemory (String srcImageFileName, ConversionType conversionType, String outputType)
    {
        Mat sourceMatrix = Imgcodecs.imread(srcImageFileName);
        Mat destMatrix = ConvertMat(sourceMatrix, conversionType);
        MatOfByte encodedImageBytes = new MatOfByte();
        ByteArrayInputStream inputStream = null;
        try
        {
            Imgcodecs.imencode("." + outputType, destMatrix, encodedImageBytes);
            inputStream = new ByteArrayInputStream(encodedImageBytes.toArray());
        }
        catch (CvException e)
        {
            // caused by error: (-215:Assertion failed) !image.empty() in function 'cv::imencode'
            // the file cannot be read
            encodedImageBytes = new MatOfByte();
            inputStream = null;
        }
        return inputStream;
    }

    private static Mat ConvertMat (Mat source, ConversionType conversionType)
    {
        switch (conversionType)
        {
            case GREYSCALE:
                return ConvertToGreyScale(source);
            case CATHODO_LUMINESCENCE_CORRECTION:
                return PerformCathodoLuminescenceCorrection(source);
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

    private static Mat PerformCathodoLuminescenceCorrection (Mat source)
    {
        int nRows = source.rows();
        int nCols = source.cols();

        // Convert to three 32-bit float components, normalized in [0-1] range
        Mat source32F = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        source.convertTo(source32F, CvType.CV_32FC3);
        //Core.multiply(source32F, new Scalar(1.0/255, 1.0/255, 1.0/255), source32F);

        // Convert in HSV
        Mat hsvMat = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Imgproc.cvtColor(source32F, hsvMat, Imgproc.COLOR_RGB2HSV);
        source32F.release();

        // Extract channels
        Mat hChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.extractChannel(hsvMat, hChannel, 0);
        Mat sChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.extractChannel(hsvMat, sChannel, 1);
        Mat vChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.extractChannel(hsvMat, vChannel, 2);

        // Apply gaussian blur with a big sigma that is dependent on the image size
        double sigma1 = Math.min(nRows, nCols) / 5.0;
        Mat blurred = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Imgproc.GaussianBlur(vChannel, blurred, new Size(0, 0), sigma1, sigma1); // the size of the filter is computed using the sigma

        // Result of Brightness
        Mat vChannelReduced = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.divide(vChannel, blurred, vChannelReduced);
        vChannel.release();

        double sigma2 = 10;
        // Filter minimo
        Mat reducedBlurred = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Imgproc.GaussianBlur(vChannelReduced, reducedBlurred, new Size(0, 0), sigma2, sigma2); // the size of the filter is computed using the sigma

        Mat vChannelNew = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        Core.MinMaxLocResult reducedBlurredMinMax = Core.minMaxLoc(reducedBlurred);
        Core.subtract(vChannelReduced, new Scalar(reducedBlurredMinMax.minVal), vChannelNew);

        /* MaxValue=getResult("Max", nResults-1);
	setThreshold(0, MaxValue);
	run("NaN Background"); */

        // recombine channels
        Mat hsvResult = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Core.insertChannel(hChannel, hsvResult, 0);
        Core.insertChannel(sChannel, hsvResult, 1);
        Core.insertChannel(vChannelNew, hsvResult, 2);

        Mat rgbResult = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Imgproc.cvtColor(hsvMat, rgbResult, Imgproc.COLOR_HSV2RGB);

        // Convert back to three 8-bit float components in [0-255] range
        Mat resultToConvert = rgbResult;
        Mat result = Mat.zeros(nRows, nCols, CvType.CV_8UC3);
        //Core.multiply(resultToConvert, new Scalar(255, 255, 255), resultToConvert);
        resultToConvert.convertTo(result, CvType.CV_8UC3);

        System.out.println("color: " + Arrays.toString(result.get(100, 100))); // TODO: delete
        return result;
    }
}

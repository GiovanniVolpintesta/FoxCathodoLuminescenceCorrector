package foxinhead.foxcathodoluminescencecorrector;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public class ImageConverter
{
    public enum ConversionType
    {
        NONE
        , GREYSCALE
    }

    private static final String[] inputSupportedTypes = { "gif", "png", "bmp", "jpg", "jpeg", "jfif" };
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
            case NONE:
            default:
                Mat dest = Mat.zeros(source.rows(), source.cols(), source.type());
                source.copyTo(dest);
                return dest;
        }
    }

    private static Mat ConvertToGreyScale (Mat source)
    {
        Mat dest = Mat.zeros(source.rows(), source.cols(), source.type());
        for (int r = 0; r < source.rows(); r++)
        {
            for (int c = 0; c < source.cols(); c++)
            {
                double[] pixel = source.get(r, c);
                double brightness = pixel[0]*0.3 + pixel[1]*0.6 + pixel[2]*0.1;
                pixel[0] = pixel[1] = pixel[2] = brightness;
                dest.put(r, c, pixel);
            }
        }
        return dest;
    }
}

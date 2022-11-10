package foxinhead.foxcathodoluminescencecorrector;

import org.opencv.core.*;

import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.net.ContentHandler;
import java.util.*;

public class ImageConverter
{

    private static final String SRC_IMAGE_CACHE_KEY = "srcImage";
    private static final String RESIZED_IMAGE_CACHE_KEY = "resizedImage";

    public enum ConversionType
    {
        NONE
        , GREYSCALE
        , CATHODO_LUMINESCENCE_CORRECTION
        , BLURRED_FILTER
        , THRESHOLD_TEST
        , CATHODO_LUMINESCENCE_CORRECTION_THRESHOLD_TEST
    }

    public enum ConversionParameter
    {
        NONE
        , PARAM_SIGMA                       // double
        , NOISE_REDUCTION_ACTIVATED         // boolean
        , MAX_CONTRAST_ACTIVATED            // boolean
        , THRESHOLD_TEST_VALUE              // double
    }

    private class ConversionCache
    {

        private String srcFilename;
        private final Map<ConversionParameter, String> params;
        private final Map<String, Mat> imagesCache;
        private final Map<String, Core.MinMaxLocResult> mimMaxLocResultCache;

        private final Map<String, MatOfByte> encodedImagesCache;

        public ConversionCache ()
        {
            srcFilename = "";
            params = new HashMap<>();
            imagesCache = new HashMap<>();
            encodedImagesCache = new HashMap<>();
            mimMaxLocResultCache = new HashMap<>();
        }

        public final boolean isSameFile (String srcFilename)
        {
            return (this.srcFilename == null && srcFilename == null)
                    || (this.srcFilename != null && this.srcFilename.equals(srcFilename));
        }

        public final boolean containsParameter (ConversionParameter paramKey) { return params.containsKey(paramKey); };
        public final String getParameter(ConversionParameter paramKey) { return params.get(paramKey); }
        public final boolean areSameParameters (Map<ConversionParameter, String> params) { return this.params == params || this.params.equals(params); }
        public final void setParameter(ConversionParameter paramKey, String paramValue) { params.put(paramKey, paramValue); }

        public final boolean containsImage(String key) { return imagesCache.containsKey(key); };
        public final Mat getImage (String key) { return imagesCache.get(key); };
        public final void cacheImage(String key, Mat image)
        {
            clearCachedImage(key);
            imagesCache.put(key, image);
        }
        public final void clearCachedImage (String key)
        {
            if (imagesCache.containsKey(key))
            {
                imagesCache.get(key).release();
                imagesCache.remove(key);
            }
        }

        public final boolean containsMinMaxLocResult(String key) { return mimMaxLocResultCache.containsKey(key); };
        public final Core.MinMaxLocResult getMinMaxLocResult (String key) { return mimMaxLocResultCache.get(key); };
        public final void cacheMinMaxLocResult(String key, Core.MinMaxLocResult minMaxLocResult) { mimMaxLocResultCache.put(key, minMaxLocResult); }
        public final void clearCachedMinMaxLocResult (String key) { mimMaxLocResultCache.remove(key); };

        public final boolean containsEncodedImage(String encodingType) { return encodedImagesCache.containsKey(encodingType); };
        public final MatOfByte getEncodedImage (String encodingType) { return encodedImagesCache.get(encodingType); };
        public final void cacheEncodedImage(String encodingType, MatOfByte encodedImage)
        {
            clearCachedEncodedImage(encodingType);
            encodedImagesCache.put(encodingType, encodedImage);
        }
        public final void clearCachedEncodedImage(String encodingType)
        {
            if (encodedImagesCache.containsKey(encodingType))
            {
                encodedImagesCache.get(encodingType).release();
                encodedImagesCache.remove(encodingType);
            }
        }

        public final void clearAllCache()
        {
            for (String key : imagesCache.keySet())
            {
                imagesCache.get(key).release();
            }
            imagesCache.clear();

            for (String key : encodedImagesCache.keySet())
            {
                encodedImagesCache.get(key).release();
            }
            encodedImagesCache.clear();

            mimMaxLocResultCache.clear();
        }

        public final void clearParams()
        {
            params.clear();
        }

        public final void init (String srcFilename)
        {
            clearParams();
            clearAllCache();
            this.srcFilename = srcFilename;
        }

        public final void clear() { init(null); }

    }

    private double cachedImageSizeRatioLowerTolerance = 0.8;
    public final void setCachedImageSizeRatioLowerTolerance(double newVal)
    {
        if (newVal < 0.001 || newVal > 1.001) throw new IllegalArgumentException("newVal must be in range ]0, 1]");
        cachedImageSizeRatioLowerTolerance = newVal;
    }
    private double cachedImageSizeRatioHigherTolerance = 1.1;
    public final void setCachedImageSizeRatioHigherTolerance(double newVal)
    {
        if (newVal < 0.999) throw new IllegalArgumentException("newVal must be greater or equal than 1");
        cachedImageSizeRatioHigherTolerance = newVal;
    }

    // png, bmp, jpg, jpeg, webp, tif, ppm, pnm: conversion supported
    // gif, jfif, pbm, pgm: conversion tested and not supported
    // other formats have not been tested (probably not supported)
    private final String[] inputSupportedTypes = { "png", "bmp", "jpg", "jpeg", "tif", "ppm", "pnm", "webp" };
    private final String[] outputSupportedTypes = { "png", "bmp", "jpg", "jpeg", "tif", "ppm", "pnm", "webp" };
    private final String[] inputFileFilters; // initialized in static constructor
    private final String[] outputFileFilters; // initialized in static constructor

    private final String defaultOutputType = "png";

    public final String[] getSupportedInputTypes() { return inputSupportedTypes; }
    public final String[] getSupportedOutputTypes() { return outputSupportedTypes; }

    public final String[] getInputFileFilters() { return inputFileFilters; }
    public final String[] getOutputFileFilters() { return outputFileFilters; }

    public final String getDefaultOutputType() { return defaultOutputType; }

    private final Map<ConversionType, ConversionCache> caches;

    public ImageConverter()
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

        Map<ConversionType, ConversionCache> tmpCaches = new HashMap<>();
        for (ConversionType key : ConversionType.values())
        {
            tmpCaches.put(key, new ConversionCache());
        }
        caches = Collections.unmodifiableMap(tmpCaches);
    }

    public final boolean isTypeSupportedAsInput (String type)
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

    public final boolean isTypeSupportedAsOutput (String type)
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

    public final void clearConvertionCache (ConversionType conversionType)
    {
        caches.get(conversionType).clear();
    }
    public final void clearAllConvertionCaches ()
    {
        for (ConversionType key : caches.keySet())
        {
            caches.get(key).clear();
        }
    }

    public final ByteArrayInputStream convertImageInMemory (String srcImageFileName, ConversionType conversionType, Map<ConversionParameter, String> params, int desiredWidth, int desiredHeight)
    {
        return internalConvertImageInMemory(srcImageFileName, conversionType, defaultOutputType, params, desiredWidth, desiredHeight);
    }

    public final ByteArrayInputStream convertImageInMemory (String srcImageFileName, ConversionType conversionType, String outputType, Map<ConversionParameter, String> params, int desiredWidth, int desiredHeight) throws IllegalArgumentException
    {
        if (isTypeSupportedAsOutput(outputType))
        {
            return internalConvertImageInMemory(srcImageFileName, conversionType, outputType, params, desiredWidth, desiredHeight);
        }
        else
        {
            throw new IllegalArgumentException("\"" + outputType + "\" is not a valid output type. Please, use one any of the following types: " + Arrays.toString(getSupportedOutputTypes()));
        }
    }

    private final ByteArrayInputStream internalConvertImageInMemory (String srcImageFileName, ConversionType conversionType, String outputType, Map<ConversionParameter, String> params, int desiredWidth, int desiredHeight)
    {
        ByteArrayInputStream inputStream = null;

        ConversionCache cache = caches.get(conversionType);

        if (!cache.isSameFile(srcImageFileName))
        {
            cache.init(srcImageFileName);
        }

        // This could clear the whole cache, if the desired size is not compatible with the cached images
        Mat source = ComputeResizedSource (srcImageFileName, conversionType, params, desiredWidth, desiredHeight);

        if (!source.empty())
        {
            if (cache.isSameFile(srcImageFileName) && cache.areSameParameters(params) && cache.containsEncodedImage(outputType))
            {
                MatOfByte encodedImageBytes = cache.getEncodedImage(outputType);
                inputStream = new ByteArrayInputStream(encodedImageBytes.toArray());
            }
            else
            {
                // the result image is cached inside che ConvertMat method, so it is not necessary to handle another cached image here
                Mat conversionOutput = ConvertMat(source, conversionType, params, desiredWidth, desiredHeight);

                MatOfByte encodedImageBytes = new MatOfByte();
                try
                {
                    Imgcodecs.imencode("." + outputType, conversionOutput, encodedImageBytes);
                    inputStream = new ByteArrayInputStream(encodedImageBytes.toArray());
                    cache.cacheEncodedImage(outputType, encodedImageBytes);
                }
                catch (CvException e)
                {
                    // caused by error: (-215:Assertion failed) !image.empty() in function 'cv::imencode'
                    // the file cannot be read
                    inputStream = null;
                }
            }
        }

        return inputStream;
    }

    private final Mat ComputeResizedSource (String srcImageFileName, ConversionType conversionType, Map<ConversionParameter, String> params, int desiredWidth, int desiredHeight)
    {
        ConversionCache cache = caches.get(conversionType);

        Mat srcImage;
        if (cache.containsImage(SRC_IMAGE_CACHE_KEY))
        {
            srcImage = cache.getImage(SRC_IMAGE_CACHE_KEY);
        }
        else
        {
            srcImage = Imgcodecs.imread(srcImageFileName);
            cache.cacheImage(SRC_IMAGE_CACHE_KEY, srcImage);
        }

        if (srcImage.empty())
        {
            return srcImage;
        }

        // Resize the image to its desired size before the conversion.
        // This allows faster conversions where the desired result size is lower than the original image

        // Sanitize the desired image size
        // If parameters are missing or not valid, the image should not be resized
        boolean shouldResizeImage = ((desiredWidth > 0 && desiredHeight > 0) && (desiredWidth < srcImage.width() || desiredHeight < srcImage.height()));
        if (shouldResizeImage)
        {
            // Correct the desired size. As an image size increment is not allowed,
            // the desired size should always be lower or equal than the original image size
            desiredWidth = Math.min(desiredWidth, srcImage.width());
            desiredHeight = Math.min(desiredHeight, srcImage.height());

            //Correct the desired size to keep the original image aspect ratio
            double originalImageRatio = srcImage.width()/((double)srcImage.height());
            // r = w / h   ->   w = h * r  ->   h = w / r
            if (originalImageRatio >= 1.0)
            {
                // The image extends in horizontal direction. Keep the width and recompute the height.
                desiredHeight = (int)(desiredWidth / originalImageRatio);
            }
            else
            {
                // The image extends in vertical direction. Keep the height and recompute the width.
                desiredWidth = (int)(desiredHeight * originalImageRatio);
            }
        }
        else
        {
            // Correct the desired size.
            // If the image should not be resized, its desired size is equal to its original size.
            desiredWidth = srcImage.width();
            desiredHeight = srcImage.height();
        }

        // See if there is already a cached resized image, and if there is, check if its size is compatible with the desired one

        Mat resizedImage = null;
        if (cache.containsImage("resizedImage"))
        {
            resizedImage = cache.getImage("resizedImage");
            // Validate if the cached image is still valid for the currently requested size
            int cachedImageWidth = resizedImage.width();
            int cachedImageHeight = resizedImage.height();
            if (desiredWidth/((double)cachedImageWidth) > cachedImageSizeRatioHigherTolerance || desiredHeight/((double)cachedImageHeight) > cachedImageSizeRatioHigherTolerance // Cond_1
                    || desiredWidth/((double)cachedImageWidth) < cachedImageSizeRatioLowerTolerance || desiredHeight/((double)cachedImageHeight) < cachedImageSizeRatioLowerTolerance) // Cond_2
            {
                // Cond_1: Cannot upscale, so the desired image cannot be greater than the cached one.
                // To avoid to clear the cache because of differences in the order of few pixels, the cache is cleared only if the
                // ratio between the desired and the cached image size is greater than a tolerance.

                // Cond_2: If the ratio between the desired and the cached image size is lower than the tolerance,
                // the cached images can be cleared to allow the conversion methods to cache smaller images,
                // which should result in faster conversions.

                // Clear all the cache, but maintain the source image

                resizedImage = null; // just nullify the handle. The image memory is released when the cache is cleared.

                if (cache.containsImage(SRC_IMAGE_CACHE_KEY))
                {
                    // Create a deep copy of the old image because the cached image memory is released upon cache clearing.
                    // srcImage will reference the new image because the old one, which is stored in cache,
                    // will be released when the cache is cleared.
                    Mat oldSrcImage = srcImage;
                    srcImage = Mat.zeros(srcImage.rows(), srcImage.cols(), srcImage.type());
                    oldSrcImage.copyTo(srcImage);
                }

                cache.clear(); // clear the cache, releasing images memory

                // if this is false, there is a problem in code,
                // because the image memory has been released and it shouldn't have been.
                assert(!srcImage.empty());

                cache.cacheImage(SRC_IMAGE_CACHE_KEY, srcImage); // cache again the source image
            }
        }

        // Check if a cached resized image still exists (it could have been cleared, or it could have never existed)
        if (cache.containsImage(RESIZED_IMAGE_CACHE_KEY))
        {
            resizedImage = cache.getImage(RESIZED_IMAGE_CACHE_KEY);
        }
        else
        {
            // rows = desiredHeight; cols = desiredWidth
            resizedImage = Mat.zeros(desiredHeight, desiredWidth, srcImage.type());
            // fx and fy are 0 because the size is taken from the size parameter.
            // The interpolation is INTER_AREA because, according to the documentation, it the best to shrink an image.
            // (as the resized image is always smaller than the original image, the operation is always a shrinking)
            Imgproc.resize(srcImage, resizedImage, resizedImage.size(), 0, 0, Imgproc.INTER_AREA);
            cache.cacheImage(RESIZED_IMAGE_CACHE_KEY, resizedImage);
        }

        return resizedImage;
    }

    private final Mat ConvertMat (Mat source, ConversionType conversionType, Map<ConversionParameter, String> params, int desiredWidth, int desiredHeight)
    {
        double sigma = params.containsKey(ConversionParameter.PARAM_SIGMA) ? Double.parseDouble(params.get(ConversionParameter.PARAM_SIGMA)) : 0.0;
        boolean performNoiseReduction = params.containsKey(ConversionParameter.NOISE_REDUCTION_ACTIVATED) && Boolean.parseBoolean(params.get(ConversionParameter.NOISE_REDUCTION_ACTIVATED));
        boolean maximizeContrast = params.containsKey(ConversionParameter.MAX_CONTRAST_ACTIVATED) && Boolean.parseBoolean(params.get(ConversionParameter.MAX_CONTRAST_ACTIVATED));
        double thresholdTestValue = params.containsKey(ConversionParameter.THRESHOLD_TEST_VALUE) ? Double.parseDouble(params.get(ConversionParameter.THRESHOLD_TEST_VALUE)) : 0.0;

        if (source.empty())
        {
            return Mat.zeros(source.rows(), source.cols(), source.type());
        }

        switch (conversionType)
        {
            case GREYSCALE:
                return ConvertToGreyScale(source);
            case CATHODO_LUMINESCENCE_CORRECTION:
                return PerformCathodoLuminescenceCorrection(source, sigma, performNoiseReduction, maximizeContrast);
            case BLURRED_FILTER:
                return PerformCathodoLuminescenceCorrectionBlur(source, sigma);
            case THRESHOLD_TEST:
                return PerformThresholdTest(source, thresholdTestValue);
            case CATHODO_LUMINESCENCE_CORRECTION_THRESHOLD_TEST:
                return PerformCathodoLuminescenceCorrectionAndThresholdTest(source, sigma, performNoiseReduction, maximizeContrast, thresholdTestValue);
            case NONE:
            default:
                return CreateImageDuplicate(source);
        }
    }

    private final Mat CreateImageDuplicate(Mat source)
    {
        ConversionCache cache = caches.get(ConversionType.NONE);
        if (cache.containsImage("result"))
        {
            return cache.getImage("result");
        }

        Mat result = Mat.zeros(source.rows(), source.cols(), source.type());
        source.copyTo(result);
        cache.cacheImage("result", result);
        return result;
    }

    private final Mat ConvertToGreyScale (Mat source)
    {
        ConversionCache cache = caches.get(ConversionType.GREYSCALE);
        if (cache.containsImage("result"))
        {
            return cache.getImage("result");
        }

        // Convert in greyscale
        Mat result = Mat.zeros(source.rows(), source.cols(), CvType.CV_8UC1);
        Imgproc.cvtColor(source, result, Imgproc.COLOR_RGB2GRAY);
        cache.cacheImage("result", result);

        return result;
    }

    private final Mat PerformCathodoLuminescenceCorrection (Mat source, double sigmaMultiplier, boolean performNoiseReduction, boolean maximizeContrast)
    {
        ConversionCache cache = caches.get(ConversionType.CATHODO_LUMINESCENCE_CORRECTION);
        return InternalPerformCathodoLuminescenceCorrection(cache, source, sigmaMultiplier, performNoiseReduction, maximizeContrast);
    }

    private final Mat InternalPerformCathodoLuminescenceCorrection (ConversionCache cache, Mat source, double sigmaMultiplier, boolean performNoiseReduction, boolean maximizeContrast)
    {
        if (!cache.containsParameter(ConversionParameter.PARAM_SIGMA) || !cache.getParameter(ConversionParameter.PARAM_SIGMA).equals(Double.toString(sigmaMultiplier)))
        {
            cache.clearCachedImage("vChannelDivided_0_255");
            cache.clearCachedImage("vChannelNew_0_255");
            cache.clearCachedImage("vChannel_CorrectGamma");
            cache.clearCachedImage("result");
            // cache the parameter value that makes the cache valid
            cache.setParameter(ConversionParameter.PARAM_SIGMA, Double.toString(sigmaMultiplier));
        }
        if (!cache.containsParameter(ConversionParameter.NOISE_REDUCTION_ACTIVATED) || !cache.getParameter(ConversionParameter.NOISE_REDUCTION_ACTIVATED).equals(Boolean.toString(performNoiseReduction)))
        {
            cache.clearCachedImage("vChannelNew_0_255");
            cache.clearCachedImage("vChannel_CorrectGamma");
            cache.clearCachedImage("result");
            // cache the parameter value that makes the cache valid
            cache.setParameter(ConversionParameter.NOISE_REDUCTION_ACTIVATED, Boolean.toString(performNoiseReduction));
        }
        if (!cache.containsParameter(ConversionParameter.MAX_CONTRAST_ACTIVATED) || !cache.getParameter(ConversionParameter.MAX_CONTRAST_ACTIVATED).equals(Boolean.toString(maximizeContrast)))
        {
            cache.clearCachedImage("vChannel_CorrectGamma");
            cache.clearCachedImage("result");
            // cache the parameter value that makes the cache valid
            cache.setParameter(ConversionParameter.MAX_CONTRAST_ACTIVATED, Boolean.toString(maximizeContrast));
        }

        if (cache.containsImage("result"))
        {
            return cache.getImage("result");
        }

        int nRows = source.rows();
        int nCols = source.cols();

        // Extract channels
        Mat hChannel = null;
        Mat sChannel = null;
        Mat vChannel = null;
        if (cache.containsImage("hChannel"))
        {
            hChannel = cache.getImage("hChannel");
        }
        if (cache.containsImage("sChannel"))
        {
            sChannel = cache.getImage("sChannel");
        }
        if (cache.containsImage("vChannel"))
        {
            vChannel = cache.getImage("vChannel");
        }
        if (hChannel == null || sChannel == null || vChannel == null)
        {
            // Convert to three 32-bit float components ranging in [0-255]
            Mat source32F = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
            source.convertTo(source32F, CvType.CV_32FC3);

            // Convert in HSV (ranging in [0-255])
            Mat hsvMat = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
            Imgproc.cvtColor(source32F, hsvMat, Imgproc.COLOR_RGB2HSV);
            source32F.release();

            // Extract channels
            if (hChannel == null)
            {
                hChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
                Core.extractChannel(hsvMat, hChannel, 0);
                cache.cacheImage("hChannel", hChannel);
            }
            if (sChannel == null)
            {
                sChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
                Core.extractChannel(hsvMat, sChannel, 1);
                cache.cacheImage("sChannel", sChannel);
            }
            if (vChannel == null)
            {
                vChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
                Core.extractChannel(hsvMat, vChannel, 2);
                cache.cacheImage("vChannel", vChannel);
            }
            hsvMat.release();
        }

        Core.MinMaxLocResult vChannelMinMax;
        if (cache.containsMinMaxLocResult("vChannelMinMax"))
        {
            vChannelMinMax = cache.getMinMaxLocResult("vChannelMinMax");
        }
        else
        {
            vChannelMinMax = Core.minMaxLoc(vChannel);
            cache.cacheMinMaxLocResult("vChannelMinMax", vChannelMinMax);
        }

        Mat vChannelDivided_0_255;
        if (cache.containsImage("vChannelDivided_0_255"))
        {
            vChannelDivided_0_255 = cache.getImage("vChannelDivided_0_255");
        }
        else
        {
            // Apply gaussian blur with a big sigma that is dependent on the image size
            Mat blurred = ComputeBlurredVChannel(vChannel, sigmaMultiplier);

            // Result of Brightness
            Mat vChannelDivided = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
            // Add 1 to the whole image to avoid divisions with 0 (which produce a NaN result).
            // This does not introduce a problem, because the image is in 0-255 range
            // and the result is remapped in 0-255 range later.
            Core.add(blurred, new Scalar(1.0), blurred);
            Core.divide(vChannel, blurred, vChannelDivided);
            blurred.release();
            Core.MinMaxLocResult vChannelDividedMinMax = Core.minMaxLoc(vChannelDivided);
            //System.out.println("vChannelDivided min = " + vChannelDividedMinMax.minVal);
            //System.out.println("vChannelDivided max = " + vChannelDividedMinMax.maxVal);

            // As vChannelDivided has been computed with a division,
            // is has very low values, resulting in a pitch black image.
            // Here the image is remapped linearly in the 0-255 range to make it useful.
            vChannelDivided_0_255 = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
            Core.subtract(vChannelDivided, new Scalar(vChannelDividedMinMax.minVal), vChannelDivided_0_255);
            vChannelDivided.release();
            Core.multiply(vChannelDivided_0_255, new Scalar(255.0 / (vChannelDividedMinMax.maxVal-vChannelDividedMinMax.minVal)), vChannelDivided_0_255);
            Core.MinMaxLocResult vChannelDivided_0_255_MinMax = Core.minMaxLoc(vChannelDivided_0_255);
            //System.out.println("vChannelDivided_0_255 min = " + vChannelDivided_0_255_MinMax.minVal);
            //System.out.println("vChannelDivided_0_255 max = " + vChannelDivided_0_255_MinMax.maxVal);

            cache.cacheImage("vChannelDivided_0_255", vChannelDivided_0_255);
        }

        Mat vChannelNew_0_255;
        // if the sigma parameter is changed, the vChannelNew must be computed again
        if (cache.containsImage("vChannelNew_0_255"))
        {
            vChannelNew_0_255 = cache.getImage("vChannelNew_0_255");
        }
        else
        {
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

            }
            else
            {
                vChannelNew_0_255 = vChannelDivided_0_255;
                // Don't release vChannelDivided_0_255 yet, otherwise also vChannelNew_0_255 is released
            }

            cache.cacheImage("vChannelNew_0_255", vChannelNew_0_255);
        }

        // Remap the channel to the original vChannel Max value
        Mat vChannel_CorrectGamma;
        if (cache.containsImage("vChannel_CorrectGamma"))
        {
            vChannel_CorrectGamma = cache.getImage("vChannel_CorrectGamma");
        }
        else
        {
            vChannel_CorrectGamma = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
            if (maximizeContrast)
            {
                vChannelNew_0_255.copyTo(vChannel_CorrectGamma);
            }
            else
            {
                Core.multiply(vChannelNew_0_255, new Scalar(vChannelMinMax.maxVal / 255.0), vChannel_CorrectGamma);
            }
            cache.cacheImage("vChannel_CorrectGamma", vChannel_CorrectGamma);
        }

        // recombine channels
        Mat hsvResult = Mat.zeros(nRows, nCols, CvType.CV_32FC3);
        Core.insertChannel(hChannel, hsvResult, 0);
        Core.insertChannel(sChannel, hsvResult, 1);
        Core.insertChannel(vChannel_CorrectGamma, hsvResult, 2);

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

        cache.cacheImage("result", result);

        return result;
    }

    private final Mat PerformThresholdTest (Mat source, double thresholdValue)
    {
        ConversionCache cache = caches.get(ConversionType.THRESHOLD_TEST);
        return InternalPerformThresholdTest(cache, source, thresholdValue);
    }

    private final Mat PerformCathodoLuminescenceCorrectionAndThresholdTest (Mat source, double sigmaMultiplier, boolean performNoiseReduction, boolean maximizeContrast, double thresholdValue)
    {
        ConversionCache cache = caches.get(ConversionType.CATHODO_LUMINESCENCE_CORRECTION_THRESHOLD_TEST);
        return InternalPerformThresholdTest(cache
                ,InternalPerformCathodoLuminescenceCorrection(cache, source, sigmaMultiplier, performNoiseReduction, maximizeContrast)
        , thresholdValue);
    }

    private final Mat InternalPerformThresholdTest (ConversionCache cache, Mat source, double thresholdValue)
    {
        if (!cache.containsParameter(ConversionParameter.THRESHOLD_TEST_VALUE) || !cache.getParameter(ConversionParameter.THRESHOLD_TEST_VALUE).equals(Double.toString(thresholdValue)))
        {
            cache.clearCachedImage("thresholdTestResult");
            // cache the parameter value that makes the cache valid
            cache.setParameter(ConversionParameter.THRESHOLD_TEST_VALUE, Double.toString(thresholdValue));
        }
        if (cache.containsImage("thresholdTestResult"))
        {
            return cache.getImage("thresholdTestResult");
        }

        Mat result = Mat.zeros(source.rows(), source.cols(), CvType.CV_32FC1);
        if (!source.empty())
        {
            // Convert to three 32-bit float components ranging in [0-255]
            Mat source32F = Mat.zeros(source.rows(), source.cols(), CvType.CV_32FC3);
            source.convertTo(source32F, CvType.CV_32FC3);

            // Convert in HSV (ranging in [0-255])
            Mat hsvMat = Mat.zeros(source.rows(), source.cols(), CvType.CV_32FC3);
            Imgproc.cvtColor(source32F, hsvMat, Imgproc.COLOR_RGB2HSV);
            source32F.release();

            // extract vChannel
            Mat vChannel = Mat.zeros(source.rows(), source.cols(), CvType.CV_32FC1);
            Core.extractChannel(hsvMat, vChannel, 2);
            cache.cacheImage("vChannel", vChannel);
            hsvMat.release();

            Imgproc.threshold(vChannel, result, thresholdValue, 255.0, Imgproc.THRESH_BINARY);
            vChannel.release();
        }

        cache.cacheImage("thresholdTestResult", result);
        return result;
    }

    private final Mat PerformCathodoLuminescenceCorrectionBlur (Mat source, double sigmaMultiplier)
    {
        ConversionCache cache = caches.get(ConversionType.BLURRED_FILTER);

        if (!cache.containsParameter(ConversionParameter.PARAM_SIGMA) || !cache.getParameter(ConversionParameter.PARAM_SIGMA).equals(Double.toString(sigmaMultiplier)))
        {
            cache.clearCachedImage("result");
            // cache the parameter value that makes the cache valid
            cache.setParameter(ConversionParameter.PARAM_SIGMA, Double.toString(sigmaMultiplier));
        }

        if (cache.containsImage("result"))
        {
            return cache.getImage("result");
        }

        Mat vChannel;
        if (cache.containsImage("vChannel"))
        {
            vChannel = cache.getImage("vChannel");
        }
        else
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
            vChannel = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
            Core.extractChannel(hsvMat, vChannel, 2);
            hsvMat.release();

            cache.cacheImage("vChannel", vChannel);
        }

        // Apply gaussian blur with a big sigma that is dependent on the image size
        Mat result = ComputeBlurredVChannel(vChannel, sigmaMultiplier);

        // Remap the blur background in 0-255 range
        Core.MinMaxLocResult vChannelNewMinMax = Core.minMaxLoc(result);
        Core.subtract(result, new Scalar(vChannelNewMinMax.minVal), result);
        Core.multiply(result, new Scalar(255.0 / (vChannelNewMinMax.maxVal - vChannelNewMinMax.minVal)), result);

        cache.cacheImage("result", result);

        return result;
    }

    /**
     * Performs an image blurring using a radius that is dependent on the image size.
     * The image must be a greyscale image represented with float pixels in the [0, 1] range.
     * @param vChannel (Mat of type CvType.CV_32FC1)
     * @param sigmaMultiplier Multiplier of the image size. The blur radius will be computed applying this multiplier to the image size.
     * @return The blurred image (Mat of type CvType.CV_32FC1)
     */
    private final Mat ComputeBlurredVChannel (Mat vChannel, double sigmaMultiplier)
    {
        assert (vChannel.type() == CvType.CV_32FC1);

        int nRows = vChannel.rows();
        int nCols = vChannel.cols();

        Core.MinMaxLocResult vChannelMinMax = Core.minMaxLoc(vChannel);

        // Apply gaussian blur with a big sigma that is dependent on the image size
        double sigma1 = Math.min(nRows, nCols) * sigmaMultiplier;
        Mat result = Mat.zeros(nRows, nCols, CvType.CV_32FC1);
        if (sigma1 >= 1)
        {
            Imgproc.GaussianBlur(vChannel, result, new Size(0, 0), sigma1, sigma1, Core.BORDER_REPLICATE); // the size of the filter is computed using the sigma
        }
        else
        {
            vChannel.copyTo(result);
        }

        return result;
    }
}

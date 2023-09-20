package com.volpintesta.IBBIC;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;

import java.util.HashMap;
import java.util.Map;

class ConversionCache {

    private String srcFilename;
    private final Map<ImageConverter.ConversionParameter, String> params;
    private final Map<String, Mat> imagesCache;
    private final Map<String, Core.MinMaxLocResult> mimMaxLocResultCache;

    private final Map<String, MatOfByte> encodedImagesCache;

    public ConversionCache() {
        srcFilename = "";
        params = new HashMap<>();
        imagesCache = new HashMap<>();
        encodedImagesCache = new HashMap<>();
        mimMaxLocResultCache = new HashMap<>();
    }

    public final boolean isSameFile(String srcFilename) {
        return (this.srcFilename == null && srcFilename == null)
                || (this.srcFilename != null && this.srcFilename.equals(srcFilename));
    }

    public final boolean containsParameter(ImageConverter.ConversionParameter paramKey) {
        return params.containsKey(paramKey);
    }

    ;

    public final String getParameter(ImageConverter.ConversionParameter paramKey) {
        return params.get(paramKey);
    }

    public final boolean areSameParameters(Map<ImageConverter.ConversionParameter, String> params) {
        return this.params == params || this.params.equals(params);
    }

    public final void setParameter(ImageConverter.ConversionParameter paramKey, String paramValue) {
        params.put(paramKey, paramValue);
    }

    public final boolean containsImage(String key) {
        return imagesCache.containsKey(key);
    }

    ;

    public final Mat getImage(String key) {
        return imagesCache.get(key);
    }

    ;

    public final void cacheImage(String key, Mat image) {
        clearCachedImage(key);
        imagesCache.put(key, image);
    }

    public final void clearCachedImage(String key) {
        if (imagesCache.containsKey(key)) {
            imagesCache.get(key).release();
            imagesCache.remove(key);
        }
    }

    public final boolean containsMinMaxLocResult(String key) {
        return mimMaxLocResultCache.containsKey(key);
    }

    ;

    public final Core.MinMaxLocResult getMinMaxLocResult(String key) {
        return mimMaxLocResultCache.get(key);
    }

    ;

    public final void cacheMinMaxLocResult(String key, Core.MinMaxLocResult minMaxLocResult) {
        mimMaxLocResultCache.put(key, minMaxLocResult);
    }

    public final void clearCachedMinMaxLocResult(String key) {
        mimMaxLocResultCache.remove(key);
    }

    ;

    public final boolean containsEncodedImage(String encodingType) {
        return encodedImagesCache.containsKey(encodingType);
    }

    ;

    public final MatOfByte getEncodedImage(String encodingType) {
        return encodedImagesCache.get(encodingType);
    }

    ;

    public final void cacheEncodedImage(String encodingType, MatOfByte encodedImage) {
        clearCachedEncodedImage(encodingType);
        encodedImagesCache.put(encodingType, encodedImage);
    }

    public final void clearCachedEncodedImage(String encodingType) {
        if (encodedImagesCache.containsKey(encodingType)) {
            encodedImagesCache.get(encodingType).release();
            encodedImagesCache.remove(encodingType);
        }
    }

    public final void clearAllCache() {
        for (String key : imagesCache.keySet()) {
            imagesCache.get(key).release();
        }
        imagesCache.clear();

        for (String key : encodedImagesCache.keySet()) {
            encodedImagesCache.get(key).release();
        }
        encodedImagesCache.clear();

        mimMaxLocResultCache.clear();
    }

    public final void clearParams() {
        params.clear();
    }

    public final void init(String srcFilename) {
        clearParams();
        clearAllCache();
        this.srcFilename = srcFilename;
    }

    public final void clear() {
        init(null);
    }

}

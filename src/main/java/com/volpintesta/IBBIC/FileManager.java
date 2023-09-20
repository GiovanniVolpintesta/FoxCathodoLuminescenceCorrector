package com.volpintesta.IBBIC;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FileManager
{
    private File workingDirectory;
    private File[] candidateFiles;

    private final ImageConverter imageConverter;

    public static String getFileType(File f)
    {
        if (!f.isDirectory())
        {
            int separatorIndex = f.getName().lastIndexOf(".");
            if (separatorIndex >= 0 && separatorIndex < f.getName().length())
            {
                return f.getName().substring(separatorIndex + 1);
            }
        }
        return "";
    }

    public boolean isFileInputSupported (File f) { return f != null && imageConverter.isTypeSupportedAsInput(getFileType(f)); }
    public boolean isFileOutputSupported (File f) { return f != null && imageConverter.isTypeSupportedAsOutput(getFileType(f)); }

    public FileManager (File initialWorkingDirectory, ImageConverter imageConverter)
    {
        this.workingDirectory = initialWorkingDirectory;
        this.imageConverter = imageConverter;
    }
    public FileManager(ImageConverter imageConverter)
    {
        this(null, imageConverter);
    }

    public File getWorkingDirectory() { return workingDirectory; }
    public File getFileAtIndex (int index) { return (index >= 0 && index < candidateFiles.length) ? candidateFiles[index] : null; }
    public int getFilesCount() { return candidateFiles.length; }

    public void setupFiles(File f)
    {
        workingDirectory = null;
        candidateFiles = new File[0];

        if (f != null)
        {
            ArrayList<File> files = new ArrayList<>();
            if (f.isDirectory())
            {
                workingDirectory = f;
                File[] directoryFiles = f.listFiles();
                if (directoryFiles != null)
                {
                    for (File tempFile : directoryFiles)
                    {
                        if (tempFile != null && tempFile.exists() && !tempFile.isDirectory() && isFileInputSupported(tempFile))
                        {
                            files.add(tempFile);
                        }
                    }
                }
            }
            else
            {
                workingDirectory = f.getParentFile();
                files.add(f);
            }
            candidateFiles = files.toArray(new File[0]);
        }
    }

    public InputStream getConvertedImageInputStream (int fileIndex, ImageConverter.ConversionType conversionType, String outputType, Map<ImageConverter.ConversionParameter, String> params, int desiredWidth, int desiredHeight) throws IllegalArgumentException
    {
        File file = getFileAtIndex(fileIndex);
        if (file != null && file.exists())
        {
            return imageConverter.convertImageInMemory(file.getAbsolutePath(), conversionType, outputType, params, desiredWidth, desiredHeight);
        }
        return null;
    }

    public void convertAndSaveFile (File srcFile, File dstFile, ImageConverter.ConversionType conversionType, Map<ImageConverter.ConversionParameter, String> params) throws IOException, IllegalArgumentException, UnsupportedEncodingException
    {
        if (srcFile != null && dstFile != null
                && !srcFile.isDirectory() && !dstFile.isDirectory())
        {
            if (!isFileInputSupported(srcFile))
            {
                throw new UnsupportedEncodingException("The source file has not a supported encoding. Only the following encodings are supported: " + Arrays.toString(imageConverter.getInputFileFilters()));
            }

            // clear conversion cache: the conversion result (saved) should not be affected by cached settings such as the images size, which are used to compute the preview images.
            imageConverter.clearConvertionCache(conversionType);

            InputStream convertedImage = imageConverter.convertImageInMemory(srcFile.getAbsolutePath(), conversionType, getFileType(dstFile), params, -1, -1);
            if (convertedImage == null || convertedImage.available() == 0)
            {
                if (convertedImage != null)
                {
                    convertedImage.close();
                }
                throw new IOException("The file conversion has failed");
            }

            if (!dstFile.createNewFile())
            {
                throw new IOException("Cannot create the destination file");
            }

            OutputStream fileOutputStream = null;
            try
            {
                fileOutputStream = new FileOutputStream(dstFile);
                byte[] bytes = new byte[1024];
                while (convertedImage.available() > 0)
                {
                    int readBytesNum = convertedImage.read(bytes, 0, bytes.length);
                    fileOutputStream.write(bytes, 0, readBytesNum);
                }
            }
            finally
            {
                convertedImage.close();
                if (fileOutputStream != null)
                {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
            }
        }
    }

    public static File resolveFileNameCollision (File f) { return resolveFileNameCollision(f, new ArrayList<>()); }
    public static File resolveFileNameCollision (File f, List<File> fileBlacklist)
    {
        if (f == null)
        {
            return null;
        }
        else if (!f.exists())
        {
            return f;
        }
        else
        {
            File directory = f.getParentFile();
            String filename = f.getName();
            String extension = "";
            int number = 1;

            // divide the extension (including the ".") from the rest of the filename
            int extensionPointIndex = filename.lastIndexOf(".");
            if (extensionPointIndex >= 0 && extensionPointIndex < filename.length())
            {
                extension = filename.substring(extensionPointIndex); // including the point
                filename = filename.substring(0, extensionPointIndex);
            }

            // Search for a numeric appendix in the form " (N)" (with any number of digits)
            // If found remove it and, initialize the new appendix number to N+1
            int spaceIndex = filename.lastIndexOf(" ");
            if (spaceIndex >= 0 && spaceIndex < filename.length())
            {
                String appendix = filename.substring(spaceIndex + 1);
                if (appendix.matches("\\([0-9]+\\)"))
                {
                    filename = filename.substring(0, spaceIndex);
                    appendix = appendix.substring(1, appendix.length() - 1); // remove parenthesis
                    number = Integer.parseInt(appendix) + 1;
                }
            }

            // Find the first file with appendix that does not exist
            File newFile;
            while (true)
            {
                newFile = new File (directory, filename + " (" + number + ")" + extension);
                if (!newFile.exists())
                {
                    boolean foundBlacklistedFile = false;
                    for (File blacklistedFile : fileBlacklist)
                    {
                        if (newFile.getPath().equals(blacklistedFile.getPath()))
                        {
                            foundBlacklistedFile = true;
                            break;
                        }
                    }
                    if (!foundBlacklistedFile)
                    {
                        break;
                    }
                }
                ++number;
            }
            return newFile;
        }
    }
}

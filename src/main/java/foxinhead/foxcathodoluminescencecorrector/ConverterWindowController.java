package foxinhead.foxcathodoluminescencecorrector;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.*;
import java.util.*;

public class ConverterWindowController
{
    private final String defaultSrcImagePath = ".\\images\\default_src_image.png";
    private final String defaultDstImagePath = ".\\images\\default_dst_image.png";
    private final String brokenFileImagePath = ".\\images\\broken_file.png";

    @FXML
    private Pane mainPane;

    @FXML
    private ImageView sourceImageView;

    @FXML
    private ImageView convertedImageView;

    @FXML
    private HBox imagesPane;

    @FXML
    private Button firstButton;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button lastButton;
    //@FXML
    //private ToggleButton maximizeToggleButton;

    @FXML
    private Button saveButton;

    private final FileManager fileManager;
    private int currentFileIndex = -1;

    public static final ImageConverter.ConversionType conversionType = ImageConverter.ConversionType.GREYSCALE;

    public ConverterWindowController ()
    {
        fileManager = new FileManager();
    }

    public void init() throws IOException
    {
        imagesPane.widthProperty().addListener((property, oldValue, newValue) ->
        {
            resizeImages(newValue.doubleValue(), imagesPane.getHeight());
        });
        imagesPane.heightProperty().addListener((property, oldValue, newValue) ->
        {
            resizeImages(imagesPane.getWidth(), newValue.doubleValue());
        });

        double minWidth = mainPane.getMinWidth();
        double minHeight = mainPane.getMinHeight();
        double sceneWidth = mainPane.getScene().getWidth();
        double sceneHeight = mainPane.getScene().getHeight();
        double windowWidth = mainPane.getScene().getWindow().getWidth();
        double windowHeight = mainPane.getScene().getWindow().getHeight();
        ((Stage)mainPane.getScene().getWindow()).setMinWidth(minWidth + windowWidth - sceneWidth);
        ((Stage)mainPane.getScene().getWindow()).setMinHeight(minHeight + windowHeight - sceneHeight);

        setupFilesCollection(fileManager.getWorkingDirectory());
    }

    public void onOpenFileButtonClick(ActionEvent actionEvent) throws IOException
    {
        Node eventTarget = (Node)actionEvent.getTarget();
        Window eventWindow = eventTarget.getScene().getWindow();

        FileChooser fileChooser = new FileChooser ();
        fileChooser.setTitle("Select an image file");
        fileChooser.setInitialDirectory(fileManager.getWorkingDirectory());
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Any image type", Arrays.asList(ImageConverter.getInputFileFilters())));

        File chosenFile = fileChooser.showOpenDialog(eventWindow);
        if (chosenFile != null)
        {
            setupFilesCollection(chosenFile);
        }
    }

    public void onOpenDirectoryButtonClick(ActionEvent actionEvent) throws IOException
    {
        Node eventTarget = (Node)actionEvent.getTarget();
        Window eventWindow = eventTarget.getScene().getWindow();

        DirectoryChooser dirChooser = new DirectoryChooser ();
        dirChooser.setTitle("Select a directory");
        dirChooser.setInitialDirectory(fileManager.getWorkingDirectory());

        File chosenDirectory = dirChooser.showDialog(eventWindow);
        if (chosenDirectory != null)
        {
            setupFilesCollection(chosenDirectory);
        }
    }

    private void setupFilesCollection (File root) throws IOException
    {
        fileManager.setupFiles(root);
        setCurrentFileIndex((fileManager.getFilesCount() > 0) ? 0 : -1);
        saveButton.setDisable(fileManager.getFilesCount() == 0);
    }

    private void setCurrentFileIndex (int index) throws IOException
    {
        currentFileIndex = -1;

        InputStream srcImageInputStream = null;
        InputStream dstImageInputStream = null;
        if (index >= 0 && index < fileManager.getFilesCount())
        {
            currentFileIndex = index;
            File currentFile = fileManager.getFileAtIndex(currentFileIndex);
            srcImageInputStream = fileManager.getSourceImageInputStream(currentFileIndex);
            try
            {
                dstImageInputStream = fileManager.getConvertedImageInputStream(currentFileIndex, conversionType, FileManager.getFileType(currentFile));
            }
            catch (IllegalArgumentException e)
            {
                // The image type is not supported as conversion output, so we'll proceed with the default conversion output type
                dstImageInputStream = fileManager.getConvertedImageInputStream(currentFileIndex, conversionType);
            }
        }
        if (srcImageInputStream == null)
        {
            srcImageInputStream = new FileInputStream(fileManager.getFilesCount() > 0 ? brokenFileImagePath : defaultSrcImagePath);
        }
        if (dstImageInputStream == null)
        {
            dstImageInputStream = new FileInputStream(fileManager.getFilesCount() > 0 ? brokenFileImagePath : defaultDstImagePath);
        }

        Image sourceImage = new Image(srcImageInputStream);
        Image convertedImage = new Image (dstImageInputStream);

        BorderPane p;
        double paneWidth = imagesPane.getWidth();
        double paneHeight = imagesPane.getHeight();
        sourceImageView.setImage(sourceImage);
        convertedImageView.setImage(convertedImage);
        resizeImages(paneWidth, paneHeight);

        //setMaxImageDimension(sourceImageView, maxImageWidth, paneHeight);
        //setMaxImageDimension(convertedImageView, maxImageWidth, paneHeight);

        firstButton.setDisable(fileManager.getFileAtIndex(currentFileIndex - 1) == null);
        previousButton.setDisable(fileManager.getFileAtIndex(currentFileIndex - 1) == null);
        nextButton.setDisable(fileManager.getFileAtIndex(currentFileIndex + 1) == null);
        lastButton.setDisable(fileManager.getFileAtIndex(currentFileIndex + 1) == null);
    }

    private void resizeImages (double paneWidth, double paneHeight)
    {
        double imageWidth = (paneWidth - imagesPane.getSpacing()) / 2;
        double imageHeight = paneHeight;
        sourceImageView.setFitWidth(Math.min(imageWidth, sourceImageView.getImage().getWidth()));
        sourceImageView.setFitHeight(Math.min(imageHeight, sourceImageView.getImage().getHeight()));
        convertedImageView.setFitWidth(Math.min(imageWidth, convertedImageView.getImage().getWidth()));
        convertedImageView.setFitHeight(Math.min(imageHeight, convertedImageView.getImage().getHeight()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveFile (Window ownerWindow)
    {
        File srcFile = fileManager.getFileAtIndex(currentFileIndex);
        if (srcFile != null)
        {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save a file");
            fileChooser.setInitialDirectory(srcFile.getParentFile());
            fileChooser.setInitialFileName(srcFile.getName());
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Any image type", Arrays.asList(ImageConverter.getOutputFileFilters())));

            File dstFile = fileChooser.showSaveDialog(ownerWindow);
            if (dstFile != null)
            {
                // Check the extension, as the user could choose extensions that are not valid (even if the filter has been set in the dialog)
                if (!FileManager.isFileOutputSupported(dstFile))
                {
                    Alert extensionAlert = new Alert(Alert.AlertType.CONFIRMATION
                            , "The file type is not supported as conversion output. Both the image type and file extension will be converted to " + ImageConverter.getDefaultOutputType().toUpperCase() + ". Would you like to proceed?"
                            , ButtonType.YES
                            , ButtonType.NO
                    );
                    Optional<ButtonType> response = extensionAlert.showAndWait();
                    if (response.isPresent() && response.get() == ButtonType.YES)
                    {
                        String filename = dstFile.getName();
                        int extensionPointIndex = filename.lastIndexOf(".");
                        if (extensionPointIndex >= 0 && extensionPointIndex < filename.length())
                        {
                            filename = filename.substring(0, extensionPointIndex); // remove point and extension
                        }
                        dstFile = new File(dstFile.getParentFile(), filename + "." + ImageConverter.getDefaultOutputType());
                    }
                    else
                    {
                        return;
                    }
                }
                
                while (dstFile.exists()) // while because the user can do something with the dialog open
                {
                    ButtonType OverwriteButton = new ButtonType("Overwrite", ButtonBar.ButtonData.OTHER);
                    ButtonType KeepBothButton = new ButtonType("Keep both files", ButtonBar.ButtonData.OTHER);
                    Alert dialog = new Alert(Alert.AlertType.CONFIRMATION
                            , "The output file already exists. What do you want to do?"
                            , OverwriteButton
                            , KeepBothButton
                            , new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                            );
                    Optional<ButtonType> response = dialog.showAndWait();
                    if (response.isPresent() && response.get() == OverwriteButton)
                    {
                        if (dstFile.exists()) // check again as some time has passed
                        {
                            dstFile.delete(); //TODO questo non può funzinare: il file è lockato dal programma e non viene eliminato. Quindi si resta in un loop infinito.
                        }
                    }
                    else if (response.isPresent() && response.get() == KeepBothButton)
                    {
                        if (dstFile.exists()) // check again as some time has passed
                        {
                            dstFile = FileManager.resolveFileNameCollision(dstFile);
                        }
                    }
                    else
                    {
                        String msg = "Save operation canceled.";
                        Alert popup = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.CLOSE);
                        popup.show();
                        return;
                    }
                }
                try
                {
                    // UnsupportedEncodingException should never be thrown because the images collection includes
                    // only supported types (see FileManager.setupFiles).
                    // IllegalArgumentException should never be called because the extension is checked previously in this method.
                    fileManager.convertAndSaveFile(srcFile, dstFile, conversionType);
                    String msg = "Conversion ended with success!";
                    Alert popup = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.CLOSE);
                    popup.show();
                }
                catch (IllegalArgumentException e)
                {
                    if (dstFile.exists())
                    {
                        dstFile.delete();
                    }
                    String msg = "The extension of the image does not correspond to any supported conversion output type. Please, repeat the save operation using one of the following extensions:\n" + Arrays.toString(ImageConverter.getSupportedOutputTypes());
                    Alert popup = new Alert(Alert.AlertType.ERROR, msg, ButtonType.CLOSE);
                    popup.show();
                }
                catch (IOException e)
                {
                    if (dstFile.exists())
                    {
                        dstFile.delete();
                    }
                    String msg = "The following output file couldn't be created correctly:\n" + dstFile.getPath();
                    Alert popup = new Alert(Alert.AlertType.ERROR, msg, ButtonType.CLOSE);
                    popup.show();
                }
            }
        }
    }

    // TODO: risolvere il problema sulle sovrascrizioni. Non si può cancellare il file sorgente. Non si deve poter sovrascrivere la cartella sorgente.
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveDirectory (Window ownerWindow)
    {
        File outputDirectory = null;

        while (true)
        {
            DirectoryChooser dirChooser = new DirectoryChooser ();
            dirChooser.setTitle("Select an output directory");
            dirChooser.setInitialDirectory(outputDirectory != null ? outputDirectory : fileManager.getWorkingDirectory());
            outputDirectory = dirChooser.showDialog(ownerWindow);
            if (outputDirectory.getPath().equals(fileManager.getWorkingDirectory().getPath()))
            {
                String directoryAlertMsg = "You cannot save in the same directory where the source files are. Choose another directory.";
                Alert popup = new Alert(Alert.AlertType.ERROR, directoryAlertMsg, ButtonType.CLOSE);
                Optional<ButtonType> response = popup.showAndWait();
            }
            else
            {
                break;
            }
        }

        // Key = source file; Value = destination file.
        HashMap<File, File> filePairs = new HashMap<>();
        ArrayList<File> renamedFiles = new ArrayList<>();
        ArrayList<File> overwrittenFiles = new ArrayList<>();
        ArrayList<File> skippedFiles = new ArrayList<>();
        ArrayList<File> changedTypeFiles = new ArrayList<>();

        int conflictsCount = 0;
        int handledConflict = 0;

        Boolean confirmedTypeChange = null;
        
        // retrieve all the files to save and all the output files
        // if any output file already exists, add the source file to a conflicts list
        // that will be handled before the conversion operations.
        for (int i = 0; i < fileManager.getFilesCount(); ++i)
        {
            File srcFile = fileManager.getFileAtIndex(i);
            File dstFile = new File(outputDirectory, srcFile.getName());

            // Handle the extension problem: FileManager.convertAndSaveFile throws IllegalArgumentException if the type
            // of the dstImage is not supported as conversion output. We can convert such images to the default output
            // type defined by ImageConverter, but the file extension should be changed accordingly, and it should be
            // changed here to correctly handle the file conflicts later.
            if (!FileManager.isFileOutputSupported(dstFile))
            {
                if (confirmedTypeChange == null) // If the popup has never been shown
                {
                    Alert extensionAlert = new Alert(Alert.AlertType.CONFIRMATION
                            , "Some of the file types are not supported as conversion output. Both the unsupported images type and files extension will be converted to " + ImageConverter.getDefaultOutputType().toUpperCase() + ". Would you like to proceed?"
                            , ButtonType.YES
                            , ButtonType.NO
                    );
                    Optional<ButtonType> response = extensionAlert.showAndWait();
                    if (response.isPresent() && response.get() == ButtonType.YES)
                    {
                        confirmedTypeChange = true; // Do not show the popup anymore
                    }
                    else
                    {
                        return; // save operation aborted
                    }
                }
                
                if (confirmedTypeChange != null && confirmedTypeChange.booleanValue())
                {
                    String filename = dstFile.getName();
                    int extensionPointIndex = filename.lastIndexOf(".");
                    if (extensionPointIndex >= 0 && extensionPointIndex < filename.length())
                    {
                        filename = filename.substring(0, extensionPointIndex); // excluding point and extension
                    }
                    dstFile = new File(dstFile.getParentFile(), filename + "." + ImageConverter.getDefaultOutputType());
                    changedTypeFiles.add(srcFile);
                }
            }

            filePairs.put(srcFile, dstFile);
            if (dstFile.exists())
            {
                ++conflictsCount;
            }
        }

        ButtonType OverwriteButton = new ButtonType("Overwrite", ButtonBar.ButtonData.OTHER);
        ButtonType OverwriteAllButton = new ButtonType("Overwrite (All)", ButtonBar.ButtonData.OTHER);
        ButtonType KeepBothButton = new ButtonType("Keep both files", ButtonBar.ButtonData.OTHER);
        ButtonType KeepBothAllButton = new ButtonType("Keep both files (All)", ButtonBar.ButtonData.OTHER);
        ButtonType SkipButton = new ButtonType("Skip this file", ButtonBar.ButtonData.OTHER);
        ButtonType CancelAllButton = new ButtonType("Cancel (All)", ButtonBar.ButtonData.OTHER);

        ButtonType ChosenOperationForAllFiles = null;

        Set<File> srcFiles = filePairs.keySet();
        for (File srcFile : srcFiles)
        {
            File dstFile = filePairs.get(srcFile);

            if (dstFile.exists())
            {
                ButtonType ChosenOperation;
                if (ChosenOperationForAllFiles != null)
                {
                    ChosenOperation = ChosenOperationForAllFiles;
                }
                else
                {
                    String msg = "The output file \"" + dstFile.getName() + "\" already exists. What do you want to do?"
                            + "\n(Handling conflict " + (handledConflict + 1) + " of " + conflictsCount + ")";
                    Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, msg
                            , OverwriteButton, OverwriteAllButton, KeepBothButton, KeepBothAllButton
                            , SkipButton, CancelAllButton
                    );
                    Optional<ButtonType> response = dialog.showAndWait();
                    ChosenOperation = response.orElse(CancelAllButton);
                    if (ChosenOperation == OverwriteAllButton
                            || ChosenOperation == KeepBothAllButton
                            || ChosenOperation == CancelAllButton)
                    {
                        ChosenOperationForAllFiles = ChosenOperation;
                    }
                }

                if (ChosenOperation == OverwriteButton || ChosenOperation == OverwriteAllButton)
                {
                    overwrittenFiles.add(dstFile);
                }
                else if (ChosenOperation == KeepBothButton || ChosenOperation == KeepBothAllButton)
                {
                    dstFile = FileManager.resolveFileNameCollision(dstFile, renamedFiles);
                    renamedFiles.add(dstFile);
                    filePairs.put(srcFile, dstFile);
                }
                else if (ChosenOperation == SkipButton)
                {
                    skippedFiles.add(srcFile);
                }
                else if (ChosenOperation == CancelAllButton)
                {
                    return;
                }
                ++handledConflict;
            }
        }

        // Do the conversions and save the files.
        // If any error is thrown during these operations, the file is added to a list
        // of not handled errors that will be shown at the end
        ArrayList<File> errorFilesList = new ArrayList<>();
        for (File srcFile : filePairs.keySet())
        {
            if (!skippedFiles.contains(srcFile))
            {
                File dstFile = filePairs.get(srcFile);
                try
                {
                    if (dstFile.exists())
                    {
                        dstFile.delete();
                    }
                    // This should never call the IllegalArgumentException, because on the top of this method it has
                    // been ensured that the files have a supported extension and conversion type. If the exception
                    // is triggered, there is a problem either in that routine or in the conversion types management.
                    // Remember that the conversion type should be aligned with the file extension, and the file
                    // conflicts should be checked with the final extension.
                    // UnsupportedEncodingException should never be thrown because the images collection includes
                    // only supported types (see FileManager.setupFiles).
                    fileManager.convertAndSaveFile(srcFile, dstFile, conversionType);
                }
                catch (IOException e)
                {
                    if (dstFile.exists())
                    {
                        dstFile.delete();
                    }
                    errorFilesList.add(srcFile);
                }
            }
        }

        String endPopupMsg = errorFilesList.isEmpty()
                ? "Conversion ended with success for all the files."
                : "Conversion ended with success for a part of the files.";
        Alert.AlertType endPopupType = errorFilesList.isEmpty()
                ? Alert.AlertType.INFORMATION
                : Alert.AlertType.WARNING;
        ButtonType detailsButton = new ButtonType("Show details", ButtonBar.ButtonData.OTHER);
        Alert endPopup = new Alert(endPopupType, endPopupMsg, detailsButton, ButtonType.CLOSE);
        endPopup.resizableProperty().set(true);
        Optional<ButtonType> endPopupResponse = endPopup.showAndWait();

        if (endPopupResponse.isPresent() && endPopupResponse.get() == detailsButton)
        {
            StringBuilder detailsPopupMsg = new StringBuilder("DIRECTORY CONVERSION DETAILS\n");
            detailsPopupMsg.append("\n working directory = \"").append(fileManager.getWorkingDirectory().getPath()).append("\"");
            detailsPopupMsg.append("\n output directory = \"").append(outputDirectory.getPath()).append("\"");
            detailsPopupMsg.append("\n");
            for (File srcFile : filePairs.keySet())
            {
                File dstFile = filePairs.get(srcFile);
                String msgLine = "\n";
                if (skippedFiles.contains(srcFile))
                {
                    msgLine += "SKIPPED CONVERSION OF " + srcFile.getName();
                }
                else if (errorFilesList.contains(srcFile))
                {
                    msgLine += "FAILED CONVERSION OF " + srcFile.getName();
                }
                else
                {
                    msgLine += overwrittenFiles.contains(dstFile) ? "OVERWRITTEN " : "CREATED ";
                    msgLine += dstFile.getName();
                    if (renamedFiles.contains(dstFile))
                    {
                        msgLine += " - ORIGINAL FILE NAME: " + srcFile.getName() + "";
                    }
                    if (changedTypeFiles.contains(srcFile))
                    {
                        msgLine += " - The source image type was not supported as conversion output, so the converted image type is now " + ImageConverter.getDefaultOutputType();
                    }
                }
                detailsPopupMsg.append(msgLine);
            }
            Alert detailsPopup = new Alert(Alert.AlertType.INFORMATION, detailsPopupMsg.toString(), ButtonType.CLOSE);
            detailsPopup.resizableProperty().set(true);
            detailsPopup.show();
        }
    }

    public void onPreviousImageButtonClick() throws IOException
    {
        if (fileManager.getFileAtIndex(currentFileIndex - 1) != null)
        {
            setCurrentFileIndex(currentFileIndex - 1);
        }
    }

    public void onNextImageButtonClick() throws IOException
    {
        if (fileManager.getFileAtIndex(currentFileIndex + 1) != null)
        {
            setCurrentFileIndex(currentFileIndex + 1);
        }
    }

    public void onFirstImageButtonClick() throws IOException
    {
        if (fileManager.getFileAtIndex(0) != null)
        {
            setCurrentFileIndex(0);
        }
    }

    public void onLastImageButtonClick() throws IOException
    {
        if (fileManager.getFileAtIndex(fileManager.getFilesCount() - 1) != null)
        {
            setCurrentFileIndex(fileManager.getFilesCount() - 1);
        }
    }

    public void onSaveButtonClick(ActionEvent actionEvent)
    {
        Node eventTarget = (Node)actionEvent.getTarget();
        Window eventWindow = eventTarget.getScene().getWindow();
        if (fileManager.getFileAtIndex(currentFileIndex) != null)
        {
            if (fileManager.getFilesCount() == 1)
            {
                saveFile(eventWindow);
            }
            else
            {
                saveDirectory(eventWindow);
            }
        }
    }

    public void onToggleMaximize()
    {
    }
}
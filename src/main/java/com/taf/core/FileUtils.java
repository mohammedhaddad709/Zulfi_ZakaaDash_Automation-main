package com.taf.core;

import org.openqa.selenium.TimeoutException;
import com.taf.core.utils.OutputPaths;
import com.taf.core.utils.dataReader.PropertyReader;
import com.taf.core.utils.logs.LogsManager;

import org.openqa.selenium.support.ui.FluentWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import static org.apache.commons.io.FileUtils.copyFile;

public class FileUtils {
    private static final String USER_DIR = PropertyReader.getProperty("user.dir")+ File.separator;
    private FileUtils() {
        // Prevent instantiation
    }

    // Renaming
    public static void renameFile(String oldName, String newName) {
        try {
            var targetFile = new File(oldName);
            String targetDirectory = targetFile.getParentFile().getAbsolutePath();
            File newFile = new File(targetDirectory + File.separator + newName);
            if (!targetFile.getPath().equals(newFile.getPath())) {
                copyFile(targetFile, newFile);
                org.apache.commons.io.FileUtils.deleteQuietly(targetFile);
                LogsManager.info("Target File Path: \"" + oldName + "\", file was renamed to \"" + newName + "\".");
            } else {
                LogsManager.info(("Target File Path: \"" + oldName + "\", already has the desired name \"" + newName + "\"."));
            }
        } catch (IOException e) {
            LogsManager.error(e.getMessage());
        }
    }


    //creating Directory
    public static void createDirectory(String path) {
        try {
            File file = new File(USER_DIR+ path);
            if (!file.exists())
            {
                file.mkdirs();
                LogsManager.info("Directory created: " + path);
            }
        }
        catch (Exception e) {
            LogsManager.error("Failed to create directory: " + path, e.getMessage());
        }
    }

    //force delete
    public static void forceDelete(File file) {
        try {
            org.apache.commons.io.FileUtils.forceDeleteOnExit(file);
            LogsManager.info("File deleted: " + file.getAbsolutePath());
        } catch (IOException e) {
            LogsManager.error("Failed to delete file: " + file.getAbsolutePath(), e.getMessage());
        }
    }

    // cleaning Directory
    public static void cleanDirectory(File file)
    {
        try {
            org.apache.commons.io.FileUtils.deleteQuietly(file); //in use > skip
        }
        catch (Exception e) {
            LogsManager.error("Failed to clean directory: " + file.getAbsolutePath(), e.getMessage());
        }
    }

    //check if the file exists
    public static boolean isFileExists( String fileName) {
        String filePath = OutputPaths.downloads() + File.separator;
        File file = new File(filePath+ fileName);
        return file.exists();
    }

    //wait for file to be downloaded
    public static boolean isFileExist(String fileName, int numberOfRetries) {
        if (numberOfRetries <= 0) {
            return isFileExists(fileName);
        }
        String filePath = OutputPaths.downloads() + File.separator;
        File target = new File(filePath + fileName).getAbsoluteFile();
        Duration timeout = Duration.ofMillis(500L * numberOfRetries);
        try {
            return new FluentWait<>(target)
                    .withTimeout(timeout)
                    .pollingEvery(Duration.ofMillis(250))
                    .until(File::exists);
        } catch (TimeoutException missing) {
            LogsManager.error("File not found after wait: " + target.getAbsolutePath());
            return false;
        } catch (Exception e) {
            LogsManager.error(e.getMessage());
            return target.exists();
        }
    }
}

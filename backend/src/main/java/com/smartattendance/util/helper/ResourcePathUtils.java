package com.smartattendance.util.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class ResourcePathUtils {

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * gets resources you have put in src/main/resources/
     * @param resourcePath is either a filename or directory/filename without a leading /
     * @return the full path of the resource
     */
    public String getResourceFilePath(String resourcePath) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
            String extension = resourcePath.contains(".")
                    ? resourcePath.substring(resourcePath.lastIndexOf('.'))
                    : "";

            Path temp = Files.createTempFile("resource-", extension);
            Files.copy(resource.getInputStream(), temp, StandardCopyOption.REPLACE_EXISTING);

            return temp.toAbsolutePath().toString();

        } catch (IOException e) {
            return "Cannot get resource" + resourcePath;
        }
    }

}

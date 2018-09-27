package org.sakaiproject.maven.plugin.component;
 
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
 
/**
 * Traverses all the js files in sourceDir and copies them to targetDir, locating any js module import statements on the
 * way and optionally adding the supplied query to the end. This is great for cache busting.
 */
@Mojo(name = "copyjs")
public class CopyJSMojo extends AbstractMojo {

  @Parameter
  private String query;

  @Parameter(required=true)
  private String sourceDir;

  @Parameter(required=true)
  private String targetDir;

  public void execute() throws MojoExecutionException {

    try {
      Files.walkFileTree(Paths.get(sourceDir), new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

          Path currentTargetDir = Paths.get(targetDir).resolve(Paths.get(sourceDir).relativize(dir));
          try {
            Files.copy(dir, currentTargetDir);
          } catch (FileAlreadyExistsException e) {
            if (!Files.isDirectory(currentTargetDir)) throw e;
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

          if (file.toString().endsWith(".js")) {
            Path targetFile = Paths.get(targetDir).resolve(Paths.get(sourceDir).relativize(file));
            transform(file, targetFile);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (Exception e) {
    }
  }

  /**
   * Copies the file from source to target, optionally appending the query to any ES6 module imports
   * on the way across.
   */
  private void transform(Path file, Path targetFile) {

    try {
      try (BufferedReader reader = Files.newBufferedReader(file);
              BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardOpenOption.CREATE)) {

        reader.lines().forEach(line -> {

          try {
            if (StringUtils.isNotEmpty(this.query) && line.matches("^(import|export).*[\"'];$")) {
              // This is an import and we want to add query strings. Parse the url from the import.
              int firstQuoteIndex = line.indexOf("\"");
              if (firstQuoteIndex == -1) {
                firstQuoteIndex = line.indexOf("'");
              }
              int lastQuoteIndex = line.indexOf("\"", firstQuoteIndex + 1);
              if (lastQuoteIndex == -1) {
                lastQuoteIndex = line.indexOf("'", firstQuoteIndex + 1);
              }
              String imported = line.substring(firstQuoteIndex + 1, lastQuoteIndex);
              imported += "?" + query;
              line = line.substring(0, firstQuoteIndex + 1) + imported + line.substring(lastQuoteIndex);
            }
            writer.write(line);
            writer.newLine();
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

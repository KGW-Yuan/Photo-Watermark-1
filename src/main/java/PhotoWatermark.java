import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * PhotoWatermark is a command-line Java program that adds watermarks to images based on their EXIF date.
 * It processes all image files in a fixed 'src/photo' directory under the project root and saves watermarked
 * images to a 'src/photo/watermark' subdirectory. Users can configure font size, color, and position interactively.
 *
 * Dependencies:
 * - metadata-extractor (for EXIF reading): Place metadata-extractor-2.18.0.jar in lib/ folder.
 * - Java 8 or higher.
 */
public class PhotoWatermark {

    private static final String PHOTO_DIR = "src/photo";
    private static final String WATERMARK_DIR = "watermark";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        // Define input and output directories relative to project root
        Path inputDir = Paths.get(PHOTO_DIR);
        Path outputDir = inputDir.resolve(WATERMARK_DIR);

        // Ensure output directory exists
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            return;
        }

        // Prompt for configurations
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter font size (e.g., 20): ");
        int fontSize = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Enter color (e.g., WHITE, BLACK, RED): ");
        Color color = parseColor(scanner.nextLine().trim().toUpperCase());

        System.out.print("Enter position (TOP_LEFT, CENTER, BOTTOM_RIGHT): ");
        Position position = parsePosition(scanner.nextLine().trim().toUpperCase());

        // Process each file in the photo directory
        File[] files = inputDir.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (isImageFile(file)) {
                    processImage(file, outputDir, fontSize, color, position);
                }
            }
        }

        System.out.println("Processing complete. Watermarked images saved in: " + outputDir.toAbsolutePath());
    }

    private static boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }

    private static void processImage(File inputFile, Path outputDir, int fontSize, Color color, Position position) {
        try {
            // Read EXIF date
            String watermarkText = getExifDate(inputFile);
            if (watermarkText == null) {
                System.err.println("No EXIF date found for " + inputFile.getName() + ". Skipping.");
                return;
            }

            // Read image
            BufferedImage image = ImageIO.read(inputFile);

            // Create graphics
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            g2d.setColor(color);

            // Calculate position
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(watermarkText);
            int textHeight = fm.getHeight();

            int x, y;
            switch (position) {
                case TOP_LEFT:
                    x = 10;
                    y = textHeight + 10;
                    break;
                case CENTER:
                    x = (image.getWidth() - textWidth) / 2;
                    y = (image.getHeight() + textHeight) / 2;
                    break;
                case BOTTOM_RIGHT:
                    x = image.getWidth() - textWidth - 10;
                    y = image.getHeight() - 10;
                    break;
                default:
                    x = 10;
                    y = textHeight + 10;
            }

            // Draw text
            g2d.drawString(watermarkText, x, y);
            g2d.dispose();

            // Save new image
            String outputFileName = inputFile.getName();
            Path outputPath = outputDir.resolve(outputFileName);
            String format = outputFileName.toLowerCase().endsWith(".png") ? "PNG" : "JPEG";
            ImageIO.write(image, format, outputPath.toFile());

            System.out.println("Watermarked " + inputFile.getName() + " -> " + outputPath);

        } catch (IOException | ImageProcessingException e) {
            System.err.println("Error processing " + inputFile.getName() + ": " + e.getMessage());
        }
    }

    private static String getExifDate(File file) throws ImageProcessingException, IOException {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (directory != null) {
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (date != null) {
                return DATE_FORMAT.format(date);
            }
        }
        return null;
    }

    private static Color parseColor(String colorStr) {
        switch (colorStr) {
            case "WHITE": return Color.WHITE;
            case "BLACK": return Color.BLACK;
            case "RED": return Color.RED;
            case "GREEN": return Color.GREEN;
            case "BLUE": return Color.BLUE;
            default: return Color.WHITE; // Default to white
        }
    }

    private static Position parsePosition(String posStr) {
        switch (posStr) {
            case "TOP_LEFT": return Position.TOP_LEFT;
            case "CENTER": return Position.CENTER;
            case "BOTTOM_RIGHT": return Position.BOTTOM_RIGHT;
            default: return Position.BOTTOM_RIGHT; // Default
        }
    }

    private enum Position {
        TOP_LEFT, CENTER, BOTTOM_RIGHT
    }
}
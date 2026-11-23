package ua.edu.nupp.kpp.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {

    private static final Logger LOGGER =
            Logger.getLogger("GreeneryCare");

    static {
        try {
            Path logDirectory = Path.of("logs");
            Files.createDirectories(logDirectory);

            FileHandler fileHandler = new FileHandler(
                    logDirectory.resolve("greenery-care.log")
                            .toString(),
                    true
            );

            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            System.err.println(
                    "Не вдалося створити файл журналу: "
                            + e.getMessage()
            );
        }
    }

    private AppLogger() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void error(
            String message,
            Throwable exception) {

        LOGGER.log(Level.SEVERE, message, exception);
    }
}


package ua.edu.nupp.kpp.client.logging;

import ua.edu.nupp.kpp.client.csv.TreeCsvImporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class ClientLogger {

    private static final Logger LOGGER =
            Logger.getLogger(TreeCsvImporter.class.getName());

    private static boolean initialized = false;

    private ClientLogger() {
    }

    public static synchronized void initialize() {

        if (initialized) {
            return;
        }

        try {
            Path logsDirectory = Path.of("logs");

            Files.createDirectories(logsDirectory);

            // Не передаём обработку родительскому Logger,
            // чтобы избежать дублирования сообщений.
            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.ALL);

            Formatter formatter = new Formatter() {

                @Override
                public String format(LogRecord record) {

                    return String.format(
                            "%1$tF %1$tT [%2$s] %3$s%n",
                            record.getMillis(),
                            record.getLevel().getName(),
                            formatMessage(record)
                    );
                }
            };

            FileHandler fileHandler = new FileHandler(
                    logsDirectory.resolve("csv-import.log")
                            .toString(),
                    1_000_000,
                    3,
                    true
            );

            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(formatter);

            ConsoleHandler consoleHandler = new ConsoleHandler();

            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(formatter);

            // Удаляем старые обработчики, если они есть.
            for (Handler handler : LOGGER.getHandlers()) {
                LOGGER.removeHandler(handler);
                handler.close();
            }

            LOGGER.addHandler(fileHandler);
            LOGGER.addHandler(consoleHandler);

            initialized = true;

            LOGGER.info("Журналювання CSV-імпорту ініціалізовано");

        } catch (IOException ex) {

            // Если запись в файл невозможна,
            // сохраняем стандартное журналирование в консоль.
            LOGGER.setUseParentHandlers(true);

            LOGGER.log(
                    Level.WARNING,
                    "Не вдалося створити файл журналу",
                    ex
            );
        }
    }
}

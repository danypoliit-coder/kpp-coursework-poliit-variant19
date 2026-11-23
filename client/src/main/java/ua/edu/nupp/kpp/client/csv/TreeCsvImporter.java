
package ua.edu.nupp.kpp.client.csv;

import javafx.concurrent.Task;
import ua.edu.nupp.kpp.client.http.ApiClient;
import ua.edu.nupp.kpp.client.model.Tree;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TreeCsvImporter extends Task<TreeCsvImporter.ImportResult> {

    private static final Logger LOGGER =
            Logger.getLogger(TreeCsvImporter.class.getName());

    private static final String HEADER =
            "species,address,latitude,longitude,"
                    + "technical_state,people_intensity,green_area";

    private final ApiClient apiClient;
    private final Path file;

    public TreeCsvImporter(ApiClient apiClient, Path file) {
        this.apiClient = apiClient;
        this.file = file;
    }

    @Override
    protected ImportResult call() throws Exception {

        LOGGER.info("Початок CSV-імпорту: " + file.getFileName());
        updateMessage("Перевірка CSV-файлу...");
        updateProgress(0, 1);

        try {
            if (!Files.isRegularFile(file)) {
                throw new IOException(
                        "CSV-файл не знайдено: " + file.toAbsolutePath()
                );
            }

            long total = countDataRows();

            int imported = 0;
            int skipped = 0;
            long processed = 0;

            List<String> errors = new ArrayList<>();

            try (BufferedReader reader = Files.newBufferedReader(
                    file, StandardCharsets.UTF_8)) {

                validateHeader(reader.readLine());

                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    if (isCancelled()) {
                        LOGGER.info(
                                "CSV-імпорт скасовано. Успішно: "
                                        + imported
                        );
                        break;
                    }

                    if (line.isBlank()) {
                        continue;
                    }

                    updateMessage(
                            "Опрацювання рядка " + lineNumber
                                    + " із файла..."
                    );

                    try {
                        Tree tree = parseTree(line);

                        long id = apiClient.createTree(tree);

                        if (id <= 0) {
                            throw new IOException(
                                    "Сервер не повернув коректний ID"
                            );
                        }

                        imported++;

                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw ex;

                    } catch (Exception ex) {
                        skipped++;

                        String reason = safeReason(ex);
                        String error =
                                "Рядок " + lineNumber + ": " + reason;

                        errors.add(error);

                        LOGGER.log(
                                Level.WARNING,
                                "Пропущено " + error
                        );
                    }

                    processed++;
                    updateProgress(processed, Math.max(total, 1));
                    updateMessage(
                            "Опрацьовано: " + processed + "/" + total
                                    + " | Імпортовано: " + imported
                                    + " | Пропущено: " + skipped
                    );
                }
            }

            ImportResult result = new ImportResult(
                    imported,
                    skipped,
                    processed,
                    total,
                    List.copyOf(errors)
            );

            LOGGER.info(
                    "CSV-імпорт завершено. Імпортовано: "
                            + imported + ", пропущено: " + skipped
            );

            updateMessage(
                    "Імпорт завершено. Успішно: " + imported
                            + ", пропущено: " + skipped
            );

            if (total == 0) {
                updateProgress(1, 1);
            }

            return result;

        } catch (Exception ex) {
            LOGGER.log(
                    Level.SEVERE,
                    "Критична помилка CSV-імпорту: " + safeReason(ex)
            );
            throw ex;
        }
    }

    private long countDataRows() throws IOException {

        try (BufferedReader reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {

            validateHeader(reader.readLine());

            long count = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    count++;
                }
            }

            return count;
        }
    }

    private void validateHeader(String header) throws IOException {

        if (header == null) {
            throw new IOException("CSV-файл порожній");
        }

        String actual = header
                .replace("\uFEFF", "")
                .trim();

        if (!HEADER.equals(actual)) {
            throw new IOException(
                    "Неправильний заголовок CSV.\nОчікується:\n"
                            + HEADER
            );
        }
    }

    private Tree parseTree(String line) {

        // Формат навчального CSV:
        // рівно 7 полів, без ком усередині значень.
        String[] values = line.split(",", -1);

        if (values.length != 7) {
            throw new IllegalArgumentException(
                    "Очікується 7 полів, отримано "
                            + values.length
            );
        }

        String species = values[0].trim();
        String address = values[1].trim();

        if (species.isEmpty()) {
            throw new IllegalArgumentException(
                    "Не вказано породу дерева"
            );
        }

        if (address.isEmpty()) {
            throw new IllegalArgumentException(
                    "Не вказано адресу"
            );
        }

        Tree tree = new Tree();

        tree.setSpecies(species);
        tree.setAddress(address);

        tree.setLatitude(
                optionalDecimal(values[2], "Широта")
        );

        tree.setLongitude(
                optionalDecimal(values[3], "Довгота")
        );

        tree.setTechnicalState(
                requiredDecimal(values[4], "Технічний стан")
        );

        tree.setPeopleIntensity(
                requiredDecimal(values[5], "Інтенсивність людей")
        );

        tree.setGreenArea(
                requiredDecimal(values[6], "Зелена зона")
        );

        checkRange(
                tree.getLatitude(),
                new BigDecimal("-90"),
                new BigDecimal("90"),
                "Широта"
        );

        checkRange(
                tree.getLongitude(),
                new BigDecimal("-180"),
                new BigDecimal("180"),
                "Довгота"
        );

        checkRange(
                tree.getTechnicalState(),
                BigDecimal.ZERO,
                new BigDecimal("100"),
                "Технічний стан"
        );

        checkRange(
                tree.getPeopleIntensity(),
                BigDecimal.ZERO,
                new BigDecimal("100"),
                "Інтенсивність людей"
        );

        checkRange(
                tree.getGreenArea(),
                BigDecimal.ZERO,
                new BigDecimal("100"),
                "Зелена зона"
        );

        return tree;
    }

    private BigDecimal optionalDecimal(
            String text,
            String fieldName) {

        if (text.trim().isEmpty()) {
            return null;
        }

        return requiredDecimal(text, fieldName);
    }

    private BigDecimal requiredDecimal(
            String text,
            String fieldName) {

        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Некоректне числове значення: " + fieldName
            );
        }
    }

    private void checkRange(
            BigDecimal value,
            BigDecimal min,
            BigDecimal max,
            String name) {

        if (value == null) {
            return;
        }

        if (value.compareTo(min) < 0
                || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    name + ": значення поза допустимим діапазоном"
            );
        }
    }

    private String safeReason(Exception ex) {
        // Не записуємо HTTP-тіла, URL із параметрами
        // або повні stack trace, які можуть містити секрети.
        if (ex instanceof java.net.ConnectException) {
            return "Сервер недоступний";
        }

        if (ex instanceof java.net.http.HttpTimeoutException) {
            return "Час очікування сервера вичерпано";
        }

        if (ex instanceof IOException
                && ex.getMessage() != null
                && ex.getMessage().startsWith("HTTP ")) {
            String status = ex.getMessage().split(":", 2)[0];
            return "Помилка сервера: " + status;
        }

        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }

        if (ex instanceof IOException) {
            return "Помилка читання файла або HTTP-запиту";
        }

        return ex.getClass().getSimpleName();
    }

    public record ImportResult(
            int imported,
            int skipped,
            long processed,
            long total,
            List<String> errors) {

        public String summary() {

            StringBuilder sb = new StringBuilder();

            sb.append("CSV-імпорт завершено.\n\n");
            sb.append("Опрацьовано: ").append(processed);
            sb.append(" із ").append(total);
            sb.append("\nІмпортовано: ").append(imported);
            sb.append("\nПропущено: ").append(skipped);

            if (!errors.isEmpty()) {
                sb.append("\n\nПомилки:\n");

                for (String error : errors) {
                    sb.append(error).append("\n");
                }
            }

            return sb.toString();
        }
    }
}

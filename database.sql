
DROP DATABASE IF EXISTS greenery_care;

CREATE DATABASE greenery_care
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE greenery_care;

CREATE TABLE trees (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       species VARCHAR(120) NOT NULL,
                       address VARCHAR(255) NOT NULL,
                       latitude DECIMAL(9,6),
                       longitude DECIMAL(9,6),
                       technical_state DECIMAL(5,2) NOT NULL,
                       people_intensity DECIMAL(5,2) NOT NULL,
                       green_area DECIMAL(5,2) NOT NULL
);

CREATE TABLE inspections (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             tree_id BIGINT NOT NULL,
                             inspection_date DATE NOT NULL,
                             notes TEXT,
                             technical_state DECIMAL(5,2) NOT NULL,
                             inspector VARCHAR(120) NOT NULL,
                             FOREIGN KEY (tree_id) REFERENCES trees(id)
                                 ON DELETE CASCADE
);

CREATE TABLE work_requests (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               tree_id BIGINT NOT NULL,
                               description VARCHAR(500) NOT NULL,
                               status ENUM(
        'СТВОРЕНО',
        'ПЕРЕВІРЕНО',
        'ЗАПЛАНОВАНО',
        'ВИКОНАНО'
    ) NOT NULL DEFAULT 'СТВОРЕНО',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               planned_date DATE,
                               FOREIGN KEY (tree_id) REFERENCES trees(id)
                                   ON DELETE CASCADE
);

CREATE TABLE work_orders (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             request_id BIGINT NOT NULL,
                             assigned_to VARCHAR(120) NOT NULL,
                             order_date DATE NOT NULL,
                             completion_notes TEXT,
                             FOREIGN KEY (request_id) REFERENCES work_requests(id)
                                 ON DELETE CASCADE
);

INSERT INTO trees
(species, address, latitude, longitude,
 technical_state, people_intensity, green_area)
VALUES
    ('Липа', 'вул. Соборності 1',
     49.588300, 34.551400, 80.00, 65.00, 70.00),
    ('Дуб', 'парк Перемоги',
     49.600000, 34.560000, 90.00, 40.00, 85.00),
    ('Береза', 'вул. Шевченка 5',
     49.592000, 34.548000, 55.00, 70.00, 60.00);

INSERT INTO inspections
(tree_id, inspection_date, notes, technical_state, inspector)
VALUES
    (1, '2026-09-10', 'Стан задовільний', 80.00, 'Іваненко І.І.'),
    (2, '2026-09-12', 'Потрібне повторне обстеження', 90.00, 'Петренко П.П.'),
    (3, '2026-09-15', 'Потрібна перевірка гілок', 55.00, 'Іваненко І.І.');

INSERT INTO work_requests
(tree_id, description, status, planned_date)
VALUES
    (1, 'Санітарне обрізання гілок', 'СТВОРЕНО', NULL),
    (2, 'Плановий огляд дерева', 'ПЕРЕВІРЕНО', NULL),
    (3, 'Обрізання сухих гілок', 'ЗАПЛАНОВАНО', '2026-11-05');

INSERT INTO work_orders
(request_id, assigned_to, order_date, completion_notes)
VALUES
    (2, 'Бригада №1', '2026-10-01', NULL),
    (3, 'Бригада №2', '2026-10-02', NULL);

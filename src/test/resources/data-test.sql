-- Вставляем тестовые компании
INSERT INTO companies (name, address, yandex_id, two_gis_id) 
VALUES ('Тестовый Ресторан', 'г. Москва, ул. Тестовая, д. 1', 12345, 67890);

INSERT INTO companies (name, address, yandex_id, two_gis_id) 
VALUES ('Тестовое Кафе', 'г. Москва, ул. Примерная, д. 2', 54321, 98760);

-- Вставляем тестовые отзывы
INSERT INTO reviews (company_id, source, rating, text, author, date, source_id)
VALUES (1, 'YANDEX', 4.5, 'Отличное место, рекомендую!', 'Иван', '2023-01-15 10:30:00', 'y123');

INSERT INTO reviews (company_id, source, rating, text, author, date, source_id)
VALUES (1, 'TWOGIS', 3.0, 'Неплохо, но можно лучше', 'Мария', '2023-02-20 14:45:00', 'g456');

INSERT INTO reviews (company_id, source, rating, text, author, date, source_id)
VALUES (2, 'YANDEX', 5.0, 'Превосходное обслуживание', 'Петр', '2023-03-10 09:15:00', 'y789');

-- Вставляем изображения отзывов
INSERT INTO review_images (review_id, url)
VALUES (1, 'https://example.com/image1.jpg');

INSERT INTO review_images (review_id, url)
VALUES (2, 'https://example.com/image2.jpg');

INSERT INTO review_images (review_id, url)
VALUES (2, 'https://example.com/image3.jpg'); 
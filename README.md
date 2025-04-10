# Review Crawler

Сервис для сбора отзывов с Яндекс.Карты и 2ГИС с использованием Selenium WebDriver.

## Запуск в Docker

1. Клонируйте репозиторий:

```bash
git clone https://github.com/Melo4ka/ReviewCrawler.git
cd ReviewCrawler
```

2. Соберите и запустите Docker образ:

```bash
docker build -t review-crawler .
docker run -d -p 8080:8080 \
  -e DB_URL=jdbc:mariadb://<db-host>:<db-port>/review_crawler \
  -e DB_USERNAME=<username> \
  -e DB_PASSWORD=<password> \
  --name review-crawler review-crawler
```

3. Приложение будет доступно по адресу `http://localhost:8080`

## Swagger UI

Для просмотра и тестирования API можно использовать Swagger UI, доступный по адресу:

```
http://localhost:8080/swagger-ui.html
```

Документация OpenAPI доступна по адресу:

```
http://localhost:8080/api-docs
```

## Конфигурация

Конфигурация приложения производится через переменные окружения:

- `DB_URL` - URL подключения к базе данных MariaDB
- `DB_USERNAME` - имя пользователя для доступа к базе данных
- `DB_PASSWORD` - пароль для доступа к базе данных

## Техническая информация

- Java 17
- Kotlin 1.9
- Spring Boot 3.4
- Selenium WebDriver с ChromeDriver
- MariaDB 
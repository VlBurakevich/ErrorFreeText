# ErrorFreeText

Сервис для автоматической проверки и исправления орфографических ошибок в тексте через **Yandex Speller API**.  
Работает асинхронно: задача создаётся через REST, затем фоновый планировщик обрабатывает её

## Стек
- **Язык и фреймворк:** Java 21, Spring Boot 3.5
- **Сборка:** Gradle 9.6, Lombok
- **БД и миграции:** PostgreSQL, Liquibase, Spring Data JPA + Hibernate
- **Инфраструктура:** Docker Compose
- **Внешние API:** Yandex Speller
- **Тестирование:** JUnit 5, Mockito

## Запуск

### 1. Полный запуск (Docker Compose — БД + приложение)

```
docker compose up --build
```
### 2. Только БД + локальный запуск (для разработки)
```
docker compose -f docker-compose-db.yaml up -d
./gradlew bootRun
```

API
- POST /tasks — создать задачу на исправление текста
- GET /tasks/{id} — получить статус обработки и исправленный текст

Возможные статусы задачи: CREATED → IN_PROGRESS → COMPLETED / FAILED 
### Конфигурация
Основные параметры в src/main/resources/application.yaml:
- `app.yandex-speller.base-url` — URL Yandex Speller API — `https://speller.yandex.net/services/spellservice.json/checkTexts`
- `app.yandex-speller.connect-timeout` — Таймаут подключения (мс) — `5000`
- `app.yandex-speller.read-timeout` — Таймаут чтения (мс) — `5000`
- `app.scheduler.delay-ms` — Интервал опроса новых задач (мс) — `5000`
- `app.text-splitter.max-chunk-size` — Макс. размер чанка (символов) — `10000`
- `spring.datasource.url` — Подключение к PostgreSQL — `jdbc:postgresql://localhost:5432/error_free`
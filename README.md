# Finance Tracker

Backend-приложение для управления личными финансами, разработанное на Java (Spring Boot). Проект реализует RESTful API для учета доходов, расходов, управления бюджетами по категориям и перевода средств между пользователями.

**Проект выполнен в соответствии с максимальными критериями (50 баллов)**.

---

## Технологический стек

*   **Язык:** Java 17+
*   **Фреймворк:** Spring Boot 3.2
*   **Сборка:** Gradle 8.5+
*   **База данных:** PostgreSQL 15
*   **Миграции БД:** Flyway
*   **ORM:** Spring Data JPA (Hibernate)
*   **Безопасность:** Spring Security (Basic Auth + BCrypt)
*   **Тестирование:** JUnit 5, Mockito, Spring Boot Test, H2
*   **Качество кода:** Spotless (Google Java Format), Checkstyle
*   **Контейнеризация:** Docker & Docker Compose
*   **CI/CD:** GitHub Actions

---

## Реализованный функционал

### Основные возможности
1.  **Авторизация**: Регистрация и вход (Basic Auth).
2.  **Управление финансами**:
    *   Создание и **редактирование** категорий (Доходы/Расходы).
    *   Установка лимитов бюджета на категории расходов.
    *   Учет транзакций.
3.  **Бюджетирование**:
    *   Автоматическая проверка лимитов.
    *   API возвращает предупреждение (`WARNING`), если бюджет превышен.
4.  **Статистика и отчеты**:
    *   Вывод общего баланса и детализация по категориям.
    *   **Фильтрация по датам** (периодам).
    *   **Экспорт данных** (JSON).

### Дополнительное задание
*   **Переводы средств**: Реализован перевод денег между пользователями.
    *   Транзакция атомарна (списание и зачисление происходят одновременно).
    *   Автоматически создаются категории "Переводы" и фиксируются операции в истории обоих пользователей.

### Инженерная культура
*   **Чистая архитектура**: Controller -> Service -> Repository. Использование DTO.
*   **Миграции**: Версионирование схемы БД через Flyway (`src/main/resources/db/migration`).
*   **Code Style**: Настроен плагин **Spotless**, который автоматически форматирует код по стандарту Google Java Style.
*   **Тестирование**: Unit и Integration тесты (>15 тестов).
*   **CI Pipeline**: Настроен workflow для GitHub Actions.

---

## Инструкция по запуску

### Предварительные требования
*   **Docker**
*   **Java 17+**

### 1. Запуск базы данных
Разверните PostgreSQL с помощью Docker Compose:
```bash
docker-compose up -d
```

### 2. Запуск приложения
Используйте Gradle Wrapper. Миграции Flyway применятся автоматически.

```bash
./gradlew bootRun
```

Приложение будет доступно по адресу: `http://localhost:8080`

---

## Тестирование и проверка качества

### Запуск тестов
В проекте реализованы Unit-тесты (бизнес-логика) и интеграционные тесты (контроллеры). Всего 17 тестов.
```bash
./gradlew test
```

### Проверка стиля кода
Проверить, соответствует ли код стандартам:
```bash
./gradlew spotlessCheck
```
Автоматически исправить форматирование:
```bash
./gradlew spotlessApply
```

---

## Документация API (Примеры для Postman)

В приложении используется **Basic Auth**.
1.  Зарегистрируйте пользователя (Auth не нужен).
2.  Для всех остальных запросов используйте логин/пароль во вкладке **Authorization -> Basic Auth**.

### 1. Пользователи

#### Регистрация
`POST /api/auth/register`
```json
{
  "username": "ivan",
  "password": "password123"
}
```

### 2. Категории

#### Создать категорию
`POST /api/finance/categories`
```json
{
  "name": "Еда",
  "type": "EXPENSE",
  "budgetLimit": 10000
}
```

#### Редактировать категорию (имя или лимит)
`PUT /api/finance/categories/{id}`
```json
{
  "name": "Еда и Напитки",
  "type": "EXPENSE",
  "budgetLimit": 12000
}
```

#### Получить список категорий
`GET /api/finance/categories`

### 3. Транзакции

#### Добавить доход/расход
`POST /api/finance/transactions`

Если лимит по категории превышен, в ответе поле `message` будет содержать предупреждение.

```json
{
  "categoryName": "Еда",
  "amount": 500,
  "description": "Обед"
}
```

**Пример ответа (Бюджет превышен):**
```json
{
    "message": "WARNING: Budget exceeded for category Еда",
    "remainingBudget": -150.00,
    "transaction": { ... }
}
```

### 4. Статистика и Экспорт

#### Получить отчет (с фильтрацией по датам)
`GET /api/finance/stats?from=2023-01-01&to=2023-12-31`
*Параметры `from` и `to` опциональны.*

#### Экспорт операций (JSON)
`GET /api/finance/export`
Возвращает список всех транзакций текущего пользователя.

### 5. Переводы (Доп. задание)

#### Перевести деньги
`POST /api/finance/transfer`

```json
{
  "receiverUsername": "maria",
  "amount": 1500
}
```

---


## Быстрая демонстрация (Bash Script)

Если у вас установлен `curl` и `jq`, вы можете прогнать полный сценарий проверки всех функций (CRUD, фильтры, экспорт) одной командой:

```bash
chmod +x demo_script.sh
./demo_script.sh
```

Скрипт выполнит следующие шаги:
1.  **Регистрация** пользователей Alice и Bob.
2.  Добавление **дохода** (создание категории + транзакция).
3.  Создание категории **расходов** с лимитом бюджета.
4.  Демонстрация **превышения бюджета** (получение WARNING).
5.  **Перевод средств** другому пользователю.
6.  Получение **общей статистики**.
7.  **Редактирование категории** (PUT-запрос): изменение названия и лимита.
8.  Получение статистики с **фильтрацией по датам**.
9.  **Экспорт** всех транзакций в JSON.

### Ожидаемый вывод скрипта

```plaintext
=== 1. Register Users ===
✅ Registered Alice and Bob.

=== 2. Add Income for Alice ===
{
  "message": "Success",
  "remainingBudget": null,
  "transaction": { ... "amount": 10000 ... }
}
✅ Alice added income.

=== 3. Add Expense Category with Budget ===
{
  "id": 2,
  "name": "Food",
  "type": "EXPENSE",
  "budgetLimit": 500
}

=== 4. Spend Money (Check Budget Warning) ===
{
  "message": "WARNING: Budget exceeded for category Food",
  "remainingBudget": -100.00,
  "transaction": { ... "amount": 600 ... }
}
⚠️  Check for WARNING in response above.

=== 5. Transfer Money Alice -> Bob ===
✅ Transferred 1000 to Bob.

=== 6. Get General Stats for Alice ===
{
  "totalIncome": 10000.00,
  "totalExpense": 1600.00,
  "expensesByCategory": {
    "Переводы": 1000.00,
    "Food": 600.00
  },
  "budgetStatus": {
    "Food": -100.00
  }
}

=== 7. Edit Category (Functional Update) ===
{
  "id": 2,
  "name": "Fine Dining",
  "type": "EXPENSE",
  "budgetLimit": 2000
}

=== 8. Get Stats with Date Filter ===
... (JSON со статистикой за период) ...

=== 9. Export All Transactions (JSON) ===
[
  {
    "categoryName": "Salary",
    "amount": 10000,
    "description": "January Salary",
    "date": "..."
  },
  {
    "categoryName": "Fine Dining",
    "amount": 600,
    "description": "Big Dinner",
    "date": "..."
  },
  {
    "categoryName": "Переводы",
    "amount": 1000,
    "description": "Transfer to bob",
    "date": "..."
  }
]

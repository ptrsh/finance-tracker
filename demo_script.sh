#!/bin/bash

# Убедитесь, что установлен curl и jq
# sudo apt install jq

HOST="http://localhost:8080/api"

echo "--------------------------------------------------"
echo "🚀 Starting Finance Tracker Demo"
echo "--------------------------------------------------"

echo -e "\n=== 1. Register Users ==="
curl -s -X POST "$HOST/auth/register" -H "Content-Type: application/json" -d '{"username":"alice", "password":"123"}'
curl -s -X POST "$HOST/auth/register" -H "Content-Type: application/json" -d '{"username":"bob", "password":"123"}'
echo -e "\n✅ Registered Alice and Bob."

echo -e "\n=== 2. Add Income for Alice (Create 'Salary' + Add Transaction) ==="
# Создаем категорию
curl -s -u alice:123 -X POST "$HOST/finance/categories" -H "Content-Type: application/json" -d '{"name":"Salary", "type":"INCOME"}' > /dev/null
# Начисляем 10000
curl -s -u alice:123 -X POST "$HOST/finance/transactions" -H "Content-Type: application/json" -d '{"categoryName":"Salary", "amount":10000, "description":"January Salary"}' | jq .
echo "✅ Alice added income."

echo -e "\n=== 3. Add Expense Category with Budget ==="
curl -s -u alice:123 -X POST "$HOST/finance/categories" -H "Content-Type: application/json" -d '{"name":"Food", "type":"EXPENSE", "budgetLimit": 500}' | jq .
echo "✅ Category 'Food' created with limit 500."

echo -e "\n=== 4. Spend Money (Check Budget Warning) ==="
# Тратим 600 (превышение)
echo "Attempting to spend 600 on Food (Limit 500)..."
response=$(curl -s -u alice:123 -X POST "$HOST/finance/transactions" -H "Content-Type: application/json" -d '{"categoryName":"Food", "amount":600, "description":"Big Dinner"}')
echo "$response" | jq .
echo "⚠️  Check for WARNING in response above."

echo -e "\n=== 5. Transfer Money Alice -> Bob ==="
curl -s -u alice:123 -X POST "$HOST/finance/transfer" -H "Content-Type: application/json" -d '{"receiverUsername":"bob", "amount":1000}'
echo -e "✅ Transferred 1000 to Bob."

echo -e "\n=== 6. Get General Stats for Alice ==="
curl -s -u alice:123 -GET "$HOST/finance/stats" | jq .

echo -e "\n=== 7. Edit Category (Functional Update) ==="
# Изменяем категорию 'Food' (ID 2) -> 'Fine Dining' с увеличенным лимитом 2000
echo "Updating category ID 2: Name -> 'Fine Dining', Limit -> 2000"
curl -s -u alice:123 -X PUT "$HOST/finance/categories/2" -H "Content-Type: application/json" -d '{"name":"Fine Dining", "type":"EXPENSE", "budgetLimit": 2000}' | jq .

echo -e "\n=== 8. Get Stats with Date Filter (2020-2030) ==="
# Проверка фильтрации (должен вернуть те же данные, так как дата попадает в диапазон)
curl -s -u alice:123 -GET "$HOST/finance/stats?from=2020-01-01&to=2030-12-31" | jq .

echo -e "\n=== 9. Export All Transactions (JSON) ==="
curl -s -u alice:123 -GET "$HOST/finance/export" | jq .

echo -e "\n--------------------------------------------------"
echo "🏁 Demo Finished Successfully"
echo "--------------------------------------------------"
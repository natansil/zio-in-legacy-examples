set -x
grpcurl -d '{"customerId": "12345", "itemId": "4321", "quantity":1}' -plaintext localhost:50053 com.example.Orders/CreateOrder
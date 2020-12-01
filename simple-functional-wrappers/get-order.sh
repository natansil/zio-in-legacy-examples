set -x
grpcurl -d '{"orderId": "'$1'"}' -plaintext localhost:50052 com.example.Orders/GetOrder
set -x
grpcurl -d '{"orderId": "'$1'"}' -plaintext localhost:50053 com.example.Orders/GetOrder
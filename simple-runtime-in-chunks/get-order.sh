set -x
grpcurl -d '{"orderId": "'$1'"}' -plaintext localhost:50051 com.example.Orders/GetOrder
set -x
grpcurl -d '{"orderId": "'$2'"}' -plaintext localhost:$1 com.example.Orders/GetOrder
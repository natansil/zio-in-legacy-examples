package runtime.in.chunks
import com.example.orders.{OrdersGrpc, CreateOrderRequest, CreateOrderReply, GetOrderRequest, GetOrderReply}
import scala.concurrent.{ExecutionContext, Future}

trait OrdersDao {
  def createOrder(req: CreateOrderRequest): Future[String]
  def getOrder(orderId: String): Future[Order]
}

object InMemoryOrdersDao extends OrdersDao {
  override def getOrder(orderId: String): Future[Order] = Future.successful(Order())

  override def createOrder(req: CreateOrderRequest): Future[String] = Future.successful(java.util.UUID.randomUUID.toString)
}

case class Order(orderId: String = "",
    customerId: String = "",
    itemId: String = "",
    quantity: Int = 0)

object Order {
  def toReply(order: Order) = GetOrderReply(order.orderId,order.customerId, order.itemId, order.quantity)
  def fromRequest(orderId: String, req: CreateOrderRequest) = Order(orderId, req.customerId, req.itemId, req.quantity)
}
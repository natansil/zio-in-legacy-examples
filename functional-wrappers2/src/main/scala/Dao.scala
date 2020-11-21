package functional.wrappers2
import com.example.orders.{OrdersGrpc, CreateOrderRequest, CreateOrderReply, GetOrderRequest, GetOrderReply}
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable
import zio.Task
import zio.ZIO

trait ZOrdersDao {
  def createOrder(req: CreateOrderRequest): Task[String]
  def getOrder(orderId: String): Task[Order]
}

object ZInMemoryOrdersDao extends ZOrdersDao {

  override def createOrder(req: CreateOrderRequest): zio.Task[String] = ZIO.fromFuture(_ => InMemoryOrdersDao.createOrder(req))

  override def getOrder(orderId: String): zio.Task[Order] = ZIO.fromFuture(_ => InMemoryOrdersDao.getOrder(orderId))

}

trait OrdersDao {
  def createOrder(req: CreateOrderRequest): Future[String]
  def getOrder(orderId: String): Future[Order]
}

object InMemoryOrdersDao extends OrdersDao {
  val orders = mutable.Map.empty[String, Order] 

  override def getOrder(orderId: String): Future[Order] = 
    orders.get(orderId).map(Future.successful).getOrElse(Future.failed(new RuntimeException("missing order")))

  override def createOrder(req: CreateOrderRequest): Future[String] = {
    val orderId = java.util.UUID.randomUUID.toString
    orders.put(orderId, Order.fromRequest(orderId, req))
    println(s">>>> order added to DAO. orders: $orders")
    Future.successful(orderId)
  }
}

case class Order(orderId: String = "",
    customerId: String = "",
    itemId: String = "",
    quantity: Int = 0)

object Order {
  def toReply(order: Order) = GetOrderReply(order.orderId,order.customerId, order.itemId, order.quantity)
  def fromRequest(orderId: String, req: CreateOrderRequest) = Order(orderId, req.customerId, req.itemId, req.quantity)
}
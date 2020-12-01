package runtime.in.chunks
import zio.Task

trait ZOrdersCache {
  def setOrder(order: Order): Task[Unit]
  def getOrder(orderId: String): Task[Option[Order]]
  def cleanup(): Task[Unit]
}

object ZOrdersCacheImpl {
  def make(): Task[ZOrdersCache] =
    for {
      ordersRef <- zio.Ref.make[Map[String, Order]](Map.empty)
    } yield new ZOrdersCache {
      override def setOrder(order: Order): Task[Unit] = 
        ordersRef.update(_.updated(order.orderId, order))

      override def getOrder(orderId: String): Task[Option[Order]] = 
        ordersRef.get.map(_.get(orderId))

      override def cleanup(): zio.Task[Unit] = Task.unit
    }
}

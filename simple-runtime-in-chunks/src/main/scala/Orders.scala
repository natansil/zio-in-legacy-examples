package runtime.in.chunks

import com.example.orders.OrdersGrpc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import io.grpc.{Server, ServerBuilder}
import io.grpc.protobuf.services.ProtoReflectionService
import com.example.orders.{OrdersGrpc, CreateOrderRequest, CreateOrderReply, GetOrderRequest, GetOrderReply}

import scala.concurrent.{ExecutionContext, Future}
import zio.Task
import zio.ZIO
import zio.UIO

class OrdersServer(
  executionContext: ExecutionContext,
  ordersDao: OrdersDao // ordersCache:  ZOrdersCache
) { self =>
  private[this] var server: Server = null
  private val port = 50051

  def start(): Unit = {
    server = ServerBuilder.forPort(port)
    .addService(OrdersGrpc.bindService(new OrdersImpl, executionContext))
    .addService(ProtoReflectionService.newInstance()).build.start
    println("Orders Server started, listening on " + port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class OrdersImpl extends OrdersGrpc.Orders {
    implicit val _executionContext = executionContext
    override def createOrder(req: CreateOrderRequest) = {
      //zCreateOrder snippet
      for {
        orderId <- ordersDao.createOrder(req)
      } yield CreateOrderReply(orderId)
    }

    override  def getOrder(request: GetOrderRequest): scala.concurrent.Future[GetOrderReply] = 
      //zGetOrder snippet
      ordersDao.getOrder(request.orderId).map(Order.toReply)
  }

}

//// ZIO helpers ////
object LegacyRuntime {
  def fromTask[Res](body: => Task[Res]): Future[Res] = {
    Runtime.unsafeRunToFuture(body)
  }
}

// order <- ZIO.fromFuture{_ => ordersDao.getOrder(orderId) }
// _ <- ordersCache.setOrder(order)


// LegacyRuntime.fromTask {
//   for {
//     maybeOrder <- ordersCache.getOrder(request.orderId)
//     order <- maybeOrder.fold(ZIO.fromFuture { _ => ordersDao.getOrder(request.orderId)})(
//       UIO(println(">>>> found in cache!!! no need to request from DB :)")) *> Task(_))
//   } yield Order.toReply(order)
// }
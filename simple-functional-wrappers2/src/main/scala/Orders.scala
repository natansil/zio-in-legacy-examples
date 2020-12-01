package functional.wrappers2

import com.example.orders.OrdersGrpc._
import com.example.orders.ZioOrders
import com.example.orders.ZioOrders.ZOrders
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import io.grpc.{Server, ServerBuilder}
import io.grpc.protobuf.services.ProtoReflectionService
import com.example.orders.{OrdersGrpc, CreateOrderRequest, CreateOrderReply, GetOrderRequest, GetOrderReply}

import scala.concurrent.{ExecutionContext, Future}
import zio.ZIO
import zio.IO
import zio.RIO
import zio.blocking.Blocking
import zio.UIO
import zio.Task

class OrdersServer(executionContext: ExecutionContext, 
                   ordersDao: ZOrdersDao,
                   ordersCache:  ZOrdersCache
                   ) { self =>
  private[this] var server: Server = null
  private val port = 50053

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

  private class OrdersImpl extends OrdersGrpc.Orders {  // ZOrders[Blocking, Any]
    implicit val _executionContext = executionContext
    override def createOrder(req: CreateOrderRequest) = {
      LegacyRuntime.fromZIO {
        for {
          orderId <- ordersDao.createOrder(req)
          order <- ordersDao.getOrder(orderId)
          _ <- ordersCache.setOrder(order)
        } yield CreateOrderReply(orderId) //().catchAll(ex => ZIO.fail(io.grpc.Status.INTERNAL))
      }
    }

    override  def getOrder(request: GetOrderRequest): scala.concurrent.Future[GetOrderReply] = 
      LegacyRuntime.fromZIO{ 
        for {
          maybeOrder <- ordersCache.getOrder(request.orderId)
          order <- maybeOrder.fold(ordersDao.getOrder(request.orderId))(
            UIO(println(">>>> found in cache!!! no need to request from DB :)")) *> Task(_))
        } yield Order.toReply(order)  //().catchAll(ex => ZIO.fail(io.grpc.Status.INTERNAL))
      }
    }

}

//// ZIO helpers ////
object LegacyRuntime {
  def fromZIO[Res](body: => RIO[zio.ZEnv, Res]): Future[Res] = {
    Runtime.unsafeRunToFuture(body)
  }
}
package functional.wrappers2

import com.example.orders.OrdersGrpc._
import com.example.orders.ZioOrders
import com.example.orders.ZioOrders.ZOrders
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.wixpress.dst.greyhound.core._


import io.grpc.{Server, ServerBuilder}
import io.grpc.protobuf.services.ProtoReflectionService
import com.example.orders.{OrdersGrpc, CreateOrderRequest, CreateOrderReply, GetOrderRequest, GetOrderReply}
import com.wixpress.dst.greyhound.future.GreyhoundProducer
import com.wixpress.dst.greyhound.core.producer.Producer

import scala.concurrent.{ExecutionContext, Future}
import com.wixpress.dst.greyhound.core.producer.ProducerRecord
import zio.ZIO
import zio.IO
import zio.RIO
import zio.blocking.Blocking

class OrdersServer(executionContext: ExecutionContext, 
                   producer: Producer.Producer,
                   ordersDao: ZOrdersDao,
                   ordersCache: zio.Ref[Map[String, Order]]
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

  private class OrdersImpl extends OrdersGrpc.Orders {  // ZOrders[Blocking, Any]
    implicit val _executionContext = executionContext
    override def createOrder(req: CreateOrderRequest) = {
      LegacyRuntime.fromZIO {
        for {
          orderId <- ordersDao.createOrder(req)
          order <- ordersDao.getOrder(orderId)
          _ <- ordersCache.update(_.updated(orderId, order))
          _ <- producer.produce(ProducerRecord("orders", orderId), Serdes.StringSerde, Serdes.StringSerde)
        } yield CreateOrderReply() //().catchAll(ex => ZIO.fail(io.grpc.Status.INTERNAL))
      }
    }

    override  def getOrder(request: GetOrderRequest): scala.concurrent.Future[GetOrderReply] = 
      LegacyRuntime.fromZIO{ordersDao.getOrder(request.orderId)}.map(Order.toReply)  //.catchAll(ex => ZIO.fail(io.grpc.Status.INTERNAL))
  }

}

//// ZIO helpers ////
object LegacyRuntime {
  def fromZIO[Res](body: => RIO[zio.ZEnv, Res]): Future[Res] = {
    Runtime.unsafeRunToFuture(body)
  }
}





// class ZOrdersServer(producer: Producer.Producer,
//                    ordersDao: ZOrdersDao,
//                    ordersCache: zio.Ref[Map[String, Order]]) extends ZOrders[Blocking, Any] {
//     override def createOrder(req: CreateOrderRequest) = {
//         (for {
//           orderId <- ordersDao.createOrder(req)
//           order <- ordersDao.getOrder(orderId)
//           _ <- ordersCache.update(_.updated(orderId, order))
//           _ <- producer.produce(ProducerRecord("orders", orderId), Serdes.StringSerde, Serdes.StringSerde)
//         } yield CreateOrderReply()).catchAll(ex => ZIO.fail(io.grpc.Status.INTERNAL))
//     }

//     override  def getOrder(request: GetOrderRequest) = 
//       ordersDao.getOrder(request.orderId).map(Order.toReply).catchAll(ex => ZIO.fail(io.grpc.Status.INTERNAL))
// }
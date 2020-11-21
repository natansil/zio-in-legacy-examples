package runtime.in.chunks

import com.example.orders.OrdersGrpc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.wixpress.dst.greyhound.core._


import io.grpc.{Server, ServerBuilder}
import io.grpc.protobuf.services.ProtoReflectionService
import com.example.orders.{OrdersGrpc, CreateOrderRequest, CreateOrderReply, GetOrderRequest, GetOrderReply}
import com.wixpress.dst.greyhound.future.GreyhoundProducer

import scala.concurrent.{ExecutionContext, Future}
import com.wixpress.dst.greyhound.core.producer.ProducerRecord
import zio.Task
import zio.ZIO

class OrdersServer(executionContext: ExecutionContext, 
                   producer: GreyhoundProducer,
                   ordersDao: OrdersDao,
                   ordersCache:  zio.Ref[Map[String, Order]]
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
      LegacyRuntime.fromTask {
        for {
          orderId <- ZIO.fromFuture{_ => ordersDao.createOrder(req) }
          order <- ZIO.fromFuture{_ => ordersDao.getOrder(orderId)}
          _ <- ordersCache.update(_.updated(orderId, order))
          _ <- ZIO.fromFuture{_ => producer.produce(ProducerRecord("orders", orderId), Serdes.StringSerde, Serdes.StringSerde)}
        } yield CreateOrderReply()
      }
    }

    override  def getOrder(request: GetOrderRequest): scala.concurrent.Future[GetOrderReply] = 
      //zGetOrder snippet
      // ordersDao.getOrder(request.orderId).map(Order.toReply)
      LegacyRuntime.fromTask{ ZIO.fromFuture{_ => ordersDao.getOrder(request.orderId)}.map(Order.toReply) }
  }

}

//// ZIO helpers ////
object LegacyRuntime {
  def fromTask[Res](body: => Task[Res]): Future[Res] = {
    Runtime.unsafeRunToFuture(body)
  }
}
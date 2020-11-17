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
                   ordersDao: OrdersDao
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
        _ <- ordersDao.createOrder(req)
        _ <- producer.produce(ProducerRecord("orders", req.customerId), Serdes.StringSerde, Serdes.StringSerde)
      } yield CreateOrderReply()
    }
    override  def getOrder(request: GetOrderRequest): scala.concurrent.Future[GetOrderReply] = 
      //zGetOrder snippet
      ordersDao.getOrder(request)
  }

}

trait OrdersDao {
  def createOrder(req: CreateOrderRequest): Future[Unit]
  def getOrder(req: GetOrderRequest): Future[GetOrderReply]
}

object InMemoryOrdersDao extends OrdersDao {

  override def getOrder(req: GetOrderRequest): Future[GetOrderReply] = Future.successful(GetOrderReply())

  override def createOrder(req: CreateOrderRequest): Future[Unit] = Future.unit
}

//// ZIO helpers ////
object LegacyRuntime {
  def fromTask[Res](body: => Task[Res]): Future[Res] = {
    Runtime.unsafeRunToFuture(body)
  }
}
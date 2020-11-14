package runtime.in.chunks

import com.example.orders.OrdersGrpc._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.wixpress.dst.greyhound.core._


import io.grpc.{Server, ServerBuilder}
import io.grpc.protobuf.services.ProtoReflectionService
import com.example.orders.{OrdersGrpc, ProduceOrderRequest, ProduceOrderReply}
import com.wixpress.dst.greyhound.future.GreyhoundProducer

import scala.concurrent.{ExecutionContext, Future}
import com.wixpress.dst.greyhound.core.producer.ProducerRecord

class OrdersServer(executionContext: ExecutionContext, producer: GreyhoundProducer) { self =>
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
    override def produceOrder(req: ProduceOrderRequest) = {
      producer.produce(ProducerRecord("orders", req.customerId), Serdes.StringSerde, Serdes.StringSerde)
              .map(_ => ProduceOrderReply())(executionContext)
    }
  }

}
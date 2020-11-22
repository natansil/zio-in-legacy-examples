package functional.wrappers2

import java.util.logging.Logger

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Random

import com.example.orders.ZioOrders.ZOrders
import com.wixpress.dst.greyhound.core._
import com.wixpress.dst.greyhound.core.admin.AdminClientConfig
import com.wixpress.dst.greyhound.core.consumer.EventLoopMetric.StartingEventLoop
import com.wixpress.dst.greyhound.core.consumer.EventLoopMetric.StoppingEventLoop
import com.wixpress.dst.greyhound.core.consumer.domain.ConsumerRecord
import com.wixpress.dst.greyhound.core.metrics.GreyhoundMetric
import com.wixpress.dst.greyhound.core.producer.ProducerRecord
import com.wixpress.dst.greyhound.core.producer._
import com.wixpress.dst.greyhound.future.ContextDecoder.aHeaderContextDecoder
import com.wixpress.dst.greyhound.future.ContextEncoder.aHeaderContextEncoder
import com.wixpress.dst.greyhound.future.ErrorHandler.anErrorHandler
import com.wixpress.dst.greyhound.future.GreyhoundConsumer._
import com.wixpress.dst.greyhound.future.GreyhoundProducerBuilder
import com.wixpress.dst.greyhound.future._
import io.grpc.ServerBuilder
import scalapb.zio_grpc.Server
import scalapb.zio_grpc.ServerMain
import scalapb.zio_grpc.ServiceList
import zio.BootstrapRuntime
import zio.Has
import zio.Layer
import zio.RIO
import zio.Task
import zio.UIO
import zio.URIO
import zio.ZEnv
import zio.ZIO
import zio.ZLayer
import zio.blocking.Blocking
import zio.blocking.effectBlocking
import zio.clock.Clock
import zio.console
import zio.console._
import zio.duration._
import zio.{Promise => ZPromise}

object Main extends App { //ServerMain
  val kafkaPort = 9092
  val bootstrapServer = s"localhost:$kafkaPort"
  val greyhoundConfig = GreyhoundConfig(bootstrapServer)

  implicit val ec = ExecutionContext.global

  Runtime.unsafeRun {
    for {
      ordersRef <- zio.Ref.make[Map[String, Order]](Map.empty)
      runtime <- ZIO.runtime[ZEnv]
      consumersBuilder = Consumer.consumerBuilderWith(greyhoundConfig, new RecordHandler[String, String] {
        override def handle(record: ConsumerRecord[String, String])(implicit ec: ExecutionContext): Future[Any] =
          runtime.unsafeRunToFuture(new ZCustomHandler(ordersRef).handle(record))
      })  
      producerR <- Producer.make(ProducerConfig(bootstrapServer)).reserve
      producer <- producerR.acquire
      
      server <- ZIO.fromFuture(_ => for {
        consumer <- consumersBuilder.build
        _ = println(">>>> consumer started")
        server = new OrdersServer(ec, producer, ZInMemoryOrdersDao, ordersRef)
      } yield server)

      _ <- ZIO.effect(server.start())
      _ <- effectBlocking(server.blockUntilShutdown())
      _ = producerR.release
    } yield ()
  }

  // override def port: Int = 50051
  // override def services: ServiceList[zio.ZEnv] =
  //   ServiceList.addM(server)
}

object Runtime extends BootstrapRuntime






  //  val server =  for {
  //     ordersRef <- zio.Ref.make[Map[String, Order]](Map.empty)
  //     runtime <- ZIO.runtime[ZEnv]
  //     consumersBuilder = Consumer.consumerBuilderWith(greyhoundConfig, new RecordHandler[String, String] {
  //       override def handle(record: ConsumerRecord[String, String])(implicit ec: ExecutionContext): Future[Any] =
  //         runtime.unsafeRunToFuture(new ZCustomHandler(ordersRef).handle(record))
  //     })  
  //     producerR <- Producer.make(ProducerConfig(bootstrapServer)).reserve
  //     producer <- producerR.acquire 
  //     consumer <- ZIO.fromFuture(_ =>consumersBuilder.build)

  //     server = new ZOrdersServer(producer, ZInMemoryOrdersDao, ordersRef)
      

  //     // _ = producerR.release
  //   } yield server

  // override def port: Int = 50051
  // override def services: ServiceList[zio.ZEnv] =
  //   ServiceList.addM(server)
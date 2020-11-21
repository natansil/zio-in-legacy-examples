package functional.wrappers2

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Random

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
import zio.BootstrapRuntime
import zio.Task
import zio.UIO
import zio.URIO
import zio.RIO
import zio.console
import zio.console._
import zio.{Promise => ZPromise}
import zio.ZIO
import zio.ZEnv
import zio.blocking.{Blocking, effectBlocking}


import java.util.logging.Logger

object Main extends App {
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
}

object Runtime extends BootstrapRuntime
package functional.wrappers

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

import java.util.logging.Logger

object Main extends App {
  val kafkaPort = 9092
  val greyhoundConfig = GreyhoundConfig(s"localhost:$kafkaPort")

  implicit val ec = ExecutionContext.global

  Runtime.unsafeRun {
    for {
      ordersRef <- zio.Ref.make[Map[String, Order]](Map.empty)
      runtime <- ZIO.runtime[ZEnv]
      consumersBuilder = Consumer.consumerBuilderWith(greyhoundConfig, new RecordHandler[String, String] {
        override def handle(record: ConsumerRecord[String, String])(implicit ec: ExecutionContext): Future[Any] =
          runtime.unsafeRunToFuture(new ZCustomHandler(ordersRef).handle(record))
      })  
      
      _server = for {
        consumer <- consumersBuilder.build
        _ = println(">>>> consumer started")
        producer <- GreyhoundProducerBuilder(greyhoundConfig).build
        server = new OrdersServer(ec ,producer, InMemoryOrdersDao, ordersRef) // ZInMemoryOrdersDao
      } yield server

      server = Await.result(_server, 10.seconds)
      _ = server.start()
      _ = server.blockUntilShutdown()
    } yield ()
  }
}

object Runtime extends BootstrapRuntime
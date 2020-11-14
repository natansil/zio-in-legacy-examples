package runtime.in.chunks

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
import zio.clock.Clock
import zio.{Promise => ZPromise}

import java.util.logging.Logger

object Main extends App {
  implicit val ec = ExecutionContext.global
  println("Hello, World!")

  // val promise = Promise[ConsumerRecord[Int, String]]

  val consumersBuilder = consumerBuilderWith(new RecordHandler[Int, String] {
    override def handle(record: ConsumerRecord[Int, String])(implicit ec: ExecutionContext): Future[Any] =
      Runtime.unsafeRunToFuture(new ZCustomHandler().handle(record))
  })

  val config = GreyhoundConfig("localhost:9092")
  val _server = for {
    producer <- GreyhoundProducerBuilder(config).build
    server = new OrdersServer(ec ,producer)
  } yield server

  val server = Await.result(_server, 2.seconds)
  server.start()
  server.blockUntilShutdown()

  // consumersBuilder.build

  def consumerBuilderWith(
      recordHandler: RecordHandler[Int, String]
  ): GreyhoundConsumersBuilder = {
    val config = GreyhoundConfig("localhost:6667")

    GreyhoundConsumersBuilder(config)
      .withConsumer(
        GreyhoundConsumer(
          initialTopics = Set("some-topic"),
          group = "group-1",
          clientId = "client-id-1",
          handle = aRecordHandler { recordHandler },
          keyDeserializer = Serdes.IntSerde,
          valueDeserializer = Serdes.StringSerde
        )
      )
  }
}

class CustomHandler() {
  def handle(record: ConsumerRecord[Int, String])(implicit ec: ExecutionContext): Future[Any] = {
    ???
  }
}

class ZCustomHandler() {
  def handle(record: ConsumerRecord[Int, String]): RIO[Clock, Unit] = {
    ???
  }
}

object Runtime extends BootstrapRuntime
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
import zio.console
import zio.console._
import zio.{Promise => ZPromise}

import java.util.logging.Logger

object Main extends App {
  val kafkaPort = 9092
  val greyhoundConfig = GreyhoundConfig(s"localhost:$kafkaPort")

  implicit val ec = ExecutionContext.global

  val consumersBuilder = consumerBuilderWith(new RecordHandler[String, String] {
    override def handle(record: ConsumerRecord[String, String])(implicit ec: ExecutionContext): Future[Any] =
      new CustomHandler().handle(record) //ZCustom snippet
  })  
  
  val _server = for {
    consumer <- consumersBuilder.build
    _ = println(">>>> consumer started")
    producer <- GreyhoundProducerBuilder(greyhoundConfig).build
    server = new OrdersServer(ec ,producer, InMemoryOrdersDao)
  } yield server

  val server = Await.result(_server, 10.seconds)
  server.start()
  server.blockUntilShutdown()


  def consumerBuilderWith(
      recordHandler: RecordHandler[String, String]
  ): GreyhoundConsumersBuilder = {
    GreyhoundConsumersBuilder(greyhoundConfig)
      .withConsumer(
        GreyhoundConsumer(
          initialTopics = Set("orders"),
          group = "group",
          clientId = "client-id",
          handle = aRecordHandler { recordHandler },
          keyDeserializer = Serdes.StringSerde,
          valueDeserializer = Serdes.StringSerde
        )
      )
  }
}

class CustomHandler() {
  def handle(record: ConsumerRecord[String, String])(implicit ec: ExecutionContext): Future[Any] = {
    Future.successful(println(s">>>> $record"))
  }
}

////// ZIO additions //////

class ZCustomHandler(ordersCache:  zio.Ref[Map[String, Order]]) {
  def handle(record: ConsumerRecord[String, String]): RIO[Console, Unit] = {
    for {
      orders <- ordersCache.get
      order = orders.get(record.value)
      _ <- console.putStrLn(s">>>> r: $record. o: $order")
    } yield ()
  }
}

object Runtime extends BootstrapRuntime
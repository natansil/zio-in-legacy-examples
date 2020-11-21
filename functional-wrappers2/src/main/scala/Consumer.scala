package functional.wrappers2

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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

import zio.RIO
import zio.console
import zio.console._

object Consumer {
  def consumerBuilderWith(
      greyhoundConfig: GreyhoundConfig,
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
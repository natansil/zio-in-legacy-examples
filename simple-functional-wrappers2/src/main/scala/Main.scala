package functional.wrappers2

import java.util.logging.Logger

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Random

import com.example.orders.ZioOrders.ZOrders
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
import zio.Schedule

object Main extends App { //ServerMain

  implicit val ec = ExecutionContext.global

  Runtime.unsafeRun {
    for {
      cache <- ZOrdersCacheImpl.make()
      _ <- cache.cleanup().repeat(Schedule.spaced(1.second)).fork      
      server = new OrdersServer(ec, ZInMemoryOrdersDao, cache)

      _ <- ZIO.effect(server.start())
      _ <- effectBlocking(server.blockUntilShutdown())
    } yield ()
  }

  // override def port: Int = 50053
  // override def services: ServiceList[zio.ZEnv] =
  //   ServiceList.addM(server)
}

object Runtime extends BootstrapRuntime





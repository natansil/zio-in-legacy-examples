package functional.wrappers

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Random

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
import zio.duration._

import java.util.logging.Logger
import zio.Schedule

object Main extends App {

  implicit val ec = ExecutionContext.global

 Runtime.unsafeRun {
    for {
      cache <- ZOrdersCacheImpl.make()
      _ <- cache.cleanup().repeat(Schedule.spaced(1.second)).fork
      server = new OrdersServer(ec, InMemoryOrdersDao, cache) // ZInMemoryOrdersDao
      _ <- ZIO.effect(server.start())
      _ <- effectBlocking(server.blockUntilShutdown())
    } yield ()
  }
}

object Runtime extends BootstrapRuntime
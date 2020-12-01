package runtime.in.chunks

import java.util.logging.Logger

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Random

import zio.BootstrapRuntime
import zio.RIO
import zio.Task
import zio.UIO
import zio.URIO
import zio.ZEnv
import zio.ZIO
import zio.console
import zio.console._
import zio.{Promise => ZPromise}

object Main extends App {

  implicit val ec = ExecutionContext.global

  val server = new OrdersServer(ec, InMemoryOrdersDao) 
  server.start()
  server.blockUntilShutdown()
}

object Runtime extends BootstrapRuntime
//// ZIO stuff - pass Ref ////
  // Runtime.unsafeRun {
  //   for {
//        ordersRef <- zio.Ref.make[Map[String, Order]](Map.empty)
//        runtime <- ZIO.runtime[ZEnv]
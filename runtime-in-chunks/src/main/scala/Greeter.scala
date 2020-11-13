package runtime.in.chunks

import com.example.protos.hello.GreeterGrpc._
import com.example.protos.hello._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

import io.grpc.{Server, ServerBuilder}
import io.grpc.protobuf.services.ProtoReflectionService
import com.example.protos.hello.{GreeterGrpc, HelloRequest, HelloReply}

import scala.concurrent.{ExecutionContext, Future}

class HelloWorldServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null
  private val port = 50051

  def start(): Unit = {
    server = ServerBuilder.forPort(port)
    .addService(GreeterGrpc.bindService(new GreeterImpl, executionContext))
    .addService(ProtoReflectionService.newInstance()).build.start
    println("Server started, listening on " + port)
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

  private class GreeterImpl extends GreeterGrpc.Greeter {
    override def sayHello(req: HelloRequest) = {
      val reply = HelloReply(message = "Hello " + req.name)
      Future.successful(reply)
    }
  }

}
import com.example.protos.hello.GreeterGrpc._
import com.example.protos.hello._
import scala.concurrent.Future

object GreeterService extends Greeter {
  override def sayHello(request: HelloRequest): Future[com.example.protos.hello.HelloReply] = ???
}
package natansil.zioexamples

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.beans.factory.annotation.Autowired
import zio.UIO
import zio._
import scala.collection.immutable.Stream.Cons
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import zio.duration._

@SpringBootApplication
class BootConfig extends CommandLineRunner {

  override def run(args: String*): Unit = {
    println(">>>> Starting Legacy Spring application")
    val context = new AnnotationConfigApplicationContext(classOf[BootConfig]);

    val producer = context.getBean("producer", classOf[Producer[Int]])
    val consumer = context.getBean("consumer", classOf[Consumer[Int]])
    val queue = context.getBean("sharedQueue", classOf[AcquiredManagedResource[Queue[Int]]])

    Runtime.unsafeRun { 
      for {
        v1 <- consumer.receive.fork
        _ <- producer.send(2)
        _ <- queue.release()
      } yield ()
    }
  }

  @Bean
  def sharedQueue(): AcquiredManagedResource[Queue[Int]] = Runtime.unsafeRunTask {
    val managedResource = Managed.make(Queue.unbounded[Int])(_.shutdown *> UIO(println(">>>> queue was shutdown!")))
    AcquiredManagedResource.acquire(managedResource)
  }

  @Bean
  def producer(sharedQueue: AcquiredManagedResource[Queue[Int]]): Producer[Int] = Runtime.unsafeRun {
    UIO(Producer.make(sharedQueue.resource))
  }

  @Bean
  def consumer(sharedQueue: AcquiredManagedResource[Queue[Int]]): Consumer[Int] = Runtime.unsafeRun {
    UIO(Consumer.make(sharedQueue.resource))
  }
}

trait Producer[T] {
    def send(value: T): UIO[Unit]
}

object Producer {
    def make[T](queue: Queue[T]): Producer[T] = new Producer[T] {
        override def send(value: T): UIO[Unit] = UIO(println(s">>>> sending $value")) *> queue.offer(value).ignore
    }
}

trait Consumer[T] {
    def receive(): UIO[T]
}

object Consumer {
    def make[T](queue: Queue[T]): Consumer[T] = new Consumer[T] {
        override def receive(): UIO[T] = queue.take.flatMap(v => UIO({println(s">>>> received $v"); v})).flatMap(v => queue.shutdown.as(v))
    }
}

object Runtime extends zio.BootstrapRuntime
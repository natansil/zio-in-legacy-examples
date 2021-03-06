package natansil.zioexamples

import zio._
import zio.clock.Clock
import zio.duration.{Duration, durationInt}

import scala.concurrent.TimeoutException

case class AcquiredManagedResource[T](resource: T, onRelease: UIO[Unit], runtime: zio.Runtime[Any]) {
  def release(): UIO[Unit] = onRelease
}

object AcquiredManagedResource {
  def acquire[R <: Has[_] : zio.Tag, T](resources: ZManaged[R, Throwable, T],
                                        releaseTimeout: Duration = 10.seconds): ZIO[Clock with R, Throwable, AcquiredManagedResource[T]] = for {
    runtime <- ZIO.runtime[Any]
    clock <- ZIO.environment[Clock with R]
    r <- resources.reserve
    acquired <- r.acquire
  } yield {
    val releaseWithTimeout = r.release(Exit.unit)
      .timeoutFail(new TimeoutException("release timed out"))(releaseTimeout)
      .provide(clock)
      .orDie.unit
    AcquiredManagedResource(acquired, releaseWithTimeout, runtime)
  }}

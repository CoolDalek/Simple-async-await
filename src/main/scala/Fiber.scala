import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.util.control.NonFatal

trait Fiber[+T](protected val scheduler: Scheduler):

  enum State:
    case New
    case Running
    case Done(result: Any)
    case Failed(exc: Throwable)
  import State.*
  protected val state: AtomicReference[State] = AtomicReference[State](New)

  final def start(): Unit =
    state.set(Running)
    scheduler.execute { () =>
      try {
        run()
      } catch {
        case NonFatal(exc) =>
          state.set(Failed(exc))
      }
    }
  end start

  protected def run(): Unit

  final def join(): T = joinEither().fold(
    exc => throw exc,
    identity,
  )

  @tailrec
  final def joinEither(): Either[Throwable, T] =
    state.get() match
      case New => Left(new IllegalStateException("Fiber is not started yet"))
      case Running => scheduler.executeNext(); joinEither()
      case Done(result) => Right(result.asInstanceOf[T])
      case Failed(exc) => Left(exc)
  end joinEither

object Fiber:

  def task[T](runnable: () => T)(using sch: Scheduler): Fiber[T] =
    new Fiber[T](sch):
      def run(): Unit =
        val result = runnable()
        state.set(State.Done(result))
      end run
  end task

  trait Completable[T] extends Fiber[T]:
    import State.*

    def complete(result: Either[Throwable, T]): Unit =
      val witness = state.get()
      val newState = result.fold(
        exc => Failed(exc),
        value => Done(value),
      )
      state.compareAndSet(witness, newState)
    end complete

  end Completable

  def race[T](left: Task[T], right: Task[T])(using sch: Scheduler): Fiber[T] =
    new Completable[T] with Fiber[T](sch):
      def run(): Unit =
        def fire(task: Task[T]) = async {
          complete(task.awaitEither)
        }.fork
        fire(left)
        fire(right)
      end run
  end race

end Fiber
import Fiber.State

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.util.control.NonFatal

import Fiber.State.*

trait Fiber[+T](protected val scheduler: Scheduler):
  protected val state = AtomicReference[State[Any]](New)

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

  inline def join(): T =
    joinTo[Id]

  @tailrec
  final def joinTo[F[+_]](using fallible: Fallible[F]): F[T] =
    import fallible.*

    state.get().asInstanceOf[State[T]] match
      case Running => scheduler.executeNext(); joinTo[F]
      case Done(result) => succeeded(result)
      case Failed(exc) => failed(exc)
      case New => failed(new IllegalStateException("Fiber is not started yet"))
  end joinTo

object Fiber:

  enum State[+T]:
    case New extends State[Nothing]
    case Running extends State[Nothing]
    case Done[T](result: T) extends State[T]
    case Failed(exc: Throwable) extends State[Nothing]
  end State

  given Fallible[State] with

    inline def failed[T](exc: Throwable): State[T] = Failed(exc)

    inline def succeeded[T](value: T): State[T] = Done(value)

  end given

  def task[T](runnable: () => T)(using sch: Scheduler): Fiber[T] =
    new Fiber[T](sch):
      def run(): Unit =
        val result = runnable()
        state.set(State.Done(result))
      end run
  end task

  def race[T](left: Task[T], right: Task[T])(using sch: Scheduler): Fiber[T] =
    new Fiber[T](sch):
      def run(): Unit =
        def fire(task: Task[T]) = async {
          val newState = task.awaitTo[State]
          val oldState = state.get()
          state.compareAndSet(oldState, newState)
        }.fork
        fire(left)
        fire(right)
      end run
  end race

end Fiber
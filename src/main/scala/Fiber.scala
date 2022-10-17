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

  def start(): Unit
  
  final def join(): T = joinEither().fold(
    exc => throw exc,
    identity,
  )

  @tailrec
  final def joinEither(): Either[Throwable, T] =
    state.get() match
      case New => throw new IllegalStateException("Fiber is not started yet")
      case Running => scheduler.executeNext(); joinEither()
      case Done(result) => Right(result.asInstanceOf[T])
      case Failed(exc) => Left(throw exc)
  end joinEither

object Fiber:

  def task[T](runnable: () => T)(using sch: Scheduler): Fiber[T] =
    new Fiber[T](sch):
      import State.*
      def start(): Unit =
        state.set(Running)
        scheduler.execute { () =>
          try {
            val result = runnable()
            state.set(Done(result))
          } catch {
            case NonFatal(exc) =>
              state.set(Failed(exc))
          }
        }
      end start
  end task

  final class Signal[T](sch: Scheduler) extends Fiber[T](sch):
    import State.*
    override val state: AtomicReference[State] = AtomicReference[State](Running)

    override def start(): Unit = ()
    
    def complete(result: Either[Throwable, T]): Unit = {
      val witness = state.get()
      val newState = result.fold(
        exc => Failed(exc),
        value => Done(value),
      )
      state.compareAndSet(witness, newState)
    }
    
  end Signal
  
  def signal[T](using scheduler: Scheduler): Signal[T] = new Signal[T](scheduler)

end Fiber
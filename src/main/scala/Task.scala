opaque type Task[+T] = Fiber[T]
opaque type TaskFiber[+T] = Fiber[T]
object Task:

  extension [T](self: Task[T])
    inline def await: T = {
      self.start()
      self.join()
    }
    inline def awaitTo[F[+_]: Fallible]: F[T] = {
      self.start()
      self.joinTo[F]
    }
    inline def fork: TaskFiber[T] = {
      self.start()
      self
    }
  end extension

  extension [T](self: TaskFiber[T])
    inline def joinTo[F[+_]: Fallible]: F[T] = self.joinTo[F]
    inline def join(): T = self.join()
  end extension

  inline def cancellable[T](f: Cancellable => T)(using sch: Scheduler): (Task[T], Cancellable) =
    val token = if(sch.syncRequired) Cancellable.volatile else Cancellable.plain
    async(f(token)) -> token
  end cancellable

  inline def race[T](left: Task[T], right: Task[T])(using Scheduler): Task[T] =
    Fiber.race(left, right)

  inline def async[T](f: => T)(using scheduler: Scheduler): Task[T] =
    Fiber.task(() => f)

export Task.async
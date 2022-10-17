opaque type Task[+T] = Fiber[T]
opaque type TaskFiber[+T] = Fiber[T]
object Task:

  extension [T](self: Task[T])
    inline def await: T = {
      self.start()
      self.join()
    }
    inline def awaitEither: Either[Throwable, T] = {
      self.start()
      self.joinEither()
    }
    inline def fork: TaskFiber[T] = {
      self.start()
      self
    }
  end extension

  extension [T](self: TaskFiber[T])
    inline def joinEither(): Either[Throwable, T] = self.joinEither()
    inline def join(): T = self.join()
  end extension

  inline def race[T](left: Task[T], right: Task[T])(using Scheduler): Task[T] =
    Fiber.race(left, right)

  inline def async[T](f: => T)(using scheduler: Scheduler): Task[T] =
    Fiber.task(() => f)

export Task.async
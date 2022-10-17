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
    inline def join(): T = self.join()
  
  def race[T](left: Task[T], right: Task[T])(using scheduler: Scheduler): Task[T] = {
    val signal = Fiber.signal[T]
    def complete(task: Task[T]) = async {
      signal.complete(task.awaitEither)
    }.start()
    complete(left)
    complete(right)
    signal
  }

  inline def async[T](f: => T)(using scheduler: Scheduler): Task[T] =
    Fiber.task(() => f)

export Task.async
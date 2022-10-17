import java.util.concurrent.{ForkJoinPool, ForkJoinWorkerThread}
import scala.concurrent.ExecutionContext

trait Scheduler extends ExecutionContext:

  def executeNext(): Unit

  def shutdown(): Unit

object Scheduler:

  def singleThreaded[T](app: Scheduler ?=> T): T =
    val scheduler = singleThreadedImpl()
    val result = app(using scheduler)
    scheduler.executeNext()
    scheduler.shutdown()
    result
  end singleThreaded

  private def singleThreadedImpl(): Scheduler = new:
    import scala.collection.mutable

    private val tasks = mutable.Queue.empty[Runnable]
    private var working = true

    def execute(runnable: Runnable): Unit = if(working) tasks.addOne(runnable)

    def reportFailure(cause: Throwable): Unit = cause.printStackTrace()

    def executeNext(): Unit = if(working) {
      if(tasks.nonEmpty) tasks.dequeue().run()
    }

    def shutdown(): Unit = working = false

  end singleThreadedImpl

  def multiThreaded[T](app: Scheduler ?=> T): T =
    val scheduler = multiThreadedImpl()
    val result = app(using scheduler)
    scheduler.shutdown()
    result
  end multiThreaded

  private def multiThreadedImpl(): Scheduler = new:
    import java.util.concurrent.*
    // Unfortunately, FJP too good into hiding working queues, so this implementation utilize ThreadPoolExecutor

    private val tasks = SynchronousQueue[Runnable]()
    private val executor =
      ThreadPoolExecutor(
        sys.runtime.availableProcessors(),
        sys.runtime.availableProcessors(),
        1,
        TimeUnit.MINUTES,
        tasks,
      )

    def execute(runnable: Runnable): Unit = executor.execute(runnable)

    def reportFailure(cause: Throwable): Unit = cause.printStackTrace()

    def executeNext(): Unit = tasks.poll(10, TimeUnit.NANOSECONDS)

    def shutdown(): Unit = executor.shutdown()

  end multiThreadedImpl

end Scheduler
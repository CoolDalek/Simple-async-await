object Example:

  def main(args: Array[String]): Unit =
    println("Singlethreaded execution")
    Scheduler.singleThreaded(test)
    println("\nMultithreaded execution")
    Scheduler.multiThreaded(test)
  end main

  def test(using Scheduler): Unit =
    async {
      println("Hello!")
    }.await
    val winner = Task.race(
      fast,
      slow,
    ).await
    println(Thread.currentThread())
    println(s"Winner $winner")
  end test

  def fast(using Scheduler): Task[Int] = async {
    println("I'll complete first")
    println(Thread.currentThread())
    1
  }

  def slow(using Scheduler): Task[Int] = async {
    println("I'll complete second")
    println(Thread.currentThread())
    Thread.sleep(100)
    2
  }

end Example

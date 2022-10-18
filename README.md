What is it?
----
Simple Scala 3 implementation of imperative-style async-await syntax without CPS transformations.

How does it work?
----
This works atop Fiber abstraction.
Fiber is a unit of concurrent execution of user-space scheduler as opposed to Thread - unit of concurrent execution of OS-level scheduler.

When we encounter a "join" operation during execution, if necessary, we "yield" our fiber - i.e. we let other fibers do work on the thread that we occupy.

Usually, this is implemented through the abstraction of continuation - stack fragments that we can suspend and resume.
The current continuation is being suspended and another continuation start working.

However, we are also capable of just passing execution to the next fiber which this implementation do.

This works through plain method invocations  - on join operation we invoke the scheduler method to start execution of the next task.
Scheduler in their turn starts executing the next task on the current thread which is also just plain method invocation.

This means that the thread is being effectively utilized, fibers work asynchronously and yet we still have result of computation as plain value.

This also means that we consume native stack frames i.e. we can potentially get StackOverflowException.

The "async" operation - is just an alias for the fiber constructor. And "await" just starts execution and join fiber.
package com.shopsphere.datagen.common.parallel

import java.util.concurrent.{
  Callable,
  ExecutionException,
  ExecutorCompletionService,
  Executors,
  Future,
  TimeUnit
}
import scala.collection.mutable
import scala.util.control.NonFatal

/**
 * Executes work concurrently while consuming results in input order.
 *
 * At most maxInFlight tasks are submitted but not yet consumed.
 * The input iterator is consumed lazily.
 */
final class ParallelChunkExecutor(
                                   parallelism: Int,
                                   maxInFlight: Int
                                 ) {
  require(parallelism > 0, "Parallelism must be positive")
  require(maxInFlight >= parallelism, "maxInFlight must be >= parallelism")

  def execute[A, B](
                     inputs: Iterator[A]
                   )(
                     generate: A => B
                   )(
                     consume: (A, B) => Unit
                   ): Unit = {
    require(inputs != null, "Inputs must not be null")
    require(generate != null, "Generate function must not be null")
    require(consume != null, "Consume function must not be null")

    val executor = Executors.newFixedThreadPool(parallelism)
    val completionService =
      new ExecutorCompletionService[(Long, A, B)](executor)

    val pending = mutable.Map.empty[Long, Future[(Long, A, B)]]
    val completedResults = mutable.Map.empty[Long, (A, B)]

    var nextSubmitIndex = 0L
    var nextConsumeIndex = 0L
    var inputExhausted = false
    var failure: Throwable = null

    def submitNext(): Unit = {
      if (!inputExhausted) {
        if (inputs.hasNext) {
          val input = inputs.next()
          val index = nextSubmitIndex
          nextSubmitIndex = Math.addExact(nextSubmitIndex, 1L)

          val future = completionService.submit(
            new Callable[(Long, A, B)] {
              override def call(): (Long, A, B) =
                (index, input, generate(input))
            }
          )

          pending.put(index, future)
        } else {
          inputExhausted = true
        }
      }
    }

    try {
      while (!inputExhausted && pending.size < maxInFlight) {
        submitNext()
      }

      while (pending.nonEmpty) {
        val completedFuture = completionService.take()

        val (index, input, result) =
          try {
            completedFuture.get()
          } catch {
            case error: ExecutionException =>
              throw Option(error.getCause).getOrElse(error)
          }

        completedResults.put(index, (input, result))

        while (completedResults.contains(nextConsumeIndex)) {
          val (readyInput, readyResult) =
            completedResults.remove(nextConsumeIndex).get

          consume(readyInput, readyResult)
          pending.remove(nextConsumeIndex)
          nextConsumeIndex = Math.addExact(nextConsumeIndex, 1L)
        }

        while (!inputExhausted && pending.size < maxInFlight) {
          submitNext()
        }
      }
    } catch {
      case NonFatal(error) =>
        failure = error
    } finally {
      if (failure != null) {
        pending.values.foreach(_.cancel(true))
      }

      executor.shutdownNow()

      try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          val shutdownError =
            new IllegalStateException(
              "Executor did not terminate within 30 seconds"
            )

          if (failure != null) failure.addSuppressed(shutdownError)
          else failure = shutdownError
        }
      } catch {
        case NonFatal(shutdownError) =>
          if (failure != null) failure.addSuppressed(shutdownError)
          else failure = shutdownError
      }
    }

    if (failure != null) throw failure
  }
}

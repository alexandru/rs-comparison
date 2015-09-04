package swave.rsc

import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observable
import swave.rsc.Model.{ State, OuterSample, InnerSample, Point }
import concurrent.duration._
import scala.concurrent.Await

object MonifuPi extends App {
  val source = Observable
    .fromIterator(new RandomDoubleValueGenerator())
    .buffer(2)
    .map { case Seq(x, y) ⇒ Point(x, y) }
    .share()

  val innerSamples = source
    .filter(_.isInner).map(InnerSample)

  val outerSamples = source
    .filter(!_.isInner).map(OuterSample)

  val done = Observable
    .merge(innerSamples, outerSamples)
    .scan(State(0, 0)) { _ withNextSample _ }
    .throttleLast(1.second)
    .map(state ⇒ f"After ${state.totalSamples}%,10d samples π is approximated as ${state.π}%.5f")
    .take(10)
    .doWork(println)
    .complete
    .asFuture

  Await.result(done, Duration.Inf)
}

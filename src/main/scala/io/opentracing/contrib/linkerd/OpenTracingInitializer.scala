package io.opentracing.contrib.linkerd


import com.twitter.finagle.Stack
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.tracing.Tracer
import io.buoyant.telemetry.{Telemeter, TelemeterConfig, TelemeterInitializer}
import io.opentracing.contrib.tracerresolver.TracerResolver

class OpenTracingInitializer extends TelemeterInitializer {
  override type Config = OpentracingConfig

  override def configClass = classOf[OpentracingConfig]

  override val configId = "io.opentracing.tracer"
}

case class OpentracingConfig(componentName: Option[String])
  extends TelemeterConfig {

  override def mk(params: Stack.Params): OpentracingTelemeter =
    new OpentracingTelemeter(
      componentName.getOrElse("linkerd")
    )
}

class OpentracingTelemeter(componentName: String) extends Telemeter {
  val stats: StatsReceiver = NullStatsReceiver

  val openTracer: io.opentracing.Tracer = TracerResolver.resolveTracer();
  lazy val tracer: Tracer = new FinagleOpenTracer(openTracer)

  def run() = Telemeter.nopRun
}


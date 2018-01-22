package io.opentracing.contrib.linkerd


import java.net.InetSocketAddress
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import com.twitter.finagle.service.TimeoutFilter
import com.twitter.finagle.thrift.thrift.Constants
import com.twitter.finagle.tracing.{Record, TraceId}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.util.{Duration, Future, Time}
import io.opentracing.Span
import io.opentracing.Tracer.SpanBuilder
import io.opentracing.tag.Tags


class SpanCache(tracer: io.opentracing.Tracer) {

  private[this] val spanMap = new ConcurrentHashMap[TraceIdKey, SpanHolder](64)
  private[this] val ttl = Duration(120, TimeUnit.SECONDS)

  private[this] val timerTask = DefaultTimer.schedule(ttl) {
    flush(ttl.ago)
  }

  def flush(deadline: Time): Future[Unit] = {

    val iter = spanMap.entrySet.iterator
    while (iter.hasNext) {
      val kv = iter.next()
      val spanHolder = kv.getValue
      if (kv.getKey.started <= deadline) {
        spanMap.remove(kv.getKey)
        spanHolder.setTag("finagle.flush", deadline.toString())
        spanHolder.finish()
      }
    }

    Future.Done
  }

  def updateRpc(traceId: TraceId, value: String): Unit = {
    val spanHolder = getSpanHolder(traceId)
    spanHolder.setTag("rpc", value)
    spanHolder.setOperationName(value)
  }

  def replaceSpan(record: Record, span: Span): Unit = {
    val spanHolderPrev = spanMap.get(new TraceIdKey(record.traceId))
    if (spanHolderPrev != null) {
      for (elem <- spanHolderPrev.logs) {
        span.log(elem._1, elem._2)
      }
      for (elem <- spanHolderPrev.tags) {
        elem._2 match {
          case x: Int => span.setTag(elem._1, x)
          case s: String => span.setTag(elem._1, s)
          case b: Boolean => span.setTag(elem._1, b)
          case d: Double => span.setTag(elem._1, d)
        }
      }
    }
    span.setTag("guid:linkerd_trace_id", record.traceId.traceId.toString())
    val spanHolder = new SpanHolder(span)
    spanHolder.replaced = true
    spanMap.put(new TraceIdKey(record.traceId), spanHolder)
  }

  def updateServiceName(traceId: TraceId, serviceName: String): Unit = {
    val spanHolder = getSpanHolder(traceId)
    spanHolder.setTag("service name", serviceName)
  }

  def update(traceId: TraceId, ia: InetSocketAddress): Unit = {
    val spanHolder = getSpanHolder(traceId)
    spanHolder.setTag("hostname", ia.getHostName)
    spanHolder.setTag("port", ia.getPort)
    spanHolder.setTag("ip", ia.getAddress.toString)
  }

  def update(record: Record, key: String, value: String): Unit = {
    val spanHolder = getSpanHolder(record.traceId)
    spanHolder.setTag(key, value)
  }

  def update(record: Record, key: String, value: Boolean): Unit = {
    val spanHolder = getSpanHolder(record.traceId)
    spanHolder.setTag(key, value)
  }

  def update(record: Record, key: String, value: Short): Unit = {
    val spanHolder = getSpanHolder(record.traceId)
    spanHolder.setTag(key, value)
  }

  def update(record: Record, key: String, value: Double): Unit = {
    val spanHolder = getSpanHolder(record.traceId)
    spanHolder.setTag(key, value)
  }

  def update(record: Record, key: String, value: InetSocketAddress): Unit = {
    val spanHolder = getSpanHolder(record.traceId)
    spanHolder.setTag(key, value.toString)
  }

  def update(record: Record, annotation: String): Unit = {
    val spanHolder = getSpanHolder(record.traceId)
    spanHolder.log(record.timestamp.inMicroseconds, annotation)

    if (annotation.equals(Constants.CLIENT_SEND)) {
      spanHolder.setTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_CLIENT)
    } else if (annotation.equals(Constants.SERVER_SEND)) {
      spanHolder.setTag(Tags.SPAN_KIND.getKey, Tags.SPAN_KIND_SERVER)
    }

    if (annotation.equals(Constants.CLIENT_RECV) ||
      annotation.equals(Constants.SERVER_SEND) ||
      annotation.equals(TimeoutFilter.TimeoutAnnotation)) {
      spanMap.remove(new TraceIdKey(record.traceId))
      spanHolder.finish()
    }
  }

  private def getSpanHolder(traceId: TraceId): SpanHolder = {
    val spanHolder = spanMap.get(new TraceIdKey(traceId))
    if (spanHolder != null) {
      spanHolder
    } else {
      val spanBuilder: SpanBuilder = tracer.buildSpan("unknown")
        .withTag("guid:linkerd_trace_id", traceId.traceId.toString())

      if (traceId._parentId.isDefined) {
        val parent = findParent(traceId._parentId.get.toLong)
        if (parent != null) {
          spanBuilder.asChildOf(parent.span)
        }
      } else if (traceId._traceId.isDefined) {
        val parent = findParent(traceId._traceId.get.toLong)
        if (parent != null) {
          spanBuilder.asChildOf(parent.span)
        } else {
          // parent is not here,
          // spans with the same tag 'guid:linkerd_trace_id' will be joined by LS tracer
        }
      }

      val span = spanBuilder.startManual()
      val spanHolderNew = new SpanHolder(span)
      spanMap.put(new TraceIdKey(traceId), spanHolderNew)
      spanHolderNew
    }
  }

  private def findParent(spanId: Long) = {
    import scala.collection.JavaConverters._

    var parent: SpanHolder = null
    for (elem <- spanMap.asScala if parent == null) {
      if (elem._1._spanId equals spanId) {
        parent = elem._2
      }
    }
    parent
  }
}


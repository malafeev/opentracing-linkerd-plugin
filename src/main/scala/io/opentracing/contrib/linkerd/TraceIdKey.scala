package io.opentracing.contrib.linkerd

import com.twitter.finagle.tracing.{SpanId, TraceId}
import com.twitter.util.Time

class TraceIdKey(traceId: TraceId) {

  val _traceId: Long = traceId._traceId.getOrElse(SpanId(0)).toLong
  val _parentId: Long = traceId._parentId.getOrElse(SpanId(0)).toLong
  val _spanId: Long = traceId.spanId.toLong
  val started: Time = Time.now


  def canEqual(other: Any): Boolean = other.isInstanceOf[TraceIdKey]

  override def equals(other: Any): Boolean = other match {
    case that: TraceIdKey =>
      (that canEqual this) &&
        _traceId == that._traceId &&
        _parentId == that._parentId &&
        _spanId == that._spanId
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(_traceId, _parentId, _spanId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}


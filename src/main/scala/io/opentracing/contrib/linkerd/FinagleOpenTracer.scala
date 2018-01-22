package io.opentracing.contrib.linkerd

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.twitter.finagle.thrift.thrift
import com.twitter.finagle.tracing
import com.twitter.finagle.tracing.{Record, TraceId, Tracer}
import io.opentracing.Span


class FinagleOpenTracer(tracer: io.opentracing.Tracer) extends Tracer {
  private val ErrorAnnotation = "%s: %s" // annotation: errorMessage
  private val spanCache = new SpanCache(tracer)

  override def record(record: Record): Unit = {
    record.annotation match {
      case tracing.Annotation.WireSend =>
        annotate(record, thrift.Constants.WIRE_SEND)
      case tracing.Annotation.WireRecv =>
        annotate(record, thrift.Constants.WIRE_RECV)
      case tracing.Annotation.WireRecvError(error: String) =>
        annotate(record, ErrorAnnotation.format(thrift.Constants.WIRE_RECV_ERROR, error))
      case tracing.Annotation.ClientSend() =>
        annotate(record, thrift.Constants.CLIENT_SEND)
      case tracing.Annotation.ClientRecv() =>
        annotate(record, thrift.Constants.CLIENT_RECV)
      case tracing.Annotation.ClientRecvError(error: String) =>
        annotate(record, ErrorAnnotation.format(thrift.Constants.CLIENT_RECV_ERROR, error))
      case tracing.Annotation.ServerSend() =>
        annotate(record, thrift.Constants.SERVER_SEND)
      case tracing.Annotation.ServerRecv() =>
        annotate(record, thrift.Constants.SERVER_RECV)
      case tracing.Annotation.ServerSendError(error: String) =>
        annotate(record, ErrorAnnotation.format(thrift.Constants.SERVER_SEND_ERROR, error))
      case tracing.Annotation.ClientSendFragment() =>
        annotate(record, thrift.Constants.CLIENT_SEND_FRAGMENT)
      case tracing.Annotation.ClientRecvFragment() =>
        annotate(record, thrift.Constants.CLIENT_RECV_FRAGMENT)
      case tracing.Annotation.ServerSendFragment() =>
        annotate(record, thrift.Constants.SERVER_SEND_FRAGMENT)
      case tracing.Annotation.ServerRecvFragment() =>
        annotate(record, thrift.Constants.SERVER_RECV_FRAGMENT)
      case tracing.Annotation.Message(value) =>
        annotate(record, value)
      case tracing.Annotation.Rpc(rpc: String) =>
        spanCache.updateRpc(record.traceId, rpc)
      case tracing.Annotation.ServiceName(serviceName: String) =>
        spanCache.updateServiceName(record.traceId, serviceName)
      case tracing.Annotation.BinaryAnnotation(key: String, value: Boolean) =>
        binaryAnnotation(
          record,
          key,
          value
        )
      case tracing.Annotation.BinaryAnnotation(key: String, value: Array[Byte]) =>
        binaryAnnotation(record, key, value)
      case tracing.Annotation.BinaryAnnotation(key: String, value: ByteBuffer) =>
        binaryAnnotation(record, key, value)
      case tracing.Annotation.BinaryAnnotation(key: String, value: Short) =>
        binaryAnnotation(
          record,
          key,
          value
        )
      case tracing.Annotation.BinaryAnnotation(key: String, value: Int) =>
        binaryAnnotation(
          record,
          key,
          value
        )
      case tracing.Annotation.BinaryAnnotation(key: String, value: Long) =>
        binaryAnnotation(
          record,
          key,
          value
        )
      case tracing.Annotation.BinaryAnnotation(key: String, value: Double) =>
        binaryAnnotation(
          record,
          key,
          value
        )
      case tracing.Annotation.BinaryAnnotation(key: String, value: String) =>
        binaryAnnotation(record, key, value)
      case tracing.Annotation.BinaryAnnotation(key: String, value: Span) =>
        setSpan(record, value)
      case tracing.Annotation.BinaryAnnotation(key: String, value) => // Throw error?
      case tracing.Annotation.LocalAddr(ia: InetSocketAddress) =>
        setEndpoint(record, ia)
      case tracing.Annotation.ClientAddr(ia: InetSocketAddress) =>
        // use a binary annotation over a regular annotation to avoid a misleading timestamp
        binaryAnnotation(
          record,
          thrift.Constants.CLIENT_ADDR,
          ia
        )
      case tracing.Annotation.ServerAddr(ia: InetSocketAddress) =>
        binaryAnnotation(
          record,
          thrift.Constants.SERVER_ADDR,
          ia
        )

    }
  }

  /**
    * Sets the endpoint in the span for any future annotations. Also
    * sets the endpoint in any previous annotations that lack one.
    */
  protected def setEndpoint(record: Record, ia: InetSocketAddress): Unit = {
    spanCache.update(record.traceId, ia)
  }

  def setSpan(record: Record, span: Span): Unit = {
    spanCache.replaceSpan(record, span)
  }

  def binaryAnnotation(record: Record, key: String, ia: InetSocketAddress): Unit = {
    spanCache.update(record, key, ia)
  }

  def binaryAnnotation(record: Record, key: String, value: Short): Unit = {
    spanCache.update(record, key, value)
  }

  def binaryAnnotation(record: Record, key: String, value: Double): Unit = {
    spanCache.update(record, key, value)
  }

  def binaryAnnotation(record: Record, key: String, value: ByteBuffer): Unit = {
    spanCache.update(record, key, new String(value.array(), StandardCharsets.UTF_8))
  }

  def binaryAnnotation(record: Record, key: String, value: String): Unit = {
    spanCache.update(record, key, value)
  }

  def binaryAnnotation(record: Record, key: String, value: Array[Byte]): Unit = {
    spanCache.update(record, key, new String(value, StandardCharsets.UTF_8))
  }

  def binaryAnnotation(record: Record, key: String, value: Boolean): Unit = {
    spanCache.update(record, key, value)
  }

  def annotate(record: Record, value: String): Unit = {
    spanCache.update(record, value)
  }

  override def sampleTrace(traceId: TraceId): Option[Boolean] = Option(true)

  override def isNull() = false

  override def isActivelyTracing(traceId: TraceId) = true
}



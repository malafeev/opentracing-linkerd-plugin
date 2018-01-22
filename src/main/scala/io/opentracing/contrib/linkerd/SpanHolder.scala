package io.opentracing.contrib.linkerd

import io.opentracing.Span

import scala.collection.mutable

class SpanHolder(val span: Span) {
  var replaced: Boolean = false
  val tags: mutable.Map[String, Any] = mutable.HashMap()
  val logs: mutable.Map[Long, String] = mutable.HashMap()

  def setTag(key: String, value: String) = {
    tags.put(key, value)
    span.setTag(key, value)
  }

  def setTag(key: String, value: Int) = {
    tags.put(key, value)
    span.setTag(key, value)
  }

  def setTag(key: String, value: Boolean) = {
    tags.put(key, value)
    span.setTag(key, value)
  }

  def setTag(key: String, value: Double) = {
    tags.put(key, value)
    span.setTag(key, value)
  }

  def setOperationName(value: String): Unit = {
    if (!replaced) {
      span.setOperationName(value)
    }
  }

  def log(timeStamp: Long, event: String) = {
    logs.put(timeStamp, event)
    span.log(timeStamp, event)
  }

  def finish() = span.finish()
}


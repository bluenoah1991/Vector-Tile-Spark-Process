package org.apache.spark.sql

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.{WKBReader, WKBWriter, WKTReader, WKTWriter}

/**
  * Created by CodeMeow on 2017/5/11.
  */

// Ref https://github.com/locationtech/geomesa/blob/master/geomesa-utils/src/main/scala/org/locationtech/geomesa/utils/text/WKUtils.scala
object WKTUtils {
  private val readerPool = new ThreadLocal[WKTReader]{
    override def initialValue() = new WKTReader
  }

  private val writePool = new ThreadLocal[WKTWriter]{
    override def initialValue() = new WKTWriter
  }

  def read(s: String): Geometry = readerPool.get.read(s)
  def write(g: Geometry): String = writePool.get.write(g)
}

object WKBUtils {
  private val readerPool = new ThreadLocal[WKBReader]{
    override def initialValue() = new WKBReader
  }

  private val writePool = new ThreadLocal[WKBWriter]{
    override def initialValue() = new WKBWriter
  }

  def read(b: Array[Byte]): Geometry = readerPool.get.read(b)
  def write(g: Geometry): Array[Byte] = writePool.get.write(g)
}
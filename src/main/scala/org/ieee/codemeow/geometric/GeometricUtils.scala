package org.ieee.codemeow.geometric

import com.vividsolutions.jts.geom.{Envelope, Geometry}
import org.apache.spark.rdd.RDD
import org.geotools.geometry.jts.JTS

/**
  * Created by CodeMeow on 2017/5/12.
  */
object GeometricUtils {

  def boundary(rdd: RDD[Geometry]): (Double, Double, Double, Double) ={
    implicit val XMinComparator = new Ordering[Geometry]{
      override def compare(geom1: Geometry, geom2: Geometry): Int ={
        val x1 = geom1.getEnvelopeInternal.getMinX
        val x2 = geom2.getEnvelopeInternal.getMinX
        return if(x1 > x2) 1 else (if(x1 < x2) -1 else 0)
      }
    }
    implicit val XMaxComparator = new Ordering[Geometry]{
      override def compare(geom1: Geometry, geom2: Geometry): Int ={
        val x1 = geom1.getEnvelopeInternal.getMaxX
        val x2 = geom2.getEnvelopeInternal.getMaxX
        return if(x1 > x2) 1 else (if(x1 < x2) -1 else 0)
      }
    }
    implicit val YMinComparator = new Ordering[Geometry]{
      override def compare(geom1: Geometry, geom2: Geometry): Int ={
        val y1 = geom1.getEnvelopeInternal.getMinY
        val y2 = geom2.getEnvelopeInternal.getMinY
        return if(y1 > y2) 1 else (if(y1 < y2) -1 else 0)
      }
    }
    implicit val YMaxComparator = new Ordering[Geometry]{
      override def compare(geom1: Geometry, geom2: Geometry): Int ={
        val y1 = geom1.getEnvelopeInternal.getMaxY
        val y2 = geom2.getEnvelopeInternal.getMaxY
        return if(y1 > y2) 1 else (if(y1 < y2) -1 else 0)
      }
    }
    val minX: Double = rdd.min()(XMinComparator).getEnvelopeInternal.getMinX
    val maxX: Double = rdd.max()(XMaxComparator).getEnvelopeInternal.getMaxX
    val minY: Double = rdd.min()(YMinComparator).getEnvelopeInternal.getMinY
    val maxY: Double = rdd.max()(YMaxComparator).getEnvelopeInternal.getMaxY

    return (minX, minY, maxX, maxY)
  }

  def intersectedTiles(geom: Geometry, zoom: Long): Seq[(Long, Long, Long)] ={
    val minX = geom.getEnvelopeInternal.getMinX
    val maxX = geom.getEnvelopeInternal.getMaxX
    val minY = geom.getEnvelopeInternal.getMinY
    val maxY = geom.getEnvelopeInternal.getMaxY
    val bbox = (minX, minY, maxX, maxY)

    val tiles = CRSUtils.mercatorToTiles(bbox, zoom)
    tiles.filter(tile => {
      val ll = CRSUtils.upperLeftMercator((tile._1, tile._2 + 1, tile._3))
      val ur = CRSUtils.upperLeftMercator((tile._1 + 1, tile._2, tile._3))
      val boundary = JTS.toGeometry(new Envelope(ll._1, ur._1, ll._2, ur._2))
      boundary.intersects(geom)
    })
  }

  def encodeTile(layerName: String, tile: (Long, Long, Long)): String ={
    val (row, column, zoom) = tile
    val code = (row % 0x1000000L) << 40 | (column % 0x1000000L) << 16 | (zoom % 0x10000L)
    s"${layerName}:${code}"
  }

  def decodeTile(key: String): (String, (Long, Long, Long)) ={
    val x = key.split(":").toSeq
    val layerName = x(0)
    val code = x(1).toLong
    val row = code >> 40
    val column = (code >> 16) % 0x1000000L
    val zoom = code % 0x10000L
    (layerName, (row, column, zoom))
  }

}

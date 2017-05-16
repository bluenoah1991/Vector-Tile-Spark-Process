package org.apache.spark.sql

import com.vividsolutions.jts.geom._
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow
import org.apache.spark.sql.types._
import org.geotools.geometry.jts.JTS

import scala.reflect.ClassTag

/**
  * Created by CodeMeow on 2017/5/11.
  */


object SQLGeometricExtensions {

  UDTRegistration.register(classOf[Point].getCanonicalName, classOf[PointUDT].getCanonicalName)
  UDTRegistration.register(classOf[MultiPoint].getCanonicalName, classOf[MultiPointUDT].getCanonicalName)
  UDTRegistration.register(classOf[LineString].getCanonicalName, classOf[LineStringUDT].getCanonicalName)
  UDTRegistration.register(classOf[MultiLineString].getCanonicalName, classOf[MultiLineStringUDT].getCanonicalName)
  UDTRegistration.register(classOf[Polygon].getCanonicalName, classOf[PolygonUDT].getCanonicalName)
  UDTRegistration.register(classOf[MultiPolygon].getCanonicalName, classOf[MultiPolygonUDT].getCanonicalName)
  UDTRegistration.register(classOf[Geometry].getCanonicalName, classOf[GeometryUDT].getCanonicalName)

  def registerExtensions(spark: SparkSession): Unit ={
    SQLGeometricFunctions.registerFunctions(spark)
  }
}

// Ref https://github.com/locationtech/geomesa/blob/master/geomesa-spark/geomesa-spark-sql/src/main/scala/org/apache/spark/sql/SQLGeometricConstructorFunctions.scala
object SQLGeometricFunctions {

  val ST_MakePoint: (Double, Double) => Point = (x: Double, y: Double) => WKTUtils.read(s"POINT($x $y)").asInstanceOf[Point]
  val ST_MakeBox2D: (Point, Point) => Geometry = (ll: Point, ur: Point) => JTS.toGeometry(new Envelope(ll.getX, ur.getX, ll.getY, ur.getY))
  val ST_GeomFromWKB:  Array[Byte] => Geometry = (array: Array[Byte]) => WKBUtils.read(array)
  val ST_Intersects: (Geometry, Geometry) => Boolean = (geom1: Geometry, geom2: Geometry) => geom1.intersects(geom2)

  def registerFunctions(spark: SparkSession): Unit = {
    spark.udf.register("ST_MakePoint", ST_MakePoint)
    spark.udf.register("ST_MakeBox2D", ST_MakeBox2D)
    spark.udf.register("ST_GeomFromWKB", ST_GeomFromWKB)
    spark.udf.register("ST_Intersects", ST_Intersects)
  }

}

// Ref https://github.com/locationtech/geomesa/blob/master/geomesa-spark/geomesa-spark-sql/src/main/scala/org/apache/spark/sql/SQLTypes.scala
abstract class AbstractGeometryUDT[T >: Null <: Geometry](override val simpleString: String)(implicit cm: ClassTag[T]) extends UserDefinedType[T]{
  override def serialize(obj: T): InternalRow = {
    new GenericInternalRow(Array[Any](WKBUtils.write(obj)))
  }

  override def sqlType: DataType = StructType(
    Seq(
      StructField("wkb", DataTypes.BinaryType)
    )
  )

  override def userClass: Class[T] = cm.runtimeClass.asInstanceOf[Class[T]]

  override def deserialize(datum: Any): T = {
    val ir = datum.asInstanceOf[InternalRow]
    WKBUtils.read(ir.getBinary(0)).asInstanceOf[T]
  }
}

private[spark] class PointUDT extends AbstractGeometryUDT[Point]("point")
private[spark] class MultiPointUDT extends AbstractGeometryUDT[MultiPoint]("multipoint")
private[spark] class LineStringUDT extends AbstractGeometryUDT[LineString]("linestring")
private[spark] class MultiLineStringUDT extends AbstractGeometryUDT[MultiLineString]("multilinestring")
private[spark] class PolygonUDT extends AbstractGeometryUDT[Polygon]("polygon")
private[spark] class MultiPolygonUDT extends AbstractGeometryUDT[MultiPolygon]("multipolygon")

private[spark] class GeometryUDT extends AbstractGeometryUDT[Geometry]("geometry"){
  private[sql] override def acceptsType(dataType: DataType): Boolean = {
    super.acceptsType(dataType) ||
      dataType.getClass == classOf[GeometryUDT] ||
      dataType.getClass == classOf[PointUDT] ||
      dataType.getClass == classOf[MultiPointUDT] ||
      dataType.getClass == classOf[LineStringUDT] ||
      dataType.getClass == classOf[MultiLineStringUDT] ||
      dataType.getClass == classOf[PolygonUDT] ||
      dataType.getClass == classOf[MultiPolygonUDT]
  }
}
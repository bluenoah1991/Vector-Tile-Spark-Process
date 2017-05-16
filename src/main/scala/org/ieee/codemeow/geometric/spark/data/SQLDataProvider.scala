package org.ieee.codemeow.geometric.spark.data

import com.vividsolutions.jts.geom.Geometry
import org.apache.spark.sql.{Dataset, Encoders, SparkSession}
import org.ieee.codemeow.geometric.Feature
import org.ieee.codemeow.geometric.spark.LayerConfiguration


/**
  * Created by CodeMeow on 2017/5/13.
  */
class SQLDataProvider(_spark: SparkSession, _layer: LayerConfiguration) extends AbstractDataProvider(_spark, _layer){

  val url = layer.kwargs.get("url").get.asInstanceOf[String]
  val dbtables = layer.kwargs.get("dbtables").get.asInstanceOf[Map[String, String]]
  val user = layer.kwargs.get("user").get.asInstanceOf[String]
  val password = layer.kwargs.get("password").get.asInstanceOf[String]
  val zoomConfig = layer.kwargs.get("zooms").get.asInstanceOf[Map[String, String]]

  // load all tables
  dbtables.foreach(tuple => {
    val sparkTableName = tuple._1
    val realTableName = tuple._2

    val mapDataFrame = spark.read.format("jdbc")
      .option("url", url)
      .option("user", user)
      .option("password", password)
      .option("dbtable", realTableName).load

    mapDataFrame.createOrReplaceTempView(sparkTableName)
  })

  override def getFeatures(layerName: String, zoom: Long): Option[Dataset[Feature]] ={
    // Ref http://stackoverflow.com/questions/38664972/why-is-unable-to-find-encoder-for-type-stored-in-a-dataset-when-creating-a-dat
    import spark.implicits._
    // Ref http://stackoverflow.com/questions/36648128/how-to-store-custom-objects-in-dataset
    implicit val featureEncoder = Encoders.kryo[Feature]

    val natureSQL = zoomConfig.get(zoom.toString)
    if(natureSQL.isEmpty){
      return None
    }

    val rawDF = spark.sql(natureSQL.get)
    val featureCollection = rawDF.map(row => {
      val id = row.getAs[Long]("__id__")
      val geom = row.getAs[Geometry]("__geometry__")
      val fields = row.schema.filter(field => {
        !Seq("__id__", "__geometry__").contains(field.name)
      }).map(field => field.name)
      val props = row.getValuesMap[String](fields)
      Feature(id, geom, props)
    })
    Some(featureCollection)
  }
}
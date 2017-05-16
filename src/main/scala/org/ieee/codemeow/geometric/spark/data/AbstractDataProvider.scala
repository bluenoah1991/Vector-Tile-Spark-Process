package org.ieee.codemeow.geometric.spark.data

import org.apache.spark.sql.{Dataset, SparkSession}
import org.ieee.codemeow.geometric.Feature
import org.ieee.codemeow.geometric.spark.LayerConfiguration

/**
  * Created by CodeMeow on 2017/5/16.
  */
abstract class AbstractDataProvider(_spark: SparkSession, _layer: LayerConfiguration) {

  val spark = _spark
  val layer = _layer

  def getFeatures(layerName: String, zoom: Long): Option[Dataset[Feature]]
}

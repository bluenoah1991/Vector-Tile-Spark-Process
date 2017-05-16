package org.ieee.codemeow.geometric.spark


import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.common.base.Preconditions
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}



/**
  * Created by CodeMeow on 2017/5/12.
  */
object Configurations {
  def fromFile(_path: String): MainConfiguration ={
    val config = new Configuration
    val path = new Path(_path)
    val fs = FileSystem.get(path.toUri, config)
    val file = fs.open(path)
    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)
    mapper.readValue(file, classOf[MainConfiguration])
  }
}

class MainConfiguration (
                          @JsonProperty("appName") _appName: String,
                          @JsonProperty("bbox") _bbox: (Double, Double, Double, Double), // lat1, lon1, lat2, lon2
                          @JsonProperty("layers") _layers: Array[LayerConfiguration],
                          @JsonProperty("sequeueFileDir") _sequeueFileDir: String
                        ) extends Serializable{
  val appName = Preconditions.checkNotNull(_appName, "appName cannot be null": Object)
  val bbox = _bbox
  val layers = Preconditions.checkNotNull(_layers, "layers cannot be null": Object)
  val sequeueFileDir = Preconditions.checkNotNull(_sequeueFileDir, "sequeueFileDir cannot be null": Object)
}

class LayerConfiguration (
                         @JsonProperty("layerName") _layerName: String,
                         @JsonProperty("minZoom") _minZoom: Int,
                         @JsonProperty("maxZoom") _maxZoom: Int,
                         @JsonProperty("dataProvider") _dataProvider: String,
                         @JsonProperty("kwargs") _kwargs: Map[String, Any]
                         ) extends Serializable{
  val layerName = Preconditions.checkNotNull(_layerName, "layerName cannot be null": Object)
  val minZoom = Preconditions.checkNotNull(_minZoom, "minZoom cannot be null": Object)
  val maxZoom = Preconditions.checkNotNull(_maxZoom, "maxZoom cannot be null": Object)
  val dataProvider = Preconditions.checkNotNull(_dataProvider, "dataProvider cannot be null": Object)
  val kwargs = Preconditions.checkNotNull(_kwargs, "kwargs cannot be null": Object)
}
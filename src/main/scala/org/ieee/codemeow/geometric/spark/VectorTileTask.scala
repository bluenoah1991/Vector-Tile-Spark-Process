package org.ieee.codemeow.geometric.spark

import org.apache.hadoop.io.compress.GzipCodec
import org.apache.spark.sql._
import org.ieee.codemeow.geometric.{Feature, GeometricUtils, MvtBuilder}
import org.ieee.codemeow.geometric.spark.data.AbstractDataProvider

/**
  * Created by CodeMeow on 2017/5/13.
  */
object VectorTileTask {

  val usage = "Usage: configFile"

  private def checkArgs(args: Array[String]) = {
    assert(args != null && args.length >= 1, usage)
  }

  def main(args: Array[String]): Unit ={
    checkArgs(args)

    // load application config
    val config: MainConfiguration = Configurations.fromFile(args(0))

    // Ref http://spark.apache.org/docs/latest/sql-programming-guide.html#starting-point-sparksession
    val spark = SparkSession.builder().appName(config.appName).getOrCreate()

    // Ref http://stackoverflow.com/questions/38664972/why-is-unable-to-find-encoder-for-type-stored-in-a-dataset-when-creating-a-dat
    import spark.implicits._
    // Ref http://stackoverflow.com/questions/36648128/how-to-store-custom-objects-in-dataset
    implicit val featureEncoder = Encoders.kryo[Feature]

    SQLGeometricExtensions.registerExtensions(spark)

    val layersCollection = config.layers.map(layer => {
      // classloader
      val constructor = Class.forName(layer.dataProvider).getConstructor(classOf[SparkSession], classOf[LayerConfiguration])
      val dataSource = constructor.newInstance(spark, layer).asInstanceOf[AbstractDataProvider]

      val tileCodeWithBytesCollection = buildLayerRDD(spark, dataSource, layer)
      tileCodeWithBytesCollection
    }).reduce((ds1, ds2) => {
      ds1.union(ds2)
    })

    layersCollection.rdd.saveAsSequenceFile(config.sequenceFileDir, Some(classOf[GzipCodec]))
  }

  def buildLayerRDD(spark: SparkSession, dataSource: AbstractDataProvider, layer: LayerConfiguration): Dataset[(String, Array[Byte])] ={
    // Ref http://stackoverflow.com/questions/38664972/why-is-unable-to-find-encoder-for-type-stored-in-a-dataset-when-creating-a-dat
    import spark.implicits._
    // Ref http://stackoverflow.com/questions/36648128/how-to-store-custom-objects-in-dataset
    //implicit val featureEncoder = Encoders.kryo[Feature]


    val layerMvtCollection = (layer.minZoom to layer.maxZoom).map(zoom => {
      val featureCollection = dataSource.getFeatures(layer.layerName, zoom)

      if(featureCollection.isEmpty){

        spark.emptyDataset[((Long, Long, Long), Seq[Feature])]
      } else {
        val featureWithTilesCollection = featureCollection.get.map(feature => {
          (feature, GeometricUtils.intersectedTiles(feature.geom, zoom))
        })

        val tileWithFeatureCollection = featureWithTilesCollection.flatMap(tuple => {
          tuple._2.map(tile => {
            (tile, tuple._1)
          })
        })

        val featureGroupByTileCollection_ = tileWithFeatureCollection.groupByKey(tuple => tuple._1)

        val featureGroupByTileCollection = featureGroupByTileCollection_.mapGroups((tile, groups) => {
          (tile, groups.map(tuple => tuple._2).toSeq)
        })

        featureGroupByTileCollection
      }

    }).reduce((ds1, ds2) => {
      ds1.union(ds2)
    })

    val tileWithBytesCollection = layerMvtCollection.map(tuple => {
      val layer_ = MvtBuilder.buildLayer(layer.layerName, tuple._1, tuple._2)
      val b = MvtBuilder.buildMvt(layer_).toByteArray
      (tuple._1, b)
    })

    val tileCodeWithBytesCollection = tileWithBytesCollection.map(tuple => {
      (GeometricUtils.encodeTile(layer.layerName, tuple._1), tuple._2)
    })

    tileCodeWithBytesCollection
  }
}

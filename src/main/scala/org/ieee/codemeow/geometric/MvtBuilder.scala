package org.ieee.codemeow.geometric

import com.wdtinc.mapbox_vector_tile.VectorTile
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Layer
import com.wdtinc.mapbox_vector_tile.adapt.jts.{JtsAdapter, UserDataIgnoreConverter}
import com.wdtinc.mapbox_vector_tile.build.{MvtLayerBuild, MvtLayerParams, MvtLayerProps}

/**
  * Created by CodeMeow on 2017/5/13.
  */
object MvtBuilder {

  def buildLayer(layerName: String, features: TraversableOnce[Feature]): Layer ={
    val layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, new MvtLayerParams)

    for(feature <- features){
      val layerProps = new MvtLayerProps
      for((key, value) <- feature.props){
        if(key == null || value == null){
        } else {
          layerProps.addKey(key)
          layerProps.addValue(value)
        }
      }

      val f = JtsAdapter.toFeatures(feature.geom, layerProps, new UserDataIgnoreConverter)
      layerBuilder.addAllFeatures(f)
    }

    return layerBuilder.build()
  }

  def buildMvt(layers: Seq[Layer]): Tile ={
    val tileBuilder = VectorTile.Tile.newBuilder()
    for(layer <- layers){
      tileBuilder.addLayers(layer)
    }
    tileBuilder.build()
  }

  def buildMvt(layer: Layer): Tile ={
    val tileBuilder = VectorTile.Tile.newBuilder()
    tileBuilder.addLayers(layer)
    tileBuilder.build()
  }

}

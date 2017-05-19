package org.ieee.codemeow.geometric

import com.vividsolutions.jts.geom.{Envelope, Geometry, GeometryFactory}
import com.wdtinc.mapbox_vector_tile.VectorTile
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile
import com.wdtinc.mapbox_vector_tile.VectorTile.Tile.Layer
import com.wdtinc.mapbox_vector_tile.adapt.jts.{IGeometryFilter, JtsAdapter, UserDataIgnoreConverter}
import com.wdtinc.mapbox_vector_tile.build.{MvtLayerBuild, MvtLayerParams, MvtLayerProps}

/**
  * Created by CodeMeow on 2017/5/13.
  */
object MvtBuilder {

  def buildLayer(layerName: String, tile: (Long, Long, Long), features: TraversableOnce[Feature]): Layer ={
    val bounds = CRSUtils.boundsMercator(tile)
    val tileEnvelope = new Envelope(bounds._1, bounds._2, bounds._3, bounds._4)

    val layerParams = new MvtLayerParams
    val layerBuilder = MvtLayerBuild.newLayerBuilder(layerName, layerParams)

    for(feature <- features){
      val geomFactory = new GeometryFactory()
      val tileGeom = JtsAdapter.createTileGeom(feature.geom, tileEnvelope, geomFactory, layerParams, AcceptAllGeomFilter)

      val layerProps = new MvtLayerProps
      for((key, value) <- feature.props){
        if(key == null || value == null){
        } else {
          layerProps.addKey(key)
          layerProps.addValue(value)
        }
      }

      val f = JtsAdapter.toFeatures(tileGeom.mvtGeoms, layerProps, new UserDataIgnoreConverter)
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

object AcceptAllGeomFilter extends IGeometryFilter{
  override def accept(geometry: Geometry) ={
    true
  }
}
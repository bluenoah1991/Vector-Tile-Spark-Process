package org.ieee.codemeow.geometric

/**
  * Created by CodeMeow on 2017/5/12.
  */

// Ref https://github.com/mapbox/mercantile/blob/master/mercantile/__init__.py
object CRSUtils {

  val MAX_ZOOM: Long = 28
  val RADIUS: Double = 6378137.0

  // Returns the upper left (lon, lat) of tile
  def upperLeft(tile: (Long, Long, Long)): (Double, Double) ={
    val (row, column, zoom) = tile
    val n = math.pow(2.0, zoom)
    val lon_deg = row / n * 360.0 - 180.0
    val lat_rad = math.atan(math.sinh(math.Pi * (1 - 2 * column / n)))
    val lat_deg = math.toDegrees(lat_rad)
    return (lon_deg, lat_deg)
  }

  // Returns the upper left Mercator coordinate (x, y) of tile
  def upperLeftMercator(tile: (Long, Long, Long)): (Double, Double) ={
    val (lng, lat) = upperLeft(tile)
    lnglatToMercator(lng, lat)
  }

  // Returns the (lon, lat) bounding box of a tile
  def bounds(tile: (Long, Long, Long)): (Double, Double, Double, Double) ={
    val (row, column, zoom) = tile
    val a = upperLeft(tile)
    val b = upperLeft((row + 1, column + 1, zoom))
    return (a._1, b._2, b._1, a._2)
  }

  // Returns the Mercator coordinate (x, y) bounding box of a tile
  def boundsMercator(tile: (Long, Long, Long)): (Double, Double, Double, Double) ={
    val (row, column, zoom) = tile
    val a = upperLeftMercator(row + 1, column, zoom)
    val b = upperLeftMercator((row, column + 1, zoom))
    return (a._1, b._1, a._2, b._2)
  }

  // Returns the Spherical Mercator (x, y) in meters
  def lnglatToMercator(lng: Double, lat: Double): (Double, Double) ={
    val x = RADIUS * math.toRadians(lng)
    val y = RADIUS * math.log(math.tan((math.Pi * 0.25) + (0.5 * math.toRadians(lat))))
    return (x, y)
  }

  // Returns the Location (lon, lat) from Spherical Mercator (x, y)
  def mercatorTolnglat(x: Double, y: Double): (Double, Double) ={
    val lng = math.toDegrees(x / RADIUS)
    val lat = math.toDegrees(math.atan(math.exp(y / RADIUS)) * 2.0 - math.Pi / 2)
    return (lng, lat)
  }

  // Returns the (row, column, zoom) tile of a location (lon, lat)
  def lnglatToTile(lng: Double, lat: Double, zoom: Long): (Long, Long, Long) ={
    val lat_rad = math.toRadians(lat)
    val n = math.pow(2.0, zoom)
    val row = math.floor((lng + 180.0) / 360.0 * n).toLong
    val column = math.floor((1.0 - math.log(math.tan(lat_rad) + (1.0 / math.cos(lat_rad))) / math.Pi) / 2.0 * n).toLong
    return (row, column, zoom)
  }

  // Returns the (row, column, zoom) tile from Spherical Mercator (x, y) in special zoom level
  def mercatorToTile(x: Double, y: Double, zoom: Long): (Long, Long, Long) ={
    val (lng, lat) = mercatorTolnglat(x, y)
    lnglatToTile(lng, lat, zoom)
  }

  // Returns the tiles included in the Mercator bbox (minX, minY, maxX, maxY) in special zoom level
  def mercatorToTiles(bbox: (Double, Double, Double, Double), zoom: Long): Seq[(Long, Long, Long)] ={
    val ul = mercatorToTile(bbox._1, bbox._4, zoom)
    val lr_ = mercatorToTile(bbox._3, bbox._2, zoom)
    val lr = (lr_._1 + 1, lr_._2 + 1, zoom)

    (ul._1 to lr._1).flatMap(row => {
      (ul._2 to lr._2).map(column => {
        (row, column, zoom)
      })
    })
  }

  // Returns the children of an (row, column, zoom) tile
  def childrenTiles(tile: (Long, Long, Long)): Seq[(Long, Long, Long)] ={
    val (row, column, zoom) = tile
    return Seq[(Long, Long, Long)](
    (row * 2, column * 2, zoom + 1),
    (row * 2 + 1, column * 2, zoom + 1),
    (row * 2 + 1, column * 2 + 1, zoom + 1),
    (row * 2, column * 2 + 1, zoom + 1)
    )
  }

  // Returns the smallest tile (row, column, zoom) containing the bbox (west, south, east, north)
  def boundingTile(bbox: (Double, Double, Double, Double)): (Long, Long, Long) ={
    val (w, s, e, n) = bbox
    val tmin = lnglatToTile(w, s, 32)
    val tmax = lnglatToTile(e, n, 32)
    val cell = (tmin._1, tmin._2, tmax._1, tmax._2)
    val zoom = getBboxZoom(cell)
    if(zoom == 0) {
      return (0, 0, 0)
    }
    val row = rshift(cell._1, (32 - zoom))
    val column = rshift(cell._2, (32 - zoom))
    return (row, column, zoom)
  }

  def rshift(value: Long, n: Long): Long ={
    return (value % 0x100000000L) >> n
  }

  def getBboxZoom(bbox: (Long, Long, Long, Long)): Long ={
    for(zoom <- 0L to MAX_ZOOM){
      val mask = 1 << (32 - (zoom + 1))
      if(((bbox._1 & mask) != (bbox._3 & mask)) || ((bbox._2 & mask) != (bbox._4 & mask))){
        return zoom
      }
    }
    return MAX_ZOOM
  }

}

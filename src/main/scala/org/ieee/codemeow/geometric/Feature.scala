package org.ieee.codemeow.geometric

import com.vividsolutions.jts.geom.Geometry

/**
  * Created by CodeMeow on 2017/5/13.
  */
case class Feature(id: Long, geom: Geometry, props: scala.collection.Map[String, String])
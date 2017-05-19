# Vector Tile Spark Process

The ***Vector Tile Spark Process*** allows developers and data scientists clip geographic data into Hadoop SequeueFiles on Spark platform.

The effect comes from [Preview Example](https://github.com/codemeow5/Vector-Tile-Spark-Process/tree/master/examples).  
![Preview](examples/preview.png)

## Features

- Parallel processing based on Apache Spark
- Adapting various data sources
- Output standard [Mapbox Vector Tiles](https://github.com/mapbox/vector-tile-spec/tree/master/2.1 "Vector Tile Specification") format
- A configuration file similar to [TileStache.Goodies.VecTiles.Provider](https://github.com/TileStache/TileStache/blob/master/TileStache/Goodies/VecTiles/server.py)

## Dependencies

- [GeoTools](http://www.geotools.org/ "GeoTools") - an open source Java library that provides tools for geospatial data
- [mapbox-vector-tile-java](https://github.com/wdtinc/mapbox-vector-tile-java "mapbox-vector-tile-java") - Java Mapbox Vector Tile Library for Encoding/Decoding

## Requirements

- Hadoop 2.7 and later
- Spark 2.1.1 and above
- Protocol Buffers 3.0.0-beta-2

## Getting Started

### Build

	$ mvn clean && mvn package

### Run

	$SPARK_HOME/bin/spark-submit --class org.ieee.codemeow.geometric.spark.VectorTileTask --master yarn --deploy-mode cluster --jars /path/to/postgresql-42.0.0.jar --driver-class-path /path/to/postgresql-42.0.0.jar /path/to/vectortile-spark-process-1.0-SNAPSHOT.jar hdfs:///path/to/vectortile-spark-process.yml

## Configuration File

	---
	# vectortile-spark-process.yml

	appName: "Vector Tile Process"
	sequenceFileDir: "hdfs:///path/to"
	layers:
	  - layerName: "layerName"
	    minZoom: "0"
	    maxZoom: "22"
	    dataProvider: "org.ieee.codemeow.geometric.spark.data.SQLDataProvider"
	    kwargs:
	      url: "jdbc:postgresql://hostname/dbname"
	      dbtables:
	        planet_osm_line: "public.planet_osm_line"
	        planet_osm_point: "public.planet_osm_point"
	        planet_osm_polygon: "public.planet_osm_polygon"
	        planet_osm_roads: "public.planet_osm_roads"
	      user: "postgres"
	      password: "postgres"
	      zooms:
	        0: "SELECT osm_id AS __id__, ST_GeomFromWKB(way) AS __geometry__ FROM ..."
	        1: "SELECT osm_id AS __id__, ST_GeomFromWKB(way) AS __geometry__ FROM ..."
			...
	        22: "SELECT osm_id AS __id__, ST_GeomFromWKB(way) AS __geometry__ FROM ..."

## Resources

- http://tilestache.org/
- https://github.com/locationtech/geomesa
- http://www.geomesa.org/documentation/current/user/spark/sparksql_functions.html
- https://github.com/mapbox/awesome-vector-tiles
- https://github.com/DataSystemsLab/GeoSpark
- https://github.com/Esri/spatial-framework-for-hadoop
- https://github.com/gbif/maps
- https://github.com/mapbox/mercantile
- https://github.com/modestmaps/modestmaps-processing
- http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/

## Issues

Find a bug or want to request a new feature? Please let us know by submitting an issue.

## Tips

1. Upgrade protobuf package version on your Spark cluster  

	>cp protobuf-java-3.0.0-beta-2.jar $SPARK_HOME/jars  

2. Use SparkSQL in the ***zooms*** section of the configuration file

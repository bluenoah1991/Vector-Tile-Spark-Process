# Preview Example

Processing Beijing OpenStreetMap data and Preview it  

![Preview](preview.png)

## Download raw data and import to PostGIS database

1. Download [beijing_china.osm.pbf](https://s3.amazonaws.com/metro-extracts.mapzen.com/beijing_china.osm.pbf) from [Mapzen](https://mapzen.com/data/metro-extracts/metro/beijing_china/)  
2. Install Osm2pgsql to your enviroment ([http://wiki.openstreetmap.org/wiki/Osm2pgsql#Installation](http://wiki.openstreetmap.org/wiki/Osm2pgsql#Installation))  
3. Import data into PostgreSQL database

	$ osm2pgsql -c -d beijing\_china\_osm -U postgres -W -H [PGHOST] beijing_china.osm.pbf
  
4. Add column ***wkb_way***

	ALTER TABLE public.planet_osm_line ADD wkb_way bytea;  
	UPDATE public.planet_osm_line SET wkb_way = ST_AsBinary(way);  

## Update your configuration file

	---
	# preview-example.yml
	
	appName: "Preview Example Task"
	sequenceFileDir: "hdfs:///preview_data"
	layers:
	  - layerName: "lines"
	    minZoom: "3"
	    maxZoom: "18"
	    dataProvider: "org.ieee.codemeow.geometric.spark.data.SQLDataProvider"
	    kwargs:
	      url: "jdbc:postgresql://hostname/beijing_china_osm"
	      dbtables:
	        planet_osm_line: "public.planet_osm_line"
	      user: "postgres"
	      password: "postgres"
	      zooms:
	        3: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        4: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        5: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        6: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        7: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        8: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        9: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        10: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        11: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        12: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        13: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        14: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        15: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        16: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        17: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"
	        18: "SELECT osm_id AS __id__, ST_GeomFromWKB(wkb_way) AS __geometry__ FROM planet_osm_line"

## Run Spark Job

	$SPARK_HOME/bin/spark-submit --class org.ieee.codemeow.geometric.spark.VectorTileTask --master yarn --deploy-mode cluster --jars /path/to/postgresql-42.0.0.jar --driver-class-path /path/to/postgresql-42.0.0.jar /path/to/vectortile-spark-process-1.0-SNAPSHOT.jar hdfs:///path/to/preview-example.yml

## Run Simple Web Server

#### Install SBT scripts runtime ([http://www.scala-sbt.org/0.13/docs/Scripts.html](http://www.scala-sbt.org/0.13/docs/Scripts.html))  
	
	// Installing conscript  
	export CONSCRIPT_HOME="$HOME/.conscript"  
	export PATH=$CONSCRIPT_HOME/bin:$PATH  
	wget https://raw.githubusercontent.com/foundweekends/conscript/master/setup.sh -O - | sh
	
	//Install command screpl and scalas  
	cs sbt/sbt --branch 0.13  
	
#### Start server  

	./scripts/SimpleWebServer.sh hdfs://hostname:port/preview_data  

## Open preview.html  

1. Modify server address on ***preview-style.json***
2. Copy ***preview.html*** and ***preview-style.json*** to web root directory
3. Visit ***http://127.0.0.1/preivew.html***

 

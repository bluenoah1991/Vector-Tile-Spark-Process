#!/usr/bin/env scalas

/***
scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.tumblr" %% "colossus" % "0.8.3",
  "org.apache.hadoop" % "hadoop-common" % "2.2.0",
  "org.apache.hadoop" % "hadoop-hdfs" % "2.2.0"
)
*/

import java.net.URI

import colossus._
import core._
import service._
import protocols.http._
import HttpMethod._
import akka.actor.ActorSystem
import colossus.protocols.http.UrlParsing._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io._
import org.apache.hadoop.util.ReflectionUtils


// load all tiles into memory (so slow)
val cache = scala.collection.mutable.Map[String, Array[Byte]]()

val config = new Configuration
config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");

// Ref http://alvinalexander.com/scala/scala-shell-script-command-line-arguments-args
val uri = URI.create(args(0))
val fs = FileSystem.get(uri, config)
val statusCollection = fs.listStatus(new Path(uri.getPath))


println("Starting load all SequenceFile...")

statusCollection.filter(status => {
  // filter _SUCCESS
  status.getPath.getName != "_SUCCESS"
}).foreach(status => {
  val path = status.getPath
  println(s"load ${path}")

  val reader = new SequenceFile.Reader(config, SequenceFile.Reader.file(path))
  val key = ReflectionUtils.newInstance(reader.getKeyClass, config).asInstanceOf[Text]
  val value = ReflectionUtils.newInstance(reader.getValueClass, config).asInstanceOf[BytesWritable]

  while(reader.next(key, value)){
    val code = key.toString()
    // Ref http://stackoverflow.com/questions/23211493/how-to-extract-data-from-hadoop-sequence-file
    val bytes = value.copyBytes()
    cache(code) = bytes
  }
})

println("Finished loading.")

// Take samples

val samples = cache.take(5) ++ cache.takeRight(5)

println("Take samples of Tiles")
samples.zipWithIndex.foreach(t => {
  val tile = t._1
  val index = t._2
  val (layerName, (row, column, zoom)) = decodeTile(tile._1)
  println(s"${index}: Layer is ${layerName} and Row is ${row} and Column is ${column} and Zoom is ${zoom}")
})

def encodeTile(layerName: String, tile: (Long, Long, Long)): String ={
  val (row, column, zoom) = tile
  val code = (row % 0x1000000L) << 40 | (column % 0x1000000L) << 16 | (zoom % 0x10000L)
  s"${layerName}:${code}"
}

def decodeTile(key: String): (String, (Long, Long, Long)) ={
  val x = key.split(":").toSeq
  val layerName = x(0)
  val code = x(1).toLong
  val row = code >> 40
  val column = (code >> 16) % 0x1000000L
  val zoom = code % 0x10000L
  (layerName, (row, column, zoom))
}

class ArrayByteEncoder extends HttpBodyEncoder[Array[Byte]]{
  def encode(data : Array[Byte]) : HttpBody ={

    val contentType = HttpHeader("Content-Type", "application/vnd.mapbox-vector-tile")
    new HttpBody(data, Some(contentType))
  }
}

class WMSService(context: ServerContext) extends HttpService(context) {

  val allowOrigin = HttpHeader("Access-Control-Allow-Origin", "*")
  val allowMethod = HttpHeader("Access-Control-Allow-Method", "GET")
  val crossDomainHeaders = HttpHeaders(allowOrigin, allowMethod)

  def handle = {
    case request @ Get on Root => {
      Callback.successful(request.ok("Simple Web Map Server!"))
    }

    case request @ Options on Root / layerName / Long(zoom) / Long(row) / fileName => {

      Callback.successful(request.ok("", crossDomainHeaders))
    }

    case request @ Get on Root / layerName / Long(zoom) / Long(row) / fileName => {

      val index = fileName.indexOf(".pbf")
      if(index == -1){
        Callback.successful(request.badRequest[String]("Wrong path format"))
      } else {
        val column = fileName.substring(0, index).toLong
        val code = encodeTile(layerName,(row, column, zoom))

        val bytes = cache.get(code)
        if(bytes.isEmpty){
          Callback.successful(request.notFound[String](s"The requested tile parameters is (${row}, ${column}, ${zoom})"))
        } else {
          implicit val encoder = new ArrayByteEncoder
          Callback.successful(request.ok(bytes.get, crossDomainHeaders))
        }
      }

    }
  }
}

class WMSInitializer(worker: WorkerRef) extends Initializer(worker) {

  def onConnect = context => new WMSService(context)

}

implicit val actorSystem = ActorSystem()
implicit val io = IOSystem()

Server.start("wms", 9000){ worker => new WMSInitializer(worker) }

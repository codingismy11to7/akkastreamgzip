package gziptest

import java.io.FileOutputStream
import java.util.zip.{Deflater, GZIPOutputStream}

import ammonite.ops._

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.util.control.Exception.ultimately

/**
  * @author steven
  *
  */
object Test extends App {
  def await[T](f: => Future[T]) = Await.result(f, Duration.Inf)

  implicit val system: ActorSystem = ActorSystem("Test")
  import system.dispatcher
  implicit val mat: Materializer = ActorMaterializer()

  val runCount = 3
  val lineCount = 1500000

  val inputFile = tmp(prefix = "sample.", suffix = ".txt")


  ultimately({
    system.terminate()
    rm! inputFile
  }) {

    def fileSize(file: Path) = f"${stat(file).size / 1024.0 / 1024.0}%.01f MB"

    {
      println("Generating input data")

      val sampleMap = read.lines(resource / "sampledata.txt").map(jsonStr => {
        val json = io.circe.parser.parse(jsonStr).fold(throw _, identity)
        val msgType = json.asObject.flatMap(_.toMap.keys.headOption).getOrElse(sys.error("error"))
        msgType -> jsonStr
      }).toMap

      val statsMap = read.lines(resource / "msgstats.csv").map(line => {
        val Array(msgType, pct) = line.split(',')
        msgType -> pct.toDouble
      }).toMap

      def lineForRand(randVal: Double) = {
        @tailrec
        def loop(accPct: Double, curr: (String, Double), remaining: Map[String, Double]): String = {
          if (remaining.isEmpty) sampleMap(curr._1) else {
            val newAcc = accPct + curr._2
            if (randVal <= newAcc)
              sampleMap(curr._1)
            else
              loop(newAcc, remaining.head, remaining.tail)
          }
        }

        loop(0, statsMap.head, statsMap.tail)
      }

      await(Source(0 until lineCount).map(_ => ByteString(lineForRand(Random.nextDouble()) + '\n'))
        .runWith(FileIO.toPath(inputFile.toNIO)))

      println(s"Wrote ${fileSize(inputFile)} to $inputFile")
    }

    val lineSource = FileIO.fromPath(inputFile.toNIO).via(Framing.delimiter(ByteString("\n"), Int.MaxValue))

    def time[T](f: => Future[T]) = {
      val start = System.nanoTime()
      f andThen {
        case _ =>
          val elapsed = (System.nanoTime() - start).nanos.toSeconds.seconds
          println(s"Took $elapsed")
      }
    }

    def compressionGzip(): Unit = {
      def doCompress(level: Int): Unit = {
        val output = tmp()
        ultimately(rm! output) {
          await {
            time(lineSource.via(Compression.gzip(level)).runWith(FileIO.toPath(output.toNIO)))
          }

          println(s"File is ${fileSize(output)}")
          println("")
        }
      }

      println("* Writing file with Compression.gzip, level=fastest")
      doCompress(Deflater.BEST_SPEED)
      println("* Writing file with Compression.gzip, level=5")
      doCompress(5)
      println("* Writing file with Compression.gzip, level=best")
      doCompress(Deflater.BEST_COMPRESSION)
    }

    def gzipOutputStream(): Unit = {
      val output = tmp()

      println("* Writing file with GZIPOutputStream")

      ultimately(rm! output) {
        val gzos = new GZIPOutputStream(new FileOutputStream(output.toIO))
        await {
          time(lineSource.runWith(StreamConverters.fromOutputStream(() => gzos)))
        }

        println(s"File is ${fileSize(output)}")
        println("")
      }
    }

    for (i <- 1 to runCount) {
      println(s"************")
      println(s"** Run #$i **")
      println(s"************")
      compressionGzip()
      gzipOutputStream()
    }
  }
}

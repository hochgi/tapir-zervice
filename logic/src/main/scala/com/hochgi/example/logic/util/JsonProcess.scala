package com.hochgi.example.logic.util

import org.apache.commons.io.IOUtils

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit.SECONDS
import zio.{Queue => _, _}
import zio.stm.TRef
import zio.process._
import zio.json._
import ast._
import zio.stream._
import scala.collection.immutable.Queue

object JsonProcess {

  final case class Config(bufferSize: Int, slidingWindow: Duration, updateTick: Duration)
  object Config {
    lazy val config: zio.Config[Config] = {

      val validateDuration: Duration => Boolean = d =>
        d.toNanos % 1000000000 == 0 && !d.isZero && !d.isNegative

      val validateDurationMsg: String =
        "Must be defined in terms of strictly positive whole seconds"

      zio.Config.int("copy-buffer-size").withDefault(default.bufferSize) ++
      zio.Config.duration("sliding-window")
        .validate(validateDurationMsg)(validateDuration)
        .withDefault(default.slidingWindow) ++
      zio.Config.duration("update-tick")
        .validate(validateDurationMsg)(validateDuration)
        .withDefault(default.updateTick)
    }.nested("hochgi", "example", "logic", "json-process").map { case (bufferSize, slidingWindow, updateTick) =>
      default.copy(
        bufferSize    = bufferSize,
        slidingWindow = slidingWindow,
        updateTick    = updateTick)
    }

    lazy val default: Config = Config(4096, Duration.fromSeconds(20), Duration.fromSeconds(2))
  }

  val live: ZLayer[Any, Throwable, TRef[WordCountState]] = ZLayer.scoped {
    for {
      conf <- ZIO.config[Config](Config.config)
      file <- ZIO.attempt {
        val inputStream = getClass.getResourceAsStream("/blackbox.amd64")
        val tempPath = Files.createTempFile("blackbox", ".amd64")
        val tempFile = tempPath.toFile
        try {
          val outputStream = Files.newOutputStream(tempPath)
          IOUtils.copy(inputStream, outputStream, conf.bufferSize)
          inputStream.close()
          outputStream.close()
          tempFile.setExecutable(true)
          tempFile.deleteOnExit()
          tempFile
        } finally {
          inputStream.close()
        }
      }
      qref <- TRef.make(WordCountState(Map.empty, Queue.empty[WordEvent])).commit
      tref <- JsonProcess(conf, file, qref).getJsonStream
    } yield tref
  }
}
final case class JsonProcess private(config: JsonProcess.Config, executable: File, tRef: TRef[WordCountState]) {

  def getJsonStream: ZIO[Any, CommandError, TRef[WordCountState]] = {
    for {
      p <- Command(executable.getAbsolutePath).run
      _ <- processSdtoutStream(p.stdout).fork // TODO: allow interrupt fiber for graceful shutdown
    } yield tRef
  }

  private def getEither(jo: Json.Obj, field: String): Either[String, Json] =
    jo.get(field).toRight(s"No such field '$field'")

  private def unfailingWordsStream(ps: ProcessStream): ZStream[Any, Nothing, Event] =
    ps.stream
      .via(ZPipeline.fromChannel(ZPipeline.utf8Decode.channel.mapError(CommandError.IOError.apply)))
      .either
      .collect { case Right(s) => s } // ignore decoding failures
      .via(ZPipeline.splitLines)
      .map(_.fromJson[Json.Obj].flatMap { obj: Json.Obj =>
        for {
          jf <- getEither(obj, "event_type")
          sf <- jf.as[String]
          jw <- getEither(obj, "data")
          sw <- jw.as[String]
          jt <- getEither(obj, "timestamp")
          lt <- jt.as[Long]
        } yield WordEvent(sf, sw, lt)
      })
      .collect[Event] { case Right(we) => we } // ignore parsing failures

  private def processSdtoutStream(ps: ProcessStream): ZIO[Any, CommandError, Unit] = {
    val ticks = ZStream
      .tick(config.updateTick)
      .mapZIO[Any, Nothing, Event](_ => Clock.currentTime(SECONDS).map(Tick.apply))

    unfailingWordsStream(ps)
      .merge(ticks)
      .runForeach {
        case we: WordEvent => tRef.update(_.updateNew(we, we.timestamp - config.slidingWindow.toSeconds)).commit
        case Tick(seconds) => tRef.update(_.dropOld(seconds - config.slidingWindow.toSeconds)).commit
      }
  }
}
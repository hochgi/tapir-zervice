package com.hochgi.example.logic.util

import org.apache.commons.io.IOUtils

import java.io.{File, IOException}
import java.nio.file.Files
import java.util.concurrent.TimeUnit.SECONDS
import zio.{Queue => _, _}
import zio.process._
import zio.json._
import ast._
import zio.stream._

import scala.collection.immutable.Queue

object JsonProcess {

  final case class ExecutableFile(fileName: String, extension: String)

  final case class Config(bufferSize:    Int,
                          slidingWindow: Duration,
                          updateTick:    Duration,
                          executable:    ExecutableFile)
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
        .withDefault(default.updateTick) ++ (
        zio.Config.string("file-name").withDefault(default.executable.fileName) ++
        zio.Config.string("extension").withDefault(default.executable.extension)
      ).nested("executable").map(ExecutableFile.tupled)
    }.nested("hochgi", "example", "logic", "json-process").map { case (bufferSize, slidingWindow, updateTick, executableFile) =>
      default.copy(
        bufferSize    = bufferSize,
        slidingWindow = slidingWindow,
        updateTick    = updateTick,
        executable    = executableFile)
    }

    lazy val default: Config = Config(
      4096,
      Duration.fromSeconds(20),
      Duration.fromSeconds(2),
      ExecutableFile("blackbox", "macosx")
    )
  }

  case class RefWithUpdatingFiber(ref: Ref[WordCountState], fib: Fiber[CommandError, Unit])

  val live: ZLayer[Any, Throwable, RefWithUpdatingFiber] = ZLayer.scoped {
    for {
      conf <- ZIO.config[Config](Config.config)
      file <- ZIO.attemptBlocking {
        val inputStream = getClass.getResourceAsStream(s"/${conf.executable.fileName}.${conf.executable.extension}")
        val tempPath = Files.createTempFile(conf.executable.fileName, "." + conf.executable.extension)
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
      wcsRef <- Ref.make(WordCountState(Map.empty, Queue.empty[WordEvent]))
      refFib <- JsonProcess(conf, file, wcsRef).getJsonStream
    } yield RefWithUpdatingFiber.tupled(refFib)
  }
}
final case class JsonProcess private(config: JsonProcess.Config, executable: File, ref: Ref[WordCountState]) {

  def getJsonStream: ZIO[Any, CommandError, (Ref[WordCountState], Fiber[CommandError, Unit])] = {
    for {
      p <- Command(executable.getAbsolutePath).run
      f <- processSdtoutStream(p.stdout).fork
    } yield (ref, f)
  }

  private def getEither(jo: Json.Obj, field: String): Either[String, Json] =
    jo.get(field).toRight(s"No such field '$field'")

  private def unfailingWordsStream(ps: ProcessStream): ZStream[Any, Nothing, Event] =
    ps.linesStream
      .orElseFail(())
      .mapErrorCause(c => c.dieOption.fold(c)(exception => Cause.Fail((), StackTrace.none)))
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
        case we: WordEvent => ref.update(_.updateNew(we, we.timestamp - config.slidingWindow.toSeconds))
        case Tick(seconds) => ref.update(_.dropOld(seconds - config.slidingWindow.toSeconds))
      }
      .unrefine {
        case t: IOException => CommandError.IOError(t)
      }
  }
}
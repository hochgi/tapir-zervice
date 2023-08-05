package com.hochgi.example.logic.util

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.Queue

class WordCountStateTests extends AnyFunSpec with Matchers {

  describe ("WordCountState") {
    val emptyWcs = WordCountState(Map.empty, Queue.empty)
    describe ("empty") {
      it ("queried") {
        emptyWcs.grouped should be(empty)
        emptyWcs.queue should be(empty)
      }
      it ("benign drop") {
        val dropped = emptyWcs.dropOld(1000)
        dropped.grouped should be(empty)
        dropped.queue should be(empty)
      }
    }

    describe("first event") {
      val one = emptyWcs.updateNew(WordEvent("lang", "scala", 5000), 0)
      it ("queried") {
        one.grouped should have size 1
        one.queue should have size 1
        one.grouped("lang") should have size 1
        one.grouped("lang")("scala") should be(1)
      }

      it ("benign dropOld") {
        val stillOne = one.dropOld(1000)
        stillOne.grouped should have size 1
        stillOne.queue should have size 1
        stillOne.grouped("lang") should have size 1
        stillOne.grouped("lang")("scala") should be(1)
      }

      it ("real dropOld") {
        val zero = one.dropOld(6000)
        zero.grouped should be (empty)
        zero.queue should be (empty)
      }

      describe ("second event same field same word") {
        val two = one.updateNew(WordEvent("lang", "scala", 7000), 0)
        it("queried") {
          two.grouped should have size 1
          two.queue should have size 2
          two.grouped("lang") should have size 1
          two.grouped("lang")("scala") should be(2)
        }

        it("benign dropOld") {
          val stillTwo = two.dropOld(1000)
          stillTwo.grouped should have size 1
          stillTwo.queue should have size 2
          stillTwo.grouped("lang") should have size 1
          stillTwo.grouped("lang")("scala") should be(2)
        }

        it("real dropOld") {
          val oneAgain = two.dropOld(6000)
          oneAgain.grouped should have size 1
          oneAgain.queue should have size 1
          oneAgain.grouped("lang") should have size 1
          oneAgain.grouped("lang")("scala") should be(1)
        }
      }

      describe ("second event same field different word") {
        val two = one.updateNew(WordEvent("lang", "java", 7000), 0)
        it("queried") {
          two.grouped should have size 1
          two.queue should have size 2
          two.grouped("lang") should have size 2
          two.grouped("lang")("scala") should be(1)
          two.grouped("lang")("java") should be(1)
        }

        it("benign dropOld") {
          val stillTwo = two.dropOld(1000)
          stillTwo.grouped should have size 1
          stillTwo.queue should have size 2
          stillTwo.grouped("lang") should have size 2
          stillTwo.grouped("lang")("scala") should be(1)
          stillTwo.grouped("lang")("java") should be(1)
        }

        it("real dropOld") {
          val oneAgain = two.dropOld(6000)
          oneAgain.grouped should have size 1
          oneAgain.queue should have size 1
          oneAgain.grouped("lang") should have size 1
          oneAgain.grouped("lang")("java") should be(1)
        }
      }

      describe ("second event different field") {
        val two = one.updateNew(WordEvent("word", "go", 7000), 0)
        it("queried") {
          two.grouped should have size 2
          two.queue should have size 2
          two.grouped("lang") should have size 1
          two.grouped("word") should have size 1
          two.grouped("lang")("scala") should be(1)
          two.grouped("word")("go") should be(1)
        }

        it("benign dropOld") {
          val stillTwo = two.dropOld(1000)
          stillTwo.grouped should have size 2
          stillTwo.queue should have size 2
          stillTwo.grouped("lang") should have size 1
          stillTwo.grouped("word") should have size 1
          stillTwo.grouped("lang")("scala") should be(1)
          stillTwo.grouped("word")("go") should be(1)
        }

        it("real dropOld") {
          val oneAgain = two.dropOld(6000)
          oneAgain.grouped should have size 1
          oneAgain.queue should have size 1
          oneAgain.grouped("word") should have size 1
          oneAgain.grouped("word")("go") should be(1)
        }
      }

      describe ("second event different field with exact timestamp") {
        val two = one.updateNew(WordEvent("word", "go", 6000), 0)
        it("queried") {
          two.grouped should have size 2
          two.queue should have size 2
          two.grouped("lang") should have size 1
          two.grouped("word") should have size 1
          two.grouped("lang")("scala") should be(1)
          two.grouped("word")("go") should be(1)
        }

        it("benign dropOld") {
          val stillTwo = two.dropOld(1000)
          stillTwo.grouped should have size 2
          stillTwo.queue should have size 2
          stillTwo.grouped("lang") should have size 1
          stillTwo.grouped("word") should have size 1
          stillTwo.grouped("lang")("scala") should be(1)
          stillTwo.grouped("word")("go") should be(1)
        }

        it("real dropOld") {
          val oneAgain = two.dropOld(6000)
          oneAgain.grouped should have size 1
          oneAgain.queue should have size 1
          oneAgain.grouped("word") should have size 1
          oneAgain.grouped("word")("go") should be(1)
        }
      }
    }
  }
}

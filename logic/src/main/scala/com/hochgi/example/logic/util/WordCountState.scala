package com.hochgi.example.logic.util

import scala.collection.immutable.Queue

case class WordCountState(grouped: Map[String, Map[String, Int]], queue: Queue[WordEvent]) {
  def dropOld(oldestSecondAllowed: Long): WordCountState =
    queue.dequeueOption.fold(this) {
      case (WordEvent(field, word, timestamp), q) => if (timestamp >= oldestSecondAllowed) this else {
        WordCountState(grouped.updatedWith(field)(_.flatMap { wc =>
          val inner = wc.updatedWith(word)(_.flatMap {
            case 1 => None
            case i => Some(i - 1)
          })

          Option.when(inner.nonEmpty)(inner)
        }), q).dropOld(oldestSecondAllowed)
      }
    }

  def updateNew(we: WordEvent, oldestSecondAllowed: Long): WordCountState = copy (
    grouped = grouped.updated(
      we.field,
      grouped
        .get(we.field)
        .fold(Map(we.word -> 1))(_.updatedWith(we.word)(_.map(1.+).orElse(Some(1))))
    ),
    queue = queue.enqueue(we)
  ).dropOld(oldestSecondAllowed)
}

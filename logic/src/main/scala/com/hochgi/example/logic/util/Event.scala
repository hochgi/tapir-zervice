package com.hochgi.example.logic.util

sealed trait Event
final case class WordEvent(field: String, word: String, timestamp: Long) extends Event
final case class Tick(epochSeconds: Long) extends Event
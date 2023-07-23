package com.hochgi.example.logic.util

object Dummy {

  def foo(i: Int, j: Int): Int =
    if (i % 2 != j % 2) i - j
    else j - i
}

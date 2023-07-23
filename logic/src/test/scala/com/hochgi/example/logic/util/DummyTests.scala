package com.hochgi.example.logic.util

import com.hochgi.example.logic.util.Dummy.foo
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class DummyTests extends AnyFunSpec with Matchers {
  describe("foo is just -") {
    it("should answer 2-1") {
      foo(2,1) shouldEqual 1
    }
  }
}
package com.hochgi.example.datatypes

/**
 * inherit and prepend error variant on an endpoint
 * with a specific type to add a new documented error response.
 *
 * If there's no need to burden contract with response,
 * just use [[GeneralError]] instead a new subtype.
 */
trait ManagedError {
  def message: String
  def statusCode: Int
}

case class BadRequest(message: String) extends ManagedError {
  override def statusCode: Int = 400
}

case class NotFound(message: String) extends ManagedError {
  override def statusCode: Int = 404
}

case class ExpectationFailed(message: String) extends ManagedError {
  override def statusCode: Int = 417
}

case class ServiceUnavailable(message: String) extends ManagedError {
  override def statusCode: Int = 503
}

/**
 * ad-hoc (undocumented in contract) errors
 */
case class GeneralError(message: String, statusCode: Int) extends ManagedError
object GeneralError {

  def tupled(tup: (String, Int)): GeneralError = GeneralError(tup._1, tup._2)

  /**
    * Intended to be rendered as a JSON response,
    * such that GeneralError looks like any other type when JSONfied,
    * but still has the status code Int free variable.
    */
  final case class Msg(message: String)
}


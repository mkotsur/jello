package com.xebialabs.jello.domain.json

import com.typesafe.config.ConfigFactory
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.{Board, Column}
import spray.http.{BasicHttpCredentials, HttpRequest}
import spray.httpx.RequestBuilding._
import spray.httpx.SprayJsonSupport._
import spray.json.{JsString, JsValue, DefaultJsonProtocol}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

trait JiraProtocol extends DefaultJsonProtocol {

  private val conf = ConfigFactory.load()

  private val apiUri = conf.getString("jira.apiUri")

  private val credentials = BasicHttpCredentials(
    conf.getString("jira.username"), conf.getString("jira.password")
  )

  case class GetTicketReq(id: String)
  case class TicketResp(id: String, key: String, fields: Map[String, JsValue])

  implicit val TicketRespFormat = jsonFormat3(TicketResp)

  // Transformers from responses to domain objects
  implicit def ticketRespToBoard(trf: Future[TicketResp]): Future[Ticket] = trf.map(tr => Ticket(tr.key, tr.fields("summary").asInstanceOf[JsString].value))

  // Transformers from requests case classes into HTTP Request objects
  implicit def getTicketToHttpReq(gt: GetTicketReq): HttpRequest =
    Get(s"$apiUri/issue/${gt.id}?fields=summary&expand=names") ~> addCredentials(credentials)



}

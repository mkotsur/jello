package com.xebialabs.jello.domain.json

import com.typesafe.config.ConfigFactory
import com.xebialabs.jello.domain.Jira.Ticket
import spray.http.{BasicHttpCredentials, HttpRequest}
import spray.httpx.RequestBuilding._
import spray.httpx.SprayJsonSupport._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

trait JiraProtocol extends DefaultJsonProtocol {

  private val conf = ConfigFactory.load()

  private val apiUri = conf.getString("jira.apiUri")
  private val estimationFieldId = conf.getString("jira.estimationFieldId")

  private val credentials = BasicHttpCredentials(
    conf.getString("jira.username"), conf.getString("jira.password")
  )

  case class GetTicketReq(id: String)
  case class TicketResp(id: String, key: String, fields: Map[String, JsValue])
  case class GetRapidBoardReq(rapidBoardId: Int)
  case class RapidTicketResp(id: Int, key: String, summary: String)
  case class RapidBoardResp(issues: Seq[RapidTicketResp])

  case class EstimateUpdateReq(ticket: Ticket)

  implicit val TicketRespFormat = jsonFormat3(TicketResp)
  implicit val RapidTicketRespFormat = jsonFormat3(RapidTicketResp)
  implicit val RapidBoardRespFormat = jsonFormat1(RapidBoardResp)



  // Transformers from responses to domain objects

  private val r2t = (tr: TicketResp) => Ticket(tr.key, tr.fields("summary").asInstanceOf[JsString].value)

  private val rapidBoardTicketResp2Ticket = (rbtr: RapidTicketResp) => Ticket(rbtr.key.toString, rbtr.summary)

  implicit def ticketRespToTicket(trf: Future[TicketResp]): Future[Ticket] = trf.map(r2t)

  implicit def ticketRespSeqToTicketSeq(trf: Future[RapidBoardResp]): Future[Seq[Ticket]] = trf.map(_.issues.map(rapidBoardTicketResp2Ticket))

  // Transformers from requests case classes into HTTP Request objects
  implicit def getTicketToHttpReq(gt: GetTicketReq): HttpRequest =
    Get(s"$apiUri/api/latest/issue/${gt.id}?fields=summary&expand=names") ~> addCredentials(credentials)


  implicit def getRangeOfTicketsToHttpReq(r: GetRapidBoardReq): HttpRequest =
    Get(s"$apiUri/greenhopper/1.0/xboard/plan/backlog/data.json?rapidViewId=${r.rapidBoardId}") ~> addCredentials(credentials)

  implicit def estimateUpdateReqToHttpReq(r: EstimateUpdateReq): HttpRequest = {
    val estimation = r.ticket.estimation.map(JsNumber(_)).getOrElse(JsNull)
    Put(
      s"$apiUri/api/2/issue/${r.ticket.id}",
      JsObject(Map("fields" -> JsObject(Map(s"customfield_$estimationFieldId" -> estimation))))
    ) ~> addCredentials(credentials)
  }


}

package com.xebialabs.jello.domain.json

import com.xebialabs.jello.conf.ConfigAware
import com.xebialabs.jello.domain.Jira.Ticket
import spray.http.{BasicHttpCredentials, HttpRequest}
import spray.httpx.RequestBuilding._
import spray.httpx.SprayJsonSupport._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

trait JiraProtocol extends DefaultJsonProtocol { self: ConfigAware =>


  private val apiRoot = config.jira.apiUri

  private val apiLatestBase = s"$apiRoot/api/latest"

  private val estimationFieldId = config.jira.estimationFieldId

  private val credentials = BasicHttpCredentials(config.jira.username, config.jira.password)

  case class GetTicketReq(id: String)
  case class TicketResp(id: String, key: String, fields: Map[String, JsValue])
  case class GetRapidBoardReq(rapidBoardId: Int)
  case class RapidTicketResp(id: Int, key: String, summary: String)
  case class RapidBoardResp(issues: Seq[RapidTicketResp])

  case class EstimateUpdateReq(ticket: Ticket)

  case class SearchReq(jql: String, fields: Seq[String] = Seq("summary"))
  case class SearchResp(total: Int, issues: Seq[TicketResp])

  implicit val TicketRespFormat = jsonFormat3(TicketResp)
  implicit val RapidTicketRespFormat = jsonFormat3(RapidTicketResp)
  implicit val RapidBoardRespFormat = jsonFormat1(RapidBoardResp)

  implicit val SearchReqFormat = jsonFormat2(SearchReq)
  implicit val SearchRespFormat = jsonFormat2(SearchResp)


  // Transformers from responses to domain objects

  private val r2t = (tr: TicketResp) => Ticket(tr.key, tr.fields("summary").asInstanceOf[JsString].value)

  private val rapidBoardTicketResp2Ticket = (rbtr: RapidTicketResp) => Ticket(rbtr.key.toString, rbtr.summary)

  implicit def ticketRespToTicket(trf: Future[TicketResp]): Future[Ticket] = trf.map(r2t)

  implicit def ticketRespSeqToTicketSeq(trf: Future[RapidBoardResp]): Future[Seq[Ticket]] = trf.map(_.issues.map(rapidBoardTicketResp2Ticket))

  implicit def searchRespSeqToTicketSeq(sr: Future[SearchResp]): Future[Seq[Ticket]] = sr.map(_.issues.map(r2t))

  // Transformers from requests case classes into HTTP Request objects
  implicit def getTicketToHttpReq(gt: GetTicketReq): HttpRequest =
    Get(s"$apiLatestBase/issue/${gt.id}?fields=summary&expand=names") ~> addCredentials(credentials)


  implicit def getRangeOfTicketsToHttpReq(r: GetRapidBoardReq): HttpRequest =
    Get(s"$apiRoot/greenhopper/1.0/xboard/plan/backlog/data.json?rapidViewId=${r.rapidBoardId}") ~> addCredentials(credentials)

  implicit def estimateUpdateReqToHttpReq(r: EstimateUpdateReq): HttpRequest = {
    val estimation = r.ticket.estimation.map(JsNumber(_)).getOrElse(JsNull)
    Put(
      s"$apiLatestBase/issue/${r.ticket.id}",
      JsObject(Map("fields" -> JsObject(Map(s"customfield_$estimationFieldId" -> estimation))))
    ) ~> addCredentials(credentials)
  }

  implicit def myPermissionsToHttpReq(r: SearchReq): HttpRequest =
    Post(s"$apiLatestBase/search", r) ~> addCredentials(credentials)


}

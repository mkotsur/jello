package com.xebialabs.jello.domain.json

import com.xebialabs.jello.conf.ConfigAware
import com.xebialabs.jello.domain.Trello._
import spray.http.HttpRequest
import spray.httpx.RequestBuilding._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

trait TrelloProtocol extends DefaultJsonProtocol { this: ConfigAware =>


  private val appKey = config.trello.appKey

  private val token = config.trello.token

  private val apiUri = config.trello.apiUri

  /**
   * Request-response case classes and objects with their json formats
   */
  case class NewBoardReq(name: String, closed: Boolean = false, defaultLists: Boolean = false)
  implicit val NewBoardReqFormat = jsonFormat3(NewBoardReq)
  
  case class BoardResp(id: String, name: String, url: String, shortUrl: String)
  implicit val BoardRespFormat = jsonFormat4(BoardResp)
  
  case class CloseBoardReq(boardId: String, value: Boolean = true)

  case class GetLabelsReq(boardId: String)

  implicit val LabelFormat = jsonFormat2(Label)

  case class LabelReq(id: String)
  implicit val LabelReqFormat = jsonFormat1(LabelReq)

  case class UpdateLabelReq(cardId: String, labelId: String)

  case class ColumnReq(name: String, pos: Int = 0)
  implicit val NewListReqFormat = jsonFormat2(ColumnReq)

  case class NewColumnResp(id: String, name: String)
  implicit val NewColumnRespFormat = jsonFormat2(NewColumnResp)
  
  case class NewCardReq(name: String, idList: String)
  implicit val NewCardReqFormat = jsonFormat2(NewCardReq)

  case class ColumnsReq(boardId: String)

  case class ListBoardsReq(member: String)

  case class DeleteBoardReq(boardId: String, member: String)

  /**
   * Directly for domain objects
   */
  implicit val CardFormat = jsonFormat2(Card)
  implicit val ColumnFormat = jsonFormat3(Column)
  implicit val TokenPermissionRespFormat = jsonFormat4(TokenPermission)
  implicit val TokenInfoRespFormat = jsonFormat4(TokenInfo)


  /**
   * Transformers from response objects (defined above) into domain objects
   */

  implicit def boardRespToBoard(b: Future[BoardResp]): Future[Board] = b.map(bb => Board(bb.id, bb.name, bb.shortUrl))

  implicit def seqBoardRespToSeqBoard(b: Future[Seq[BoardResp]]): Future[Seq[Board]] = b.map(bb => bb.map(b => Board(b.id, b.name, b.shortUrl)))

  implicit def columnRespToColumn(b: Future[NewColumnResp]): Future[Column] = b.map(bb => Column(bb.id, bb.name))


  /**
   * Transformers from requests case classes into HTTP Request objects
   */

  implicit def newBoardReqToHttpReq(b: NewBoardReq): HttpRequest =
    Post(s"$apiUri/boards?key=$appKey&token=$token", b)

  implicit def closeBoardReqToHttpReq(req: CloseBoardReq): HttpRequest =
    Put(s"$apiUri/boards/${req.boardId}/closed?key=$appKey&token=$token", Map[String, Boolean]("value" -> req.value))

  implicit def newCardReqToHttpReq(nc: NewCardReq): HttpRequest = Post(s"$apiUri/cards?key=$appKey&token=$token", nc)

  implicit def columnReqToHttpReq(bc:(String, ColumnReq)): HttpRequest =
    Post(s"$apiUri/boards/${bc._1}/lists?key=$appKey&token=$token", bc._2)

  implicit def listsReqToHttpReq(listsReq: ColumnsReq): HttpRequest =
    Get(s"$apiUri/boards/${listsReq.boardId}/lists?cards=open&card_fields=name&fields=name&key=$appKey&token=$token")

  implicit def getLabelsReqToHttpReq(labelsReq: GetLabelsReq): HttpRequest =
    Get(s"$apiUri/boards/${labelsReq.boardId}/labels?key=$appKey&token=$token")

  implicit def updateLabelsReqToHttpReq(ulr: UpdateLabelReq): HttpRequest =
    Post(s"$apiUri/cards/${ulr.cardId}/idLabels?key=$appKey&token=$token", Map("value" -> ulr.labelId))

  implicit def listBoardsReqToHttpReq(req: ListBoardsReq): HttpRequest =
    Get(s"$apiUri/members/${req.member}/boards?key=$appKey&token=$token")

  implicit def deleteBoardReqToHttpReq(req: DeleteBoardReq): HttpRequest =
    Delete(s"$apiUri/boards/${req.boardId}/members/${req.member}?key=$appKey&token=$token")

  val TokenInfoReq: HttpRequest = Get(s"$apiUri/tokens/$token?key=$appKey")

}

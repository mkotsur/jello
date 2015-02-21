package com.xebialabs.jello.domain.json

import com.typesafe.config.ConfigFactory
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.Trello.{Board, Column}
import spray.http.HttpRequest
import spray.httpx.RequestBuilding._
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

trait TrelloProtocol extends DefaultJsonProtocol {

  private val conf = ConfigFactory.load()

  private val appKey = conf.getString("trello.appKey")

  private val token = conf.getString("trello.appToken")

  private val apiUri = conf.getString("trello.apiUri")


  case class NewBoardReq(name: String, closed: Boolean = false, defaultLists: Boolean = false)
  case class BoardResp(id: String, name: String, url: String, shortUrl: String)
  case class CloseBoardReq(value: Boolean = true)
  case class ColumnReq(name: String, pos: Int = 0)
  case class NewColumnResp(id: String, name: String)
  case class NewCard(name: String, idList: String)
  case class NewCardResp(id: String)
  case class ListsReq(boardId: String)

  case class ListsItemResp(id: String, name: String, cards: Seq[ListsItemCardResp])
  case class ListsItemCardResp(id: String, name: String)

  implicit val NewBoardReqFormat = jsonFormat3(NewBoardReq)
  implicit val CloseBoardReqFormat = jsonFormat1(CloseBoardReq)
  implicit val NewBoardRespFormat = jsonFormat4(BoardResp)
  implicit val NewListReqFormat = jsonFormat2(ColumnReq)
  implicit val NewListRespFormat = jsonFormat2(NewColumnResp)
  implicit val NewCardReqFormat = jsonFormat2(NewCard)
  implicit val NewCardRespFormat = jsonFormat1(NewCardResp)

  implicit val ListsItemCardRespFormat = jsonFormat2(ListsItemCardResp)
  implicit val ListsItemRespFormat = jsonFormat3(ListsItemResp)


  // Transformers from responses to domain objects
  implicit def boardRespToBoard(b: Future[BoardResp]): Future[Board] = b.map(bb => Board(bb.id, bb.shortUrl))
  implicit def columnRespToColumn(b: Future[NewColumnResp]): Future[Column] = b.map(bb => Column(bb.id, bb.name))

  implicit def listsItemRespToTickets(list: ListsItemResp): Seq[Ticket] = {
    val estimation = Some(list.name.toInt)
    list.cards.map {
      card => Ticket(
        card.name.split(" ").head,
        card.name.split(" ").tail.mkString(" "),
        estimation
      )
    }
  }

  // Transformers from requests case classes into HTTP Request objects
  implicit def newBoardReqToHttpReq(b: NewBoardReq): HttpRequest =
    Post(s"$apiUri/boards?key=$appKey&token=$token", b)

  implicit def closeBoardReqToHttpReq(idAndReq: (String, CloseBoardReq)): HttpRequest =
    Put(s"$apiUri/boards/${idAndReq._1}/closed?key=$appKey&token=$token", idAndReq._2
    )

  implicit def newCardReqToHttpReq(nc: NewCard): HttpRequest = Post(s"$apiUri/cards?key=$appKey&token=$token", nc)

  implicit def columnReqToHttpReq(bc:(String, ColumnReq)): HttpRequest =
    Post(s"$apiUri/boards/${bc._1}/lists?key=$appKey&token=$token", bc._2)

  implicit def listsReqToHttpReq(listsReq: ListsReq): HttpRequest =
    Get(s"$apiUri/boards/${listsReq.boardId}/lists?cards=open&card_fields=name&fields=name&key=$appKey&token=$token")

}

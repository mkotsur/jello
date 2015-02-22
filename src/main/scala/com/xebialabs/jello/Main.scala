package com.xebialabs.jello

import com.xebialabs.jello.domain.Trello.{Card, Board}
import com.xebialabs.jello.domain.{Jira, Trello}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.io.StdIn
import scala.concurrent.duration._

object Main extends App {

  val jira = new Jira()
  val trello = new Trello()
  val jello = new Jello(jira, trello)


  println("Enter board id:")
  val rapidBoardId = StdIn.readLine().trim.toInt

  println("Enter ticket to start with:")
  val rangeBegin = StdIn.readLine()

  println("Enter ticket to end with:")
  val rangeEnd = StdIn.readLine()

  jira.getRapidBoardTickets(rapidBoardId, rangeBegin, rangeEnd).thenPrint().flatMap {
    tickets => jello.prepareForEstimation(tickets.map(_.id)).thenPrint()
  } flatMap { board =>
    Future {
      println(s"Please do the estimations at: ${board.shortUrl} and press enter when you're done.")
      StdIn.readLine()
    } flatMap { line =>
      jello.saveEstimationsFrom(board)
    } flatMap {nothing =>
      trello.archiveBoard(board.id)
    }
  }


}

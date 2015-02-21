package com.xebialabs.jello

import com.xebialabs.jello.domain.{Jira, Trello}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn


object Main extends App {

  val jira = new Jira()
  val trello = new Trello()
  val jello = new Jello(jira, trello)

  println("Enter ticket to start with:")
  val rangeBegin = StdIn.readLine()

  println("Enter ticket to end with:")
  val rangeEnd = StdIn.readLine()

  jira.getRapidBoardTickets(4, rangeBegin, rangeEnd).thenPrint().flatMap {
    tickets => jello.prepareForEstimation(tickets.map(_.id)).thenPrint()
  } flatMap { board =>
    Future {
      println(s"Please do the estimations at: ${board.shortUrl} and press enter when you're done.")
      StdIn.readLine()
    } flatMap { line =>
      jello.saveEstimationsFrom(board.id)
    } flatMap {nothing =>
      trello.archiveBoard(board.id)
    }
  }



  //
  //  printFuture(new Trello().createBoard().flatMap {
  //    case b =>
  //      println("Creating ", b)
  //      b.putTickets(Seq(Ticket("D-111", "Hello"), Ticket("D-222", "Hello333")))
  //  })



}

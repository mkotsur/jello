package com.xebialabs.jello.domain.tickets

import com.xebialabs.jello.domain.Jira
import com.xebialabs.jello.domain.Jira.Ticket
import com.xebialabs.jello.domain.tickets.RangeConverter.{TicketId, TicketRange, TicketsParser}
import org.parboiled2._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object RangeConverter {

  case class TicketId(id: String)
  case class TicketRange(boardId: Int, beginWith: String, endWith: String)

  class TicketsParser(val input: ParserInput) extends Parser {

    def InputLine = rule { Tokens ~ EOI }

    def Digits      = rule { oneOrMore(CharPredicate.Digit) }
    def Separator   = rule { zeroOrMore(' ') ~ ',' ~ zeroOrMore(' ')}.asInstanceOf[Rule0]
    def StoryId     = rule { capture(oneOrMore(CharPredicate.UpperAlpha) ~ '-' ~ Digits)}
    def TicketIdObj = rule { capture(oneOrMore(CharPredicate.UpperAlpha) ~ '-' ~ Digits) ~> TicketId}
    def StoryIds    = rule { oneOrMore(StoryId).separatedBy(Separator) ~> ((ids: Seq[String]) => {
      ids.map(TicketId)
    })}

    def Range       = rule {
      capture(Digits) ~> (_.toInt) ~ "@" ~ StoryId ~ ".." ~ StoryId ~> TicketRange
    }

    def Token       = rule { Range | TicketIdObj }
    def Tokens      = rule { oneOrMore(Token).separatedBy(Separator) }

  }

}

trait RangeConverter {

  def inputToTickets(input: String)(implicit jira: Jira): Future[Seq[Ticket]] = {

    val parser = new TicketsParser(input)

    parser.InputLine.run() match {
      case Success(ids) =>
        ids.foldLeft(Future(Seq[Ticket]()))((acc: Future[Seq[Ticket]], item: AnyRef) => item match {
          case TicketId(id) =>
            val ticketFuture = jira.getTicket(id) recover {
              case cause => throw new RuntimeException(s"Ticket $id could not be found", cause)
            }
            acc.flatMap { seq =>  ticketFuture.map( t => seq :+ t)}
          case TicketRange(boardId, begin, end) =>
            acc.flatMap( seq => jira.getRapidBoardTickets(boardId, begin, end).map( tt => seq ++ tt))
        })

      case Failure(ex: ParseError) =>
        Future.failed(new IllegalArgumentException(parser.formatError(ex)))

      case Failure(ex) =>
        Future.failed(new IllegalArgumentException("Could not parse input", ex))
    }
  }


}

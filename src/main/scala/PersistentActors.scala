import java.util.Date

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor

object PersistentActors extends App {

  /**
   * Scenario:we have a business and an accountant which keeps track of our invoices.
   */
  //Command
  case class Invoice(recipient: String, date: Date, amount: Int)
  //Event
  case class InvoiceRecorded(id: Int, recipient: String, date: Date, amount: Int)

  class Accountant extends PersistentActor with ActorLogging {

    var latestInvoiceId = 0 //maintained by accountant
    var totalAmount = 0

    override def persistenceId: String = "simple-accountant" //best practice is to make it unique

    override def receiveCommand: Receive = {

      case Invoice(recipient, data, amount) =>

      /**
       * When you receive a command:
       * 1. you create an event to persist into the store
       * 2. you persist the event, the pass in a callback that will get triggered once the event is written.
       * 3. we update the actor's state when the event has persisted.
       */
        log.info(s"Received invoice for amount: $amount")

        val event = InvoiceRecorded(latestInvoiceId, recipient, data, amount) //sent to journal
        persist(event) /**time gap: all other messages send to this actor are stashed**/{ e =>
          //safe to access mutable state here, taken care of akka-persistence, it makes sure no other thread access actor during a callback
          //update state, log something or send messages to other actors....
          latestInvoiceId += 1
          totalAmount += amount
              //we can correctly identify the sender of the command
          //sender () ! "PersistenceACK"
          log.info(s"Persisted $e as invoice #${e.id}, for total amount $totalAmount")
        }
        //we are not obliged to persist, we can act like a normal actor. i.e
      case "print" => log.info(s"latest invoice id: $latestInvoiceId, total amount: $totalAmount")
    }
    //called when actor starts or restarted as part of Supervised strategy.
    override def receiveRecover: Receive = {
     //follow the logic in the persist steps of receiveCommand
      case InvoiceRecorded(id, _, _, amount) =>
        latestInvoiceId = id
        totalAmount += amount
        log.info(s"Recovered invoice #$id for amount #$amount, total amount: $totalAmount")
    }
  }

  val system = ActorSystem("PersistentActors")
  val accountant = system.actorOf(Props[Accountant], "SimpleAccountant")
/*  for(i <- 1 to 4) {
    accountant ! Invoice("the sofa company", new Date(), i * 1000)
  }*/
}
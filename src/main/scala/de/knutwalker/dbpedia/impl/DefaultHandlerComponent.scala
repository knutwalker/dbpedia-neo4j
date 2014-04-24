package de.knutwalker.dbpedia.impl

import de.knutwalker.dbpedia.components.{ SettingsComponent, ParserComponent, MetricsComponent, GraphComponent, HandlerComponent }
import org.neo4j.graphdb.{ DynamicRelationshipType, RelationshipType }
import org.semanticweb.yars.nx.namespace.{ SKOS, RDFS, RDF }
import org.semanticweb.yars.nx.{ Literal, Node, Resource, BNode }
import scala.collection.{Seq => GSeq}
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.util.{ Success, Failure, Try }

trait DefaultHandlerComponent extends HandlerComponent {
  this: GraphComponent with MetricsComponent with SettingsComponent ⇒

  import ParserComponent._

  val handler: Handler = new DefaultHandler

  private final class DefaultHandler extends Handler {

    private val PREFLABEL = new Resource(SKOS.NS + "prefLabel")

    private val rels = new mutable.AnyRefMap[String, RelationshipType](100)

    private def relTypeFor(name: String): RelationshipType = {
      rels.getOrElseUpdate(name, DynamicRelationshipType.withName(name))
    }

    def isLabel(p: Node) = p == RDFS.LABEL || p == PREFLABEL

    private def handleSubject(subject: Resource, labels: Seq[String], values: Seq[String]): NodeType = {
      graph.getAndUpdateResource(subject.toString, values, Labels.resource :: Nil, labels)
    }

    private def handleResource(resource: Resource): NodeType = {
      graph.getOrCreateResource(resource.toString, Nil, Labels.resource :: Nil, Nil)
    }

    private def createBNode(subject: BNode): NodeType = {
      graph.getOrCreateBNode(subject.toString, Labels.bNode :: Nil)
    }

    private def handleBNode(subject: BNode): NodeType = {
      graph.getOrCreateBNode(subject.toString, Labels.bNode :: Nil)
    }

    private def handleSubject(subject: Node, labels: Seq[String], values: Seq[String]): NodeType = subject match {
      case x: BNode    ⇒ createBNode(x)
      case x: Resource ⇒ handleSubject(x, labels, values)
    }

    private def handleObject(obj: Literal): NodeType = {
      val value = obj.toString
      val tpe = Option(obj.getDatatype).toList.map(_.toString)

      graph.createLiteral(value, tpe)
    }

    private def handleObject(obj: Node): NodeType = obj match {
      case x: Literal  ⇒ handleObject(x)
      case x: BNode    ⇒ handleBNode(x)
      case x: Resource ⇒ handleResource(x)
    }

    private def handlePredicate(subj: NodeType, obj: NodeType, pred: String): Unit = {
      val relType = relTypeFor(pred)
      graph.createRel(subj, obj, pred, relType)
    }

    private def handleStatement(subjectNode: NodeType, nodes: Statement): Unit = {
      val predicate = nodes(1)
      val obj = nodes(2)

      val predicateName = predicate.toString
      val objNode = handleObject(obj)

      handlePredicate(subjectNode, objNode, predicateName)
    }

    private def handleStatements(subject: Node, labels: Seq[String], values: Seq[String], nodes: Statements): Unit = {
      val subjectNode = handleSubject(subject, labels, values)
      nodes.foreach(handleStatement(subjectNode, _))
    }

    def handleStatements(subject: Node, nodes: Statements): Unit = metrics.time("subject-nodes") {
      val (rdfTypes, allStatements) = nodes.partition(_(1) == RDF.TYPE)
      val (labels, statements) = allStatements.partition(n ⇒ isLabel(n(1)))

      val rdfLabels = rdfTypes.map(_(2).toString)
      val schemaLabels = labels.map(_(2).toString)

      handleStatements(subject, rdfLabels, schemaLabels, statements)

      metrics.tripleAdded(nodes.length)

      graph.subjectAdded()
    }

    private def handleStatements(nodes: Statements): Unit = {
      val subject = nodes.head(0)
      handleStatements(subject, nodes)
    }

    private def handleBatch(batch: GSeq[Statements]): Try[Unit] = metrics.time("graph-tx") {
      graph.withinTx {
        batch.foreach(handleStatements)
      }
    }

    def apply(nodesBatches: Iterator[GSeq[Statements]]): Unit = {
      val it = nodesBatches
      while (it.hasNext) {
        handleBatch(it.next()) match {
          case Failure(e) ⇒ e.printStackTrace()
          case Success(x) ⇒ // metrics.report()
        }
      }
    }

    def shutdown(): Unit = {
      metrics.time("shutdown") {
        Try(graph.shutdown())
      }
    }
  }
}

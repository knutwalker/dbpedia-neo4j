package de.knutwalker.dbpedia.impl

import de.knutwalker.dbpedia.importer.{ SettingsComponent, MetricsComponent, GraphComponent, HandlerComponent }
import de.knutwalker.dbpedia.{ Literal, BNode, Statement, Node, Resource }
import scala.collection.mutable
import scala.util.Try

trait DefaultHandlerComponent extends HandlerComponent {
  this: GraphComponent with MetricsComponent with SettingsComponent ⇒

  val handler: Handler = new DefaultHandler

  private final class DefaultHandler extends Handler {

    private val RdfType = Resource("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
    private val PrefLabel = Resource("http://www.w3.org/2004/02/skos/core#prefLabel")
    private val RdfsLabel = Resource("http://www.w3.org/2000/01/rdf-schema#label")

    private val rels = new mutable.AnyRefMap[String, RelType](100)

    private def relTypeFor(name: String): RelType = {
      rels.getOrElseUpdate(name, graph.createRelTypeFor(name))
    }

    def isLabel(p: Node) = p == RdfsLabel || p == PrefLabel

    private def handleSubject(subject: Resource, labels: List[String], value: Option[String]): NodeType = {
      graph.getAndUpdateResource(subject.toString, value, Labels.resource :: labels)
    }

    private def handleResource(resource: Resource): NodeType = {
      graph.getOrCreateResource(resource.toString, None, Labels.resource :: Nil)
    }

    private def createBNode(subject: BNode): NodeType = {
      graph.getOrCreateBNode(subject.toString, Labels.bNode :: Nil)
    }

    private def handleBNode(subject: BNode): NodeType = {
      graph.getOrCreateBNode(subject.toString, Labels.bNode :: Nil)
    }

    private def handleSubject(subject: Node, labels: List[String], value: Option[String]): NodeType = subject match {
      case x: BNode    ⇒ createBNode(x)
      case x: Resource ⇒ handleSubject(x, labels, value)
    }

    private def handleObject(obj: Literal): NodeType = {
      val value = obj.toString
      val tpe = obj.dt.toList.map(_.toString)

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
      val predicate = nodes.p
      val obj = nodes.o

      val predicateName = predicate.toString
      val objNode = handleObject(obj)

      handlePredicate(subjectNode, objNode, predicateName)
    }

    private def handleStatements(subject: Node, labels: List[String], value: Option[String], nodes: List[Statement]) = {
      val subjectNode = handleSubject(subject, labels, value)
      nodes.foreach(handleStatement(subjectNode, _))
    }

    def apply(subject: Node, nodes: List[Statement]): Unit = metrics.time("subject-nodes") {
      val (rdfTypes, allStatements) = nodes.partition(_.p == RdfType)
      val (labels, statements) = allStatements.partition(n ⇒ isLabel(n.p))

      val rdfLabels = rdfTypes.map(_.o.toString)
      val schemaLabels = labels.map(_.o.toString)

      handleStatements(subject, rdfLabels, schemaLabels.headOption, statements)

      metrics.tripleAdded(nodes.length)

      graph.subjectAdded()
    }

    def shutdown(): Unit = {
      metrics.time("shutdown") {
        Try(graph.shutdown())
      }
    }
  }

}

package de.knutwalker.dbpedia.components

import org.neo4j.graphdb.{ DynamicLabel, Label, RelationshipType }
import scala.util.Try

trait GraphComponent {
  this: SettingsComponent with MetricsComponent ⇒

  type NodeType

  def graph: Graph

  object Labels {
    val subj: Label = DynamicLabel.label("Subject")
    val pred: Label = DynamicLabel.label("Predicate")
    val obj: Label = DynamicLabel.label("Object")
    val bNode: Label = DynamicLabel.label("BNode")
    val resource: Label = DynamicLabel.label("Resource")
    val literal: Label = DynamicLabel.label("Literal")
    val bNodeType: Label = DynamicLabel.label("BNodeType")
    val resourceType: Label = DynamicLabel.label("ResourceType")
  }

  object Properties {
    val uri = "uri"
    val value = "value"
    val nodeType = "nodeType"
    val dataType = "dataType"
  }

  trait Graph {

    def DB_PATH = settings.graphDbDir

    private def timeGetBNode(subject: String) = metrics.time("lookup-bnode") {
      getBNode(subject)
    }

    private def timeCreateBNode(subject: String, labels: Seq[Label], dynamicLabels: Seq[String]) = metrics.time("create-bnode") {
      val r = createBNode(subject, labels, dynamicLabels)
      metrics.nodeAdded()
      r
    }

    private def timeGetResource(uri: String) = metrics.time("lookup-resource") {
      getResourceNode(uri)
    }

    private def timeCreateResource(uri: String, values: Seq[String], labels: Seq[Label], dynamicLabels: Seq[String]) = metrics.time("create-resource") {
      val r = createResourceNode(uri, values, labels, dynamicLabels)
      metrics.nodeAdded()
      r
    }

    private def timeUpdateResource(id: NodeType, uri: String, values: Seq[String], labels: Seq[Label], dynamicLabels: Seq[String]) = metrics.time("update-resource") {
      updateResourceNode(id, uri, values, labels, dynamicLabels)
    }

    private def timeCreateLiteral(literal: String, labels: Seq[String]) = metrics.time("create-literal") {
      val r = createLiteralNode(literal, labels)
      metrics.nodeAdded()
      r
    }

    private def timeCreateRelationship(src: NodeType, dest: NodeType, rel: String, relType: RelationshipType) = metrics.time("create-rel") {
      val r = createRelationship(src, dest, rel, relType)
      metrics.relAdded()
      r
    }

    final def getOrCreateBNode(subject: String, labels: Seq[Label] = Nil, dynamicLabels: Seq[String] = Nil) =
      timeGetBNode(subject).getOrElse(timeCreateBNode(subject, labels, dynamicLabels))

    final def getOrCreateResource(uri: String, values: Seq[String], labels: Seq[Label] = Nil, dynamicLabels: Seq[String] = Nil) =
      timeGetResource(uri).getOrElse(timeCreateResource(uri, values, labels, dynamicLabels))

    final def getAndUpdateResource(uri: String, values: Seq[String], labels: Seq[Label], dynamicLabels: Seq[String]): NodeType = {
      timeGetResource(uri) match {
        case Some(id) ⇒ timeUpdateResource(id, uri, values, labels, dynamicLabels)
        case None     ⇒ timeCreateResource(uri, values, labels, dynamicLabels)
      }
    }

    final def createLiteral(literal: String, labels: Seq[String]) = timeCreateLiteral(literal, labels)

    final def createRel(src: NodeType, dest: NodeType, rel: String, relType: RelationshipType) =
      timeCreateRelationship(src, dest, rel, relType)

    protected def getBNode(subject: String): Option[NodeType]

    protected def createBNode(subject: String, labels: Seq[Label], dynamicLabels: Seq[String]): NodeType

    protected def getResourceNode(uri: String): Option[NodeType]

    protected def createResourceNode(uri: String, values: Seq[String], labels: Seq[Label], dynamicLabels: Seq[String]): NodeType

    protected def updateResourceNode(id: NodeType, uri: String, values: Seq[String], labels: Seq[Label], dynamicLabels: Seq[String]): NodeType

    protected def createLiteralNode(literal: String, labels: Seq[String]): NodeType

    protected def createRelationship(src: NodeType, dest: NodeType, rel: String, relType: RelationshipType): Unit

    def withinTx[A](body: ⇒ A): Try[A]

    def subjectAdded(): Unit

    def shutdown(): Unit
  }

}

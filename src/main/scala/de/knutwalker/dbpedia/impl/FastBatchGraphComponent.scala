package de.knutwalker.dbpedia.impl

import com.carrotsearch.hppc.{ ObjectLongMap, ObjectLongOpenHashMap }
import de.knutwalker.dbpedia.components.{ MetricsComponent, SettingsComponent, GraphComponent }
import java.util
import org.neo4j.graphdb.{ DynamicLabel, Label, RelationshipType }
import org.neo4j.helpers.collection.MapUtil
import org.neo4j.unsafe.batchinsert.BatchInserters
import scala.collection.JavaConverters._
import scala.util.Try

trait FastBatchGraphComponent extends GraphComponent {
  this: SettingsComponent with MetricsComponent ⇒

  type NodeType = Long

  lazy val graph: Graph = new FastBatchGraph

  private final class FastBatchGraph extends Graph {

    private val inserter = {
      val config = MapUtil.stringMap(
        "neostore.nodestore.db.mapped_memory", "90M",
        "neostore.relationshipstore.db.mapped_memory", "3G",
        "neostore.propertystore.db.mapped_memory", "50M",
        "neostore.propertystore.db.strings.mapped_memory", "100M",
        "neostore.propertystore.db.arrays.mapped_memory", "0M")

      BatchInserters.inserter(DB_PATH, config)
    }

    if (settings.createDeferredIndices) {
      inserter.createDeferredSchemaIndex(Labels.resource).on(Properties.uri).create()
      inserter.createDeferredSchemaIndex(Labels.literal).on(Properties.value).create()
    }

    private val labels: util.Map[String, Label] = new util.HashMap(32)

    private val resources: ObjectLongMap[String] = new ObjectLongOpenHashMap(settings.approximatedResources)
    private val bnodes: ObjectLongMap[String] = new ObjectLongOpenHashMap(settings.txSize)

    private def getLabel(label: String): Label = {
      val l = labels.get(label)
      if (l eq null) {
        val neoLabel = DynamicLabel.label(label)
        labels.put(label, neoLabel)
        neoLabel
      } else l
    }

    private def addValues(p: util.Map[String, AnyRef], vs: Seq[String]) = {
      vs.foreach(v ⇒ p.put(Properties.value, v))
    }

    private def makeLabels(dynamicLabels: Seq[String]): Seq[Label] = dynamicLabels.map(getLabel)

    private def get(cache: ObjectLongMap[String], key: String): Option[Long] = {
      val n = cache.getOrDefault(key, -1)
      if (n == -1) None else Some(n)
    }

    private def set(cache: ObjectLongMap[String], key: String, properties: util.Map[String, AnyRef], labels: Seq[Label]): NodeType = {
      val n = inserter.createNode(properties, labels: _*)
      cache.put(key, n)
      n
    }

    private def props(k: String, v: AnyRef): java.util.Map[String, AnyRef] = {
      val p = new java.util.HashMap[String, AnyRef](1)
      p.put(k, v)
      p
    }

    protected def getBNode(subject: String) = get(bnodes, subject)

    protected def createBNode(subject: String, labels: Seq[Label], dynamicLabels: Seq[String]) = {
      val ls = labels ++ makeLabels(dynamicLabels)
      set(bnodes, subject, null, ls)
    }

    protected def getResourceNode(uri: String) = get(resources, uri)

    protected def createResourceNode(uri: String, values: Seq[String], labels: Seq[Label], dynamicLabels: Seq[String]) = {
      val p = props(Properties.uri, uri)
      addValues(p, values)
      val ls = labels ++ makeLabels(dynamicLabels)
      set(resources, uri, p, ls)
    }

    def updateResourceNode(id: Long, uri: String, values: Seq[String], labels: Seq[Label], dynamicLabels: Seq[String]) = {

      val p = props(Properties.uri, uri)
      addValues(p, values)

      val ls = labels ++ makeLabels(dynamicLabels) ++ inserter.getNodeLabels(id).asScala

      inserter.setNodeLabels(id, ls.distinct: _*)
      inserter.setNodeProperties(id, p)

      id
    }

    def createLiteralNode(literal: String, labels: Seq[String]) = {
      val p = props(Properties.value, literal)
      val ls = Labels.literal +: makeLabels(labels)
      val n = inserter.createNode(p, ls: _*)
      n
    }

    def createRelationship(src: Long, dest: Long, rel: String, relType: RelationshipType): Unit = {
      val p = props(Properties.uri, rel)
      inserter.createRelationship(src, dest, relType, p)
    }

    def subjectAdded() = ()

    def withinTx[A](body: ⇒ A) = Try(body)

    def shutdown() = inserter.shutdown()
  }

}


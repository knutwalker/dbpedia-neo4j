DBpedia Batch Importer for Neo4j
================================

Imports [DBPedia](http://dbpedia.org/) dumps into [Neo4j](http://www.neo4j.org/)

This importer aims for faster import times, but less restrictive (and thus, less performant) implementations might follow.
In particular, it can only perform a full-load on an offline, ideally empty, database.
Importing the same file with two executions might result in duplicated nodes or a corrupt database.


## Quickstart

```bash
./sbt "run dewiki-20140320-article-categories.nt.gz"
```


## Usage

### Requirements

- JDK7 (Probably Oracle JDK)
- (SBT and Scala 2.11) - will be pulled automatically, if you do not have these installed


_1_. Build the project

```bash
./sbt assembly
```

_2_. Run the jar against one or more scripts. Always run all files in one run, otherwise the database will have incorrect data

```bash
./dbpedia-neo4j.jar ./dewiki-20140320-article-categories.nt.gz
```

_3_. Specify run options
e.g. You can change the Neo4j database dir with the `dbpedia.db-dir` system property and give the script more memory to run

```bash
java -server -Xms1g -Xmx8g -Ddbpedia.db-dir=import.db -jar dbpedia-neo4j.jar {article_categories,category_labels,instance_types,labels,skos_categories}_de.nt.bz2
```


## Input files

The importer can work regular `nt` or `nq` files, or de-compress `gz` or `bz2` archives on the fly (plain-text files are the fastest, of course).
It helps a lot, if the files are ordered by subject, so that every statement for one particular subject comes in one block; otherwise the data might be incorrect (esp. in regard to the `rdf:type` and `rdfs:label` handling).


## Data model

Generally, a triple will be stored as a `(s)-[p]->(o)` with the following additions:
- the subject gets the label `:Resource` or `:BNode`
- the object gets the label  `:Resource` or `:BNode` or `:Literal`
- the relationship gets a type, that is equivalent to the predicate's URI
- `:Resource`s have a `uri` property and `:Literal`s a `value` property

On top of that, that following changes are implemented:
- relationships of `rdf:type` will be discarded and the particular object uri will be added as a label to the subject
- relationships of `rdfs:label` or `skos:prefLabel` will be discarded and the particular object literal will be added as a `value` property to the subject. (When there might be more than one of these labels, currently only one of these gets actually stored)

That means, you can use the `uri` property as a unique-id-kind-of property and the `value` property as a pretty-output-end-user-kind-of property (this also makes great node captions for the neo4j browser UI)

The importer can create or maintain a schema index for later use. See [Configuration](#configuration)


## Configuration

The importer uses [Typesafe Config](https://github.com/typesafehub/config#overview) for configuration, for details on how to write the config, please see their documentation.

The following keys are used, see [reference.conf](src/main/resources/reference.conf) for details
- `dbpedia.db-dir`
- `dbpedia.tx-size`
- `dbpedia.approx-resources`
- `dbpedia.deferred-index`

Probably the most important setting would be `db-dir`, which is the location of the database directory for neo4j.
To be precise, this must match your `org.neo4j.server.database.location` property from `conf/neo4j-server.properties`

If you set `dbpedia.deferred-index` to true, the importer will create a schema index on `:Resource(uri)` and `:Literal(value)`.
These indices will (and can) not be used during the importing process, they only release you from the burden, to remember to add these later on by yourself.


## Under the hood

The importer uses the [batch API](http://docs.neo4j.org/chunked/2.0.2/batchinsert.html) with all its implications, such as only using a single thread with no transactions.
The importer maintains an in-memory map for URIs <-> NodeIDs mappings, that is, the node cache, that prevents multiple resources nodes for the same URI.
This cache is ephemeral and will be destroyed when the importer terminates. There is no usage of any index, lookup, or cache that actually goes to the database.
This is also the reason, that you have to do everything in one go.


## Metrics

The importer logs several runtime metrics through the excellent [metrics library](http://metrics.codahale.com/).
Meters for inserted nodes and relationships are reported during the process, a full report is given at the end.
here's an example output:

    [nodes]: count=2800168 rate=17652.78/s
    [rels]: count=5953013 rate=37528.85/s
    [triples]: count=11975105 rate=75493.05/s
    [create-rel]: count=5953013 rate=37807.85/s [0.00ms, 0.01ms] ~0.00ms ±0.00ms p95=0.00ms p99=0.01ms p999=0.01ms
    [create-resource]: count=2800168 rate=17781.77/s [0.00ms, 0.09ms] ~0.00ms ±0.00ms p95=0.01ms p99=0.01ms p999=0.09ms
    [graph-tx]: count=498 rate=3.15/s [83.61ms, 2586.21ms] ~281.81ms ±271.38ms p95=681.58ms p99=1828.87ms p999=2586.21ms
    [import]: count=1 rate=0.01/s [151272.85ms, 151272.85ms] ~151272.85ms ±0.00ms p95=151272.85ms p99=151272.85ms p999=151272.85ms
    [lookup-resource]: count=10930877 rate=69411.97/s [0.00ms, 0.01ms] ~0.00ms ±0.00ms p95=0.00ms p99=0.00ms p999=0.01ms
    [shutdown]: count=1 rate=0.14/s [7243.15ms, 7243.15ms] ~7243.15ms ±0.00ms p95=7243.15ms p99=7243.15ms p999=7243.15ms
    [subject-nodes]: count=4977864 rate=31606.02/s [0.00ms, 0.81ms] ~0.02ms ±0.04ms p95=0.04ms p99=0.15ms p999=0.80ms
    [update-resource]: count=2328089 rate=20607.95/s [0.01ms, 0.75ms] ~0.03ms ±0.06ms p95=0.03ms p99=0.33ms p999=0.75ms

- `rels` and `nodes` count, whenever a relationship or node, resp., is added to the database.
- `triples` counts how many triples were processed during the import, event the ones that did not result in new nodes or relationships
- `create-rel` measures, how long it takes to create a new relationship. The count should be equivalent to `edges`
- `create-resource` measures, how long it takes to create a resource node
- `create-literal` measures, how long it takes to create a literal node
- `create-bnode` measures, how long it takes to create a blank node. These last three should add up to `nodes`
- `subject-nodes` measures, how long it takes to handle all statements for a particular subject.
- `lookup-resource` measures the lookup time in the cache to determine, whether or not a resource already exists in the graph
- `lookup-bnode` measures the lookup time in the cache to determine, whether or not a blank node already exists in the graph
- `update-resource` measures, how long it takes to update labels and properties of an already existing resource
- `graph-tx` measures the time of one "transaction"
- `import` measures, how long the actual import process takes
- `shutdown` measures, how long the shutdown process after the import takes. This includes flushing to disk and potentially creating schema indices

## Credits

Credit goes to [@zazi](https://github.com/zazi) and [@sojoner](https://github.com/sojoner) for outlining the data model definition and possibly creating the need for this importer

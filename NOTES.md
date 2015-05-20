
#### Akka Streams Notes

* Remember it's *pull* based, not push based. Walk through flows from the end to the beginning, not the other way around.
* Get to know the operators
** Example: prefixAndTail - is it really a tuple? nope, it's a stream of tuples
* Use logging to help
** Provide practical example of using it to debug

* Examples
** Broadcast with subsequent zip
*** Streams must be same size - add buffers on non-drop side to equalize
** Broadcast with concat - NO!

* Bad flows
** Parsing response by splitting into 2 streams
````scala
    val bcast = b.add(Broadcast[Array[String]](2))
    val header = b.add(Flow[Array[String]].take(1).expand[Array[String], Array[String]](identity)(s => (s, s))/*.buffer(1, OverflowStrategy.backpressure)*/)
    val rows = b.add(Flow[Array[String]].drop(1))
    val zip = b.add(Zip[Array[String], Array[String]])
    val print = b.add(Sink.fold[Int, Array[String]](0){ (i, r) => logger.info(s"parsed record $i: ${r.mkString(",")}"); i + 1 })
    val merge = b.add(Flow[(Array[String], Array[String])].map(t => t._1.zip(t._2).foldLeft(mutable.LinkedHashMap[String, String]())((m, p) => m += p ).asInstanceOf[Quote]).log("merge", extractMerged).withAttributes(logLevels))

    parse ~> bcast ~> header ~> zip.in0
             bcast ~> rows   ~> zip.in1
                                zip.out ~> merge
             bcast ~> print
````
** Converting stream of objects into CSV by splitting streams into header/rows
````scala
  // Convert a stream of quotes to CSV, first writing header then rows
  lazy val quoteToCsvFlow = Flow() { implicit b =>
    val columns = b.add(Flow[Quote].take(1).map(_.keys.toSeq).expand[Seq[String], Seq[String]](identity)(s => (s, s)))
    val header = b.add(Flow[(Seq[String], Quote)].take(1).map(_._1.mkString(",")))
    val rows = b.add(Flow[(Seq[String], Quote)].mapConcat { t =>
      val (cols, quote) = t
      immutable.Seq("\n", cols.map(quote(_)).mkString(","))
    })

    val in = b.add(Broadcast[Quote](3))
    val print = b.add(Sink.fold[Int, Quote](0){ (i, q) => logger.info(s"received quote $i: $q"); i + 1 })
    val zip = b.add(Zip[Seq[String], Quote])
    val bcast = b.add(Broadcast[(Seq[String], Quote)](2))
    val concat = b.add(Concat[String])

    in ~> columns ~> zip.in0
    in            ~> zip.in1
                     zip.out ~> bcast ~> header ~> concat
                                bcast ~> rows   ~> concat
    in ~> print

    (in.in, concat.out)
  }
````

* Logging
````scala
  val logLevels = OperationAttributes.logLevels(onElement = DebugLevel)

    var i = 0
    def extractRecord(record: Array[String]): String = {
      i = i + 1
      s"$i: ${record(0)}"
    }
    var j = 0
    def extractMerged(merged: (collection.Map[String, String])): String = {
      j = j + 1
      s"$j: ${merged.values.head}"
    }
````
[![Build Status](https://travis-ci.org/gphat/censorinus.svg?branch=master)](https://travis-ci.org/gphat/censorinus)

**This project was forked from
[Censorinus](https://github.com/gphat/censorinus) with the resultant
artifacts moved in maven and package coordinate space**

Censorinus is a Scala \*StatsD client with multiple personalities.

# Features

* No dependencies, just boring Scala and Java stuff.
* Client-side sampling, i.e. don't send it to across the network to reduce traffic. Or you can bypass it and still supply a sample rate if you wanna do it on your end.
* [StatsD Compatibility](https://github.com/etsy/statsd/blob/master/docs/metric_types.md)
* [DogStatsD Compatibility](http://docs.datadoghq.com/guides/dogstatsd/#datagram-format)
* Asynchronous or Synchronous, your call!
  * Async: Option for max queue size to avoid overwhelming your upstream and/or having an ubounded queue
  * Option for batching to reduce syscall overhead
* UDP only
* "Swallows" relevant exceptions (`IO`, `Unresolveable`) by **default** to prevent runtime errors breaking your service

# Using It

Censorinus is available on Maven Central.

```scala
// Use %% and don't worry about Scala version
libraryDependencies += "llc.flyingwalrus" %% "censorinus" % "2.1.13"
// Or add the Dep for Scala 2.12
libraryDependencies += "llc.flyingwalrus" % "censorinus_2.12" % "2.1.13"
// Or add the Dep for Scala 2.11
libraryDependencies += "llc.flyingwalrus" % "censorinus_2.11" % "2.1.13"
```

You should create a single instance of a client reuse it throughout your
application. It's thread-safe and all that shit! Note that unless you call
`shutdown` on your client, it will keep it's datagram socket open forever.

# Examples

## StatsD

Censorinus is compatible with the StatsD specification as defined [here](https://github.com/etsy/statsd/blob/master/docs/metric_types.md).

```scala
import github.gphat.censorinus.StatsDClient

val c = new StatsDClient(hostname = "some.host", port = 8125)

// Optional sample rate, works with all methods!
c.counter(name = "foo.count", value = 2, sampleRate = 0.5)
c.increment(name = "foo.count") // Defaults to 1
c.decrement(name = "foo.count") // Defaults to -1
c.gauge(name = "foo.temperature", value = 84.0)
c.histogram(name = "foo.depth", value = 123.0)
c.meter(name = "foo.depth", value = 12.0)
c.set(name = "foo.users.seen", value = "gphat")
c.timer(name = "foo.users.duration", milliseconds = 123)
```

## DogStatsD

Censorinus is compatible with the DogStatsD specification as defined
[here](http://docs.datadoghq.com/guides/dogstatsd/#datagram-format). It's
basically the same as the StatsD client but each method has a `tags` argument
that takes a `Seq[String]` of tags. It also supports some additional Features
such as histograms, events and service checks.

```scala
import github.gphat.censorinus.DogStatsDClient

val c = new DogStatsDClient(hostname = "some.host", port = 8125)

// Not gonna list 'em all since the methods are the same, but allow tags!
c.counter(name = "foo.count", value = 2, tags = Seq("foo:bar"))
c.timer(name = "foo.users.duration", milliseconds = 123, tags = Seq("foo:gorch"))
```

### Service checks

The Datadog client also supports [service checks](http://docs.datadoghq.com/guides/dogstatsd/#service-checks):

```scala
client.serviceCheck("some_service", DogStatsDClient.SERVICE_CHECK_OK, tags = Seq("foo:bar"))
```

Note that the prefix on the client will be used for the service check as well.

### Events

The Datadog client also supports [events](http://docs.datadoghq.com/guides/dogstatsd/#service-checks).

```scala
client.event(
  name = "I am an event",
  text = "And here are my contents",
  aggregationKey = Some("abc-1234"),
  priority = Some(DogStatsDClient.EVENT_PRIORITY_LOW),
  sourceTypeName = Some("nagios"),
  alertType = Some(DogStatsDClient.EVENT_ALERT_TYPE_ERROR),
  tags = Seq("a:b")
)
```

# Prefixes

If all your metrics start with a common string like a service or team name then
you can safe yourself by using prefixes when instantiating a client:

```scala
val c = new DogStatsDClient(hostname = "some.host", port = 8125, prefix = "mycoolapp")
c.counter(name = "foo.count") // Resulting metric will be mycoolapp.foo.count
```

# Asynchronous (default behavior)

Metrics are locally queued via a BlockingQueue and emptied out in single
ThreadExecutor thread. Messages are sent as quickly as the blocking `take` in
that thread can fetch an item from the head of the queue.

You may provide a `maxQueueSize` when creating a client. Doing so will prevent
the accidental unbounded growth of the metric send queue. If the limit is reached
then new metrics **will be dropped** until the queue has room again. Logs will
be emitted in this case for every `consecutiveDropWarnThreshold` drops. You can
adjust this when instantiating a client.

**Note:** You can call `c.shutdown` to forcibly end things. The threads in this
executor are flagged as deaemon threads so ending your program will cause any
unsent metrics to be lost.

## Batching

Specifying a `maxBatchSize` in characters will trigger batching of metrics into
newline-separated batches when more than one metric is queued. This may reduce
overhead due to fewer syscalls being required to send along UDP packets. Note:
Be sure and choose a value that is compatible with your network's MTU! 1500 is
usually safe but loopback devices can often be larger. Also verify that your
upstream metric system can handle newline delimited metrics!

# Synchronous

If you instantiate the client with `asynchronous=false` then the various metric
methods will immediately emit your metric synchronously using the underlying
sending mechanism. This might be great for UDP but other backends may have
a high penalty!

```scala
val c = new Client(asynchronous = false)
```

# Sampling

All methods have a `sampleRate` parameter that will be used randomly determine
if the value should be enqueued and transmitted downstream. This lets you
decrease the rate at which metrics are sent and the work that the downstream
aggregation needs to do. Note that only the counter type natively understands
sample rate. Other types are lossy.

```scala
c.counter(name = "foo.count", value = 2, sampleRate = 0.5)
```

Note that StatsD's counters support an additional sample rate argument, since
counters can be multiplied by the sample rate downstream to give an accurate
number.

## Bypassing

You can also supply a `bypassSampler = true` argument to any of the client's
methods to send the metric regardless. Note that the sample rate will *also* be
sent. This is a convenience method to allow you to do your own sampling and pass
that along to this library.

# Notes

* All metric names and such are encoded as UTF-8.
* If you prefer to catch your own exceptions or log them somehow when failing to deliver or resolve the upstream target, look for `allowExceptions` on your client of choice and set to true!
* Infinite values are dropped silently, since they can't be expressed downstream.

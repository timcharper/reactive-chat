# Reactive Chat

Silly chat room service using akka sharding.

# Deployment

Requires Zookeeper to run.

Package is created using sbt native packager:

    sbt universal:packageBin

Package will be in ./target/universal/reactive-chat-0.1-SNAPSHOT.zip

# Running

To run, extract the above zip somewhere. Then, create the following script and adjust the settings as needed:

```
#/usr/bin/env bash
export AKKA_IP=127.0.0.1
export AKKA_PORT=2015
export STATSD_HOST=127.0.0.1
export STATSD_PORT=8125
export ZK_URL=mesos-1.dev.vagrant:2181
export HTTP_PORT=8080
export HTTP_ADDRESS=0.0.0.0

reactive-chat-0.1-SNAPSHOT/bin/reactive-chat
```

(most notably, you'll need to put the appropriate IP and port for your Zookeeper instance; statsd silently fails if misconfigured)

To run two instances, simply create another copy of that script with a different akka port and http port. Keep everything else the same.

# Watching metrics

A poor mans statsd receiver is netcat:

```
nc -l -u 8125
```

Run this before starting the app, and you should see the stats start flowing.

# Interacting with the chat server

To listen to a room:

```
curl localhost:8080/room/5
```

To post to a room:

```
curl localhost:8080/room/5 -X PUT --data "Hello world"
```

Health check:

```
curl localhost:8080/health
```

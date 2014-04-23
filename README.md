embedded-redis
==============

Redis embedded server

This is a fork of https://github.com/nielspeter/embedded-redis


Maven dependency
==============

Currently embedded-redis is available in sonatype repository:

Dependency configuration:
```
<dependency>
  <groupId>com.orange.redis-embedded</groupId>
  <artifactId>embedded-redis</artifactId>
  <version>0.4</version>
</dependency>
```
More at https://clojars.org/org.clojars.gaelbreard.redis.embedded/embedded-redis

Usage example
==============

Running RedisServer is as simple as:
```
RedisServer redisServer = new RedisServer();
redisServer.start();
// do some work
redisServer.stop();
```
You can also provide RedisServer with a version to run:
```
RedisServer redisServer = new RedisServer("2.8.9");
```
A simple redis integration test would look like this:
```
public class SomeIntegrationTestThatRequiresRedis {
  private RedisServer redisServer;
  
  @Before
  public void setup() throws Exception {
    redisServer = new RedisServer();
    redisServer.start();
  }
  
  @Test
  public void test() throws Exception {
    // testing code that requires redis running
  }
  
  @After
  public void tearDown() throws Exception {
    redisServer.stop();
  }
}
```


Redis version
==============

RedisServer runs os-dependent executable enclosed in jar.

Currently it includes the following binaries:

- Redis 2.8.5 in case of Linux/Mac OS X

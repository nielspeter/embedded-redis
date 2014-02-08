embedded-redis
==============

Redis embedded server for Java integration testing

This is a fork of https://github.com/kstyrc/embedded-redis


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
RedisServer redisServer = new RedisServer("2.8.5");
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

RedisServer runs os-dependent executable enclosed in jar. Currently is uses:
- Redis 2.8.5 in case of Linux/Mac OS X

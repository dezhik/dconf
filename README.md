# Dynamic configuration service

Server & client for dynamic feature toggling and on-fly reconfiguration of your services. 
Configuration changes made in web UI would be fetched & applied by clients within seconds.

## Web & api server

Build server jar from sources and fetch all dependencies
```
$ gradle getDeps
```

Run web-server:
```
$ java -cp "./runtime/*" com.dezhik.conf.server.ConfServer
```

Web server with default configuration will startup on localhost:8080 only if you have mongodb running on 127.0.0.1:27017.
You can pass custom params to adjust the default web-server configuration. 

### Advanced server configurations:

| Param key  | Default value             |
|------------|---------------------------|
| server.host | localhost                 |
| server.port | 8080                      |
| mongodb.url | mongodb://127.0.0.1:27017 |
| mongodb.name | conf                      |
| mongodb.collection.name | conf                      |
| mongodb.user |                           |
| mongodb.password |                           |


## Client
Build and publish client library:
```
$ gradle publishClientLibPublicationToMavenLocal
```

Resulting lib format is ```dconf-client-%CURRENT_VERSION%.jar```

### Advanced client configurations:

| Param key  | Default value | Comment |
| --- |---------------| --- |
| module.name | default       |
| conf.startup.sync.required | true          | Throws IllegalStateException if failed to connect to web-server after 3 attemps.



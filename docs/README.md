# Multithreaded Java 8 WebServer

Welcome, this repo hosts a webserver purelay implemented in Java without relying heavily on preexisting libraries or frameworks. The aim of this exercise was to setup a whole end-to-end project in Java 8 and thereby ensuring that key technical features such as Etag, GZip, Mime-Types are implemented. Furthermore basic multithreading was implemented to allow users to access the server simultaneously.

Please find the JavaDocs here: https://hadrianutherae.github.io/Multithreaded-Java-WebServer/


## MVP build
The MVP has the following features implemented
- [x] Implement Etag functionality (If-Match, If-None-Match, If-Modified-Since)
- [x] Implement GZip compression for quick transfer of files
- [x] Basic Multithreading, where each new request opens up a new thread. No threadpool!
- [x] Serving a given directory and all of its subdirectories
- [x] Basic explorer-like interface, to navigate up and down in folders
- [x] Serving files using their appropriate Mime-Types, basic Mime-Type matching based on filename endings
- [x] File downloads using the original name of the file
- [ ] Threading using Threadpool
- [ ] Aborted requests leads to a short hang of the server -> more robustness measures
- [ ] More HTTP headers...



## Java Executable
Libraries are built along with the core of this project into a Jar Executable. The application takes two commands when it is called from the CLI, firstly the path which will be served and secondly, the port on which the server will listen. If the port is already bound to another process, an exception will be thrown and you might need to find another port, which is not bound yet.

```
java -jar uber-JavaWebServer-1.0-SNAPSHOT.jar "/" "65535"
```
## Dockerfile and image

You can directly create your own Docker container by running the supplied dockerfile or alternatively, you may access a prebuilt image down below.

To run the docker image clone this image:
```
docker pull hadrianutherae/javawebserver
```
When running the container, make sure your ports are redirected from the container to your machine, where you want to browse from.

# Multithreaded Java 8 WebServer

Welcome, this repo hosts a webserver purelay implemented in Java without relying heavily on preexisting libraries or frameworks. The aim of this exercise was to setup a whole end-to-end project in Java 8 and thereby ensuring that key technical features such as Etag, GZip, Mime-Types are implemented. Furthermore basic multithreading was implemented to allow users to access the server simultaneously.


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



To run the docker image clone this image:
```
docker pull hadrianutherae/javawebserver
```

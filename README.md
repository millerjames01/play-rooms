# Play Rooms

A proof-of-concept project leveraging Play! Framework, Akka typed actors, and websockets 
to create a real-time multi-chatroom web application.

## Description

You can demo the application by running the web application from a local server
and passing messages back and forth from different browsers or different computer on your local
network.

## Getting Started

### Dependencies

* sbt

### Executing program

1) First, change the `my.host` setting in the application.conf file from `localhost` to your IP address (e.g., `192.0.0.x`).
This step is only necessary if you want to test the functionality on different devices on your network. 
2) Then, run `sbt` inside of the main directory. Once the shell is loaded, type `run`. 
3) Load the webpage in a browser at `localhost:9000` (or `{your IP address}:9000` if you are accessing from another
device than the computer the server is running on).
4) Make a room and chat into the box.

## Authors

Contributors names and contact info

ex. Jim Miller

## Version History

* 0.2 (_COMING SOON_)
    * Aliases for different chatters
    * Different mobile styles
    * Unit testing
    * Enhanced security (CSRH filters and cross-origin checks)
* 0.1
    * Initial Release

## Acknowledgments

I wouldn't have been able to build this application without the help of these two projects.
* [Alvin Alexander: "Akka Typed: Finding Actors with the Receptionist"](https://alvinalexander.com/scala/akka-typed-how-lookup-find-actor-receptionist/)
* [Play Scala Websocket Example](https://github.com/playframework/play-samples/tree/2.8.x/play-scala-websocket-example)

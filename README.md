# ai-for-games [![Build Status](https://travis-ci.org/heyarne/pure-vernunft-darf-niemals-siegen.svg?branch=master)](https://travis-ci.org/heyarne/pure-vernunft-darf-niemals-siegen)

This is an attempt at solving the HTW IMI Master course "AI for Games and Interactive Systems" with Clojure. We'll see how that works out.

[![Pure Vernunft Darf Niemals Siegen](https://img.youtube.com/vi/e1szcpyzsAE/0.jpg)](https://www.youtube.com/watch?v=e1szcpyzsAE)

## Installation

You need [leiningen](https://leiningen.org/) to build this. You should check the leiningen webpage for instructions how to install it.

## Usage

First start the server:

```
$ ./run-server.sh
```

Afterwards you can run the clients like so:

```
$ lein run
```

The server should output something along those lines:

```
Start with 3 integer parameters: <window width> <window height> [<time limit to send move in seconds>] [noanim]
	defaults: 1200 880 8
Server listening on 192.168.0.11:22135
Server waiting for 3 connections...
Incoming connection from 127.0.0.1
Welcome 
 successfully connected
Server waiting for 2 connections...
Incoming connection from 127.0.0.1
Welcome 
 successfully connected
Server waiting for 1 connections...
Incoming connection from 127.0.0.1
Welcome 
 successfully connected
Press <space> to start the game...
```

**NOTE:** This will very likely change in the future.

## Tests

There are quite a couple of tests in `./test`. You can run them with `lein test`.

## Options

**TODO:** Add CLI options
 

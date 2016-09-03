# [Mooncal.ch](http://mooncal.ch/)

Mooncal is a web app for generating personal moon calendars.
It offers the display of moon phases as well as some special events about the moon.
The calendar can be exported to personal calendar applications.


## Development
### Dependencies
Mooncal is built on the following frameworks:

* [Play Framework](https://www.playframework.com/)
* [AngularJS](https://angularjs.org/)
* [Bootstrap](http://getbootstrap.com/)

### Prerequisites
* Java 8 (JDK)
* [Typesafe Activator](https://www.typesafe.com/activator/download)

### Running
    cd mooncal
    activator run

### Testing
    cd mooncal
    activator test

### Debugging
    cd mooncal
    activator -jvm-debug 5005 run
And in your IDE add a remote debugging configuration.

### Supporting Mooncal
* Feel free to [make a donation](http://mooncal.ch/#/about)
* Feel free to develop a new feature/fix a bug/...
    * Please develop according to [git-flow](https://github.com/nvie/gitflow)
    * Please write also a test if reasonable

## Running
### Prerequisites
* Java 8 (JDK)

### Installation
* Unzip the zip-file to any folder
* Start `bin/mooncal.bat` (Windows) or `bin/mooncal` (Linux/Mac) within the extraction folder
    * Detailed configuration can be done like any other [Play Application](https://www.playframework.com/documentation/2.4.x/ProductionConfiguration)
* Mooncal is now available at [http://localhost:9000](http://localhost:9000/)

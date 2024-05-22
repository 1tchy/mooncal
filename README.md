# [Mooncal.ch](http://mooncal.ch/)

Mooncal is a web app for generating personal moon calendars.
It offers the display of moon phases as well as some special events about the moon.
The calendar can be exported to personal calendar applications.


## Development
### Dependencies
Mooncal is built on the following frameworks:

* [Play Framework](https://www.playframework.com/)
* [Angular](https://angular.io/)
* [Bootstrap](http://getbootstrap.com/)

### Prerequisites
* Java 21 (JDK)
* [Node.js](https://nodejs.org/) (version 10 or higher)
* [sbt](https://www.scala-sbt.org/download.html)

### Running
    cd mooncal
    sbt run

### Testing
    cd mooncal
    sbt test

### Debugging
    cd mooncal
    sbt -jvm-debug 5005 run
And in your IDE add a remote debugging configuration.

### Updating moon landings
* https://en.wikipedia.org/wiki/List_of_missions_to_the_Moon#Missions_by_date
* https://en.wikipedia.org/wiki/Moon_landing#21st_century_uncrewed_soft_landings_and_attempts

### Supporting Mooncal
* Feel free to [make a donation](https://mooncal.ch/en/about)
* Feel free to develop a new feature/fix a bug/...
    * Please develop according to [git-flow](https://github.com/nvie/gitflow)
    * Please write also a test if reasonable

## Running
### Prerequisites
* Java 21 (JDK)

### Installation
* Unzip the zip-file to any folder
* Start `bin/mooncal.bat` (Windows) or `bin/mooncal` (Linux/Mac) within the extraction folder
    * Detailed configuration can be done like any other [Play Application](https://www.playframework.com/documentation/2.4.x/ProductionConfiguration)
* Mooncal is now available at [http://localhost:9000](http://localhost:9000/)

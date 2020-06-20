# ezAuctions
Spigot Page: [https://www.spigotmc.org/resources/ezauctions.42574/](https://www.spigotmc.org/resources/ezauctions.42574/)

A simple, text-based auction plugin <br> <br>
Based off of floAuction & Auctions <br>

### Plugin Dependencies
This plugin requires your server to have `Vault` installed. If you do not have it installed, it can be found 
[here](https://www.spigotmc.org/resources/vault.34315/).

## Developers
Elian, Silverwolfg11

## Website / Server
Made for and used by http://urbanmc.net/

## Building
Clone the project from gitlab, then run `mvn clean package` in your terminal at the project directory to build the project.

## API
To depend on this plugin in your own project, add the following to your maven / gradle project.

Repository:
```
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
Dependency:
```
<dependency>
    <groupId>com.gitlab.elian1203</groupId>
    <artifactId>ezAuctions</artifactId>
    <version>1.5.5</version>
</dependency>
```
You can view API usage [here](https://gitlab.com/elian1203/ezAuctions/-/wikis/api).
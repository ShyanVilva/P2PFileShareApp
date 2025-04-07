
Requirements:
Java JDK 11 or higher
Maven

On line 51 in "PeerDiscovery.java" and line 37 in "SelfAdvertising.java" please change the IP to your IPv4 address which you can find using 'ipconfig' in terminal. 

1. Build the project using Maven: mvn clean install

2. Run the program: mvn compile exec:java


To test functionality, simply open two terminals and communicate between the two.
If a feature isn't working correctly please restart the program, I was having some issues with multithreading and couldn't get it fixed in time.

Files downloaded from the server are stored in "downloads"
Files advertised by a client are stored in "shared"

 

# Project content

Download APK [here](https://github.com/Eragoneq/trustchain-superapp/actions/runs/8752680842/artifacts/1429266695). <!-- LINK HERE -->

For documented details regarding the app usage, click [here](https://github.com/Eragoneq/trustchain-superapp/blob/master/doc/BlockchainNetworkingProject/UtpTesting.md). <!-- LINK HERE -->

For documented details regarding benchmarking done over the course time, click [here](https://github.com/Eragoneq/trustchain-superapp/blob/master/doc/BlockchainNetworkingProject/Benchmarking.md). <!-- LINK HERE -->

## _All relevant work for the project on respective repositories is linked below:_

### [uTP4j](https://github.com/PieterCarton/utp4j/)

For documented details on relevant changes and benchmarking, click [here](https://github.com/PieterCarton/utp4j/blob/master/CHANGES.md).

### [trustchain-superapp](https://github.com/Eragoneq/trustchain-superapp)

### [kotlin-ipv8](https://github.com/Eragoneq/kotlin-ipv8)

For more documented details about the uTP integration, click [here](https://github.com/Eragoneq/kotlin-ipv8/blob/master/doc/UtpBinaryTransfer.md)

## Overall project overview

### Library
  - Contacted the original author and got a hold of the repository
  - Found and addressed the basic issues of dependencies, formatting and testing
  - Updated the version and migrated to use gradle
  - TODO: Update license and readme

### Benchmarking
  - Created a lot more test files to test parameters
  - Analyzed the algorithm and its parameters
  - Created a small framework for further testing in simulated network environment
  - Wrote additional unit tests for real life cases

### App
  - Created a debug screen for uTP connections
  - Used dynamically updating peer status
  - Created a debug log screen

### IPv8
  - Created proof of concept for opening another port with NAT puncturing
  - Extensive discussion about the IPv8 socket vs own socket
  - Finished with the extended socket multiplexing approach
  - Using separate UtpCommunity to inform users about the capabilities
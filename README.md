![image](https://github.com/Informatievlaanderen/VSDS-Testbed/assets/15314807/5e483312-1b24-40da-837f-968df9528511)

##  PoC contents  

The PoC’s resources are included in a ZIP archive containing everything you need to run the PoC and begin experimenting. This archive includes the following:  

• [./config]: Includes the configuration for the different components and the PoC’s data (imported upon startup). 
• [./test_suite]: The root of the test suite. Note that when creating a new test suite archive the root folder itself should not be added to the archive (doing so would need a change in reference paths). 
• [./test-services]: The complete project for the extension test services application.  
• [./deploy_test_suite.bat]: A simple script using command line tools to ZIP and redeploy the test suite using the Test Bed’s REST API (you can adapt this for your own tools to ZIP and do the HTTP POST – I have used 7zip and curl).  
• [./docker-compose.yml]: The PoC’s definition file that you use to build, install and run the PoC (see next section). 
• [./export.zip]: The data archive used to seed the Test Bed with the PoC’s data. This is replicated in the [./config/data] folder but I’m adding it again as upon initial startup the Test Bed removes this file (to avoid re-processing it). 
• [test_suite.zip]: This is the complete test suite (see [./test_suite]) ready to be deployed to the Test Bed. This is not really needed but I’m adding it for illustration purposes.  

##  Installing and running the PoC  

To install and use the PoC follow these steps:  

1. Make sure you have Docker and Docker Compose installed.  

2. Port 9000 should be available on your workstation. If not, you can change this in [./docker-compose.yml] for the gitb-ui container.  
3. Extract the ZIP archive to a folder and open a command prompt to the folder.
4. Issue “docker compose up -d” 

	a. This command will pull all relevant images and build the image of the supporting test services from the included source project in [./test-services].

	b. You will know that the Test Bed is ready to access by tailing the logs of the “gitb-ui” container. The first time you start this up it may take a couple of minutes as it will create the DB and populate everything using the PoC’s data. Once you see a message “Listening for HTTP on /0.0.0.0:9000” everything will be ready to use.

5. Extract persisted export password: 12345

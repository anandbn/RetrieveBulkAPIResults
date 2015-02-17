# RetrieveBulkAPIResults

The goal of this Java program is to extract the job id from the log file generated by data loader along with the additional parameters and generate the success and error CSV files.

It uses the following libraries:

- Force.com WSC 
- Apache Commons CLI
- Apache Commons Logging
- Log4J

## High level program logic

The program performs the following steps

1. Extract the Bulk API Job Id from the log file that is passed as input parameter
2. Login to Salesforce using the username,password and endpoint url provided
3. Get all batches corresponding to the job id
4. Iterate through each batch and append based on the results append the request to the success or error file

Most of the logic is self contained in `RetrieveResult.java`

## How to build the project

### Pre-requistes

1. If you are building this from command line you will need Maven 3.2.3 (http://maven.apache.org/) or later.
2. Java 1.6 
3. Eclipse with m2eclipse plugin (http://eclipse.org/m2e/)

### Building from Command line

1. Clone the project

```
git clone git@github.com:anandbn/RetrieveBulkAPIResults.git
```
__Note__ : You can also download a ZIP from git, unzip into a local directory and continue with the rest of the steps.

2. Confirm `mvn` is installed with the command `mvn -version`. This should return something like below (results will vary based on Java and OS of your PC).

```
Apache Maven 3.2.3 (33f8c3e1027c3ddde99d3cdebad2656a31e8fdf4; 2014-08-11T16:58:10-04:00)
Maven home: /Users/anarasimhan/dev/apache-maven-3.2.3
Java version: 1.6.0_65, vendor: Apple Inc.
Java home: /System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home
Default locale: en_US, platform encoding: MacRoman
OS name: "mac os x", version: "10.10.2", arch: "x86_64", family: "mac"
```

2. Go to the project directory and run `mvn clean package` to build the project from command line.

This will generate a `target` directory that will contain two JAR files

- `dataloadutils-0.0.1-SNAPSHOT-jar-with-dependencies.jar` : Contains all classes in a single JAR file
- `dataloadutils-0.0.1-SNAPSHOT.jar` : Only contains the classes in this project

### Importing to Eclipse

The git repository includes all required files to import the project into eclipse. Ensure that m2eclipse plugin is installed and configured prior to importing the project into eclipse.

## Running the program locally

`RetrieveResults` is the Java class that is the main entry point for this program. It takes 8 input parameters as below

- `-u <username>` : Salesforce Username to be used
- `-p <password>` : Encrypted password string. This is the same password string that is used in the data loader configuration file
- `-logFile <location of the log file>` : The log file location which is used to extract the Job Id for which the success and error files need to be retrieved
- `-url <loginEndpoint>` : The SOAP Login endpoint url. 
- `-successFile <success file location>`: Location where the success file needs to be generated
- `-errorFile <error file location>` : Location where the error file needs to be generated
- `operation update|insert|upsert` : What type of data load operation was done.
- `encryptionKeyFile <key file location>` : Location of the encryption key file that was used to encrpyt the password. This is an optional parameter and if not passed will use the default encryption key.

To execute the program locally or on a server use the below command:

```
java -jar target/dataloadutils-0.0.1-SNAPSHOT-jar-with-dependencies.jar -u <username> -p <password> -url <login_url> -logFile <log file location> -successFile <success file location> -errorFile <error file location> -operation insert|update|upsert

```
## Distributing the JAR files

1. Once you have built the project (command line or eclipse) in order to distribute it, delete the following directories under `target`:

- archive-tmp						
- classes							
- test-classes
- maven-archiver
- maven-status

2. ZIP the `target` directory and that should serve as your distribution.



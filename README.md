This is the source distribution of Ostfalia-Praktomat, a programming course manager which can also be used as a simple 
grading backend for different programming languages. 

The code is an old fork (2012) from the KIT Praktomat (https://github.com/KITPraktomatTeam/Praktomat).
It adds the ProFormA interface (https://github.com/ProFormA/proformaxml) which enables Praktomat 
to be used as a grading back-end in e.g. learning management systems. 

The code is currently only used as a 'docker composition'. 
So the installation manual is not up-to-date.

Docker 
============

In order to set up the docker composition execute the following steps:

1. Download and copy all libraries you need for grading 
    (e.g. junit-4.12.jar, hamcrest-core-1.3.jar, checkstyle-7.6-all.jar, ....) 
    and adjust your settings_docker.py (JAVA_LIBS, CHECKSTYLE_VER)

2. build and start the docker containers

        docker-compose up

3. initialise the docker containes (i.e. create database schema)
    
        docker exec -ti praktomat ./init_database.sh

Then Praktomat is available on port 80 in your web browser:  

        http://localhost

For login see the credentials in your docker-compose.yml file (SUPERUSER and PASSWORD). 

If you only want to use Praktomat as a grading back-end the appropriate URI is

        http://localhost:80/api/v2/submissions

or (circumventing the web server)

        http://localhost:8010/api/v2/submissions 



ProFormA API (CURL)
============

The supported ProFormA format version is 2.0. 

A typical grading HTTP request in CURL is

    curl -X POST --form submission.xml=@submission.xml -F "{solutionfilename}=@{solutionfile}" -F "{taskfilename}=@{taskfile}" http://localhost:8010/api/v2/submissions

with the following 'submission.xml'

  
    <?xml version="1.0" encoding="utf-8"?>
    <submission xmlns="urn:proforma:v2.0">
        <external-task uuid="{UUID}">http-file:{taskfilename}</external-task>
        <external-submission>http-file:{solutionfilename}</external-submission>
        <lms url="{your URI}">
            <submission-datetime>{timestamp}</submission-datetime>
            <user-id>{user id}</user-id>
            <course-id>{course id}</course-id>
        </lms>
        <result-spec format="xml" structure="separate-test-feedback" lang="de">
            <student-feedback-level>{level}</student-feedback-level>
            <teacher-feedback-level>{level}</teacher-feedback-level>
        </result-spec>
    </submission>"

'submission.xml' can be transferred as a separate file or simply as data. 
Files are sent as normal 'file upload'.
 
Note that embedding the submission file(s) into submission.xml as embedded-txt-file is also supported.

A sample for a timestamp is: 

        2019-04-03T01:01:01+01:00 


### Submission with more than one file

For submitting more than one file you have the following options: 

* create a list of embedded text files in the files section
* send one zip file containing all submission files (relative paths needed in Java)
* set http-file as file name list (comma separated without blanks) and use standard file upload 

Sample for http-file for Java submission files:

        http-file:de/ostfalia/sample/User.java,de/ostfalia/sample/File.java

You can also omit the relative path for Java source files: 

        http-file:User.java,File.java
        
Praktomat parses the source code in order to determine the package which results in the relative path.

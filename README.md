# Spring boot 3 app for solving the following task:

As an api consumer, given username and header “Accept: application/json”, I would like to list all his github repositories, which are not forks. Information, which I require in the response, is:



* Repository Name

* Owner Login

* For each branch it’s name and last commit sha



As an api consumer, given not existing github user, I would like to receive 404 response in such a format:

{

    “status”: ${responseCode}

    “message”: ${whyHasItHappened}

}

# Running the app:

1. Download the executable demo-0.0.1-SNAPSHOT.jar from build\libs\demo-0.0.1-SNAPSHOT.jar 
2. Run `java -jar demo-0.0.1-SNAPSHOT.jar` from terminal
3. You will be prompted to choose to authenticate the session (github token needed) or not
4. You will then be prompted to input a username for the search (header: “Accept: application/json”  is always the same so it is hardcoded)
5. After getting the response you can try inputting another username or quit.
6. Source code is in src\main\java\com\example\demo\DemoApplication.java 

# Quirks:

* Calling the github API without authenticating will quickly result in a 403 error (rate limit is only 60 requests/hour) 
* If the given user has no repositories (or all of them are forks) then you will immediately be prompted to try again.

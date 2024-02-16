package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@SpringBootApplication
public class DemoApplication {


	// Run app
	public static void main(String[] args) throws IOException {
		SpringApplication.run(DemoApplication.class, args);
		Scanner scanner = new Scanner(System.in);

		WebClient webClient = buildWebClient(scanner);

		while (true) {
			String username = getUserInput(scanner);
			String url = String.format("https://api.github.com/users/%s/repos", username);

			AtomicBoolean encounteredError = new AtomicBoolean(false);
			String responseBody = fetchData(webClient, url, encounteredError);

			//Print error message if 404'd else parse the response to get the result
			if (encounteredError.get()) {
				System.out.println(responseBody);
			} else {
				processRepositories(username, webClient, responseBody);
			}

			if (!tryAgain(scanner)) {
				break;
			}
		}

		System.out.println("Exiting program.");
		System.exit(0);
	}

	//Build a WebClient object either with or without authentication from user
	private static WebClient buildWebClient(Scanner scanner){
		while (true) {
			System.out.println("Welcome, do you want to proceed with [y] or without [n] authentication?");
			String choice = scanner.nextLine().toLowerCase();
			if (choice.equals("n")) {
				WebClient webClient = WebClient.builder()
						.defaultHeader(HttpHeaders.ACCEPT, "application/json") //As defined in the question
						.build();
				return webClient;
			} else if (choice.equals("y")) {
				System.out.println("\nPlease enter your token: ");
				String token = scanner.nextLine();
				WebClient webClient = WebClient.builder()
						.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.defaultHeader(HttpHeaders.ACCEPT, "application/json") //As defined in the question
						.build();
				return webClient;

			} else {
				System.out.println("Invalid choice. Please enter 'y' or 'n'.");
			}
		}


	}

	//Take the username of the github account
	private static String getUserInput(Scanner scanner) {
		System.out.print("\nEnter username: ");
		return scanner.nextLine();
	}

	//Retrieve the response body from a url request
	private static String fetchData(WebClient webClient, String url, AtomicBoolean encounteredError) {
		String responseBody = webClient.get()
				.uri(url)
				.retrieve()
				.bodyToMono(String.class)
				.onErrorResume(WebClientResponseException.class, ex -> {
					String fullErrorResponse = handleErrorResponse(ex);
					encounteredError.set(true);
					return Mono.just(fullErrorResponse);
				})
				.block();
		return responseBody;
	}

	//Build response string in the correct format if fetchData encountered an error code
	private static String handleErrorResponse(WebClientResponseException ex) {
		String statusCode = ex.getStatusCode().toString().substring(0, 3);
		Pattern pattern = Pattern.compile("\"message\"[^,]*,");
		Matcher matcher = pattern.matcher(ex.getResponseBodyAsString());
		if (matcher.find()) {
			String whyHasItHappened = matcher.group();
            return String.format("\n{\n\t\"status\": %s\n\t%s\n}\n", statusCode, whyHasItHappened);
		}
		return "";
	}

	//Extract values from the json response body for every repository (Repository Name, Owner Login, For each branch it’s name and last commit sha) and print it
	private static void processRepositories(String username, WebClient webClient, String responseBody) throws IOException {
		int i = 0;
		List<Map<String, Object>> resultList = jsonToListOfMaps(responseBody);

		while (i < resultList.size()) {
			Map<String, Object> repository = resultList.get(i);

			//Skip over forks
			if (isFork(repository)) {
				i++;
				continue;
			}

			String repoName = (String) repository.get("name");
			String ownerLogin = getOwnerLogin(repository);

			AtomicBoolean failsafe = new AtomicBoolean(false);
			String branchesJson = fetchData(webClient, getBranchesUrl(username, repoName), failsafe);

			//If one of the requests gets an error immediately terminate the loop and return error message
			if (failsafe.get()){
				System.out.println(branchesJson);
				break;
			} else {
				List<Map<String, Object>> branchesList = jsonToListOfMaps(branchesJson);
				StringBuilder branches = buildBranchesString(branchesList);

				printRepositoryInformation(repoName, ownerLogin, branches);
				i++;
			}
		}
	}

	//Check if repository is a fork
	private static boolean isFork(Map<String, Object> repository) {
		return (boolean) repository.get("fork");
	}

	//Get owner login
	private static String getOwnerLogin(Map<String, Object> repository) {
		Map<String, Object> ownerMap = (Map<String, Object>) repository.get("owner");
		return (String) ownerMap.get("login");
	}

	//Get url for the branches of desired repo
	private static String getBranchesUrl(String username, String repoName) {
		return String.format("https://api.github.com/repos/%s/%s/branches", username, repoName);
	}

	//Build string displaying a list of repos each showing the name and sha
	private static StringBuilder buildBranchesString(List<Map<String, Object>> branchesList) {
		StringBuilder branches = new StringBuilder("[");
		for (Map<String, Object> branch : branchesList) {
			String branchName = (String) branch.get("name");
			Map<String, Object> branchCommit = (Map<String, Object>) branch.get("commit");
			String branchSha = (String) branchCommit.get("sha");

			branches.append("{Branch name: " + branchName + ", Last commit sha: " + branchSha + "}, ");
		}
		int branchLen = branches.length();

		//Handle case if repo is empty
		if (branchLen>1){
			branches.replace(branchLen  - 2, branchLen , "]");
			return branches;
		} else {
			return new StringBuilder("[]");
		}
	}

	//List the acquired data about the github user's repos
	private static void printRepositoryInformation(String repoName, String ownerLogin, StringBuilder branches) {
		System.out.println("Repository Name: " + repoName);
		System.out.println("Owner Login: " + ownerLogin);
		System.out.println("Branches: " + branches);
		System.out.println("\n");
	}

	//Convert the response string into a List<Map<String, Object>>
	private static List<Map<String, Object>> jsonToListOfMaps(String jsonString) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readValue(jsonString, new TypeReference<List<Map<String, Object>>>() {});
	}

	//Try another request or quit
	private static boolean tryAgain(Scanner scanner) {
		while (true) {
			System.out.print("Try again? [y/n]: ");
			String choice = scanner.nextLine().toLowerCase();
			if (choice.equals("n")) {
				return false;
			} else if (choice.equals("y")) {
				return true;
			} else {
				System.out.println("Invalid choice. Please enter 'y' or 'n'.");
			}
		}
	}
}


package de.refactoringBot.controller.main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.stereotype.Component;

import de.refactoringBot.model.botIssue.BotIssue;
import de.refactoringBot.model.configuration.GitConfiguration;
import de.refactoringBot.model.outputModel.botPullRequest.BotPullRequest;
import de.refactoringBot.model.outputModel.botPullRequest.BotPullRequests;
import de.refactoringBot.model.outputModel.botPullRequestComment.BotPullRequestComment;
import de.refactoringBot.model.refactoredIssue.RefactoredIssue;

/**
 * This class performs bot specific operations.
 * 
 * @author Stefan Basaric
 *
 */
@Component
public class BotController {

	/**
	 * This method checks if the maximal amount of pull requests created by the bot
	 * is reached.
	 * 
	 * @param requests
	 * @param gitConfig
	 * @throws Exception
	 */
	public void checkAmountOfBotRequests(BotPullRequests requests, GitConfiguration gitConfig) throws Exception {

		// Init counter
		int counter = 0;
		// Iterate requests
		for (BotPullRequest request : requests.getAllPullRequests()) {
			// If request belongs to bot
			if (request.getCreatorName().equals(gitConfig.getBotName())) {
				counter++;
			}
		}

		// Check if max amount is reached
		if (counter >= gitConfig.getMaxAmountRequests()) {
			throw new Exception("Maximal amount of requests reached." + "(Maximum = " + gitConfig.getMaxAmountRequests()
					+ "; Currently = " + counter + " bot requests are open)");
		}
	}

	/**
	 * The Method creates a RefactoredIssue-Object from a
	 * Analysis-Service-Refactoring.
	 * 
	 * @param issue
	 * @param gitConfig
	 * @return refactoredIssue
	 */
	public RefactoredIssue buildRefactoredIssue(BotIssue issue, GitConfiguration gitConfig) {
		// Create object
		RefactoredIssue refactoredIssue = new RefactoredIssue();

		// Create timestamp
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date now = new Date();
		String date = sdf.format(now);

		// Fill object
		refactoredIssue.setCommentServiceID(issue.getCommentServiceID());
		refactoredIssue.setRepoName(gitConfig.getRepoName());
		refactoredIssue.setRepoOwner(gitConfig.getRepoOwner());
		refactoredIssue.setRepoService(gitConfig.getRepoService());
		refactoredIssue.setDateOfRefactoring(date);
		refactoredIssue.setAnalysisService(gitConfig.getAnalysisService());
		refactoredIssue.setAnalysisServiceProjectKey(gitConfig.getAnalysisServiceProjectKey());
		refactoredIssue.setRefactoringOperation(issue.getRefactoringOperation());

		return refactoredIssue;
	}

	/**
	 * The Method creates a RefactoredIssue-Object from a
	 * Request-Comment-Refactoring.
	 * 
	 * @param issue
	 * @param gitConfig
	 * @return refactoredIssue
	 */
	public RefactoredIssue buildRefactoredIssue(BotPullRequest request, BotPullRequestComment comment,
			GitConfiguration gitConfig) {
		// Create object
		RefactoredIssue refactoredIssue = new RefactoredIssue();

		// Create timestamp
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date now = new Date();
		String date = sdf.format(now);

		// Fill object
		refactoredIssue.setCommentServiceID(comment.getCommentID().toString());
		refactoredIssue.setRepoName(gitConfig.getRepoName());
		refactoredIssue.setRepoOwner(gitConfig.getRepoOwner());
		refactoredIssue.setRepoService(gitConfig.getRepoService());
		refactoredIssue.setDateOfRefactoring(date);

		if (gitConfig.getAnalysisService() != null) {
			refactoredIssue.setAnalysisService(gitConfig.getAnalysisService());
		}

		if (gitConfig.getAnalysisServiceProjectKey() != null) {
			refactoredIssue.setAnalysisServiceProjectKey(gitConfig.getAnalysisServiceProjectKey());
		}

		return refactoredIssue;
	}

	/**
	 * This method finds the Root-Folder of a repository. That is the folder that
	 * contains the src folder.
	 * 
	 * @param repoFolder
	 * @return rootFolder
	 * @throws IOException 
	 */
	public String findRootFolder(String repoFolder) throws Exception {
		// Get root folder of project
		File dir = new File(repoFolder);

		// Get paths to all java files of the project
		List<File> files = (List<File>) FileUtils.listFilesAndDirs(dir, TrueFileFilter.INSTANCE,
				TrueFileFilter.INSTANCE);
		for (File file : files) {
			if (file.isDirectory() && file.getName().equals("src")) {
				String srcPath = file.getAbsolutePath();
				return srcPath;
			}
		}
		
		// If no src-folder found
		throw new Exception("No src-folder found inside this java-project!");
	}
}

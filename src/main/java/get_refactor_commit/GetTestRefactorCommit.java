package get_refactor_commit;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static github_util.OpenRepository.openRepository;

public class GetTestRefactorCommit {
    static final String CSV_INPUT_FILE = "src/main/resources/input/all.csv";
    static final String CSV_OUTPUT_FILE = "src/main/resources/output/test_refactor_commits.csv";
    static final String CSV_ERROR_REPOSITORY_FILE = "src/main/resources/error/test_refactor_commits.csv";
    static final String CSV_ERROR_COMMIT_FILE = "src/main/resources/error/test_refactor_commits.csv";
    static List<String> repositoryErrorList = new ArrayList<>();
    static List<String> commitErrorList = new ArrayList<>();

    public static void main(String[] args) throws IOException, CsvException {
        writeHeaderCSV();

        FileReader csv = new FileReader(CSV_INPUT_FILE);
        CSVReader csvReader = new CSVReaderBuilder(csv).build();
        List<String[]> commitsInfo = csvReader.readAll();

        processRecords(commitsInfo);
        writeToErrorCSV(CSV_ERROR_COMMIT_FILE, commitErrorList);
        writeToErrorCSV(CSV_ERROR_REPOSITORY_FILE, repositoryErrorList);
    }

    public static void writeToErrorCSV(String filePath, List<String> data) {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String item : data) {
                writer.append(item);
                writer.append("\n"); // 改行を追加
            }
            System.out.println("CSVファイルに書き込みが完了しました。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void processRecords(List<String[]> records) {
        for (String[] record : records) {
            processRepositoryURL(record[0]);
            System.out.println("Processed repository: " + record[0]);
        }
    }

    public static void processRepositoryURL(String repositoryURL) {
        String repoDir = "repos/" + extractRepoName(repositoryURL);
        File inputDir = new File(repoDir);
        try {
            Repository repository = openRepository(repositoryURL+".git", inputDir);
            Git git = new Git(repository);
            processCommits(git, repoDir, repositoryURL);
        } catch (Exception e) {
            System.err.println("Error processing repository: " + e.getMessage());
            repositoryErrorList.add(repositoryURL);
        }
    }

    private static void processCommits(Git git, String repoDIr, String repositoryURL) {
        Repository repository = git.getRepository();
        try {
            Iterable<RevCommit> commits = git.log().call();
            for (RevCommit commit : commits) {
                processCommit(git, commit, repository, repoDIr, repositoryURL);
            }
        } catch (Exception e) {
            System.err.println("Error processing commits: " + e.getMessage());
            commitErrorList.add(repositoryURL);
        }
    }

    private static void processCommit(Git git, RevCommit commit, Repository repository, String repoDIr, String repositoryURL) {
        String repoName = extractRepoName(repositoryURL);
        String commitID = commit.getId().getName();
        String parentCommitID;
        String message = commit.getFullMessage();
        String commitDate = commit.getAuthorIdent().getWhen().toString();
        String author = commit.getAuthorIdent().getName();
        String commitURL = repositoryURL + "/commit/" + commitID;

        // If parentNum == 1 then this process continues
        if (checkParentCommitNumber(commit)){
            parentCommitID = commit.getParent(0).getName();
        } else {
            return;
        }

        List<String> changedFiles = getChangeFileList(git, commit, repository);

        if (checkChangeFile(changedFiles) && checkCommitMessage(message)){
            writeCommitToCSV(repoName, repositoryURL, commitID, parentCommitID, commitURL, message, commitDate, author, changedFiles);
        }
    }


    public static boolean checkCommitMessage(String message) {
        String lowerCaseMessage = message.toLowerCase();
        if (Pattern.compile("\\btest\\b").matcher(lowerCaseMessage).find() && Pattern.compile("\\brefactor\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        if (Pattern.compile("\\btest\\b").matcher(lowerCaseMessage).find() && Pattern.compile("\\brefactoring\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        if (Pattern.compile("\\btest\\b").matcher(lowerCaseMessage).find() && Pattern.compile("\\brefactored\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        if (Pattern.compile("\\btestrefactoring\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        if (Pattern.compile("\\btestrefactor\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        if (Pattern.compile("\\brefactoredtest\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        if (Pattern.compile("\\brefactortest\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        if (Pattern.compile("\\brefactoringtest\\b").matcher(lowerCaseMessage).find()){
            return true;
        }
        return false;
    }

    public static boolean checkChangeFile(List<String> changedFiles) {
        for (String file : changedFiles){
            if (file.endsWith("test.java") || file.endsWith("Test.java")){
                return true;
            }
        }
        return false;
    }

    private static boolean checkParentCommitNumber(RevCommit commit) {
        return commit.getParentCount() == 1;
    }

    private static void writeCommitToCSV(String repoName, String repositoryURL, String commitID, String parentCommitID, String commitURL, String message, String commitDate, String author, List<String> changedFiles) {
        message = "\"" + message.replaceAll("\"", "\"\"") + "\"";
        String files = "[" + String.join(" ", changedFiles) + "]";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CSV_OUTPUT_FILE, true))) {
            writer.write(String.join(",", Arrays.asList(
                    repoName,
                    repositoryURL,
                    commitID,
                    parentCommitID,
                    commitURL,
                    message,
                    commitDate,
                    author,
                    files
            )));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing commit to CSV file: " + e.getMessage());
        }
    }

    private static List<String> getChangeFileList(Git git, RevCommit commit, Repository repository) {
        List<String> changedFiles = new ArrayList<>();
        try {
            RevTree parentTree = commit.getParent(0).getTree();
            RevTree commitTree = commit.getTree();

            CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
            CanonicalTreeParser commitTreeParser = new CanonicalTreeParser();

            parentTreeParser.reset(repository.newObjectReader(), parentTree.getId());
            commitTreeParser.reset(repository.newObjectReader(), commitTree.getId());

            List<DiffEntry> diffs = git.diff()
                    .setNewTree(commitTreeParser)
                    .setOldTree(parentTreeParser)
                    .call();

            for (DiffEntry diff : diffs) {
                changedFiles.add(diff.getNewPath());
            }

        } catch (Exception e) {
            System.err.println("Error getting changed files: " + e.getMessage());
        }
        return changedFiles;
    }

    public static String extractRepoName(String repository) {
        String prefix = "https://github.com/";
        if (repository.startsWith(prefix)) {
            return repository.substring(prefix.length());
        }
        return "";
    }

    private static void writeHeaderCSV() {
        List<String> header = Arrays.asList(
                "repository_name",
                "repository_url",
                "commit_id",
                "parent_commit_id",
                "commit_url",
                "commit_message",
                "commit_date",
                "commit_author",
                "changed_files"
        );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(GetTestRefactorCommit.CSV_OUTPUT_FILE))) {
            writer.write(String.join(",", header));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing header to CSV file: " + e.getMessage());
        }
    }

}

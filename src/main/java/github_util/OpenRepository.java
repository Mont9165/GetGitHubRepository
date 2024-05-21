package github_util;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;


public class OpenRepository {


    public static Repository openRepository(String repositoryUrl, File inputFile) throws IOException, GitAPIException {
        try {
            return Git.open(inputFile).getRepository();
        } catch (Exception e) {
            FileUtils.deleteDirectory(inputFile);
            System.out.println("Clone Repository");
            cloneRepository(repositoryUrl, inputFile.toString());
            return Git.open(inputFile).getRepository();
        }
    }

    private static void cloneRepository(String repositoryUrl, String targetDirectory) throws IOException, GitAPIException {
        Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(Paths.get(targetDirectory).toFile())
                .call();
    }


}

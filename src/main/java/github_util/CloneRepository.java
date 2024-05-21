package github_util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Paths;

public class CloneRepository {
    String repositoryUrl;
    String targetDirectory;

    private void cloneRepository(String repositoryUrl, String targetDirectory) throws GitAPIException {
        Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(Paths.get(targetDirectory).toFile())
                .call();
    }
}

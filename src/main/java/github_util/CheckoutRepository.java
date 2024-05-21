package github_util;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

public class CheckoutRepository {
    Git git;
    String commitID;

    private void checkoutRepository(Git git, String commitID) throws GitAPIException {
        CheckoutCommand checkout = git.checkout();
        checkout.setName(commitID);
        checkout.call();
    }
}

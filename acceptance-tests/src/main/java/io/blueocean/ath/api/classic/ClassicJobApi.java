package io.blueocean.ath.api.classic;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import io.blueocean.ath.BaseUrl;
import io.blueocean.ath.GitRepositoryRule;
import io.blueocean.ath.model.Folder;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.mockito.AdditionalMatchers;
import org.mockito.internal.matchers.GreaterThan;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.support.ui.FluentWait;


import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Singleton
public class ClassicJobApi {

    private Logger logger = Logger.getLogger(ClassicJobApi.class);

    @Inject @BaseUrl
    String base;

    @Inject
    JenkinsServer jenkins;

    public void deletePipeline(String pipeline) throws IOException {
        deletePipeline(null, pipeline);
    }

    public void deletePipeline(FolderJob folder, String pipeline) throws IOException {
        try {
            jenkins.deleteJob(folder, pipeline);
            logger.info("Deleted pipeline " + pipeline);
        } catch(HttpResponseException e) {
            if(e.getStatusCode() != 404) {
                throw e;
            }
        }
    }
    public void createFreeStyleJob(String jobName, String command, String ...folders) throws IOException {
        deletePipeline(jobName);
        URL url = Resources.getResource(this.getClass(), "freestyle.xml");
        jenkins.createJob(null, jobName, Resources.toString(url, Charsets.UTF_8).replace("{{command}}", command));
        logger.info("Created freestyle job "+ jobName);
    }

    public void createMultlBranchPipeline(FolderJob folder, String pipelineName, String repositoryPath) throws IOException {
        deletePipeline(folder, pipelineName);
        URL url = Resources.getResource(this.getClass(), "multibranch.xml");
        jenkins.createJob(folder, pipelineName, Resources.toString(url, Charsets.UTF_8).replace("{{repo}}", repositoryPath));
        logger.info("Created multibranch pipeline: "+ pipelineName);
        jenkins.getJob(folder, pipelineName).build();
    }

    public FolderJob getFolder(Folder folder, boolean createMissing) throws IOException {
        if(folder == null || folder.getFolders().length == 0) {
            return null;
        }

        Job job = jenkins.getJob(folder.get(0));
        if(job == null && createMissing) {
            jenkins.createFolder(folder.get(0));
            job = jenkins.getJob(folder.get(0));
        }
        FolderJob ret = jenkins.getFolderJob(job).get();

        for (int i = 1; i < folder.getFolders().length; i++) {
            job = jenkins.getJob(ret, folder.get(i));
            if(job == null && createMissing) {
                jenkins.createFolder(ret, folder.get(i));
                job = jenkins.getJob(ret, folder.get(i));
            }
            ret = jenkins.getFolderJob(job).get();
        }
        return ret;
    }

    public void createMultlBranchPipeline(FolderJob folder, String pipelineName, GitRepositoryRule repository) throws IOException {
        createMultlBranchPipeline(folder, pipelineName, repository.gitDirectory.getAbsolutePath());
    }

    public void createFolders(Folder folder, boolean deleteRoot) throws IOException {
        if(deleteRoot) {
            jenkins.deleteJob(folder.get(0));
        }
        jenkins.createFolder(folder.get(0));
        FolderJob lastFolder = jenkins.getFolderJob(jenkins.getJob(folder.get(0))).get();

        for (int i = 1; i < folder.getFolders().length; i++) {
            lastFolder.createFolder(folder.get(0));
            lastFolder = jenkins.getFolderJob(lastFolder.getJob(folder.get(0))).get();
        };
    }

    public <T> T until(Function<JenkinsServer, T> function, long timeoutInMS) {
        return new FluentWait<JenkinsServer>(jenkins)
            .pollingEvery(500, TimeUnit.MILLISECONDS)
            .withTimeout(timeoutInMS, TimeUnit.MILLISECONDS)
            .ignoring(NotFoundException.class)
            .until((JenkinsServer server) -> function.apply(server));
    }
}

